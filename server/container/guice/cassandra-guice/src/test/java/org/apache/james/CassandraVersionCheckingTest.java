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
package org.apache.james;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionDAO;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionManager;
import org.apache.james.backends.cassandra.versions.SchemaVersion;
import org.apache.james.modules.protocols.ImapGuiceProbe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CassandraVersionCheckingTest {

    private static final String LOCAL_HOST = "127.0.0.1";
    private static final SchemaVersion MIN_VERSION = new SchemaVersion(2);
    private static final SchemaVersion MAX_VERSION = new SchemaVersion(4);

    @RegisterExtension
    static CassandraJmapTestExtension testExtension = CassandraJmapTestExtension.Builder
        .withDefaultModules()
        .ignoreEach()
        .build();

    private GuiceJamesServer jamesServer;
    private SocketChannel socketChannel;
    private CassandraSchemaVersionDAO versionDAO;

    @BeforeEach
    public void setUp() throws IOException {
        socketChannel = SocketChannel.open();
        versionDAO = mock(CassandraSchemaVersionDAO.class);
    }

    @AfterEach
    public void tearDown() throws IOException {
        socketChannel.close();
        if (jamesServer != null) {
            jamesServer.stop();
        }
    }

    @Test
    public void serverShouldStartSuccessfullyWhenMaxVersion() throws Exception {
        when(versionDAO.getCurrentSchemaVersion())
            .thenReturn(CompletableFuture.completedFuture(Optional.of(MAX_VERSION)));

        jamesServer = testExtension.createJmapServer(
            binder -> binder.bind(CassandraSchemaVersionDAO.class)
                .toInstance(versionDAO),
            binder -> binder.bind(CassandraSchemaVersionManager.class)
                .toInstance(new CassandraSchemaVersionManager(versionDAO, MIN_VERSION, MAX_VERSION)));

        assertThatServerStartCorrectly();
    }

    @Test
    public void serverShouldStartSuccessfullyWhenBetweenMinAndMaxVersion() throws Exception {
        when(versionDAO.getCurrentSchemaVersion())
            .thenReturn(CompletableFuture.completedFuture(Optional.of(MIN_VERSION.next())));

        jamesServer = testExtension.createJmapServer(
            binder -> binder.bind(CassandraSchemaVersionDAO.class)
                .toInstance(versionDAO),
            binder -> binder.bind(CassandraSchemaVersionManager.class)
                .toInstance(new CassandraSchemaVersionManager(versionDAO, MIN_VERSION, MAX_VERSION)));

        assertThatServerStartCorrectly();
    }

    @Test
    public void serverShouldStartSuccessfullyWhenMinVersion() throws Exception {
        when(versionDAO.getCurrentSchemaVersion())
            .thenReturn(CompletableFuture.completedFuture(Optional.of(MIN_VERSION)));

        jamesServer = testExtension.createJmapServer(
            binder -> binder.bind(CassandraSchemaVersionDAO.class)
                .toInstance(versionDAO),
            binder -> binder.bind(CassandraSchemaVersionManager.class)
                .toInstance(new CassandraSchemaVersionManager(versionDAO, MIN_VERSION, MAX_VERSION)));

        assertThatServerStartCorrectly();
    }

    @Test
    public void serverShouldNotStartWhenUnderMinVersion() throws Exception {
        when(versionDAO.getCurrentSchemaVersion())
            .thenReturn(CompletableFuture.completedFuture(Optional.of(MIN_VERSION.previous())));

        jamesServer = testExtension.createJmapServer(
            binder -> binder.bind(CassandraSchemaVersionDAO.class)
                .toInstance(versionDAO),
            binder -> binder.bind(CassandraSchemaVersionManager.class)
                .toInstance(new CassandraSchemaVersionManager(versionDAO, MIN_VERSION, MAX_VERSION)));

        assertThatThrownBy(()  -> jamesServer.start())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void serverShouldNotStartWhenAboveMaxVersion() throws Exception {
        when(versionDAO.getCurrentSchemaVersion())
            .thenReturn(CompletableFuture.completedFuture(Optional.of(MAX_VERSION.next())));

        jamesServer = testExtension.createJmapServer(
            binder -> binder.bind(CassandraSchemaVersionDAO.class)
                .toInstance(versionDAO),
            binder -> binder.bind(CassandraSchemaVersionManager.class)
                .toInstance(new CassandraSchemaVersionManager(versionDAO, MIN_VERSION, MAX_VERSION)));

        assertThatThrownBy(()  -> jamesServer.start())
            .isInstanceOf(IllegalStateException.class);
    }

    private void assertThatServerStartCorrectly() throws Exception {
        jamesServer.start();
        socketChannel.connect(new InetSocketAddress(LOCAL_HOST, jamesServer.getProbe(ImapGuiceProbe.class).getImapPort()));
        assertThat(getServerConnectionResponse(socketChannel))
            .startsWith("* OK JAMES IMAP4rev1 Server");
    }

    private String getServerConnectionResponse(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1000);
        socketChannel.read(byteBuffer);
        byte[] bytes = byteBuffer.array();

        return new String(bytes, Charset.forName("UTF-8"));
    }

}
