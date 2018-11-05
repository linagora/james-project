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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.IOUtils;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.blob.api.BlobStoreContract;
import org.apache.james.blob.api.HashBlobId;
import org.apache.james.blob.memory.MemoryBlobStore;
import org.apache.james.util.CompletableFutureUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class JoiningBlobStoreTest implements BlobStoreContract {

    private static class ThrowingBlobStore implements BlobStore {

        @Override
        public CompletableFuture<BlobId> save(byte[] data) {
            if (Arrays.equals(data, BLOB_CONTENT_CAUSES_THROWING_DIRECTLY)) {
                throw new RuntimeException("not supported");
            }

            return CompletableFutureUtil.exceptionallyFuture(new RuntimeException("not supported"));
        }

        @Override
        public CompletableFuture<BlobId> save(InputStream data) {
            try {
                if (Arrays.equals(IOUtils.toByteArray(data), BLOB_CONTENT_CAUSES_THROWING_DIRECTLY)) {
                    throw new RuntimeException("not supported");
                }

                return CompletableFutureUtil.exceptionallyFuture(new RuntimeException("not supported"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public CompletableFuture<byte[]> readBytes(BlobId blobId) {
            return CompletableFutureUtil.exceptionallyFuture(new RuntimeException("not supported"));
        }

        @Override
        public InputStream read(BlobId blobId) {
            throw new RuntimeException("not supported");
        }
    }

    private static final HashBlobId.Factory BLOB_ID_FACTORY = new HashBlobId.Factory();
    private static final MemoryBlobStore PRIMARY_BLOB_STORE = new MemoryBlobStore(BLOB_ID_FACTORY);
    private static final MemoryBlobStore SECONDARY_BLOB_STORE = new MemoryBlobStore(BLOB_ID_FACTORY);
    private static final ThrowingBlobStore THROWING_BLOB_STORE = new ThrowingBlobStore();

    private static final byte [] BLOB_CONTENT = "blob content".getBytes();
    private static final byte [] BLOB_CONTENT_CAUSES_THROWING_DIRECTLY = "blob content causes throwing directly".getBytes();

    @AfterEach
    void tearDown() {
        PRIMARY_BLOB_STORE.clear();
        SECONDARY_BLOB_STORE.clear();
    }

    @Override
    public BlobStore testee() {
        return new JoiningBlobStore(PRIMARY_BLOB_STORE, SECONDARY_BLOB_STORE);
    }

    @Override
    public BlobId.Factory blobIdFactory() {
        return BLOB_ID_FACTORY;
    }

    @Test
    void readShouldReturnFromPrimaryWhenAvailable() throws Exception {
        BlobId blobId = PRIMARY_BLOB_STORE.save(BLOB_CONTENT).get();
        BlobStore joiningBlobStore = testee();

        assertThat(joiningBlobStore.read(blobId))
            .hasSameContentAs(new ByteArrayInputStream(BLOB_CONTENT));
    }

    @Test
    void readShouldReturnFallbackToSecondaryWhenNotAvailable() throws Exception {
        BlobId blobId = SECONDARY_BLOB_STORE.save(BLOB_CONTENT).get();
        BlobStore joiningBlobStore = testee();

        assertThat(joiningBlobStore.read(blobId))
            .hasSameContentAs(new ByteArrayInputStream(BLOB_CONTENT));
    }

    @Test
    void readBytesShouldReturnFromPrimaryWhenAvailable() throws Exception {
        BlobId blobId = PRIMARY_BLOB_STORE.save(BLOB_CONTENT).get();
        BlobStore joiningBlobStore = testee();

        assertThat(joiningBlobStore.readBytes(blobId).get())
            .isEqualTo(BLOB_CONTENT);
    }

    @Test
    void readBytesShouldReturnFallbackToSecondaryWhenNotAvailable() throws Exception {
        BlobId blobId = SECONDARY_BLOB_STORE.save(BLOB_CONTENT).get();
        BlobStore joiningBlobStore = testee();

        assertThat(joiningBlobStore.readBytes(blobId).get())
            .isEqualTo(BLOB_CONTENT);
    }

    @Test
    void saveShouldWriteToPrimary() throws Exception {
        BlobStore joiningBlobStore = testee();
        BlobId blobId = joiningBlobStore.save(BLOB_CONTENT).get();

        assertThat(PRIMARY_BLOB_STORE.readBytes(blobId).get())
            .isEqualTo(BLOB_CONTENT);
    }

    @Test
    void saveShouldNotWriteToSecondary() throws Exception {
        BlobStore joiningBlobStore = testee();
        BlobId blobId = joiningBlobStore.save(BLOB_CONTENT).get();

        assertThat(SECONDARY_BLOB_STORE.readBytes(blobId).get())
            .isEmpty();
    }

    @Test
    void saveShouldNotWriteToSecondaryEvenWhenPrimaryFails() throws Exception {
        JoiningBlobStore joiningBlobStore = new JoiningBlobStore(THROWING_BLOB_STORE, SECONDARY_BLOB_STORE);
        assertThatThrownBy(() -> joiningBlobStore.save(BLOB_CONTENT_CAUSES_THROWING_DIRECTLY))
            .isInstanceOf(RuntimeException.class);

        assertThat(SECONDARY_BLOB_STORE.size())
            .isEqualTo(0);
    }

    @Test
    void saveShouldNotWriteToSecondaryEvenWhenPrimaryCompleteExceptionally() throws Exception {
        JoiningBlobStore joiningBlobStore = new JoiningBlobStore(THROWING_BLOB_STORE, SECONDARY_BLOB_STORE);
        assertThat(joiningBlobStore.save(BLOB_CONTENT))
            .isCompletedExceptionally();

        assertThat(SECONDARY_BLOB_STORE.size())
            .isEqualTo(0);
    }

    @Test
    void saveInputStreamShouldWriteToPrimary() throws Exception {
        BlobStore joiningBlobStore = testee();
        BlobId blobId = joiningBlobStore.save(new ByteArrayInputStream(BLOB_CONTENT)).get();

        assertThat(PRIMARY_BLOB_STORE.readBytes(blobId).get())
            .isEqualTo(BLOB_CONTENT);
    }

    @Test
    void saveInputStreamShouldNotWriteToSecondary() throws Exception {
        BlobStore joiningBlobStore = testee();
        BlobId blobId = joiningBlobStore.save(new ByteArrayInputStream(BLOB_CONTENT)).get();

        assertThat(SECONDARY_BLOB_STORE.readBytes(blobId).get())
            .isEmpty();
    }

    @Test
    void saveInputStreamShouldNotWriteToSecondaryEvenWhenPrimaryFails() throws Exception {
        JoiningBlobStore joiningBlobStore = new JoiningBlobStore(THROWING_BLOB_STORE, SECONDARY_BLOB_STORE);
        assertThatThrownBy(() -> joiningBlobStore.save(new ByteArrayInputStream(BLOB_CONTENT_CAUSES_THROWING_DIRECTLY)))
            .isInstanceOf(RuntimeException.class);

        assertThat(SECONDARY_BLOB_STORE.size())
            .isEqualTo(0);
    }

    @Test
    void saveInputStreamShouldNotWriteToSecondaryEvenWhenPrimaryCompleteExceptionally() throws Exception {
        JoiningBlobStore joiningBlobStore = new JoiningBlobStore(THROWING_BLOB_STORE, SECONDARY_BLOB_STORE);
        assertThat(joiningBlobStore.save(new ByteArrayInputStream(BLOB_CONTENT)))
            .isCompletedExceptionally();

        assertThat(SECONDARY_BLOB_STORE.size())
            .isEqualTo(0);
    }
}