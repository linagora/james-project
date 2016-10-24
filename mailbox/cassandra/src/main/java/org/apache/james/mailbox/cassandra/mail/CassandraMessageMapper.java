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

import static com.datastax.driver.core.querybuilder.QueryBuilder.decr;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.incr;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.james.backends.cassandra.utils.FunctionRunnerWithRetry;
import org.apache.james.mailbox.FlagsBuilder;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.cassandra.mail.utils.MessageDeletedDuringFlagsUpdateException;
import org.apache.james.mailbox.cassandra.table.CassandraMailboxCountersTable;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.ComposedMessageIdWithFlags;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.FlagsUpdateCalculator;
import org.apache.james.mailbox.store.SimpleMessageMetaData;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Assignment;
import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

public class CassandraMessageMapper implements MessageMapper {

    private final Session session;
    private final ModSeqProvider modSeqProvider;
    private final MailboxSession mailboxSession;
    private final UidProvider uidProvider;
    private final int maxRetries;
    private final AttachmentMapper attachmentMapper;
    private final CassandraMessageDAO messageDAO;
    private final CassandraMessageIdDAO messageIdDAO;
    private final CassandraMessageIdToImapUidDAO imapUidDAO;
    private final MessageId.Factory messageIdFactory;

    public CassandraMessageMapper(Session session, UidProvider uidProvider, ModSeqProvider modSeqProvider, 
            MailboxSession mailboxSession, int maxRetries, AttachmentMapper attachmentMapper,
            CassandraMessageDAO messageDAO, CassandraMessageIdDAO messageIdDAO, CassandraMessageIdToImapUidDAO imapUidDAO) {
        this.session = session;
        this.uidProvider = uidProvider;
        this.modSeqProvider = modSeqProvider;
        this.mailboxSession = mailboxSession;
        this.maxRetries = maxRetries;
        this.attachmentMapper = attachmentMapper;
        this.messageDAO = messageDAO;
        this.messageIdDAO = messageIdDAO;
        this.imapUidDAO = imapUidDAO;
        this.messageIdFactory = new CassandraMessageId.Factory();
    }

    @Override
    public long countMessagesInMailbox(Mailbox mailbox) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        ResultSet results = session.execute(
            select(CassandraMailboxCountersTable.COUNT)
                .from(CassandraMailboxCountersTable.TABLE_NAME)
                .where(eq(CassandraMailboxCountersTable.MAILBOX_ID, mailboxId.asUuid())));
        return results.isExhausted() ? 0 : results.one().getLong(CassandraMailboxCountersTable.COUNT);
    }

    @Override
    public long countUnseenMessagesInMailbox(Mailbox mailbox) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        ResultSet results = session.execute(
            select(CassandraMailboxCountersTable.UNSEEN)
                .from(CassandraMailboxCountersTable.TABLE_NAME)
                .where(eq(CassandraMailboxCountersTable.MAILBOX_ID, mailboxId.asUuid())));
        if (!results.isExhausted()) {
            Row row = results.one();
            if (row.getColumnDefinitions().contains(CassandraMailboxCountersTable.UNSEEN)) {
                return row.getLong(CassandraMailboxCountersTable.UNSEEN);
            }
        }
        return 0;
    }

    @Override
    public void delete(Mailbox mailbox, MailboxMessage message) {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        messageIdDAO.retrieve(mailboxId, message.getUid()).join()
            .ifPresent(composedMessageId -> deleteUsingMailboxId(composedMessageId));
    }

    private void deleteUsingMailboxId(ComposedMessageIdWithFlags composedMessageIdWithFlags) {
        ComposedMessageId composedMessageId = composedMessageIdWithFlags.getComposedMessageId();
        CassandraMessageId messageId = (CassandraMessageId) composedMessageId.getMessageId();
        CassandraId mailboxId = (CassandraId) composedMessageId.getMailboxId();
        MessageUid uid = composedMessageId.getUid();
        CompletableFuture.allOf(imapUidDAO.delete(messageId, mailboxId),
                messageIdDAO.delete(mailboxId, uid))
            .join();

        decrementCount(mailboxId);
        if (!composedMessageIdWithFlags.getFlags().contains(Flag.SEEN)) {
            decrementUnseen(mailboxId);
        }
    }

    private Optional<ComposedMessageIdWithFlags> retrieveMessageId(CassandraId mailboxId, MailboxMessage message) {
        return messageIdDAO.retrieve(mailboxId, message.getUid()).join();
    }

    @Override
    public Iterator<MailboxMessage> findInMailbox(Mailbox mailbox, MessageRange set, FetchType ftype, int max) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        return messageDAO.retrieveMessages(toMessageIds(messageIdDAO.retrieveMessages(mailboxId, set)),
                    ftype, Optional.of(max), attachmentMapper::getAttachments).stream()
                .sorted(Comparator.comparing(MailboxMessage::getUid))
                .iterator();
    }

    private List<CassandraMessageId> toMessageIds(List<ComposedMessageIdWithFlags> composedMessageIdWithFlags) {
        return composedMessageIdWithFlags.stream()
                .map(ComposedMessageIdWithFlags::getComposedMessageId)
                .map(ComposedMessageId::getMessageId)
                .map(messageId -> (CassandraMessageId) messageId)
                .collect(Guavate.toImmutableList());
    }

    @Override
    public List<MessageUid> findRecentMessageUidsInMailbox(Mailbox mailbox) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        return messageDAO.retrieveMessages(toMessageIds(messageIdDAO.retrieveMessages(mailboxId, MessageRange.all())),
                    FetchType.Metadata, Optional.empty(), attachmentMapper::getAttachments).stream()
                .filter(MailboxMessage::isRecent)
                .flatMap(message -> imapUidDAO.retrieve((CassandraMessageId) message.getMessageId(), Optional.ofNullable(mailboxId)).join())
                .map(ComposedMessageIdWithFlags::getComposedMessageId)
                .map(ComposedMessageId::getUid)
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public MessageUid findFirstUnseenMessageUid(Mailbox mailbox) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        return messageDAO.retrieveMessages(toMessageIds(messageIdDAO.retrieveMessages(mailboxId, MessageRange.all())), 
                    FetchType.Metadata, Optional.empty(), attachmentMapper::getAttachments).stream()
                .filter(message -> !message.isSeen())
                .flatMap(message -> imapUidDAO.retrieve((CassandraMessageId) message.getMessageId(), Optional.ofNullable(mailboxId)).join())
                .map(ComposedMessageIdWithFlags::getComposedMessageId)
                .map(ComposedMessageId::getUid)
                .sorted()
                .findFirst()
                .orElse(null);
    }

    @Override
    public Map<MessageUid, MessageMetaData> expungeMarkedForDeletionInMailbox(Mailbox mailbox, MessageRange set) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        return messageDAO.retrieveMessages(toMessageIds(messageIdDAO.retrieveMessages(mailboxId, set)), 
                    FetchType.Metadata, Optional.empty(), attachmentMapper::getAttachments).stream()
                .filter(MailboxMessage::isDeleted)
                .peek(message -> delete(mailbox, message))
                .collect(Collectors.toMap(MailboxMessage::getUid, SimpleMessageMetaData::new));
    }

    @Override
    public MessageMetaData move(Mailbox destinationMailbox, MailboxMessage original) throws MailboxException {
        CassandraId originalMailboxId = (CassandraId) original.getMailboxId();
        MessageMetaData messageMetaData = copy(destinationMailbox, original);
        retrieveMessageId(originalMailboxId, original)
                .ifPresent(messageId -> deleteUsingMailboxId(messageId));
        return messageMetaData;
    }

    @Override
    public void endRequest() {
        // Do nothing
    }

    @Override
    public long getHighestModSeq(Mailbox mailbox) throws MailboxException {
        return modSeqProvider.highestModSeq(mailboxSession, mailbox);
    }

    @Override
    public MessageMetaData add(Mailbox mailbox, MailboxMessage message) throws MailboxException {
        message.setUid(uidProvider.nextUid(mailboxSession, mailbox));
        message.setModSeq(modSeqProvider.nextModSeq(mailboxSession, mailbox));
        return addToMailbox(mailbox, message);
    }

    private MessageMetaData addToMailbox(Mailbox mailbox, MailboxMessage message) throws MailboxException {
        MessageMetaData messageMetaData = save(mailbox, message);
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        if (!message.isSeen()) {
            incrementUnseen(mailboxId);
        }
        incrementCount(mailboxId);
        return messageMetaData;
    }

    @Override
    public Iterator<UpdatedFlags> updateFlags(Mailbox mailbox, FlagsUpdateCalculator flagUpdateCalculator, MessageRange set) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        return messageDAO.retrieveMessages(toMessageIds(messageIdDAO.retrieveMessages(mailboxId, set)), 
                    FetchType.Metadata, Optional.empty(), attachmentMapper::getAttachments).stream()
                .map(message -> updateFlagsOnMessage(mailbox, flagUpdateCalculator, message))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek((updatedFlags) -> manageUnseenMessageCounts(mailbox, updatedFlags.getOldFlags(), updatedFlags.getNewFlags()))
                .collect(Collectors.toList()) // This collect is here as we need to consume all the stream before returning result
                .iterator();
    }

    @Override
    public <T> T execute(Transaction<T> transaction) throws MailboxException {
        return transaction.run();
    }

    @Override
    public MessageMetaData copy(Mailbox mailbox, MailboxMessage original) throws MailboxException {
        SimpleMailboxMessage newMessage = SimpleMailboxMessage.copy(mailbox.getMailboxId(), original, com.google.common.base.Optional.of(messageIdFactory.generate()));
        newMessage.setUid(uidProvider.nextUid(mailboxSession, mailbox));
        newMessage.setModSeq(modSeqProvider.nextModSeq(mailboxSession, mailbox));
        newMessage.setFlags(new FlagsBuilder().add(original.createFlags()).add(Flag.RECENT).build());
        return addToMailbox(mailbox, newMessage);
    }

    @Override
    public com.google.common.base.Optional<MessageUid> getLastUid(Mailbox mailbox) throws MailboxException {
        return uidProvider.lastUid(mailboxSession, mailbox);
    }

    private void decrementCount(CassandraId mailboxId) {
        updateMailbox(mailboxId, decr(CassandraMailboxCountersTable.COUNT));
    }

    private void incrementCount(CassandraId mailboxId) {
        updateMailbox(mailboxId, incr(CassandraMailboxCountersTable.COUNT));
    }

    private void decrementUnseen(CassandraId mailboxId) {
        updateMailbox(mailboxId, decr(CassandraMailboxCountersTable.UNSEEN));
    }

    private void incrementUnseen(CassandraId mailboxId) {
        updateMailbox(mailboxId, incr(CassandraMailboxCountersTable.UNSEEN));
    }

    private void updateMailbox(CassandraId mailboxId, Assignment operation) {
        session.execute(update(CassandraMailboxCountersTable.TABLE_NAME).with(operation).where(eq(CassandraMailboxCountersTable.MAILBOX_ID, mailboxId.asUuid())));
    }

    private MessageMetaData save(Mailbox mailbox, MailboxMessage message) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        insertIds(message, mailboxId)
            .thenApply(Throwing.function(voide -> {
                messageDAO.save(mailbox, message);
                return voide;
            }))
            .join();
        return new SimpleMessageMetaData(message);
    }

    private CompletableFuture<Void> insertIds(MailboxMessage message, CassandraId mailboxId) {
        ComposedMessageIdWithFlags composedMessageIdWithFlags = ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(mailboxId, (CassandraMessageId) message.getMessageId(), message.getUid()))
                .flags(message.createFlags())
                .build();
        return CompletableFuture.allOf(messageIdDAO.insert(composedMessageIdWithFlags),
                imapUidDAO.insert(composedMessageIdWithFlags));
    }

    private void manageUnseenMessageCounts(Mailbox mailbox, Flags oldFlags, Flags newFlags) {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        if (oldFlags.contains(Flag.SEEN) && !newFlags.contains(Flag.SEEN)) {
            incrementUnseen(mailboxId);
        }
        if (!oldFlags.contains(Flag.SEEN) && newFlags.contains(Flag.SEEN)) {
            decrementUnseen(mailboxId);
        }
    }

    private Optional<UpdatedFlags> updateFlagsOnMessage(Mailbox mailbox, FlagsUpdateCalculator flagUpdateCalculator, MailboxMessage message) {
        return tryMessageFlagsUpdate(flagUpdateCalculator, mailbox, message)
            .map(Optional::of)
            .orElse(handleRetries(mailbox, flagUpdateCalculator, (CassandraMessageId) message.getMessageId()));
    }

    private Optional<UpdatedFlags> tryMessageFlagsUpdate(FlagsUpdateCalculator flagUpdateCalculator, Mailbox mailbox, MailboxMessage message) {
        try {
            long oldModSeq = message.getModSeq();
            Flags oldFlags = message.createFlags();
            Flags newFlags = flagUpdateCalculator.buildNewFlags(oldFlags);
            message.setFlags(newFlags);
            message.setModSeq(modSeqProvider.nextModSeq(mailboxSession, mailbox));
            if (messageDAO.conditionalSave(message, oldModSeq).join()) {
                return Optional.of(new UpdatedFlags(message.getUid(), message.getModSeq(), oldFlags, newFlags));
            } else {
                return Optional.empty();
            }
        } catch (MailboxException e) {
            throw Throwables.propagate(e);
        }
    }

    private Optional<UpdatedFlags> handleRetries(Mailbox mailbox, FlagsUpdateCalculator flagUpdateCalculator, CassandraMessageId messageId) {
        try {
            return Optional.of(
                new FunctionRunnerWithRetry(maxRetries)
                    .executeAndRetrieveObject(() -> retryMessageFlagsUpdate(mailbox, messageId, flagUpdateCalculator)));
        } catch (MessageDeletedDuringFlagsUpdateException e) {
            mailboxSession.getLog().warn(e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private Optional<UpdatedFlags> retryMessageFlagsUpdate(Mailbox mailbox, CassandraMessageId messageId, FlagsUpdateCalculator flagUpdateCalculator) {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        return tryMessageFlagsUpdate(flagUpdateCalculator,
            mailbox,
            messageDAO.retrieveMessages(ImmutableList.of(messageId), FetchType.Metadata, Optional.empty(), attachmentMapper::getAttachments).stream()
                .findFirst()
                .orElseThrow(() -> new MessageDeletedDuringFlagsUpdateException(mailboxId, messageId)));
    }

}
