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

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.cassandra.ids.CassandraMessageId;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.junit.jupiter.api.Test;

import com.datastax.driver.core.utils.UUIDs;

public interface CassandraMessageIdToImapUidDAOContract {
    CassandraMessageId.Factory messageIdFactory = new CassandraMessageId.Factory();

    CassandraMessageIdToImapUidDAO testee();

    @Test
    default void deleteShouldNotThrowWhenRowDoesntExist() {
        testee().delete(messageIdFactory.of(UUIDs.timeBased()), CassandraId.timeBased())
            .join();
    }

    @Test
    default void deleteShouldDeleteWhenRowExists() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        testee().insert(ComposedMessageIdWithMetaData.builder()
                    .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                    .flags(new Flags())
                    .modSeq(1)
                    .build())
                .join();

        testee().delete(messageId, mailboxId).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee().retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages).isEmpty();
    }

    @Test
    default void deleteShouldDeleteOnlyConcernedRowWhenMultipleRowExists() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        CassandraId mailboxId2 = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        CompletableFuture.allOf(
            testee().insert(ComposedMessageIdWithMetaData.builder()
                    .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                    .flags(new Flags())
                    .modSeq(1)
                    .build()),
            testee().insert(ComposedMessageIdWithMetaData.builder()
                    .composedMessageId(new ComposedMessageId(mailboxId2, messageId, messageUid2))
                    .flags(new Flags())
                    .modSeq(1)
                    .build()))
        .join();

        testee().delete(messageId, mailboxId).join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(new ComposedMessageId(mailboxId2, messageId, messageUid2))
                .flags(new Flags())
                .modSeq(1)
                .build();
        Stream<ComposedMessageIdWithMetaData> messages = testee().retrieve(messageId, Optional.empty()).join();
        assertThat(messages).containsOnly(expectedComposedMessageId);
    }

    @Test
    default void insertShouldWork() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        testee().insert(ComposedMessageIdWithMetaData.builder()
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
        Stream<ComposedMessageIdWithMetaData> messages = testee().retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages).containsOnly(expectedComposedMessageId);
    }

    @Test
    default void updateShouldReturnTrueWhenOldModSeqMatches() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageIdWithMetaData composedMessageIdWithFlags = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .modSeq(1)
                .build();
        testee().insert(composedMessageIdWithFlags).join();

        Boolean result = testee().updateMetadata(composedMessageIdWithFlags, 1).join();

        assertThat(result).isTrue();
    }

    @Test
    default void updateShouldReturnFalseWhenOldModSeqDoesntMatch() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageIdWithMetaData composedMessageIdWithFlags = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                .flags(new Flags())
                .modSeq(1)
                .build();
        testee().insert(composedMessageIdWithFlags).join();

        Boolean result = testee().updateMetadata(composedMessageIdWithFlags, 3).join();

        assertThat(result).isFalse();
    }

    @Test
    default void updateShouldUpdateModSeq() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee().insert(ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags())
                .modSeq(1)
                .build())
            .join();

        ComposedMessageIdWithMetaData expectedComposedMessageId = ComposedMessageIdWithMetaData.builder()
                .composedMessageId(composedMessageId)
                .flags(new Flags())
                .modSeq(2)
                .build();
        testee().updateMetadata(expectedComposedMessageId, 1).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee().retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages).containsOnly(expectedComposedMessageId);
    }

    @Test
    default void updateShouldUpdateAnsweredFlag() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee().insert(ComposedMessageIdWithMetaData.builder()
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
        testee().updateMetadata(expectedComposedMessageId, 1).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee().retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages).containsOnly(expectedComposedMessageId);
    }

    @Test
    default void updateShouldUpdateDeletedFlag() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee().insert(ComposedMessageIdWithMetaData.builder()
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
        testee().updateMetadata(expectedComposedMessageId, 1).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee().retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages).containsOnly(expectedComposedMessageId);
    }

    @Test
    default void updateShouldUpdateDraftFlag() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee().insert(ComposedMessageIdWithMetaData.builder()
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
        testee().updateMetadata(expectedComposedMessageId, 1).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee().retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages).containsOnly(expectedComposedMessageId);
    }

    @Test
    default void updateShouldUpdateFlaggedFlag() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee().insert(ComposedMessageIdWithMetaData.builder()
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
        testee().updateMetadata(expectedComposedMessageId, 1).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee().retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages).containsOnly(expectedComposedMessageId);
    }

    @Test
    default void updateShouldUpdateRecentFlag() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee().insert(ComposedMessageIdWithMetaData.builder()
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
        testee().updateMetadata(expectedComposedMessageId, 1).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee().retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages).containsOnly(expectedComposedMessageId);
    }

    @Test
    default void updateShouldUpdateSeenFlag() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee().insert(ComposedMessageIdWithMetaData.builder()
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
        testee().updateMetadata(expectedComposedMessageId, 1).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee().retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages).containsOnly(expectedComposedMessageId);
    }

    @Test
    default void updateShouldUpdateUserFlag() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee().insert(ComposedMessageIdWithMetaData.builder()
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
        testee().updateMetadata(expectedComposedMessageId, 1).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee().retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages).containsOnly(expectedComposedMessageId);
    }

    @Test
    default void updateShouldUpdateUserFlags() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);

        ComposedMessageId composedMessageId = new ComposedMessageId(mailboxId, messageId, messageUid);
        testee().insert(ComposedMessageIdWithMetaData.builder()
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
        testee().updateMetadata(expectedComposedMessageId, 1).join();

        Stream<ComposedMessageIdWithMetaData> messages = testee().retrieve(messageId, Optional.of(mailboxId)).join();
        assertThat(messages).containsOnly(expectedComposedMessageId);
    }

    @Test
    default void retrieveShouldReturnOneMessageWhenKeyMatches() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        testee().insert(ComposedMessageIdWithMetaData.builder()
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
        Stream<ComposedMessageIdWithMetaData> messages = testee().retrieve(messageId, Optional.of(mailboxId)).join();

        assertThat(messages).containsOnly(expectedComposedMessageId);
    }

    @Test
    default void retrieveShouldReturnMultipleMessagesWhenMessageIdMatches() {
        CassandraMessageId messageId = messageIdFactory.of(UUIDs.timeBased());
        CassandraId mailboxId = CassandraId.timeBased();
        CassandraId mailboxId2 = CassandraId.timeBased();
        MessageUid messageUid = MessageUid.of(1);
        MessageUid messageUid2 = MessageUid.of(2);
        CompletableFuture.allOf(
                testee().insert(ComposedMessageIdWithMetaData.builder()
                    .composedMessageId(new ComposedMessageId(mailboxId, messageId, messageUid))
                    .flags(new Flags())
                    .modSeq(1)
                    .build()),
                testee().insert(ComposedMessageIdWithMetaData.builder()
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
        Stream<ComposedMessageIdWithMetaData> messages = testee().retrieve(messageId, Optional.empty()).join();

        assertThat(messages).containsOnly(expectedComposedMessageId, expectedComposedMessageId2);
    }
}
