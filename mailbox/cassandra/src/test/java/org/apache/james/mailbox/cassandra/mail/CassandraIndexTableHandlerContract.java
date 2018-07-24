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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.mail.Flags;

import org.apache.james.mailbox.FlagsBuilder;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.cassandra.ids.CassandraMessageId;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.junit.jupiter.api.Test;

import com.github.steveash.guavate.Guavate;

public interface CassandraIndexTableHandlerContract {

    CassandraId MAILBOX_ID = CassandraId.timeBased();
    MessageUid MESSAGE_UID = MessageUid.of(18L);
    CassandraMessageId CASSANDRA_MESSAGE_ID = new CassandraMessageId.Factory().generate();
    int UID_VALIDITY = 15;
    long MODSEQ = 17;
    Mailbox mailbox = new SimpleMailbox(MailboxPath.forUser("user", "name"),
        UID_VALIDITY, MAILBOX_ID);


    class Testee {
        private final CassandraMailboxCounterDAO mailboxCounterDAO;
        private final CassandraMailboxRecentsDAO mailboxRecentsDAO;
        private final CassandraApplicableFlagDAO applicableFlagDAO;
        private final CassandraFirstUnseenDAO firstUnseenDAO;
        private final CassandraIndexTableHandler indexTableHandler;
        private final CassandraDeletedMessageDAO deletedMessageDAO;

        public Testee(CassandraMailboxCounterDAO mailboxCounterDAO, CassandraMailboxRecentsDAO mailboxRecentsDAO,
                      CassandraApplicableFlagDAO applicableFlagDAO, CassandraFirstUnseenDAO firstUnseenDAO,
                      CassandraIndexTableHandler indexTableHandler, CassandraDeletedMessageDAO deletedMessageDAO) {
            this.mailboxCounterDAO = mailboxCounterDAO;
            this.mailboxRecentsDAO = mailboxRecentsDAO;
            this.applicableFlagDAO = applicableFlagDAO;
            this.firstUnseenDAO = firstUnseenDAO;
            this.indexTableHandler = indexTableHandler;
            this.deletedMessageDAO = deletedMessageDAO;
        }
    }

    Testee indexTableHandlerTestee();

    @Test
    default void updateIndexOnAddShouldIncrementMessageCount() throws Exception {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);



        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        Optional<Long> actual = indexTableHandlerTestee().mailboxCounterDAO.countMessagesInMailbox(mailbox).join();
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(1);
    }

    @Test
    default void updateIndexOnAddShouldIncrementUnseenMessageCountWhenUnseen() throws Exception {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);

        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        Optional<Long> actual = indexTableHandlerTestee().mailboxCounterDAO.countUnseenMessagesInMailbox(mailbox).join();
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(1);
    }

    @Test
    default void updateIndexOnAddShouldNotIncrementUnseenMessageCountWhenSeen() throws Exception {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags(Flags.Flag.SEEN));
        when(message.getUid()).thenReturn(MESSAGE_UID);

        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        Optional<Long> actual = indexTableHandlerTestee().mailboxCounterDAO.countUnseenMessagesInMailbox(mailbox).join();
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(0);
    }

    @Test
    default void updateIndexOnAddShouldNotAddRecentWhenNoRecent() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);

        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        assertThat(indexTableHandlerTestee().mailboxRecentsDAO.getRecentMessageUidsInMailbox(MAILBOX_ID).join()
            .collect(Guavate.toImmutableList()))
            .isEmpty();
    }

    @Test
    default void updateIndexOnAddShouldAddRecentWhenRecent() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags(Flags.Flag.RECENT));
        when(message.getUid()).thenReturn(MESSAGE_UID);

        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        assertThat(indexTableHandlerTestee().mailboxRecentsDAO.getRecentMessageUidsInMailbox(MAILBOX_ID).join()
            .collect(Guavate.toImmutableList()))
            .containsOnly(MESSAGE_UID);
    }

    @Test
    default void updateIndexOnDeleteShouldDecrementMessageCount() throws Exception {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnDelete(new ComposedMessageIdWithMetaData(
                new ComposedMessageId(MAILBOX_ID, CASSANDRA_MESSAGE_ID, MESSAGE_UID),
                new Flags(Flags.Flag.RECENT),
                MODSEQ),
            MAILBOX_ID).join();

        Optional<Long> actual = indexTableHandlerTestee().mailboxCounterDAO.countMessagesInMailbox(mailbox).join();
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(0);
    }

    @Test
    default void updateIndexOnDeleteShouldDecrementUnseenMessageCountWhenUnseen() throws Exception {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnDelete(new ComposedMessageIdWithMetaData(
                new ComposedMessageId(MAILBOX_ID, CASSANDRA_MESSAGE_ID, MESSAGE_UID),
                new Flags(),
                MODSEQ),
            MAILBOX_ID).join();

        Optional<Long> actual = indexTableHandlerTestee().mailboxCounterDAO.countUnseenMessagesInMailbox(mailbox).join();
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(0);
    }

    @Test
    default void updateIndexOnDeleteShouldNotDecrementUnseenMessageCountWhenSeen() throws Exception {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnDelete(new ComposedMessageIdWithMetaData(
                new ComposedMessageId(MAILBOX_ID, CASSANDRA_MESSAGE_ID, MESSAGE_UID),
                new Flags(Flags.Flag.SEEN),
                MODSEQ),
            MAILBOX_ID).join();

        Optional<Long> actual = indexTableHandlerTestee().mailboxCounterDAO.countUnseenMessagesInMailbox(mailbox).join();
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(1);
    }

    @Test
    default void updateIndexOnDeleteShouldRemoveRecentWhenRecent() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags(Flags.Flag.RECENT));
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnDelete(new ComposedMessageIdWithMetaData(
                new ComposedMessageId(MAILBOX_ID, CASSANDRA_MESSAGE_ID, MESSAGE_UID),
                new Flags(Flags.Flag.RECENT),
                MODSEQ),
            MAILBOX_ID).join();

        assertThat(indexTableHandlerTestee().mailboxRecentsDAO.getRecentMessageUidsInMailbox(MAILBOX_ID).join()
            .collect(Guavate.toImmutableList()))
            .isEmpty();
    }

    @Test
    default void updateIndexOnDeleteShouldRemoveUidFromRecentAnyway() {
        // Clean up strategy if some flags updates missed
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags(Flags.Flag.RECENT));
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnDelete(new ComposedMessageIdWithMetaData(
                new ComposedMessageId(MAILBOX_ID, CASSANDRA_MESSAGE_ID, MESSAGE_UID),
                new Flags(),
                MODSEQ),
            MAILBOX_ID).join();

        assertThat(indexTableHandlerTestee().mailboxRecentsDAO.getRecentMessageUidsInMailbox(MAILBOX_ID).join()
            .collect(Guavate.toImmutableList()))
            .isEmpty();
    }

    @Test
    default void updateIndexOnDeleteShouldDeleteMessageFromDeletedMessage() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().deletedMessageDAO.addDeleted(MAILBOX_ID, MESSAGE_UID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnDelete(new ComposedMessageIdWithMetaData(
                new ComposedMessageId(MAILBOX_ID, CASSANDRA_MESSAGE_ID, MESSAGE_UID),
                new Flags(),
                MODSEQ),
            MAILBOX_ID).join();

        assertThat(
            indexTableHandlerTestee().deletedMessageDAO
                .retrieveDeletedMessage(MAILBOX_ID, MessageRange.all())
                .join()
                .collect(Guavate.toImmutableList()))
            .isEmpty();
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldNotChangeMessageCount() throws Exception {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags(Flags.Flag.RECENT))
            .oldFlags(new Flags())
            .modSeq(MODSEQ)
            .build()).join();

        Optional<Long> actual = indexTableHandlerTestee().mailboxCounterDAO.countMessagesInMailbox(mailbox).join();
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(1);
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldDecrementUnseenMessageCountWhenSeenIsSet() throws Exception {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags(Flags.Flag.SEEN))
            .oldFlags(new Flags())
            .modSeq(MODSEQ)
            .build()).join();

        Optional<Long> actual = indexTableHandlerTestee().mailboxCounterDAO.countUnseenMessagesInMailbox(mailbox).join();
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(0);
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldSaveMessageInDeletedMessageWhenDeletedFlagIsSet() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags(Flags.Flag.DELETED))
            .oldFlags(new Flags())
            .modSeq(MODSEQ)
            .build()).join();

        assertThat(
            indexTableHandlerTestee().deletedMessageDAO
                .retrieveDeletedMessage(MAILBOX_ID, MessageRange.all())
                .join()
                .collect(Guavate.toImmutableList()))
            .containsExactly(MESSAGE_UID);
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldRemoveMessageInDeletedMessageWhenDeletedFlagIsUnset() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().deletedMessageDAO.addDeleted(MAILBOX_ID, MESSAGE_UID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags())
            .oldFlags(new Flags(Flags.Flag.DELETED))
            .modSeq(MODSEQ)
            .build()).join();

        assertThat(
            indexTableHandlerTestee().deletedMessageDAO
                .retrieveDeletedMessage(MAILBOX_ID, MessageRange.all())
                .join()
                .collect(Guavate.toImmutableList()))
            .isEmpty();
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldNotRemoveMessageInDeletedMessageWhenDeletedFlagIsNotUnset() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().deletedMessageDAO.addDeleted(MAILBOX_ID, MESSAGE_UID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags())
            .oldFlags(new Flags(Flags.Flag.SEEN))
            .modSeq(MODSEQ)
            .build()).join();

        assertThat(
            indexTableHandlerTestee().deletedMessageDAO
                .retrieveDeletedMessage(MAILBOX_ID, MessageRange.all())
                .join()
                .collect(Guavate.toImmutableList()))
            .containsExactly(MESSAGE_UID);
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldNotSaveMessageInDeletedMessageWhenDeletedFlagIsNotSet() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags(Flags.Flag.RECENT))
            .oldFlags(new Flags())
            .modSeq(MODSEQ)
            .build()).join();

        assertThat(
            indexTableHandlerTestee().deletedMessageDAO
                .retrieveDeletedMessage(MAILBOX_ID, MessageRange.all())
                .join()
                .collect(Guavate.toImmutableList()))
            .isEmpty();
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldIncrementUnseenMessageCountWhenSeenIsUnset() throws Exception {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags(Flags.Flag.SEEN));
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags())
            .oldFlags(new Flags(Flags.Flag.SEEN))
            .modSeq(MODSEQ)
            .build()).join();

        Optional<Long> actual = indexTableHandlerTestee().mailboxCounterDAO.countUnseenMessagesInMailbox(mailbox).join();
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(1);
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldNotChangeUnseenCountWhenBothSeen() throws Exception {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags(Flags.Flag.SEEN));
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags(Flags.Flag.SEEN))
            .oldFlags(new Flags(Flags.Flag.SEEN))
            .modSeq(MODSEQ)
            .build()).join();

        Optional<Long> actual = indexTableHandlerTestee().mailboxCounterDAO.countUnseenMessagesInMailbox(mailbox).join();
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(0);
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldNotChangeUnseenCountWhenBothUnSeen() throws Exception {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags())
            .oldFlags(new Flags())
            .modSeq(MODSEQ)
            .build()).join();

        Optional<Long> actual = indexTableHandlerTestee().mailboxCounterDAO.countUnseenMessagesInMailbox(mailbox).join();
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(1);
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldAddRecentOnSettingRecentFlag() {
        // Clean up strategy if some flags updates missed
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags(Flags.Flag.RECENT))
            .oldFlags(new Flags())
            .modSeq(MODSEQ)
            .build()).join();

        assertThat(indexTableHandlerTestee().mailboxRecentsDAO.getRecentMessageUidsInMailbox(MAILBOX_ID).join()
            .collect(Guavate.toImmutableList()))
            .containsOnly(MESSAGE_UID);
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldRemoveRecentOnUnsettingRecentFlag() {
        // Clean up strategy if some flags updates missed
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags(Flags.Flag.RECENT));
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags())
            .oldFlags(new Flags(Flags.Flag.RECENT))
            .modSeq(MODSEQ)
            .build()).join();

        assertThat(indexTableHandlerTestee().mailboxRecentsDAO.getRecentMessageUidsInMailbox(MAILBOX_ID).join()
            .collect(Guavate.toImmutableList()))
            .isEmpty();
    }

    @Test
    default void updateIndexOnAddShouldUpdateFirstUnseenWhenUnseen() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        Optional<MessageUid> actual = indexTableHandlerTestee().firstUnseenDAO.retrieveFirstUnread(MAILBOX_ID).join();
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(MESSAGE_UID);
    }

    @Test
    default void updateIndexOnAddShouldSaveMessageInDeletedWhenDeleted() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags(Flags.Flag.DELETED));
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        assertThat(
            indexTableHandlerTestee().deletedMessageDAO
                .retrieveDeletedMessage(MAILBOX_ID, MessageRange.all())
                .join()
                .collect(Guavate.toImmutableList()))
            .containsExactly(MESSAGE_UID);
    }

    @Test
    default void updateIndexOnAddShouldNotSaveMessageInDeletedWhenNotDeleted() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        assertThat(
            indexTableHandlerTestee().deletedMessageDAO
                .retrieveDeletedMessage(MAILBOX_ID, MessageRange.all())
                .join()
                .collect(Guavate.toImmutableList()))
            .isEmpty();
    }

    @Test
    default void updateIndexOnAddShouldNotUpdateFirstUnseenWhenSeen() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags(Flags.Flag.SEEN));
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        Optional<MessageUid> actual = indexTableHandlerTestee().firstUnseenDAO.retrieveFirstUnread(MAILBOX_ID).join();
        assertThat(actual.isPresent()).isFalse();
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldUpdateLastUnseenWhenSetToSeen() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags(Flags.Flag.SEEN))
            .oldFlags(new Flags())
            .modSeq(MODSEQ)
            .build()).join();

        Optional<MessageUid> actual = indexTableHandlerTestee().firstUnseenDAO.retrieveFirstUnread(MAILBOX_ID).join();
        assertThat(actual.isPresent()).isFalse();
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldUpdateLastUnseenWhenSetToUnseen() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags(Flags.Flag.SEEN));
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags())
            .oldFlags(new Flags(Flags.Flag.SEEN))
            .modSeq(MODSEQ)
            .build()).join();

        Optional<MessageUid> actual = indexTableHandlerTestee().firstUnseenDAO.retrieveFirstUnread(MAILBOX_ID).join();
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(MESSAGE_UID);
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldNotUpdateLastUnseenWhenKeepUnseen() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags())
            .oldFlags(new Flags())
            .modSeq(MODSEQ)
            .build()).join();

        Optional<MessageUid> actual = indexTableHandlerTestee().firstUnseenDAO.retrieveFirstUnread(MAILBOX_ID).join();
        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(MESSAGE_UID);
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldNotUpdateLastUnseenWhenKeepSeen() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags(Flags.Flag.SEEN));
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags(Flags.Flag.SEEN))
            .oldFlags(new Flags(Flags.Flag.SEEN))
            .modSeq(MODSEQ)
            .build()).join();

        Optional<MessageUid> actual = indexTableHandlerTestee().firstUnseenDAO.retrieveFirstUnread(MAILBOX_ID).join();
        assertThat(actual.isPresent()).isFalse();
    }

    @Test
    default void updateIndexOnDeleteShouldUpdateFirstUnseenWhenUnseen() {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(new Flags());
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnDelete(new ComposedMessageIdWithMetaData(
            new ComposedMessageId(MAILBOX_ID, CASSANDRA_MESSAGE_ID, MESSAGE_UID),
            new Flags(),
            MODSEQ), MAILBOX_ID).join();

        Optional<MessageUid> actual = indexTableHandlerTestee().firstUnseenDAO.retrieveFirstUnread(MAILBOX_ID).join();
        assertThat(actual.isPresent()).isFalse();
    }

    @Test
    default void updateIndexOnAddShouldUpdateApplicableFlag() {
        Flags customFlags = new Flags("custom");
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(customFlags);
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        Flags applicableFlag = indexTableHandlerTestee().applicableFlagDAO.retrieveApplicableFlag(MAILBOX_ID).join().get();

        assertThat(applicableFlag).isEqualTo(customFlags);
    }

    @Test
    default void updateIndexOnFlagsUpdateShouldUnionApplicableFlag() {
        Flags customFlag = new Flags("custom");
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(customFlag);
        when(message.getUid()).thenReturn(MESSAGE_UID);
        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        Flags customBis = new Flags("customBis");
        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(customBis)
            .oldFlags(customFlag)
            .modSeq(MODSEQ)
            .build()).join();

        Flags applicableFlag = indexTableHandlerTestee().applicableFlagDAO.retrieveApplicableFlag(MAILBOX_ID).join().get();

        assertThat(applicableFlag).isEqualTo(new FlagsBuilder().add(customFlag, customBis).build());
    }

    @Test
    default void applicableFlagShouldKeepAllFlagsEvenTheMessageRemovesFlag(){
        Flags messageFlags = FlagsBuilder.builder()
            .add("custom1", "custom2", "custom3")
            .build();

        MailboxMessage message = mock(MailboxMessage.class);
        when(message.createFlags()).thenReturn(messageFlags);
        when(message.getUid()).thenReturn(MESSAGE_UID);

        indexTableHandlerTestee().indexTableHandler.updateIndexOnAdd(message, MAILBOX_ID).join();

        indexTableHandlerTestee().indexTableHandler.updateIndexOnFlagsUpdate(MAILBOX_ID, UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .newFlags(new Flags())
            .oldFlags(messageFlags)
            .modSeq(MODSEQ)
            .build()).join();

        Flags applicableFlag = indexTableHandlerTestee().applicableFlagDAO.retrieveApplicableFlag(MAILBOX_ID).join().get();
        assertThat(applicableFlag).isEqualTo(messageFlags);
    }
}
