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


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.cassandra.modules.CassandraMessageModule;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.datastax.driver.core.utils.UUIDs;
import com.github.steveash.guavate.Guavate;

public class CassandraImapUidDAOTest {

    private CassandraCluster cassandra;

    private CassandraImapUidDAO testee;

    @Before
    public void setUp() {
        cassandra = CassandraCluster.create(new CassandraMessageModule());
        cassandra.ensureAllTables();

        testee = new CassandraImapUidDAO(cassandra.getConf());
    }

    @After
    public void tearDown() {
        cassandra.clearAllTables();
    }

    @Test
    public void deleteShouldNotThrowWhenRowDoesntExist() {
        testee.delete(CassandraMessageId.of(UUIDs.timeBased()), CassandraId.timeBased())
            .join();
    }

    @Test
    public void deleteShouldDeleteWhenRowExists() {
        CassandraMessageId messageId = CassandraMessageId.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        testee.insert(messageId, mailboxId, messageUid).join();
        ComposedMessageId expectedComposedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        Stream<ComposedMessageId> insertedMessages = testee.retrieve(messageId, Optional.of(mailboxId));
        assertThat(insertedMessages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);

        testee.delete(messageId, mailboxId).join();

        Stream<ComposedMessageId> messages = testee.retrieve(messageId, Optional.of(mailboxId));
        assertThat(messages.collect(Guavate.toImmutableList())).isEmpty();
    }

    @Test
    public void insertShouldInsert() {
        CassandraMessageId messageId = CassandraMessageId.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        testee.insert(messageId, mailboxId, messageUid).join();

        ComposedMessageId expectedComposedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        Stream<ComposedMessageId> messages = testee.retrieve(messageId, Optional.of(mailboxId));
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void retrieveShouldRetrieveWhenKeyMatches() {
        CassandraMessageId messageId = CassandraMessageId.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        testee.insert(messageId, mailboxId, messageUid).join();

        ComposedMessageId expectedComposedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        Stream<ComposedMessageId> messages = testee.retrieve(messageId, Optional.of(mailboxId));

        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void retrieveShouldRetrieveMultipleWhenMessageIdMatches() {
        CassandraMessageId messageId = CassandraMessageId.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        CassandraId mailboxId2 = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        testee.insert(messageId, mailboxId, messageUid).join();
        testee.insert(messageId, mailboxId2, messageUid2).join();

        ComposedMessageId expectedComposedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        ComposedMessageId expectedComposedMessageId2 = new ComposedMessageId(mailboxId2, messageId, messageUid2);
        Stream<ComposedMessageId> messages = testee.retrieve(messageId, Optional.empty());

        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId, expectedComposedMessageId2);
    }
}
