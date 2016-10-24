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

import java.util.List;
import java.util.Optional;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.cassandra.modules.CassandraMessageModule;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.datastax.driver.core.utils.UUIDs;

public class CassandraMessageIdDAOTest {

    private CassandraCluster cassandra;

    private CassandraMessageIdDAO testee;

    @Before
    public void setUp() {
        cassandra = CassandraCluster.create(new CassandraMessageModule());
        cassandra.ensureAllTables();

        testee = new CassandraMessageIdDAO(cassandra.getConf());
    }

    @After
    public void tearDown() {
        cassandra.clearAllTables();
    }

    @Test
    public void deleteShouldNotThrowWhenRowDoesntExist() {
        testee.delete(CassandraId.timeBased(), MessageUid.of(1))
            .join();
    }

    @Test
    public void deleteShouldDeleteWhenRowExists() {
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        CassandraMessageId messageId = CassandraMessageId.of(UUIDs.timeBased());
        testee.insert(mailboxId, messageUid, messageId).join();
        Optional<CassandraMessageId> insertedMessages = testee.retrieve(mailboxId, messageUid).join();
        assertThat(insertedMessages.get()).isEqualTo(messageId);

        testee.delete(mailboxId, messageUid).join();

        Optional<CassandraMessageId> message = testee.retrieve(mailboxId, messageUid).join();
        assertThat(message.isPresent()).isFalse();
    }

    @Test
    public void insertShouldInsert() {
        CassandraMessageId messageId = CassandraMessageId.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        testee.insert(mailboxId, messageUid, messageId).join();

        Optional<CassandraMessageId> message = testee.retrieve(mailboxId, messageUid).join();
        assertThat(message.get()).isEqualTo(messageId);
    }

    @Test
    public void retrieveShouldRetrieveWhenKeyMatches() {
        CassandraMessageId messageId = CassandraMessageId.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        testee.insert(mailboxId, messageUid, messageId).join();

        Optional<CassandraMessageId> message = testee.retrieve(mailboxId, messageUid).join();

        assertThat(message.get()).isEqualTo(messageId);
    }

    @Test
    public void retrieveMessageIdsShouldRetrieveAllWhenRangeAll() {
        CassandraMessageId messageId = CassandraMessageId.of(UUIDs.timeBased());
        CassandraMessageId messageId2 = CassandraMessageId.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        testee.insert(mailboxId, messageUid, messageId).join();
        testee.insert(mailboxId, messageUid2, messageId2).join();

        List<CassandraMessageId> messages = testee.retrieveMessageIds(mailboxId, MessageRange.all(), FetchType.Full);

        assertThat(messages).containsOnly(messageId, messageId2);
    }

    @Test
    public void retrieveMessageIdsShouldRetrieveSomeWhenRangeFrom() {
        CassandraMessageId messageId = CassandraMessageId.of(UUIDs.timeBased());
        CassandraMessageId messageId2 = CassandraMessageId.of(UUIDs.timeBased());
        CassandraMessageId messageId3 = CassandraMessageId.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        MessageUid messageUid3 = MessageUid.of(3);
        testee.insert(mailboxId, messageUid, messageId).join();
        testee.insert(mailboxId, messageUid2, messageId2).join();
        testee.insert(mailboxId, messageUid3, messageId3).join();

        List<CassandraMessageId> messages = testee.retrieveMessageIds(mailboxId, MessageRange.from(messageUid2), FetchType.Full);

        assertThat(messages).containsOnly(messageId2, messageId3);
    }

    @Test
    public void retrieveMessageIdsShouldRetrieveSomeWhenRange() {
        CassandraMessageId messageId = CassandraMessageId.of(UUIDs.timeBased());
        CassandraMessageId messageId2 = CassandraMessageId.of(UUIDs.timeBased());
        CassandraMessageId messageId3 = CassandraMessageId.of(UUIDs.timeBased());
        CassandraMessageId messageId4 = CassandraMessageId.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        MessageUid messageUid3 = MessageUid.of(3);
        MessageUid messageUid4 = MessageUid.of(4);
        testee.insert(mailboxId, messageUid, messageId).join();
        testee.insert(mailboxId, messageUid2, messageId2).join();
        testee.insert(mailboxId, messageUid3, messageId3).join();
        testee.insert(mailboxId, messageUid4, messageId4).join();

        List<CassandraMessageId> messages = testee.retrieveMessageIds(mailboxId, MessageRange.range(messageUid2, messageUid3), FetchType.Full);

        assertThat(messages).containsOnly(messageId2, messageId3);
    }

    @Test
    public void retrieveMessageIdsShouldRetrieveOneWhenRangeOne() {
        CassandraMessageId messageId = CassandraMessageId.of(UUIDs.timeBased());
        CassandraMessageId messageId2 = CassandraMessageId.of(UUIDs.timeBased());
        CassandraMessageId messageId3 = CassandraMessageId.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        MessageUid messageUid3 = MessageUid.of(3);
        testee.insert(mailboxId, messageUid, messageId).join();
        testee.insert(mailboxId, messageUid2, messageId2).join();
        testee.insert(mailboxId, messageUid3, messageId3).join();

        List<CassandraMessageId> messages = testee.retrieveMessageIds(mailboxId, MessageRange.one(messageUid2), FetchType.Full);

        assertThat(messages).containsOnly(messageId2);
    }
}
