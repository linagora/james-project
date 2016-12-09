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

package org.apache.james.mailbox.store;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.mail.Flags;
import javax.mail.internet.SharedInputStream;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageIdManager;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.quota.QuotaManager;
import org.apache.james.mailbox.quota.QuotaRootResolver;
import org.apache.james.mailbox.store.event.MailboxEventDispatcher;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageIdMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;
import org.apache.james.mailbox.store.quota.QuotaChecker;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

public class StoreMessageIdManager implements MessageIdManager {

    private static final Function<MailboxMessage, MetadataWithMailboxId> EXTRACT_METADATA_FUNCTION = new Function<MailboxMessage, MetadataWithMailboxId>() {
        @Override
        public MetadataWithMailboxId apply(MailboxMessage mailboxMessage) {
            return new MetadataWithMailboxId(new SimpleMessageMetaData(mailboxMessage), mailboxMessage.getMailboxId());
        }
    };

    private final MailboxSessionMapperFactory mailboxSessionMapperFactory;
    private final MailboxEventDispatcher dispatcher;
    private final MessageId.Factory messageIdFactory;
    private final QuotaManager quotaManager;
    private final QuotaRootResolver quotaRootResolver;

    public StoreMessageIdManager(MailboxSessionMapperFactory mailboxSessionMapperFactory, MailboxEventDispatcher dispatcher,
                                 MessageId.Factory messageIdFactory,
                                 QuotaManager quotaManager, QuotaRootResolver quotaRootResolver) {
        this.mailboxSessionMapperFactory = mailboxSessionMapperFactory;
        this.dispatcher = dispatcher;
        this.messageIdFactory = messageIdFactory;
        this.quotaManager = quotaManager;
        this.quotaRootResolver = quotaRootResolver;
    }

    @Override
    public void setFlags(Flags newState, MessageManager.FlagsUpdateMode replace, MessageId messageId, MailboxSession mailboxSession) throws MailboxException {
        MessageIdMapper messageIdMapper = mailboxSessionMapperFactory.getMessageIdMapper(mailboxSession);
        Map<MailboxId, UpdatedFlags> updatedFlags = messageIdMapper.setFlags(newState, replace, messageId);
        for (Map.Entry<MailboxId, UpdatedFlags> entry : updatedFlags.entrySet()) {
            dispatchFlagsChange(mailboxSession, entry.getKey(), entry.getValue());
        }
    }

    private void dispatchFlagsChange(MailboxSession mailboxSession, MailboxId mailboxId, UpdatedFlags updatedFlags) throws MailboxException {
        Mailbox mailbox = mailboxSessionMapperFactory.getMailboxMapper(mailboxSession).findMailboxById(mailboxId);
        if (updatedFlags.flagsChanged()) {
            dispatcher.flagsUpdated(mailboxSession, updatedFlags.getUid(), mailbox, updatedFlags);
        }
    }

    @Override
    public List<MessageResult> getMessages(List<MessageId> messageIds, final MessageResult.FetchGroup minimal, MailboxSession mailboxSession) throws MailboxException {
        try {
            MessageIdMapper messageIdMapper = mailboxSessionMapperFactory.createMessageIdMapper(mailboxSession);
            return FluentIterable.from(messageIdMapper.find(messageIds, MessageMapper.FetchType.getFetchType(minimal)))
                .transform(messageResultConverter(minimal))
                .toList();
        } catch (WrappedException wrappedException) {
            throw wrappedException.unwrap();
        }
    }

    @Override
    public void delete(MessageId messageId, final List<MailboxId> mailboxIds, MailboxSession mailboxSession) throws MailboxException {
        MessageIdMapper messageIdMapper = mailboxSessionMapperFactory.getMessageIdMapper(mailboxSession);
        MailboxMapper mailboxMapper = mailboxSessionMapperFactory.getMailboxMapper(mailboxSession);

        Iterable<MetadataWithMailboxId> metadatasWithMailbox = FluentIterable
            .from(messageIdMapper.find(ImmutableList.of(messageId), MessageMapper.FetchType.Metadata))
            .filter(inMailbox(mailboxIds))
            .transform(EXTRACT_METADATA_FUNCTION);

        messageIdMapper.delete(messageId, mailboxIds);

        for (MetadataWithMailboxId metadataWithMailboxId : metadatasWithMailbox) {
            dispatcher.expunged(mailboxSession, metadataWithMailboxId.messageMetaData, mailboxMapper.findMailboxById(metadataWithMailboxId.mailboxId));
        }
    }

    @Override
    public void setInMailboxes(MessageId messageId, List<MailboxId> mailboxIds, MailboxSession mailboxSession) throws MailboxException {
        MessageIdMapper messageIdMapper = mailboxSessionMapperFactory.getMessageIdMapper(mailboxSession);
        final List<MailboxId> alreadyInMailboxes = messageIdMapper.findMailboxes(messageId);

        List<MailboxMessage> mailboxMessages = messageIdMapper.find(ImmutableList.of(messageId), MessageMapper.FetchType.Full);
        if (mailboxMessages.isEmpty()) {
            throw new MailboxException("Can not retrieve message with Id " + messageId);
        } else {
            MailboxMessage mailboxMessage = mailboxMessages.get(0);

            validateQuota(mailboxIds, mailboxSession, mailboxMessage);

            setInMailboxes(messageIdMapper,
                mailboxMessage,
                FluentIterable.from(mailboxIds)
                    .filter(notIn(alreadyInMailboxes))
                    .toList(),
                mailboxSession);
        }
    }

    private void validateQuota(List<MailboxId> mailboxIds, MailboxSession mailboxSession, MailboxMessage mailboxMessage) throws MailboxException {
        MailboxMapper mailboxMapper = mailboxSessionMapperFactory.getMailboxMapper(mailboxSession);
        for (MailboxId mailboxId: mailboxIds) {
            new QuotaChecker(quotaManager, quotaRootResolver, mailboxMapper.findMailboxById(mailboxId))
                .tryAddition(1, mailboxMessage.getFullContentOctets());
        }
    }

    private void setInMailboxes(MessageIdMapper messageIdMapper, MailboxMessage mailboxMessage, List<MailboxId> mailboxIds, MailboxSession mailboxSession) throws MailboxException {
        MailboxMapper mailboxMapper = mailboxSessionMapperFactory.getMailboxMapper(mailboxSession);
        for (MailboxId mailboxId : mailboxIds) {

            SimpleMailboxMessage copy = SimpleMailboxMessage.copy(mailboxId, mailboxMessage);
            MessageMetaData metaData = save(mailboxSession, messageIdMapper, mailboxId, copy);
            dispatcher.added(mailboxSession, metaData, mailboxMapper.findMailboxById(mailboxId));
        }
    }

    private MessageMetaData save(MailboxSession mailboxSession, MessageIdMapper messageIdMapper, MailboxId mailboxId, MailboxMessage mailboxMessage) throws MailboxException {
        validateQuota(ImmutableList.of(mailboxId), mailboxSession, mailboxMessage);
        long modSeq = mailboxSessionMapperFactory.getModSeqProvider().nextModSeq(mailboxSession, mailboxId);
        MessageUid uid = mailboxSessionMapperFactory.getUidProvider().nextUid(mailboxSession, mailboxId);
        mailboxMessage.setModSeq(modSeq);
        mailboxMessage.setUid(uid);
        messageIdMapper.save(mailboxMessage);
        return new SimpleMessageMetaData(uid, modSeq, mailboxMessage.createFlags(), mailboxMessage.getFullContentOctets(), mailboxMessage.getInternalDate(), mailboxMessage.getMessageId());
    }

    /**
     * Create a new {@link MailboxMessage} for the given data
     */
    protected MailboxMessage createMessage(Date internalDate, int size, int bodyStartOctet, SharedInputStream content, Flags flags, PropertyBuilder propertyBuilder, List<MessageAttachment> attachments, MailboxId mailboxId) throws MailboxException {
        return new SimpleMailboxMessage(messageIdFactory.generate(), internalDate, size, bodyStartOctet, content, flags, propertyBuilder, mailboxId, attachments);
    }

    private Function<MailboxMessage, MessageResult> messageResultConverter(final MessageResult.FetchGroup minimal) {
        return new Function<MailboxMessage, MessageResult>() {
            @Override
            public MessageResult apply(MailboxMessage input) {
                try {
                    return ResultUtils.loadMessageResult(input, minimal);
                } catch (MailboxException e) {
                    throw new WrappedException(e);
                }
            }
        };
    }

    private Predicate<MailboxMessage> inMailbox(final List<MailboxId> mailboxIds) {
        return new Predicate<MailboxMessage>() {
            @Override
            public boolean apply(MailboxMessage mailboxMessage) {
                return mailboxIds.contains(mailboxMessage.getMailboxId());
            }
        };
    }

    private Predicate<MailboxId> notIn(final List<MailboxId> alreadyInMailboxes) {
        return new Predicate<MailboxId>() {
            @Override
            public boolean apply(MailboxId input) {
                return !alreadyInMailboxes.contains(input);
            }
        };
    }

    private static class MetadataWithMailboxId {
        private final MessageMetaData messageMetaData;
        private final MailboxId mailboxId;

        public MetadataWithMailboxId(MessageMetaData messageMetaData, MailboxId mailboxId) {
            this.messageMetaData = messageMetaData;
            this.mailboxId = mailboxId;
        }
    }

    private static class WrappedException extends RuntimeException {
        private final MailboxException cause;

        public WrappedException(MailboxException cause) {
            this.cause = cause;
        }

        public MailboxException unwrap() throws MailboxException {
            throw cause;
        }
    }
}
