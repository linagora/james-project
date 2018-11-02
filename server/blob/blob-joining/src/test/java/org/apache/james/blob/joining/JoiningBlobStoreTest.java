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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.backends.cassandra.init.configuration.CassandraConfiguration;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.blob.api.BlobStoreContract;
import org.apache.james.blob.api.HashBlobId;
import org.apache.james.blob.cassandra.CassandraBlobModule;
import org.apache.james.blob.cassandra.CassandraBlobsDAO;
import org.apache.james.blob.objectstorage.ContainerName;
import org.apache.james.blob.objectstorage.DockerSwift;
import org.apache.james.blob.objectstorage.DockerSwiftExtension;
import org.apache.james.blob.objectstorage.ObjectStorageBlobsDAO;
import org.apache.james.blob.objectstorage.ObjectStorageBlobsDAOBuilder;
import org.apache.james.blob.objectstorage.swift.Credentials;
import org.apache.james.blob.objectstorage.swift.Identity;
import org.apache.james.blob.objectstorage.swift.PassHeaderName;
import org.apache.james.blob.objectstorage.swift.SwiftTempAuthObjectStorage;
import org.apache.james.blob.objectstorage.swift.TenantName;
import org.apache.james.blob.objectstorage.swift.UserHeaderName;
import org.apache.james.blob.objectstorage.swift.UserName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class JoiningBlobStoreTest implements BlobStoreContract {

    private static class ThrowingBlobStore implements BlobStore {

        @Override
        public CompletableFuture<BlobId> save(byte[] data) {
            throw new RuntimeException("not supported");
        }

        @Override
        public CompletableFuture<BlobId> save(InputStream data) {
            throw new RuntimeException("not supported");
        }

        @Override
        public CompletableFuture<byte[]> readBytes(BlobId blobId) {
            throw new RuntimeException("not supported");
        }

        @Override
        public InputStream read(BlobId blobId) {
            throw new RuntimeException("not supported");
        }
    }

    private static final HashBlobId.Factory BLOB_ID_FACTORY = new HashBlobId.Factory();
    private static final ThrowingBlobStore THROWING_BLOB_STORE = new ThrowingBlobStore();

    private static final byte [] BLOB_CONTENT = "blob content".getBytes();
    private static final int CHUNK_SIZE = 10240;

    private static final TenantName TENANT_NAME = TenantName.of("test");
    private static final UserName USER_NAME = UserName.of("tester");
    private static final Credentials PASSWORD = Credentials.of("testing");
    private static final Identity SWIFT_IDENTITY = Identity.of(TENANT_NAME, USER_NAME);

    @RegisterExtension
    static CassandraClusterExtension cassandraExtension = new CassandraClusterExtension(CassandraBlobModule.MODULE);
    @RegisterExtension
    static DockerSwiftExtension swiftExtension = new DockerSwiftExtension();

    private CassandraBlobsDAO cassandraBlobStore;
    private ContainerName containerName;
    private SwiftTempAuthObjectStorage.Configuration testConfig;
    private org.jclouds.blobstore.BlobStore underlayerSwiftBlobStore;
    private ObjectStorageBlobsDAO swiftBlobStore;

    @BeforeEach
    void setUp(DockerSwift dockerSwift) {
        // Setting up cassandra blobstore
        cassandraBlobStore = new CassandraBlobsDAO(cassandraExtension.getCassandraCluster().getConf(),
            CassandraConfiguration.builder()
                .blobPartSize(CHUNK_SIZE)
                .build(),
            new HashBlobId.Factory());

        // Setting up swift blobstore
        containerName = ContainerName.of(UUID.randomUUID().toString());
        testConfig = SwiftTempAuthObjectStorage.configBuilder()
            .endpoint(dockerSwift.swiftEndpoint())
            .identity(SWIFT_IDENTITY)
            .credentials(PASSWORD)
            .tempAuthHeaderUserName(UserHeaderName.of("X-Storage-User"))
            .tempAuthHeaderPassName(PassHeaderName.of("X-Storage-Pass"))
            .build();
        BlobId.Factory blobIdFactory = blobIdFactory();
        ObjectStorageBlobsDAOBuilder daoBuilder = ObjectStorageBlobsDAO
            .builder(testConfig)
            .container(containerName)
            .blobIdFactory(blobIdFactory);
        underlayerSwiftBlobStore = daoBuilder.getSupplier().get();
        swiftBlobStore = daoBuilder.build();
        swiftBlobStore.createContainer(containerName);
    }

    @AfterEach
    void tearDown() {
        underlayerSwiftBlobStore.deleteContainer(containerName.value());
        underlayerSwiftBlobStore.getContext().close();
    }

    @Override
    public BlobStore testee() {
        return new JoiningBlobStore(swiftBlobStore, cassandraBlobStore);
    }

    @Override
    public BlobId.Factory blobIdFactory() {
        return BLOB_ID_FACTORY;
    }

    @Test
    void readShouldReturnFromPrimaryWhenAvailable() throws Exception {
        BlobId blobId = swiftBlobStore.save(BLOB_CONTENT).get();

        assertThat(testee().read(blobId))
            .hasSameContentAs(new ByteArrayInputStream(BLOB_CONTENT));
    }

    @Test
    void readShouldReturnFallbackToSecondaryWhenNotAvailable() throws Exception {
        BlobId blobId = cassandraBlobStore.save(BLOB_CONTENT).get();

        assertThat(testee().read(blobId))
            .hasSameContentAs(new ByteArrayInputStream(BLOB_CONTENT));
    }

    @Test
    void readBytesShouldReturnFromPrimaryWhenAvailable() throws Exception {
        BlobId blobId = swiftBlobStore.save(BLOB_CONTENT).get();

        assertThat(testee().readBytes(blobId).get())
            .isEqualTo(BLOB_CONTENT);
    }

    @Test
    void readBytesShouldReturnFallbackToSecondaryWhenNotAvailable() throws Exception {
        BlobId blobId = cassandraBlobStore.save(BLOB_CONTENT).get();

        assertThat(testee().readBytes(blobId).get())
            .isEqualTo(BLOB_CONTENT);
    }

    @Test
    void saveShouldWriteToPrimary() throws Exception {
        BlobId blobId = testee().save(BLOB_CONTENT).get();

        assertThat(swiftBlobStore.readBytes(blobId).get())
            .isEqualTo(BLOB_CONTENT);
    }

    @Test
    void saveShouldNotWriteToSecondary() throws Exception {
        BlobId blobId = testee().save(BLOB_CONTENT).get();

        assertThat(cassandraBlobStore.readBytes(blobId).get())
            .isEmpty();
    }

    @Test
    void saveShouldNotWriteToSecondaryEvenWhenPrimaryFails() throws Exception {
        CassandraBlobsDAO cassandraBlobStoreSpider = spy(cassandraBlobStore);
        JoiningBlobStore testee = new JoiningBlobStore(THROWING_BLOB_STORE, cassandraBlobStoreSpider);
        assertThatThrownBy(() -> testee.save(BLOB_CONTENT))
            .isInstanceOf(RuntimeException.class);

        verifyNoMoreInteractions(cassandraBlobStoreSpider);
    }

    @Test
    void saveInputStreamShouldWriteToPrimary() throws Exception {
        BlobId blobId = testee().save(new ByteArrayInputStream(BLOB_CONTENT)).get();

        assertThat(swiftBlobStore.readBytes(blobId).get())
            .isEqualTo(BLOB_CONTENT);
    }

    @Test
    void saveInputStreamShouldNotWriteToSecondary() throws Exception {
        BlobId blobId = testee().save(new ByteArrayInputStream(BLOB_CONTENT)).get();

        assertThat(cassandraBlobStore.readBytes(blobId).get())
            .isEmpty();
    }

    @Test
    void saveInputStreamShouldNotWriteToSecondaryEvenWhenPrimaryFails() throws Exception {
        CassandraBlobsDAO cassandraBlobStoreSpider = spy(cassandraBlobStore);
        JoiningBlobStore testee = new JoiningBlobStore(THROWING_BLOB_STORE, cassandraBlobStoreSpider);
        assertThatThrownBy(() -> testee.save(new ByteArrayInputStream(BLOB_CONTENT)))
            .isInstanceOf(RuntimeException.class);

        verifyNoMoreInteractions(cassandraBlobStoreSpider);
    }
}