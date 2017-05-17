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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.mailbox.FlagsBuilder;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.cassandra.modules.CassandraMessageModule;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.apache.james.mailbox.store.FlagsUpdateCalculator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.datastax.driver.core.utils.UUIDs;
import com.github.steveash.guavate.Guavate;

public class CassandraMessageIdToImapUidDAOTest {

    private CassandraCluster cassandra;
    private CassandraMessageId.Factory messageIdFactory;

    private CassandraMessageIdToImapUidDAO testee;

    @Before
    public void setUp() {
        cassandra = CassandraCluster.create(new CassandraMessageModule());
        cassandra.ensureAllTables();

        messageIdFactory = new CassandraMessageId.Factory();
        testee = new CassandraMessageIdToImapUidDAO(cassandra.getConf(), messageIdFactory);
    }

    @After
    public void tearDown() {
        cassandra.clearAllTables();
    }

    @Test
    public void deleteShouldNotThrowWhenRowDoesntExist() {
        testee.delete(messageIdFactory.of(UUIDs.timeBased()), CassandraId.timeBased())
            .join();
    }

    @Test
    public void deleteShouldDeleteWhenRowExists() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        testee.insert(ComposedMessageIdWithMetaData.builder()
                    .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                    .flags(new Flags())
                    .modSeq(1)
                    .build())
                .join();

        testee.delete(messageId, mailboxId).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).isEmpty();
    }

    @Test
    public void deleteShouldDeleteOnlyConcernedRowWhenMultipleRowExists() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        CassandraId mailboxId2 = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        CompletableFuture.allOf(
            testee.insert(ComposedMessageIdWithMetaData.builder()
                    .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                    .flags(new Flags())
                    .modSeq(1)
                    .build()),
            testee.insert(ComposedMessageIdWithMetaData.builder()
                    .composedMessageId(new ComposedMessageId(mailboxId2, messageId, messageUid2))
                    .flags(new Flags())
                    .modSeq(1)
                    .build()))
        .join();

        testee.delete(messageId, mailboxId).join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(new ComposedMessageId(mailboxId2, messageId, messageUid2))
                .flags(new Flags())
                .modSeq(1)
                .build();
        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.empty()).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void insertShouldWork() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        testee.insert(ComposedMessageIdWithMetaData.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .modSeq(1)
                .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .modSeq(1)
                .build();
        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void updateShouldUpdateModSeq() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags())
                .modSeq(1)
                .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags(Flag.ANSWERED))
                .modSeq(2)
                .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags(Flag.ANSWERED), MessageManager.FlagsUpdateMode.ADD);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }


    @Test
    public void retrieveShouldGuarantyModSeqMonotic() {
        int modSeq10 = 10;
        int modSeq2 = 2;

        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new Flags())
            .modSeq(modSeq10)
            .build())
            .join();

        ComposedMessageIdWithMetaData composedIdForUpdate = ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new Flags(Flag.ANSWERED))
            .modSeq(modSeq2)
            .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags(Flag.ANSWERED), MessageManager.FlagsUpdateMode.ADD);
        testee.updateMetadata(composedIdForUpdate, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new Flags(Flag.ANSWERED))
            .modSeq(modSeq10)
            .build());
    }

    @Test
    public void updateShouldUpdateAnsweredFlag() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags())
                .modSeq(1)
                .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags(Flag.ANSWERED))
                .modSeq(2)
                .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags(Flag.ANSWERED), MessageManager.FlagsUpdateMode.ADD);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void updateShouldWorkWithAddUpdateMode() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new Flags(Flag.DRAFT))
            .modSeq(1)
            .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new FlagsBuilder().add(Flag.DRAFT, Flag.ANSWERED).build())
            .modSeq(2)
            .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags(Flag.ANSWERED), MessageManager.FlagsUpdateMode.ADD);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void updateShouldWorkWithReplaceUpdateMode() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new Flags(Flag.DRAFT))
            .modSeq(1)
            .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new FlagsBuilder().add(Flag.ANSWERED).build())
            .modSeq(2)
            .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags(Flag.ANSWERED), MessageManager.FlagsUpdateMode.REPLACE);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void updateShouldWorkWithRemoveUpdateMode() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new Flags(Flag.DRAFT))
            .modSeq(1)
            .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new Flags())
            .modSeq(2)
            .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags(Flag.DRAFT), MessageManager.FlagsUpdateMode.REMOVE);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void updateShouldWorkWithAddUpdateModeAndUserFlags() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new Flags("flags1"))
            .modSeq(1)
            .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new FlagsBuilder().add("flags1", "flags2").build())
            .modSeq(2)
            .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags("flags2"), MessageManager.FlagsUpdateMode.ADD);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void updateShouldWorkWithReplaceUpdateModeAndUserFlags() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new Flags("flags1"))
            .modSeq(1)
            .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new Flags("flags2"))
            .modSeq(2)
            .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags("flags2"), MessageManager.FlagsUpdateMode.REPLACE);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void updateShouldWorkWithRemoveUpdateModeAndUserFlags() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new Flags("flags1"))
            .modSeq(1)
            .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
            .composedMessageId(composedMessageId)
            .flags(new Flags())
            .modSeq(2)
            .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags("flags1"), MessageManager.FlagsUpdateMode.REMOVE);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void updateShouldUpdateDeletedFlag() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags())
                .modSeq(1)
                .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags(Flag.DELETED))
                .modSeq(2)
                .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags(Flag.DELETED), MessageManager.FlagsUpdateMode.ADD);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void updateShouldUpdateDraftFlag() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags())
                .modSeq(1)
                .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags(Flag.DRAFT))
                .modSeq(2)
                .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags(Flag.DRAFT), MessageManager.FlagsUpdateMode.ADD);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void updateShouldUpdateFlaggedFlag() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags())
                .modSeq(1)
                .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags(Flag.FLAGGED))
                .modSeq(2)
                .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags(Flag.FLAGGED), MessageManager.FlagsUpdateMode.ADD);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void updateShouldUpdateRecentFlag() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags())
                .modSeq(1)
                .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags(Flag.RECENT))
                .modSeq(2)
                .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags(Flag.RECENT), MessageManager.FlagsUpdateMode.ADD);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void updateShouldUpdateSeenFlag() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags())
                .modSeq(1)
                .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags(Flag.SEEN))
                .modSeq(2)
                .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags(Flag.SEEN), MessageManager.FlagsUpdateMode.ADD);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void updateShouldUpdateUserFlag() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags())
                .modSeq(1)
                .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags(Flag.USER))
                .modSeq(2)
                .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(new Flags(Flag.USER), MessageManager.FlagsUpdateMode.ADD);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void updateShouldUpdateUserFlags() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee.insert(ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags())
                .modSeq(1)
                .build())
            .join();

        Flags flags = new Flags();
        flags.add("myCustomFlag");
        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(flags)
                .modSeq(2)
                .build();
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(flags, MessageManager.FlagsUpdateMode.ADD);
        testee.updateMetadata(expectedComposedMessageId, flagsUpdateCalculator).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void retrieveShouldReturnOneMessageWhenKeyMatches() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        testee.insert(ComposedMessageIdWithMetaData.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .modSeq(1)
                .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .modSeq(1)
                .build();
        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.of(mailboxId)).join();

        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId);
    }

    @Test
    public void retrieveShouldReturnMultipleMessagesWhenMessageIdMatches() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        CassandraId mailboxId2 = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        CompletableFuture.allOf(
                testee.insert(ComposedMessageIdWithMetaData.builder()
                    .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                    .flags(new Flags())
                    .modSeq(1)
                    .build()),
                testee.insert(ComposedMessageIdWithMetaData.builder()
                    .composedMessageId(new ComposedMessageId(mailboxId2, messageId, messageUid2))
                    .flags(new Flags())
                    .modSeq(1)
                    .build()))
        .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .modSeq(1)
                .build();
        ComposedMessageIdWithMetaData expectedComposedMessageId2 = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(new ComposedMessageId(mailboxId2, messageId, messageUid2))
                .flags(new Flags())
                .modSeq(1)
                .build();
        Stream<ComposedMessageIdWithMetaData> messages = testee.retrieve(messageId, Optional.empty()).join();

        assertThat(messages.collect(Guavate.toImmutableList())).containsOnly(expectedComposedMessageId, expectedComposedMessageId2);
    }
}
