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
import java.util.concurrent.CompletionStage;

import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BlobStore;

import com.github.fge.lambdas.Throwing;
import com.google.common.annotations.VisibleForTesting;

public class JoiningBlobStore implements BlobStore {

    private final BlobStore primaryBlobStore;
    private final BlobStore secondaryBlobStore;

    @VisibleForTesting
    JoiningBlobStore(BlobStore primaryBlobStore, BlobStore secondaryBlobStore) {
        this.primaryBlobStore = primaryBlobStore;
        this.secondaryBlobStore = secondaryBlobStore;
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
        return primaryBlobStore.readBytes(blobId)
            .thenCompose(primaryResultInBytes -> readFromSecondaryIfNeeded(blobId, primaryResultInBytes));
    }

    @Override
    public InputStream read(BlobId blobId) {
        return Optional.ofNullable(primaryBlobStore.read(blobId))
            .filter(Throwing.<InputStream>predicate(inputStream -> inputStream.available() > 0).sneakyThrow())
            .orElseGet(() -> secondaryBlobStore.read(blobId));
    }

    private CompletionStage<byte[]> readFromSecondaryIfNeeded(BlobId blobId, byte[] primaryResultInBytes) {
        if (isNullOrEmpty(primaryResultInBytes)) {
            return secondaryBlobStore.readBytes(blobId);
        }
        return CompletableFuture.completedFuture(primaryResultInBytes);
    }

    private boolean isNullOrEmpty(byte [] bytes) {
        return bytes == null || bytes.length == 0;
    }
}
