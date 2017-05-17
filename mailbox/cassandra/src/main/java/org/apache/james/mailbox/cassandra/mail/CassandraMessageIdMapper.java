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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.mail.Flags;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.FlagsUpdateCalculator;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageIdMapper;
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;
import org.apache.james.util.CompletableFutureUtil;
import org.apache.james.util.FluentFutureStream;
import org.apache.james.util.OptionalConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.functions.FunctionChainer;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableMap;

public class CassandraMessageIdMapper implements MessageIdMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraMessageIdMapper.class);

    private final MailboxMapper mailboxMapper;
    private final CassandraMailboxDAO mailboxDAO;
    private final CassandraMessageIdToImapUidDAO imapUidDAO;
    private final CassandraMessageIdDAO messageIdDAO;
    private final CassandraMessageDAO messageDAO;
    private final CassandraIndexTableHandler indexTableHandler;
    private final CassandraModSeqProvider modSeqProvider;
    private final AttachmentLoader attachmentLoader;

    public CassandraMessageIdMapper(MailboxMapper mailboxMapper, CassandraMailboxDAO mailboxDAO, CassandraAttachmentMapper attachmentMapper,
                                    CassandraMessageIdToImapUidDAO imapUidDAO, CassandraMessageIdDAO messageIdDAO, CassandraMessageDAO messageDAO,
                                    CassandraIndexTableHandler indexTableHandler, CassandraModSeqProvider modSeqProvider) {
        this.mailboxMapper = mailboxMapper;
        this.mailboxDAO = mailboxDAO;
        this.imapUidDAO = imapUidDAO;
        this.messageIdDAO = messageIdDAO;
        this.messageDAO = messageDAO;
        this.indexTableHandler = indexTableHandler;
        this.modSeqProvider = modSeqProvider;
        this.attachmentLoader = new AttachmentLoader(attachmentMapper);
    }

    @Override
    public List<MailboxMessage> find(List<MessageId> messageIds, FetchType fetchType) {
        return findAsStream(messageIds, fetchType)
            .collect(Guavate.toImmutableList());
    }

    private Stream<SimpleMailboxMessage> findAsStream(List<MessageId> messageIds, FetchType fetchType) {
        return CompletableFutureUtil.allOf(
            messageIds.stream()
                .map(messageId -> imapUidDAO.retrieve((CassandraMessageId) messageId, Optional.empty())))
            .thenApply(stream -> stream.flatMap(Function.identity()))
            .thenApply(stream -> stream.collect(Guavate.toImmutableList()))
            .thenCompose(composedMessageIds -> messageDAO.retrieveMessages(composedMessageIds, fetchType, Optional.empty()))
            .thenCompose(stream -> CompletableFutureUtil.allOf(
                stream.map(pair -> mailboxExists(pair.getLeft())
                    .thenApply(b -> Optional.of(pair).filter(any -> b)))))
            .thenApply(stream -> stream.flatMap(OptionalConverter::toStream))
            .thenApply(stream -> stream.map(loadAttachments(fetchType)))
            .thenCompose(CompletableFutureUtil::allOf)
            .join()
            .map(toMailboxMessages())
            .sorted(Comparator.comparing(MailboxMessage::getUid));
    }

    private CompletableFuture<Boolean> mailboxExists(CassandraMessageDAO.MessageWithoutAttachment messageWithoutAttachment) {
        CassandraId cassandraId = (CassandraId) messageWithoutAttachment.getMailboxId();
        return mailboxDAO.retrieveMailbox(cassandraId)
            .thenApply(optional -> {
                if (!optional.isPresent()) {
                    LOGGER.info("Mailbox {} have been deleted but message {} is still attached to it.",
                        cassandraId,
                        messageWithoutAttachment.getMessageId());
                    return false;
                }
                return true;
            });
    }

    private Function<Pair<CassandraMessageDAO.MessageWithoutAttachment, Stream<CassandraMessageDAO.MessageAttachmentRepresentation>>,
                     CompletableFuture<Pair<CassandraMessageDAO.MessageWithoutAttachment, Stream<MessageAttachment>>>>
                     loadAttachments(FetchType fetchType) {
        if (fetchType == FetchType.Full || fetchType == FetchType.Body) {
            return pair -> attachmentLoader
                .getAttachments(pair.getRight().collect(Guavate.toImmutableList()))
                .thenApply(attachments -> Pair.of(pair.getLeft(), attachments.stream()));
        } else {
            return pair -> CompletableFuture.completedFuture(Pair.of(pair.getLeft(), Stream.of()));
        }
    }

    private FunctionChainer<Pair<CassandraMessageDAO.MessageWithoutAttachment, Stream<MessageAttachment>>, SimpleMailboxMessage> toMailboxMessages() {
        return Throwing.function(pair -> pair.getLeft()
            .toMailboxMessage(pair.getRight()
                .collect(Guavate.toImmutableList())));
    }

    @Override
    public List<MailboxId> findMailboxes(MessageId messageId) {
        return imapUidDAO.retrieve((CassandraMessageId) messageId, Optional.empty()).join()
            .map(ComposedMessageIdWithMetaData::getComposedMessageId)
            .map(ComposedMessageId::getMailboxId)
            .collect(Guavate.toImmutableList());
    }

    @Override
    public void save(MailboxMessage mailboxMessage) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailboxMessage.getMailboxId();
        mailboxMapper.findMailboxById(mailboxId);
        CassandraMessageId messageId = (CassandraMessageId) mailboxMessage.getMessageId();
        ComposedMessageIdWithMetaData composedMessageIdWithMetaData = ComposedMessageIdWithMetaData.builder()
            .composedMessageId(new ComposedMessageId(mailboxId, messageId, mailboxMessage.getUid()))
            .flags(mailboxMessage.createFlags())
            .modSeq(mailboxMessage.getModSeq())
            .build();
        messageDAO.save(mailboxMessage)
            .thenCompose(voidValue -> CompletableFuture.allOf(
                imapUidDAO.insert(composedMessageIdWithMetaData),
                messageIdDAO.insert(composedMessageIdWithMetaData)))
            .thenCompose(voidValue -> indexTableHandler.updateIndexOnAdd(mailboxMessage, mailboxId))
            .join();
    }

    @Override
    public void delete(MessageId messageId, List<MailboxId> mailboxIds) {
        CassandraMessageId cassandraMessageId = (CassandraMessageId) messageId;
        mailboxIds.stream()
            .map(mailboxId -> retrieveAndDeleteIndices(cassandraMessageId, Optional.of((CassandraId) mailboxId)))
            .reduce((f1, f2) -> CompletableFuture.allOf(f1, f2))
            .orElse(CompletableFuture.completedFuture(null))
            .join();
    }


    private CompletableFuture<Void> retrieveAndDeleteIndices(CassandraMessageId messageId, Optional<CassandraId> mailboxId) {
        return imapUidDAO.retrieve(messageId, mailboxId)
            .thenCompose(composedMessageIds -> composedMessageIds
                .map(this::deleteIds)
                .reduce((f1, f2) -> CompletableFuture.allOf(f1, f2))
                .orElse(CompletableFuture.completedFuture(null)));
    }

    @Override
    public void delete(MessageId messageId) {
        CassandraMessageId cassandraMessageId = (CassandraMessageId) messageId;
        retrieveAndDeleteIndices(cassandraMessageId, Optional.empty())
            .thenCompose(voidValue -> messageDAO.delete(cassandraMessageId))
            .join();
    }

    private CompletableFuture<Void> deleteIds(ComposedMessageIdWithMetaData metaData) {
        CassandraMessageId messageId = (CassandraMessageId) metaData.getComposedMessageId().getMessageId();
        CassandraId mailboxId = (CassandraId) metaData.getComposedMessageId().getMailboxId();
        return CompletableFuture.allOf(
            imapUidDAO.delete(messageId, mailboxId),
            messageIdDAO.delete(mailboxId, metaData.getComposedMessageId().getUid()))
            .thenCompose(voidValue -> indexTableHandler.updateIndexOnDelete(metaData, mailboxId));
    }

    @Override
    public Map<MailboxId, UpdatedFlags> setFlags(MessageId messageId, List<MailboxId> mailboxIds, Flags newState, MessageManager.FlagsUpdateMode updateMode) throws MailboxException {
        FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(newState, updateMode);
        Set<CassandraId> cassandraIds = mailboxIds.stream()
            .map(mailboxId -> (CassandraId) mailboxId)
            .collect(Guavate.toImmutableSet());

        ImmutableMap<CassandraId, Long> modseqs = generateModSeqs(cassandraIds);

        return FluentFutureStream.of(cassandraIds.stream()
                .map(mailboxId -> imapUidDAO.retrieve((CassandraMessageId) messageId, Optional.of(mailboxId))
                    .thenApply(Stream::findAny)))
            .flatMap(OptionalConverter::toStream)
            .thenComposeOnAll(composedId ->
                flagsUpdateWithRetry(flagsUpdateCalculator, composedId, modseqs.get(composedId.getComposedMessageId().getMailboxId()))
                    .thenCompose(this::updateCounts))
            .join()
            .collect(Guavate.entriesToImmutableMap());
    }

    private ImmutableMap<CassandraId, Long> generateModSeqs(Set<CassandraId> cassandraIds) throws MailboxException {
        ImmutableMap<CassandraId, Optional<Long>> modseqsOptionals = CompletableFutureUtil.allOf(
            cassandraIds.stream()
                .map(cassandraId -> modSeqProvider.nextModSeq(cassandraId)
                    .thenApply(modSeq -> Pair.of(cassandraId, modSeq))))
            .join()
            .collect(Guavate.entriesToImmutableMap());

        Optional<CassandraId> cassandraIdWithoutModSeq = modseqsOptionals.entrySet().stream()
            .filter(entry -> !entry.getValue().isPresent())
            .findAny()
            .map(Map.Entry::getKey);
        if (cassandraIdWithoutModSeq.isPresent()) {
            throw new MailboxException("Can not generate modSeq for " + cassandraIdWithoutModSeq.get());
        }
        return modseqsOptionals.entrySet().stream()
            .map(entry -> Pair.of(entry.getKey(), entry.getValue().get()))
            .collect(Guavate.entriesToImmutableMap());
    }

    private CompletableFuture<Pair<MailboxId, UpdatedFlags>> flagsUpdateWithRetry(FlagsUpdateCalculator flagsUpdateCalculator,
                                                               ComposedMessageIdWithMetaData composedId, long newModSeq) {
        UpdatedFlags.Builder updatedFlagsBuilder = UpdatedFlags.builder()
            .uid(composedId.getComposedMessageId().getUid())
            .oldFlags(composedId.getFlags())
            .newFlags(flagsUpdateCalculator.buildNewFlags(composedId.getFlags()));

        if (flagsUpdateCalculator.buildNewFlags(composedId.getFlags()).equals(composedId.getFlags())) {
            return CompletableFuture.completedFuture(
                Pair.of(composedId.getComposedMessageId().getMailboxId(),
                    updatedFlagsBuilder
                        .modSeq(composedId.getModSeq())
                        .build()));
        }
        return updateFlags(composedId, flagsUpdateCalculator, newModSeq)
            .thenApply(any ->
                Pair.of(composedId.getComposedMessageId().getMailboxId(),
                    updatedFlagsBuilder
                        .modSeq(newModSeq)
                        .build()));
    }

    private CompletableFuture<Void> updateFlags(ComposedMessageIdWithMetaData composedId, FlagsUpdateCalculator flagsUpdateCalculator,
                             long nextModSeq) {
        ComposedMessageIdWithMetaData composedIdWithNewModSeq = new ComposedMessageIdWithMetaData(
            composedId.getComposedMessageId(),
            composedId.getFlags(),
            nextModSeq);

        return imapUidDAO.updateMetadata(composedIdWithNewModSeq, flagsUpdateCalculator)
            .thenCompose(any -> messageIdDAO.updateMetadata(composedIdWithNewModSeq, flagsUpdateCalculator));
    }

    private CompletableFuture<Pair<MailboxId, UpdatedFlags>> updateCounts(Pair<MailboxId, UpdatedFlags> pair) {
        CassandraId cassandraId = (CassandraId) pair.getLeft();
        return indexTableHandler.updateIndexOnFlagsUpdate(cassandraId, pair.getRight())
            .thenApply(voidValue -> pair);
    }
}
