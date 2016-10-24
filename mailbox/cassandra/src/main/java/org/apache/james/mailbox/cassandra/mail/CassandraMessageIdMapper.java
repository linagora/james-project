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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageIdMapper;
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.Message;

import com.github.steveash.guavate.Guavate;

public class CassandraMessageIdMapper implements MessageIdMapper {

    private final MailboxMapper mailboxMapper;
    private final AttachmentMapper attachmentMapper;
    private final CassandraMessageIdToImapUidDAO imapUidDAO;
    private final CassandraMessageIdDAO messageIdDAO;
    private final CassandraMessageDAO messageDAO;

    public CassandraMessageIdMapper(MailboxMapper mailboxMapper, AttachmentMapper attachmentMapper,
            CassandraMessageIdToImapUidDAO imapUidDAO, CassandraMessageIdDAO messageIdDAO, CassandraMessageDAO messageDAO) {
        this.mailboxMapper = mailboxMapper;
        this.attachmentMapper = attachmentMapper;
        this.imapUidDAO = imapUidDAO;
        this.messageIdDAO = messageIdDAO;
        this.messageDAO = messageDAO;
    }

    @Override
    public List<Message> find(List<MessageId> messageIds, FetchType fetchType) {
        List<CassandraMessageId> cassandraMessageIds = messageIds.stream()
            .map(id -> (CassandraMessageId) id)
            .collect(Collectors.toList());
        return messageDAO.retrieveMessages(cassandraMessageIds, fetchType, Optional.empty(), attachmentMapper::getAttachments).stream()
                .sorted(Comparator.comparing(MailboxMessage::getUid))
                .collect(Guavate.toImmutableList());
    }

    @Override
    public List<MailboxId> findMailboxes(MessageId messageId) {
        return imapUidDAO.retrieve((CassandraMessageId) messageId, Optional.empty()).join()
            .map(ComposedMessageIdWithMetaData::getComposedMessageId)
            .map(ComposedMessageId::getMailboxId)
            .collect(Guavate.toImmutableList());
    }

    @Override
    public void save(MailboxMessage mailboxMessage) throws MailboxNotFoundException, MailboxException {
        CassandraId mailboxId = (CassandraId) mailboxMessage.getMailboxId();
        messageDAO.save(mailboxMapper.findMailboxById(mailboxId), mailboxMessage).join();
        CassandraMessageId messageId = (CassandraMessageId) mailboxMessage.getMessageId();
        ComposedMessageIdWithMetaData composedMessageIdWithMetaData = ComposedMessageIdWithMetaData.builder()
                    .composedMessageId(new ComposedMessageId(mailboxId, messageId, mailboxMessage.getUid()))
                    .flags(mailboxMessage.createFlags())
                    .modSeq(mailboxMessage.getModSeq())
                    .build();
        CompletableFuture.allOf(imapUidDAO.insert(composedMessageIdWithMetaData),
                messageIdDAO.insert(composedMessageIdWithMetaData))
            .join();
    }

    @Override
    public void delete(MessageId messageId) {
        CassandraMessageId cassandraMessageId = (CassandraMessageId) messageId;
        messageDAO.delete(cassandraMessageId).join();
        imapUidDAO.retrieve(cassandraMessageId, Optional.empty()).join()
            .map(ComposedMessageIdWithMetaData::getComposedMessageId)
            .forEach(composedMessageId -> deleteIds(composedMessageId));
    }

    private void deleteIds(ComposedMessageId composedMessageId) {
        CassandraMessageId messageId = (CassandraMessageId) composedMessageId.getMessageId();
        CassandraId mailboxId = (CassandraId) composedMessageId.getMailboxId();
        CompletableFuture.allOf(imapUidDAO.delete(messageId, mailboxId),
                messageIdDAO.delete(mailboxId, composedMessageId.getUid()))
            .join();
    }
}
