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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.james.modules.protocols.ImapGuiceProbe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class JamesServerWithRetryConnectionTest {
    private static final long WAITING_TIME = TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS);

    private static DockerElasticSearchTestExtension esExtension = new DockerElasticSearchTestExtension();
    @RegisterExtension
    static CassandraJmapTestExtension testExtension = CassandraJmapTestExtension.Builder
        .withExtension(esExtension)
        .build();

    private SocketChannel socketChannel;
    private ExecutorService executorService;

    @BeforeEach
    public void setUp() throws IOException {
        executorService = Executors.newFixedThreadPool(1);
        socketChannel = SocketChannel.open();
    }

    @AfterEach
    public void after() throws IOException {
        socketChannel.close();
        executorService.shutdownNow();
    }

    @Test
    public void serverShouldStartAtDefault(GuiceJamesServer jamesServer) throws Exception {
        assertThatServerStartCorrectly(jamesServer);
    }

    @Test
    public void serverShouldRetryToConnectToCassandraWhenStartService(GuiceJamesServer jamesServer) throws Exception {
        DockerCassandraRule cassandra = testExtension.getCassandra();
        cassandra.pause();

        waitToStartContainer(WAITING_TIME, cassandra::unpause);

        assertThatServerStartCorrectly(jamesServer);
    }

    @Test
    public void serverShouldRetryToConnectToElasticSearchWhenStartService(GuiceJamesServer jamesServer) throws Exception {
        esExtension.pause();

        waitToStartContainer(WAITING_TIME, esExtension::unpause);

        assertThatServerStartCorrectly(jamesServer);
    }

    interface StartAction {
        void execute();
    }
    
    private void waitToStartContainer(long waitingTime, StartAction action) {
        executorService.submit(() -> {
            try {
                Thread.sleep(waitingTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            action.execute();
        });
    }

    private void assertThatServerStartCorrectly(GuiceJamesServer jamesServer) throws Exception {
        socketChannel.connect(new InetSocketAddress("127.0.0.1", jamesServer.getProbe(ImapGuiceProbe.class).getImapPort()));
        assertThat(getServerConnectionResponse(socketChannel)).startsWith("* OK JAMES IMAP4rev1 Server");
    }

    private String getServerConnectionResponse(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1000);
        socketChannel.read(byteBuffer);
        byte[] bytes = byteBuffer.array();
        return new String(bytes, Charset.forName("UTF-8"));
    }
}
