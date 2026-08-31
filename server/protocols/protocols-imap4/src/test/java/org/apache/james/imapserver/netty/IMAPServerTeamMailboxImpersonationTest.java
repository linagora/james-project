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

package org.apache.james.imapserver.netty;

import static org.apache.james.jmap.JMAPTestingConstants.LOCALHOST_IP;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.james.core.Username;
import org.apache.james.imap.processor.fetch.FetchProcessor;
import org.apache.james.mailbox.Authorizator;
import org.apache.james.mailbox.inmemory.manager.InMemoryIntegrationResources;
import org.apache.james.mailbox.store.FakeAuthenticator;
import org.apache.james.mailbox.store.FakeAuthorizator;
import org.apache.james.protocols.lib.mock.ConfigLoader;
import org.apache.james.util.ClassLoaderUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("checkstyle:membername")
class IMAPServerTeamMailboxImpersonationTest extends AbstractIMAPServerTest {
    private IMAPServer imapServer;
    private int port;
    private SocketChannel clientConnection;

    @AfterEach
    void tearDown() throws Exception {
        if (clientConnection != null) {
            clientConnection.close();
        }
        if (imapServer != null) {
            imapServer.destroy();
        }
    }

    private InMemoryIntegrationResources resourcesWithAuthorizator(Authorizator authorizator) {
        FakeAuthenticator auth = new FakeAuthenticator();
        auth.addUser(USER, USER_PASS);
        auth.addUser(USER2, USER_PASS);
        auth.addUser(USER3, USER_PASS);
        authenticator = auth;
        return InMemoryIntegrationResources.builder()
            .authenticator(auth)
            .authorizator(authorizator)
            .inVmEventBus()
            .defaultAnnotationLimits()
            .defaultMessageParser()
            .scanningSearchIndex()
            .noPreDeletionHooks()
            .storeQuotaManager()
            .build();
    }

    private void startServer(String configFile, Authorizator authorizator) throws Exception {
        HierarchicalConfiguration<ImmutableNode> config = ConfigLoader.getConfig(
            ClassLoaderUtils.getSystemResourceAsSharedStream(configFile));
        imapServer = createImapServer(config, resourcesWithAuthorizator(authorizator),
            FetchProcessor.LocalCacheConfiguration.DEFAULT);
        port = imapServer.getListenAddresses().get(0).getPort();
    }

    private String authenticatePlain(Username authzid, Username authcid, String password) throws Exception {
        clientConnection = SocketChannel.open();
        clientConnection.connect(new InetSocketAddress(LOCALHOST_IP, port));
        readBytes(clientConnection);

        clientConnection.write(ByteBuffer.wrap("a0 AUTHENTICATE PLAIN\r\n".getBytes(StandardCharsets.UTF_8)));
        readStringUntil(clientConnection, s -> s.startsWith("+"));

        String credentials = Base64.getEncoder().encodeToString(
            (authzid.asString() + "\0" + authcid.asString() + "\0" + password)
                .getBytes(StandardCharsets.US_ASCII));
        clientConnection.write(ByteBuffer.wrap((credentials + "\r\n").getBytes(StandardCharsets.US_ASCII)));

        return readStringUntil(clientConnection, s -> s.startsWith("a0")).getLast();
    }

    @Test
    void adminImpersonationShouldGrantSetAclRight() throws Exception {
        startServer("imapServerAdminUsers.xml", FakeAuthorizator.defaultReject());

        String authReply = authenticatePlain(USER2, USER, USER_PASS);
        assertThat(authReply).startsWith("a0 OK");

        clientConnection.write(ByteBuffer.wrap("a1 SETACL INBOX anyone r\r\n".getBytes(StandardCharsets.UTF_8)));
        String setaclReply = readStringUntil(clientConnection, s -> s.startsWith("a1")).getLast();
        assertThat(setaclReply).startsWith("a1 OK");
    }

    @Test
    void delegatedRegularUserShouldGrantSetAclRight() throws Exception {
        startServer("imapServer.xml", FakeAuthorizator.forGivenUserAndDelegatedUser(USER3, USER2));

        String authReply = authenticatePlain(USER2, USER3, USER_PASS);
        assertThat(authReply).startsWith("a0 OK");

        clientConnection.write(ByteBuffer.wrap("a1 SETACL INBOX anyone r\r\n".getBytes(StandardCharsets.UTF_8)));
        String setaclReply = readStringUntil(clientConnection, s -> s.startsWith("a1")).getLast();
        assertThat(setaclReply).startsWith("a1 OK");
    }

    @Test
    void nonDelegatedRegularUserShouldNotAllowImpersonation() throws Exception {
        startServer("imapServer.xml", FakeAuthorizator.defaultReject());

        String authReply = authenticatePlain(USER2, USER3, USER_PASS);
        assertThat(authReply).contains("NO");
    }
}
