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

package org.apache.james.mailbox.cassandra.mail;

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageV2Table.BlobParts;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageV2Table.Blobs;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.inject.Inject;

import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.mailbox.cassandra.BlobId;
import org.apache.james.mailbox.cassandra.PartId;
import org.apache.james.util.CompletableFutureUtil;
import org.apache.james.util.FluentFutureStream;
import org.apache.commons.lang3.tuple.Pair;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.utils.UUIDs;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Bytes;

public class CassandraBlobsDAO {

    public static final int CHUNK_SIZE = 1024;
    private final CassandraAsyncExecutor cassandraAsyncExecutor;
    private final PreparedStatement insert;
    private final PreparedStatement insertPart;
    private final PreparedStatement delete;
    private final PreparedStatement select;
    private final PreparedStatement selectPart;

    @Inject
    public CassandraBlobsDAO(Session session) {
        this.cassandraAsyncExecutor = new CassandraAsyncExecutor(session);
        this.insert = prepareInsert(session);
        this.delete = prepareDelete(session);
        this.select = prepareSelect(session);

        this.insertPart = prepareInsertPart(session);
        this.selectPart = prepareSelectPart(session);
    }

    private PreparedStatement prepareSelect(Session session) {
        return session.prepare(select()
            .from(Blobs.TABLE_NAME)
            .where(eq(Blobs.ID, bindMarker(Blobs.ID))));
    }

    private PreparedStatement prepareSelectPart(Session session) {
        return session.prepare(select()
                .from(BlobParts.TABLE_NAME)
                .where(eq(BlobParts.ID, bindMarker(BlobParts.ID))));
    }

    private PreparedStatement prepareInsert(Session session) {
        return session.prepare(insertInto(Blobs.TABLE_NAME)
                .value(Blobs.ID, bindMarker(Blobs.ID))
                .value(Blobs.POSITION, bindMarker(Blobs.POSITION))
                .value(Blobs.PART, bindMarker(Blobs.PART)));
    }

    private PreparedStatement prepareInsertPart(Session session) {
        return session.prepare(insertInto(BlobParts.TABLE_NAME)
                .value(BlobParts.ID, bindMarker(BlobParts.ID))
                .value(BlobParts.DATA, bindMarker(BlobParts.DATA)));
    }

    private PreparedStatement prepareDelete(Session session) {
        return session.prepare(QueryBuilder.delete()
                .from(Blobs.TABLE_NAME)
                .where(eq(Blobs.ID, bindMarker(Blobs.ID))));
    }

    public CompletableFuture<Optional<BlobId>> save(byte[] data) {
        if (data == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        BlobId blobId = BlobId.forPayload(data);
        return FluentFutureStream.of(
            saveBlobParts(data, blobId)
                .map(pair ->
                    cassandraAsyncExecutor.executeVoid(insert.bind()
                        .setString(Blobs.ID, blobId.getId())
                        .setLong(Blobs.POSITION, pair.getKey())
                        .setString(Blobs.PART, pair.getValue().getId()))))
            .completableFuture()
            .thenApply(any -> Optional.of(blobId));
    }

    public CompletableFuture<byte[]> read(BlobId blobId) {
        return readBlob(blobId)
            .thenApply(resultSet -> CassandraUtils.convertToStream(resultSet)
                .map(row -> Pair.of(row.getLong(Blobs.POSITION), PartId.from(row.getString(Blobs.PART))))
                .collect(Guavate.toImmutableMap(Pair::getKey, Pair::getValue)))
            .thenCompose(positionToIds -> CompletableFutureUtil.chainAll(
                positionToIds.values().stream(),
                this::readPart))
            .thenApply(this::readRows);
    }

    private byte[] readRows(Stream<Optional<Row>> rows) {
        ImmutableList<byte[]> parts = rows.filter(Optional::isPresent)
            .map(row -> rowToData(row.get()))
            .collect(Guavate.toImmutableList());

        return Bytes.concat(parts.toArray(new byte[parts.size()][]));
    }

    private byte[] rowToData(Row row) {
        byte[] data = new byte[row.getBytes(BlobParts.DATA).remaining()];
        row.getBytes(BlobParts.DATA).get(data);
        return data;
    }

    public CompletableFuture<ResultSet> readBlob(BlobId blobId) {
        return cassandraAsyncExecutor.execute(select.bind()
                .setString(Blobs.ID, blobId.getId()));
    }

    private CompletableFuture<Optional<Row>> readPart(PartId partId) {
        if (partId == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return cassandraAsyncExecutor.executeSingleRow(selectPart.bind()
                .setString(BlobParts.ID, partId.getId()));
    }

    private PartId writePart(ByteBuffer data, BlobId blobId, int position) {
        PartId partId = PartId.create(blobId, position);
        cassandraAsyncExecutor.executeVoid(insertPart.bind()
                .setString(BlobParts.ID, partId.getId())
                .setBytes(BlobParts.DATA, data));
        return partId;
    }

    private Stream<Pair<Integer, PartId>> saveBlobParts(byte[] data, BlobId blobId) {
        int size = data.length;
        int fullChunkCount = size / CHUNK_SIZE;

        return Stream.concat(
                IntStream.range(0, fullChunkCount)
                        .mapToObj(i -> Pair.of(i, writePart(getWrap(data, i * CHUNK_SIZE, CHUNK_SIZE), blobId, i))),
                saveFinalByteBuffer(data, fullChunkCount * CHUNK_SIZE, fullChunkCount, blobId));
    }

    private Stream<Pair<Integer, PartId>> saveFinalByteBuffer(byte[] data, int offset, int index, BlobId blobId) {
        if (offset == data.length) {
            return Stream.of();
        }
        return Stream.of(Pair.of(index, writePart(getWrap(data, offset, data.length - offset), blobId, index)));
    }

    private ByteBuffer getWrap(byte[] data, int offset, int length) {
        return ByteBuffer.wrap(data, offset, length);
    }
}
