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

import static org.apache.james.CassandraJmapTestExtension.EMBEDDED_ES;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.EnumSet;

import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.apache.james.jmap.methods.GetMessageListMethod;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.modules.TestJMAPServerModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class JamesCapabilitiesServerTest {

    private GuiceJamesServer server;

    @RegisterExtension
    static CassandraJmapTestExtension testExtension = CassandraJmapTestExtension.builder()
        .modules(
            (binder) -> binder.bind(PersistenceAdapter.class).to(MemoryPersistenceAdapter.class),
            new TestJMAPServerModule(GetMessageListMethod.DEFAULT_MAXIMUM_LIMIT))
        .extensions(EMBEDDED_ES)
        .ignoreEach()
        .build();

    @AfterEach
    public void teardown() {
        server.stop();
    }
    
    private GuiceJamesServer createCassandraJamesServer(final MailboxManager mailboxManager) throws IOException {
        return testExtension.createJmapServer(
            binder -> binder.bind(MailboxManager.class).toInstance(mailboxManager));
    }
    
    @Test
    public void startShouldFailWhenNoMoveCapability() throws Exception {
        MailboxManager mailboxManager = mock(MailboxManager.class);
        when(mailboxManager.getSupportedMailboxCapabilities())
            .thenReturn(EnumSet.complementOf(EnumSet.of(MailboxManager.MailboxCapabilities.Move)));
        when(mailboxManager.getSupportedMessageCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.MessageCapabilities.class));
        when(mailboxManager.getSupportedSearchCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.SearchCapabilities.class));

        server = createCassandraJamesServer(mailboxManager);
        
        assertThatThrownBy(() -> server.start()).isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    public void startShouldFailWhenNoACLCapability() throws Exception {
        MailboxManager mailboxManager = mock(MailboxManager.class);
        when(mailboxManager.getSupportedMailboxCapabilities())
            .thenReturn(EnumSet.complementOf(EnumSet.of(MailboxManager.MailboxCapabilities.ACL)));
        when(mailboxManager.getSupportedMessageCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.MessageCapabilities.class));
        when(mailboxManager.getSupportedSearchCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.SearchCapabilities.class));

        server = createCassandraJamesServer(mailboxManager);
        
        assertThatThrownBy(() -> server.start()).isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    public void startShouldFailWhenNoAttachmentCapability() throws Exception {
        MailboxManager mailboxManager = mock(MailboxManager.class);
        when(mailboxManager.getSupportedMailboxCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.MailboxCapabilities.class));
        when(mailboxManager.getSupportedMessageCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.MessageCapabilities.class));
        when(mailboxManager.getSupportedSearchCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.SearchCapabilities.class));

        server = createCassandraJamesServer(mailboxManager);

        assertThatThrownBy(() -> server.start()).isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    public void startShouldFailWhenNoAttachmentSearchCapability() throws Exception {
        MailboxManager mailboxManager = mock(MailboxManager.class);
        when(mailboxManager.getSupportedMailboxCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.MailboxCapabilities.class));
        when(mailboxManager.getSupportedMessageCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.MessageCapabilities.class));
        when(mailboxManager.getSupportedSearchCapabilities())
            .thenReturn(EnumSet.complementOf(EnumSet.of(MailboxManager.SearchCapabilities.Attachment)));

        server = createCassandraJamesServer(mailboxManager);

        assertThatThrownBy(() -> server.start()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void startShouldFailWhenNoAttachmentFileNameSearchCapability() throws Exception {
        MailboxManager mailboxManager = mock(MailboxManager.class);
        when(mailboxManager.getSupportedMailboxCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.MailboxCapabilities.class));
        when(mailboxManager.getSupportedMessageCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.MessageCapabilities.class));
        when(mailboxManager.getSupportedSearchCapabilities())
            .thenReturn(EnumSet.complementOf(EnumSet.of(MailboxManager.SearchCapabilities.AttachmentFileName)));

        server = createCassandraJamesServer(mailboxManager);

        assertThatThrownBy(() -> server.start()).isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    public void startShouldFailWhenNoMultimailboxSearchCapability() throws Exception {
        MailboxManager mailboxManager = mock(MailboxManager.class);
        when(mailboxManager.getSupportedMailboxCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.MailboxCapabilities.class));
        when(mailboxManager.getSupportedMessageCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.MessageCapabilities.class));
        when(mailboxManager.getSupportedSearchCapabilities())
            .thenReturn(EnumSet.complementOf(EnumSet.of(MailboxManager.SearchCapabilities.MultimailboxSearch)));

        server = createCassandraJamesServer(mailboxManager);

        assertThatThrownBy(() -> server.start()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void startShouldFailWhenNoUniqueIDCapability() throws Exception {
        MailboxManager mailboxManager = mock(MailboxManager.class);
        when(mailboxManager.getSupportedMailboxCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.MailboxCapabilities.class));
        when(mailboxManager.getSupportedMessageCapabilities())
            .thenReturn(EnumSet.complementOf(EnumSet.of(MailboxManager.MessageCapabilities.UniqueID)));
        when(mailboxManager.getSupportedSearchCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.SearchCapabilities.class));

        server = createCassandraJamesServer(mailboxManager);

        assertThatThrownBy(() -> server.start()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void startShouldSucceedWhenRequiredCapabilities() throws Exception {
        MailboxManager mailboxManager = mock(MailboxManager.class);
        when(mailboxManager.hasCapability(MailboxManager.MailboxCapabilities.Move))
            .thenReturn(true);
        when(mailboxManager.hasCapability(MailboxManager.MailboxCapabilities.ACL))
            .thenReturn(true);
        when(mailboxManager.getSupportedMessageCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.MessageCapabilities.class));
        when(mailboxManager.getSupportedSearchCapabilities())
            .thenReturn(EnumSet.allOf(MailboxManager.SearchCapabilities.class));

        server = createCassandraJamesServer(mailboxManager);

        server.start();
    }

}
