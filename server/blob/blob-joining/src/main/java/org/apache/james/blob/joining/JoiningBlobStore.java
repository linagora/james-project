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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.google.common.annotations.VisibleForTesting;

public class JoiningBlobStore implements BlobStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(JoiningBlobStore.class);

    private static class JoiningBlobStoreFallBack implements BlobStore {

        private final BlobStore primaryBlobStore;
        private final BlobStore secondaryBlobStore;

        JoiningBlobStoreFallBack(BlobStore primaryBlobStore, BlobStore secondaryBlobStore) {
            this.primaryBlobStore = primaryBlobStore;
            this.secondaryBlobStore = secondaryBlobStore;
        }

        @Override
        public CompletableFuture<BlobId> save(byte[] data) {
            return this.primaryBlobStore.save(data)
                .exceptionally(throwable -> {
                    LOGGER.error("primary complete save bytes exceptionally, fall back to second blob store", throwable);
                    return this.secondaryBlobStore.save(data).join();
                });
        }

        @Override
        public CompletableFuture<BlobId> save(InputStream data) {
            return this.primaryBlobStore.save(data)
                .exceptionally(throwable -> {
                    LOGGER.error("primary complete save InputStream exceptionally, fall back to second blob store", throwable);
                    return this.secondaryBlobStore.save(data).join();
                });
        }

        @Override
        public CompletableFuture<byte[]> readBytes(BlobId blobId) {
            return this.primaryBlobStore.readBytes(blobId)
                .exceptionally(throwable -> {
                    LOGGER.error("primary complete readBytes exceptionally, fall back to second blob store", throwable);
                    return this.secondaryBlobStore.readBytes(blobId).join();
                })
                .thenCompose(primaryResultInBytes -> readFromSecondaryIfNeeded(blobId, primaryResultInBytes));
        }

        @Override
        public InputStream read(BlobId blobId) {
            throw new UnsupportedOperationException("handles only futures");
        }

        private CompletionStage<byte[]> readFromSecondaryIfNeeded(BlobId blobId, byte[] primaryResultInBytes) {
            if (isNullOrEmpty(primaryResultInBytes)) {
                return this.secondaryBlobStore.readBytes(blobId);
            }
            return CompletableFuture.completedFuture(primaryResultInBytes);
        }

        private boolean isNullOrEmpty(byte [] bytes) {
            return Optional.ofNullable(bytes)
                .map(bytesToRead -> bytesToRead.length == 0)
                .orElse(true);
        }
    }

    private final JoiningBlobStoreFallBack fallBackBlobStore;
    private final BlobStore primaryBlobStore;
    private final BlobStore secondaryBlobStore;

    @VisibleForTesting
    JoiningBlobStore(BlobStore primaryBlobStore, BlobStore secondaryBlobStore) {
        this.primaryBlobStore = primaryBlobStore;
        this.secondaryBlobStore = secondaryBlobStore;
        this.fallBackBlobStore = new JoiningBlobStoreFallBack(primaryBlobStore, secondaryBlobStore);
    }

    @Override
    public CompletableFuture<BlobId> save(byte[] data) {
        try {
            return fallBackBlobStore.save(data);
        } catch (Exception e) {
            LOGGER.error("exception directly happens while saving bytes data, fall back to secondary blob store", e);
            return secondaryBlobStore.save(data);
        }
    }

    @Override
    public CompletableFuture<BlobId> save(InputStream data) {
        try {
            return fallBackBlobStore.save(data);
        } catch (Exception e) {
            LOGGER.error("exception directly happens while saving InputStream data, fall back to secondary blob store", e);
            return secondaryBlobStore.save(data);
        }
    }

    @Override
    public CompletableFuture<byte[]> readBytes(BlobId blobId) {
        try {
            return fallBackBlobStore.readBytes(blobId);
        } catch (Exception e) {
            LOGGER.error("exception directly happens while readBytes, fall back to secondary blob store", e);
            return secondaryBlobStore.readBytes(blobId);
        }
    }

    @Override
    public InputStream read(BlobId blobId) {
        try {
            return Optional.ofNullable(primaryBlobStore.read(blobId))
                .filter(Throwing.<InputStream>predicate(inputStream -> inputStream.available() > 0).sneakyThrow())
                .orElseGet(() -> secondaryBlobStore.read(blobId));
        } catch (Exception e) {
            LOGGER.error("exception directly happens while read, fall back to secondary blob store", e);
            return secondaryBlobStore.read(blobId);
        }
    }

    @VisibleForTesting
    BlobStore getPrimaryBlobStore() {
        return primaryBlobStore;
    }

    @VisibleForTesting
    BlobStore getSecondaryBlobStore() {
        return secondaryBlobStore;
    }
}
