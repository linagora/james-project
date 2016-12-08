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
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageIdMapper;
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;

import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.functions.FunctionChainer;
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
        List<ComposedMessageIdWithMetaData> composedMessageIds = messageIds.stream()
            .map(messageId -> imapUidDAO.retrieve((CassandraMessageId) messageId, Optional.empty()))
            .flatMap(CompletableFuture::join)
            .collect(Guavate.toImmutableList());
        return messageDAO.retrieveMessages(composedMessageIds, fetchType, Optional.empty()).join()
                .map(loadAttachments())
                .map(toMailboxMessages())
                .sorted(Comparator.comparing(MailboxMessage::getUid))
                .collect(Guavate.toImmutableList());
    }

    private Function<Pair<MailboxMessage, Stream<MessageAttachmentById>>, Pair<MailboxMessage, Stream<MessageAttachment>>> loadAttachments() {
        return pair -> Pair.of(pair.getLeft(), new AttachmentLoader(attachmentMapper).getAttachments(pair.getRight().collect(Guavate.toImmutableList())));
    }

    private FunctionChainer<Pair<MailboxMessage, Stream<MessageAttachment>>, SimpleMailboxMessage> toMailboxMessages() {
        return Throwing.function(pair -> {
            return SimpleMailboxMessage.cloneWithAttachments(pair.getLeft(), 
                    pair.getRight().collect(Guavate.toImmutableList()));
        });
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
        imapUidDAO.retrieve(cassandraMessageId, Optional.empty())
            .thenAccept(composedMessageIds -> composedMessageIds
                .map(ComposedMessageIdWithMetaData::getComposedMessageId)
                .forEach(composedMessageId -> deleteIds(composedMessageId))
                )
            .join();
    }

    private CompletableFuture<Void> deleteIds(ComposedMessageId composedMessageId) {
        CassandraMessageId messageId = (CassandraMessageId) composedMessageId.getMessageId();
        CassandraId mailboxId = (CassandraId) composedMessageId.getMailboxId();
        return CompletableFuture.allOf(imapUidDAO.delete(messageId, mailboxId),
                messageIdDAO.delete(mailboxId, composedMessageId.getUid()));
    }
}
