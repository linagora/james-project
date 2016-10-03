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
import static org.apache.james.mailbox.cassandra.table.CassandraMessageIds.MESSAGE_ID;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.ATTACHMENTS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.BODY_CONTENT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.BODY_START_OCTET;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.FULL_CONTENT_OCTETS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.HEADER_CONTENT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.INTERNAL_DATE;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.MOD_SEQ;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.PROPERTIES;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.TEXTUAL_LINE_COUNT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.DELETED;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.RECENT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.SEEN;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.backends.cassandra.utils.FunctionRunnerWithRetry;
import org.apache.james.mailbox.FlagsBuilder;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.cassandra.mail.utils.MessageDeletedDuringFlagsUpdateException;
import org.apache.james.mailbox.cassandra.table.CassandraMailboxCountersTable;
import org.apache.james.mailbox.cassandra.table.CassandraMessageTable;
import org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Attachments;
import org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Properties;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.AttachmentId;
import org.apache.james.mailbox.model.Cid;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.FlagsUpdateCalculator;
import org.apache.james.mailbox.store.SimpleMessageMetaData;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.MessageIdProvider;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.SimpleProperty;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.querybuilder.Assignment;
import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Bytes;

public class CassandraMessageMapper implements MessageMapper {

    private final Session session;
    private final ModSeqProvider modSeqProvider;
    private final MessageIdProvider messageIdProvider;
    private final MailboxSession mailboxSession;
    private final UidProvider uidProvider;
    private final int maxRetries;
    private final AttachmentMapper attachmentMapper;
    private final CassandraMessageDAO messageDAO;
    private final CassandraMessageIdDAO messageIdDAO;
    private final CassandraImapUidDAO imapUidDAO;

    public CassandraMessageMapper(Session session, UidProvider uidProvider, ModSeqProvider modSeqProvider, MessageIdProvider messageIdProvider, MailboxSession mailboxSession, int maxRetries, AttachmentMapper attachmentMapper,
            CassandraMessageDAO messageDAO, CassandraMessageIdDAO messageIdDAO, CassandraImapUidDAO imapUidDAO) {
        this.session = session;
        this.uidProvider = uidProvider;
        this.modSeqProvider = modSeqProvider;
        this.messageIdProvider = messageIdProvider;
        this.mailboxSession = mailboxSession;
        this.maxRetries = maxRetries;
        this.attachmentMapper = attachmentMapper;
        this.messageDAO = messageDAO;
        this.messageIdDAO = messageIdDAO;
        this.imapUidDAO = imapUidDAO;
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
        retrieveMessageId(mailboxId, message)
                .ifPresent(messageId -> deleteUsingMailboxId(messageId, mailboxId, message));
    }

    private void deleteUsingMailboxId(CassandraMessageId messageId, CassandraId mailboxId, MailboxMessage message) {
        imapUidDAO.delete(messageId, mailboxId);
        messageIdDAO.delete(mailboxId, message.getUid());
        decrementCount(mailboxId);
        if (!message.isSeen()) {
            decrementUnseen(mailboxId);
        }
    }

    private Optional<CassandraMessageId> retrieveMessageId(CassandraId mailboxId, MailboxMessage message) {
        return messageIdDAO.retrieve(mailboxId, message.getUid())
            .findFirst();
    }

    @Override
    public Iterator<MailboxMessage> findInMailbox(Mailbox mailbox, MessageRange set, FetchType ftype, int max) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        return CassandraUtils.convertToStream(messageDAO.retrieveMessages(messageIdDAO.retrieveMessageIds(mailboxId, set, ftype), ftype, Optional.of(max)))
            .map(row -> message(row, ftype))
            .sorted(Comparator.comparing(MailboxMessage::getUid))
            .iterator();
    }

    @Override
    public List<MessageUid> findRecentMessageUidsInMailbox(Mailbox mailbox) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        return CassandraUtils.convertToStream(messageDAO.retrieveMessages(messageIdDAO.retrieveMessageIds(mailboxId, MessageRange.all(), FetchType.Metadata), FetchType.Metadata, Optional.empty()))
            .filter(row -> row.getBool(RECENT))
            .flatMap((row) -> imapUidDAO.retrieve(CassandraMessageId.of(row.getString(MESSAGE_ID)), Optional.ofNullable(mailboxId)))
            .map(UniqueMessageId::getMessageUid)
            .sorted()
            .collect(Collectors.toList());
    }

    @Override
    public MessageUid findFirstUnseenMessageUid(Mailbox mailbox) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        return CassandraUtils.convertToStream(messageDAO.retrieveMessages(messageIdDAO.retrieveMessageIds(mailboxId, MessageRange.all(), FetchType.Metadata), FetchType.Metadata, Optional.empty()))
            .filter(row -> !row.getBool(SEEN))
            .flatMap((row) -> imapUidDAO.retrieve(CassandraMessageId.of(row.getString(MESSAGE_ID)), Optional.ofNullable(mailboxId)))
            .map(UniqueMessageId::getMessageUid)
            .sorted()
            .findFirst()
            .orElse(null);
    }

    @Override
    public Map<MessageUid, MessageMetaData> expungeMarkedForDeletionInMailbox(Mailbox mailbox, MessageRange set) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        return CassandraUtils.convertToStream(messageDAO.retrieveMessages(messageIdDAO.retrieveMessageIds(mailboxId, set, FetchType.Metadata), FetchType.Metadata, Optional.empty()))
            .filter(row -> row.getBool(DELETED))
            .map(row -> message(row, FetchType.Metadata))
            .peek((message) -> delete(mailbox, message))
            .collect(Collectors.toMap(MailboxMessage::getUid, SimpleMessageMetaData::new));
    }

    @Override
    public MessageMetaData move(Mailbox destinationMailbox, MailboxMessage original) throws MailboxException {
        CassandraId originalMailboxId = (CassandraId) original.getMailboxId();
        MessageMetaData messageMetaData = copy(destinationMailbox, original);
        CassandraId mailboxId = (CassandraId) destinationMailbox.getMailboxId();
        retrieveMessageId(mailboxId, original)
                .ifPresent(messageId -> deleteUsingMailboxId(messageId, originalMailboxId, original));
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
        message.setMessageId(messageIdProvider.from(mailbox.getMailboxId(), message.getUid()));
        return addMessageToMailbox(message, mailbox);
    }

    private MessageMetaData addMessageToMailbox(MailboxMessage message, Mailbox mailbox) throws MailboxException {
        message.setUid(uidProvider.nextUid(mailboxSession, mailbox));
        message.setModSeq(modSeqProvider.nextModSeq(mailboxSession, mailbox));
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
        return CassandraUtils.convertToStream(messageDAO.retrieveMessages(messageIdDAO.retrieveMessageIds(mailboxId, set, FetchType.Metadata), FetchType.Metadata, Optional.empty()))
            .map((row) -> updateFlagsOnMessage(mailbox, flagUpdateCalculator, row))
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
        original.setFlags(new FlagsBuilder().add(original.createFlags()).add(Flag.RECENT).build());
        return addMessageToMailbox(original, mailbox);
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

    private MailboxMessage message(Row row, FetchType fetchType) {
        try {
            UniqueMessageId messageId = retrieveUniqueMessageId(CassandraMessageId.of(row.getString(MESSAGE_ID)));

            SimpleMailboxMessage message =
                    new SimpleMailboxMessage(
                            row.getDate(INTERNAL_DATE),
                            row.getLong(FULL_CONTENT_OCTETS),
                            row.getInt(BODY_START_OCTET),
                            buildContent(row, fetchType),
                            getFlags(row),
                            getPropertyBuilder(row),
                            messageId.getMailboxId(),
                            getAttachments(row, fetchType));
            message.setUid(messageId.getMessageUid());
            message.setMessageId(messageId.getMessageId());
            message.setModSeq(row.getLong(MOD_SEQ));
            return message;
        } catch (MailboxException e) {
            throw Throwables.propagate(e);
        }
    }

    private UniqueMessageId retrieveUniqueMessageId(CassandraMessageId messageId) throws MailboxException {
        return imapUidDAO.retrieve(messageId, Optional.empty())
            .findFirst()
            .orElseThrow(() -> new MailboxException("Message not found: " + messageId));
    }

    private Flags getFlags(Row row) {
        Flags flags = new Flags();
        for (String flag : CassandraMessageTable.Flag.ALL) {
            if (row.getBool(flag)) {
                flags.add(CassandraMessageTable.Flag.JAVAX_MAIL_FLAG.get(flag));
            }
        }
        row.getSet(CassandraMessageTable.Flag.USER_FLAGS, String.class)
            .stream()
            .forEach(flags::add);
        return flags;
    }

    private PropertyBuilder getPropertyBuilder(Row row) {
        PropertyBuilder property = new PropertyBuilder(
            row.getList(PROPERTIES, UDTValue.class).stream()
                .map(x -> new SimpleProperty(x.getString(Properties.NAMESPACE), x.getString(Properties.NAME), x.getString(Properties.VALUE)))
                .collect(Collectors.toList()));
        property.setTextualLineCount(row.getLong(TEXTUAL_LINE_COUNT));
        return property;
    }

    private List<MessageAttachment> getAttachments(Row row, FetchType fetchType) {
        switch (fetchType) {
        case Full:
        case Body:
            List<UDTValue> udtValues = row.getList(ATTACHMENTS, UDTValue.class);
            Map<AttachmentId,Attachment> attachmentsById = attachmentsById(row, udtValues);

            return udtValues
                    .stream()
                    .map(Throwing.function(x -> 
                        MessageAttachment.builder()
                            .attachment(attachmentsById.get(attachmentIdFrom(x)))
                            .name(x.getString(Attachments.NAME))
                            .cid(com.google.common.base.Optional.fromNullable(x.getString(Attachments.CID)).transform(Cid::from))
                            .isInline(x.getBool(Attachments.IS_INLINE))
                            .build()))
                    .collect(Guavate.toImmutableList());
        default:
            return ImmutableList.of();
        }
    }

    private Map<AttachmentId,Attachment> attachmentsById(Row row, List<UDTValue> udtValues) {
        Map<AttachmentId, Attachment> map = new HashMap<>();
        attachmentMapper.getAttachments(attachmentIds(udtValues)).stream()
                .forEach(att -> map.put(att.getAttachmentId(), att));
        return map;
    }

    private List<AttachmentId> attachmentIds(List<UDTValue> udtValues) {
        return udtValues.stream()
            .map(this::attachmentIdFrom)
            .collect(Guavate.toImmutableList());
    }

    private AttachmentId attachmentIdFrom(UDTValue udtValue) {
        return AttachmentId.from(udtValue.getString(Attachments.ID));
    }

    private MessageMetaData save(Mailbox mailbox, MailboxMessage message) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        insertIds(message, mailboxId);
        messageDAO.save(mailbox, message);
        return new SimpleMessageMetaData(message);
    }

    private void insertIds(MailboxMessage message, CassandraId mailboxId) {
        messageIdDAO.insert(mailboxId, message.getUid(), message.getMessageId());
        imapUidDAO.insert(message.getMessageId(), mailboxId, message.getUid());
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

    private Optional<UpdatedFlags> updateFlagsOnMessage(Mailbox mailbox, FlagsUpdateCalculator flagUpdateCalculator, Row row) {
        return tryMessageFlagsUpdate(flagUpdateCalculator, mailbox, message(row, FetchType.Metadata))
            .map(Optional::of)
            .orElse(handleRetries(mailbox, flagUpdateCalculator, CassandraMessageId.of(row.getString(MESSAGE_ID))));
    }

    private Optional<UpdatedFlags> tryMessageFlagsUpdate(FlagsUpdateCalculator flagUpdateCalculator, Mailbox mailbox, MailboxMessage message) {
        try {
            long oldModSeq = message.getModSeq();
            Flags oldFlags = message.createFlags();
            Flags newFlags = flagUpdateCalculator.buildNewFlags(oldFlags);
            message.setFlags(newFlags);
            message.setModSeq(modSeqProvider.nextModSeq(mailboxSession, mailbox));
            if (messageDAO.conditionalSave(message, oldModSeq)) {
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
            message(Optional.ofNullable(messageDAO.selectMessageData(ImmutableList.of(messageId), FetchType.Metadata).one())
                .orElseThrow(() -> new MessageDeletedDuringFlagsUpdateException(mailboxId, messageId)),
                FetchType.Metadata));
    }

    private SharedByteArrayInputStream buildContent(Row row, FetchType fetchType) {
        switch (fetchType) {
            case Full:
                return new SharedByteArrayInputStream(getFullContent(row));
            case Headers:
                return new SharedByteArrayInputStream(getFieldContent(HEADER_CONTENT, row));
            case Body:
                return new SharedByteArrayInputStream(getBodyContent(row));
            case Metadata:
                return new SharedByteArrayInputStream(new byte[]{});
            default:
                throw new RuntimeException("Unknown FetchType " + fetchType);
        }
    }

    private byte[] getFullContent(Row row) {
        return Bytes.concat(getFieldContent(HEADER_CONTENT, row), getFieldContent(BODY_CONTENT, row));
    }

    private byte[] getBodyContent(Row row) {
        return Bytes.concat(new byte[row.getInt(BODY_START_OCTET)], getFieldContent(BODY_CONTENT, row));
    }

    private byte[] getFieldContent(String field, Row row) {
        byte[] headerContent = new byte[row.getBytes(field).remaining()];
        row.getBytes(field).get(headerContent);
        return headerContent;
    }

}
