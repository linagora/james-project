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

import javax.mail.Flags;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.cassandra.modules.CassandraMessageModule;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.ComposedMessageIdWithFlags;
import org.apache.james.mailbox.model.MessageRange;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CassandraMessageIdDAOTest {

    private CassandraCluster cassandra;

    private CassandraMessageId.Factory messageIdFactory;
    private CassandraMessageIdDAO testee;


    @Before
    public void setUp() {
        cassandra = CassandraCluster.create(new CassandraMessageModule());
        cassandra.ensureAllTables();

        messageIdFactory = new CassandraMessageId.Factory();
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
        CassandraMessageId messageId = messageIdFactory.generate();
        testee.insert(ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .build())
            .join();

        testee.delete(mailboxId, messageUid).join();

        Optional<ComposedMessageIdWithFlags> message = testee.retrieve(mailboxId, messageUid).join();
        assertThat(message.isPresent()).isFalse();
    }

    @Test
    public void deleteShouldDeleteOnlyConcernedRowWhenMultipleRowExists() {
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        CassandraMessageId messageId = messageIdFactory.generate();
        CassandraMessageId messageId2 = messageIdFactory.generate();
        testee.insert(ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .build())
            .join();

        testee.insert(ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId2, messageUid2))
                .flags(new Flags())
                .build())
            .join();

        testee.delete(mailboxId, messageUid).join();

        Optional<ComposedMessageIdWithFlags> message = testee.retrieve(mailboxId, messageUid).join();
        assertThat(message.isPresent()).isFalse();
        Optional<ComposedMessageIdWithFlags> messageNotDeleted = testee.retrieve(mailboxId, messageUid2).join();
        assertThat(messageNotDeleted.isPresent()).isTrue();
    }

    @Test
    public void insertShouldInsert() {
        CassandraMessageId messageId = messageIdFactory.generate();
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageIdWithFlags composedMessageIdWithFlags = ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .build();
        testee.insert(composedMessageIdWithFlags).join();

        Optional<ComposedMessageIdWithFlags> message = testee.retrieve(mailboxId, messageUid).join();
        assertThat(message.get()).isEqualTo(composedMessageIdWithFlags);
    }

    @Test
    public void retrieveShouldRetrieveWhenKeyMatches() {
        CassandraMessageId messageId = messageIdFactory.generate();
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        ComposedMessageIdWithFlags composedMessageIdWithFlags = ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .build();
        testee.insert(composedMessageIdWithFlags).join();

        Optional<ComposedMessageIdWithFlags> message = testee.retrieve(mailboxId, messageUid).join();

        assertThat(message.get()).isEqualTo(composedMessageIdWithFlags);
    }

    @Test
    public void retrieveMessagesShouldRetrieveAllWhenRangeAll() {
        CassandraMessageId messageId = messageIdFactory.generate();
        CassandraMessageId messageId2 = messageIdFactory.generate();
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        ComposedMessageIdWithFlags composedMessageIdWithFlags = ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .build();
        testee.insert(composedMessageIdWithFlags).join();

        ComposedMessageIdWithFlags composedMessageIdWithFlags2 = ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId2, messageUid2))
                .flags(new Flags())
                .build();
        testee.insert(composedMessageIdWithFlags2).join();

        List<ComposedMessageIdWithFlags> messages = testee.retrieveMessages(mailboxId, MessageRange.all());

        assertThat(messages).containsOnly(composedMessageIdWithFlags, composedMessageIdWithFlags2);
    }

    @Test
    public void retrieveMessagesShouldRetrieveSomeWhenRangeFrom() {
        CassandraMessageId messageId = messageIdFactory.generate();
        CassandraMessageId messageId2 = messageIdFactory.generate();
        CassandraMessageId messageId3 = messageIdFactory.generate();
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        MessageUid messageUid3 = MessageUid.of(3);
        testee.insert(ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .build())
            .join();

        ComposedMessageIdWithFlags composedMessageIdWithFlags = ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId2, messageUid2))
                .flags(new Flags())
                .build();
        testee.insert(composedMessageIdWithFlags).join();

        ComposedMessageIdWithFlags composedMessageIdWithFlags2 = ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId3, messageUid3))
                .flags(new Flags())
                .build();
        testee.insert(composedMessageIdWithFlags2).join();

        List<ComposedMessageIdWithFlags> messages = testee.retrieveMessages(mailboxId, MessageRange.from(messageUid2));

        assertThat(messages).containsOnly(composedMessageIdWithFlags, composedMessageIdWithFlags2);
    }

    @Test
    public void retrieveMessagesShouldRetrieveSomeWhenRange() {
        CassandraMessageId messageId = messageIdFactory.generate();
        CassandraMessageId messageId2 = messageIdFactory.generate();
        CassandraMessageId messageId3 = messageIdFactory.generate();
        CassandraMessageId messageId4 = messageIdFactory.generate();
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        MessageUid messageUid3 = MessageUid.of(3);
        MessageUid messageUid4 = MessageUid.of(4);
        testee.insert(ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .build())
            .join();

        ComposedMessageIdWithFlags composedMessageIdWithFlags = ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId2, messageUid2))
                .flags(new Flags())
                .build();
        testee.insert(composedMessageIdWithFlags).join();

        ComposedMessageIdWithFlags composedMessageIdWithFlags2 = ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId3, messageUid3))
                .flags(new Flags())
                .build();
        testee.insert(composedMessageIdWithFlags2).join();

        testee.insert(ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId4, messageUid4))
                .flags(new Flags())
                .build())
            .join();

        List<ComposedMessageIdWithFlags> messages = testee.retrieveMessages(mailboxId, MessageRange.range(messageUid2, messageUid3));

        assertThat(messages).containsOnly(composedMessageIdWithFlags, composedMessageIdWithFlags2);
    }

    @Test
    public void retrieveMessagesShouldRetrieveOneWhenRangeOne() {
        CassandraMessageId messageId = messageIdFactory.generate();
        CassandraMessageId messageId2 = messageIdFactory.generate();
        CassandraMessageId messageId3 = messageIdFactory.generate();
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        MessageUid messageUid3 = MessageUid.of(3);
        testee.insert(ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .build())
            .join();

        ComposedMessageIdWithFlags composedMessageIdWithFlags = ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId2, messageUid2))
                .flags(new Flags())
                .build();
        testee.insert(composedMessageIdWithFlags).join();

        testee.insert(ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId3, messageUid3))
                .flags(new Flags())
                .build())
            .join();

        List<ComposedMessageIdWithFlags> messages = testee.retrieveMessages(mailboxId, MessageRange.one(messageUid2));

        assertThat(messages).containsOnly(composedMessageIdWithFlags);
    }
}
