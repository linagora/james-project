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

package org.apache.james.blob.joining;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.IOUtils;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.blob.cassandra.CassandraBlobsDAO;
import org.apache.james.blob.objectstorage.ObjectStorageBlobsDAO;

import com.github.fge.lambdas.Throwing;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

public class JoiningBlobStore implements BlobStore {

    private final BlobStore primaryBlobStore;
    private final BlobStore secondaryBlobStore;

    @VisibleForTesting
    @Inject
    JoiningBlobStore(ObjectStorageBlobsDAO swiftBlobStore, CassandraBlobsDAO cassandraBlobStore) {
        this.primaryBlobStore = swiftBlobStore;
        this.secondaryBlobStore = cassandraBlobStore;
    }

    @VisibleForTesting
    JoiningBlobStore(BlobStore primaryBlobStore, CassandraBlobsDAO cassandraBlobStore) {
        this.primaryBlobStore = primaryBlobStore;
        this.secondaryBlobStore = cassandraBlobStore;
    }

    @Override
    public CompletableFuture<BlobId> save(byte[] data) {
        return primaryBlobStore.save(data);
    }

    @Override
    public CompletableFuture<BlobId> save(InputStream data) {
        return primaryBlobStore.save(data);
    }

    @Override
    public CompletableFuture<byte[]> readBytes(BlobId blobId) {
        return CompletableFuture
            .supplyAsync(Throwing.supplier(() -> IOUtils.toByteArray(read(blobId))).sneakyThrow());
    }

    @Override
    public InputStream read(BlobId blobId) {
        return Optional.ofNullable(primaryBlobStore.read(blobId))
            .filter(Throwing.<InputStream>predicate(inputStream -> inputStream.available() > 0).sneakyThrow())
            .orElseGet(() -> secondaryBlobStore.read(blobId));
    }
}
