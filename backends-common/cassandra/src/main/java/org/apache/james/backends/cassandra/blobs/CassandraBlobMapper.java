/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.backends.cassandra.blobs;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.util.FluentFutureStream;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public class CassandraBlobMapper {


    public static final int CHUNK_SIZE = 1024;
    private final CassandraAsyncExecutor executor;

    public CassandraBlobMapper(Session session) {
        this.executor = new CassandraAsyncExecutor(session);
    }

    public CompletableFuture<UUID> write(byte[] data) {
        UUID uuid = UUIDs.timeBased();
        return FluentFutureStream.of(
            computeBlobParts(data).map(pair -> savePart(uuid, pair)))
            .completableFuture()
        .thenApply(any -> uuid);
    }

    private CompletableFuture<Void> savePart(UUID uuid, Pair<Integer, ByteBuffer> pair) {
        UUID partUuid = UUIDs.timeBased();
        return executor.executeVoid(
            insertInto(CassandraBlobModule.PART_TABLE_NAME)
                .value(CassandraBlobModule.PART, partUuid)
                .value(CassandraBlobModule.DATA, pair.getValue()))
            .thenCompose(any -> executor.executeVoid(
                insertInto(CassandraBlobModule.BLOB_TABLE_NAME)
                    .value(CassandraBlobModule.ID, uuid)
                    .value(CassandraBlobModule.POSITION, pair.getKey())
                    .value(CassandraBlobModule.PART, partUuid)));
    }

    private Stream<Pair<Integer, ByteBuffer>> computeBlobParts(byte[] data) {
        int size = data.length;
        int fullChunkCount = size / CHUNK_SIZE;

        return Stream.concat(
            IntStream.range(0, fullChunkCount)
                .mapToObj(i -> Pair.of(i, ByteBuffer.wrap(data, i * CHUNK_SIZE, CHUNK_SIZE))),
            computeFinalByteBuffer(data, fullChunkCount * CHUNK_SIZE, fullChunkCount));
    }

    private Stream<Pair<Integer, ByteBuffer>> computeFinalByteBuffer(byte[] data, int offset, int index) {
        if (offset == data.length) {
            return Stream.of();
        }
        return Stream.of(Pair.of(index, ByteBuffer.wrap(data, offset, data.length - offset)));
    }

    public InputStream read(UUID id) {
        ImmutableMap<Long, UUID> partIds = CassandraUtils.convertToStream(executor.execute(
            select().from(CassandraBlobModule.BLOB_TABLE_NAME)
                .where(eq(CassandraBlobModule.ID, id)))
            .join())
            .map(row -> Pair.of(row.getLong(CassandraBlobModule.POSITION), row.getUUID(CassandraBlobModule.PART)))
            .collect(Guavate.toImmutableMap(Pair::getKey, Pair::getValue));
        return new SequenceInputStream(
                new CassandraBackedInputStreamEnumeration(i -> readPart(partIds.get(i))));
    }

    public CompletableFuture<Optional<Row>> readPart(UUID partId) {
        if (partId == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return executor.executeSingleRow(select().from(CassandraBlobModule.PART_TABLE_NAME)
            .where(eq(CassandraBlobModule.PART, partId)));
    }

    public static class CassandraBackedInputStreamEnumeration implements Enumeration<InputStream> {

        private final Function<Long, CompletableFuture<Optional<Row>>> partFetcher;
        private long nextPartOffset;
        private CompletableFuture<Optional<Row>> preFetch;
        private Optional<Row> nextRow;

        public CassandraBackedInputStreamEnumeration(Function<Long, CompletableFuture<Optional<Row>>> partFetcher) {
            this.partFetcher = partFetcher;
            this.nextPartOffset = 0;
            this.nextRow = Optional.empty();
            fetch();
        }

        private void fetch() {
            preFetch = partFetcher.apply(nextPartOffset);
            nextPartOffset++;
        }

        @Override
        public boolean hasMoreElements() {
            CompletableFuture<Optional<Row>> nextRowFuture = preFetch;
            fetch();
            nextRow = nextRowFuture.join();
            return nextRow.isPresent();
        }

        @Override
        public InputStream nextElement() {
            Preconditions.checkState(nextRow.isPresent(), "Expecting content while calling nextElement. Have you called 'hasMoreElements' before?");
            return new ByteArrayInputStream(getRowContent(nextRow.get()));
        }

        private byte[] getRowContent(Row row) {
            byte[] data = new byte[row.getBytes(CassandraBlobModule.DATA).remaining()];
            row.getBytes(CassandraBlobModule.DATA).get(data);
            return data;
        }
    }
}
