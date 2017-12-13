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

package org.apache.james.jmap.methods;

import static org.apache.james.jmap.methods.Method.JMAP_PREFIX;
import static org.apache.james.jmap.methods.Pipeline.endWith;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.apache.james.jmap.methods.Pipeline.MailboxConditionSupplier;
import org.apache.james.jmap.methods.ValueWithId.CreationMessageEntry;
import org.apache.james.jmap.methods.ValueWithId.MessageWithId;
import org.apache.james.jmap.model.BlobId;
import org.apache.james.jmap.model.CreationMessage;
import org.apache.james.jmap.model.CreationMessage.DraftEmailer;
import org.apache.james.jmap.model.Envelope;
import org.apache.james.jmap.model.Keyword;
import org.apache.james.jmap.model.Message;
import org.apache.james.jmap.model.MessageFactory;
import org.apache.james.jmap.model.MessageFactory.MetaDataWithContent;
import org.apache.james.jmap.model.MessageProperties;
import org.apache.james.jmap.model.MessageProperties.MessageProperty;
import org.apache.james.jmap.model.SetError;
import org.apache.james.jmap.model.SetMessagesError;
import org.apache.james.jmap.model.SetMessagesRequest;
import org.apache.james.jmap.model.SetMessagesResponse;
import org.apache.james.jmap.model.SetMessagesResponse.Builder;
import org.apache.james.jmap.model.mailbox.Role;
import org.apache.james.jmap.utils.SystemMailboxesProvider;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.metrics.api.TimeMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.functions.FunctionChainer;
import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;


public class SetMessagesCreationProcessor implements SetMessagesProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SetMailboxesCreationProcessor.class);
    private final MessageFactory messageFactory;
    private final SystemMailboxesProvider systemMailboxesProvider;
    private final AttachmentChecker attachmentChecker;
    private final MetricFactory metricFactory;
    private final MailboxManager mailboxManager;
    private final MailboxId.Factory mailboxIdFactory;
    private final MessageAppender messageAppender;
    private final MessageSender messageSender;
    
    @VisibleForTesting @Inject
    SetMessagesCreationProcessor(MessageFactory messageFactory,
                                 SystemMailboxesProvider systemMailboxesProvider,
                                 AttachmentChecker attachmentChecker,
                                 MetricFactory metricFactory,
                                 MailboxManager mailboxManager,
                                 MailboxId.Factory mailboxIdFactory, MessageAppender messageAppender, MessageSender messageSender) {
        this.messageFactory = messageFactory;
        this.systemMailboxesProvider = systemMailboxesProvider;
        this.attachmentChecker = attachmentChecker;
        this.metricFactory = metricFactory;
        this.mailboxManager = mailboxManager;
        this.mailboxIdFactory = mailboxIdFactory;
        this.messageAppender = messageAppender;
        this.messageSender = messageSender;
    }

    @Override
    public SetMessagesResponse process(SetMessagesRequest request, MailboxSession mailboxSession) {
        TimeMetric timeMetric = metricFactory.timer(JMAP_PREFIX + "SetMessageCreationProcessor");

        Builder responseBuilder = SetMessagesResponse.builder();
        request.getCreate()
            .forEach(create -> handleCreate(create, responseBuilder, mailboxSession));

        timeMetric.stopAndPublish();
        return responseBuilder.build();
    }

    private Builder handleCreate(CreationMessageEntry create, Builder responseBuilder, MailboxSession mailboxSession) {
        try {
            List<MailboxId> mailboxIds = toMailboxIds(create);
            return Pipeline
                .forOperations(
                    when(mailboxIds.isEmpty())
                        .then(invalidEmptyMailboxIds(create)),
                    whenNot(allMailboxOwned(mailboxIds, mailboxSession))
                        .then(invalidMailboxOwner(create)),
                    when(isTargeting(create, mailboxSession, Role.OUTBOX))
                        .then(outboxPipeline(create, mailboxSession)),
                    when(isDraftSaving(create))
                        .then(saveDraftPipeline(create, mailboxSession)),
                    when(isTargeting(create, mailboxSession, Role.DRAFTS))
                        .then(invalidDraftFlag(create)),
                    endWith(invalidMailboxIds(create)))
                .executeFirst(responseBuilder);
        } catch (MailboxNotFoundException e) {
            return responseBuilder.notCreated(create.getCreationId(),
                    SetError.builder()
                        .type("error")
                        .description(e.getMessage())
                        .build());

        } catch (MailboxException | MessagingException e) {
            LOG.error("Unexpected error while creating message", e);
            return responseBuilder.notCreated(create.getCreationId(),
                    SetError.builder()
                        .type("error")
                        .description("unexpected error")
                        .build());
        }
    }

    private MailboxConditionSupplier isDraftSaving(CreationMessageEntry create) {
        return () -> isDraft(create.getValue());
    }

    private MailboxConditionSupplier isTargeting(CreationMessageEntry create, MailboxSession mailboxSession, Role role) {
        return () -> isAppendToMailboxWithRole(role, create.getValue(), mailboxSession);
    }

    private ImmutableList<MailboxId> toMailboxIds(CreationMessageEntry create) {
        return create.getValue().getMailboxIds()
            .stream()
            .distinct()
            .map(mailboxIdFactory::fromString)
            .collect(Guavate.toImmutableList());
    }

    private Pipeline<Builder> outboxPipeline(CreationMessageEntry entry, MailboxSession session) throws MailboxException, MessagingException {
        CreationMessage creationMessage = entry.getValue();
        return Pipeline.forOperations(
            whenNot(creationMessage.isValid())
                .then(invalidMessage(entry)),
            whenNot(isSender(creationMessage.getFrom(), session))
                .then(invalidSender(entry, session)),
            checkAttachmentStep(entry, session),
            endWith(builder -> sendMail(entry, builder, session)));
    }

    private Pipeline<Builder> saveDraftPipeline(CreationMessageEntry entry, MailboxSession session) throws MailboxException, MessagingException {
        return Pipeline.forOperations(
            when(() -> isTargetingAMailboxWithRole(Role.OUTBOX, entry, session))
                .then(invalidMailboxIds(entry)),
            checkAttachmentStep(entry, session),
            Pipeline.endWith(builder -> {
                MessageWithId created = handleDraftMessages(entry, session);
                return builder.created(created.getCreationId(), created.getValue());
            }));
    }

    private Builder sendMail(CreationMessageEntry entry, Builder responseBuilder, MailboxSession session) throws MailboxException, MessagingException {
        MetaDataWithContent newMessage = messageAppender.appendMessageInMailboxes(entry, toMailboxIds(entry), session);
        Message jmapMessage = messageFactory.fromMetaDataWithContent(newMessage);
        Envelope envelope = Envelope.fromMessage(jmapMessage);
        messageSender.sendMessage(newMessage, envelope, session);
        MessageWithId created = new MessageWithId(entry.getCreationId(), jmapMessage);
        return responseBuilder.created(created.getCreationId(), created.getValue());
    }

    private Boolean isDraft(CreationMessage creationMessage) {
        if (creationMessage.getOldKeyword().isPresent()) {
            return creationMessage.getOldKeyword().get()
                        .isDraft()
                        .orElse(false);
        }
        return creationMessage
            .getKeywords()
            .map(keywords -> keywords.contains(Keyword.DRAFT))
            .orElse(false);
    }

    private MessageWithId handleDraftMessages(CreationMessageEntry entry, MailboxSession session) throws MailboxException, MessagingException {
        MetaDataWithContent newMessage = messageAppender.appendMessageInMailboxes(entry, toMailboxIds(entry), session);
        Message jmapMessage = messageFactory.fromMetaDataWithContent(newMessage);
        return new ValueWithId.MessageWithId(entry.getCreationId(), jmapMessage);
    }
    
    private boolean isAppendToMailboxWithRole(Role role, CreationMessage entry, MailboxSession mailboxSession) throws MailboxException {
        return getMailboxWithRole(mailboxSession, role)
                .map(entry::isOnlyIn)
                .orElse(false);
    }

    private boolean isTargetingAMailboxWithRole(Role role, CreationMessageEntry entry, MailboxSession mailboxSession) throws MailboxException {
        return getMailboxWithRole(mailboxSession, role)
                .map(entry.getValue()::isIn)
                .orElse(false);
    }

    private Optional<MessageManager> getMailboxWithRole(MailboxSession mailboxSession, Role role) throws MailboxException {
        return systemMailboxesProvider.getMailboxByRole(role, mailboxSession).findFirst();
    }

    @VisibleForTesting
    boolean allMailboxOwned(List<MailboxId> mailboxIds, MailboxSession session) {
        FunctionChainer<MailboxId, MessageManager> findMailbox = Throwing.function(mailboxId -> mailboxManager.getMailbox(mailboxId, session));
        return mailboxIds.stream()
            .map(findMailbox.sneakyThrow())
            .map(Throwing.function(MessageManager::getMailboxPath))
            .allMatch(path -> path.belongsTo(session));
    }

    private boolean isSender(Optional<DraftEmailer> from, MailboxSession session) {
        return from.flatMap(DraftEmailer::getEmail)
            .filter(email -> session.getUser().isSameUser(email))
            .isPresent();
    }

    private Set<MessageProperties.MessageProperty> collectMessageProperties(List<ValidationResult> validationErrors) {
        Splitter propertiesSplitter = Splitter.on(',').trimResults().omitEmptyStrings();
        return validationErrors.stream()
                .flatMap(err -> propertiesSplitter.splitToList(err.getProperty()).stream())
                .flatMap(MessageProperty::find)
                .collect(Collectors.toSet());
    }

    private Pipeline.ConditionalStep<Builder> checkAttachmentStep(CreationMessageEntry entry, MailboxSession session) throws MailboxException {
        List<BlobId> attachmentNotFound = attachmentChecker.listAttachmentNotFounds(entry, session);
        return whenNot(attachmentNotFound.isEmpty())
            .then(invalidAttachments(entry, attachmentNotFound));
    }

    private Pipeline.Operation<Builder> invalidAttachments(CreationMessageEntry entry, List<BlobId> attachmentNotFound) {
        return builder -> builder.notCreated(entry.getCreationId(),
            SetMessagesError.builder()
                .type("invalidProperties")
                .properties(MessageProperty.attachments)
                .attachmentsNotFound(attachmentNotFound)
                .description("Attachment not found")
                .build());
    }

    private Pipeline.Operation<Builder> invalidSender(CreationMessageEntry entry, MailboxSession session) {
        String allowedSender = session.getUser().getUserName();
        return builder -> builder.notCreated(entry.getCreationId(),
            SetError.builder()
                .type("invalidProperties")
                .properties(MessageProperty.from)
                .description("Invalid 'from' field. Must be " +
                    allowedSender)
                .build());
    }

    private Pipeline.Operation<Builder> invalidMessage(CreationMessageEntry entry) {
        return builder -> builder.notCreated(entry.getCreationId(),
            buildSetErrorFromValidationResult(entry.getValue().validate()));
    }

    private SetError buildSetErrorFromValidationResult(List<ValidationResult> validationErrors) {
        return SetError.builder()
            .type("invalidProperties")
            .properties(collectMessageProperties(validationErrors))
            .description(formatValidationErrorMessge(validationErrors))
            .build();
    }

    private String formatValidationErrorMessge(List<ValidationResult> validationErrors) {
        return validationErrors.stream()
            .map(err -> err.getProperty() + ": " + err.getErrorMessage())
            .collect(Collectors.joining("\\n"));
    }

    private Pipeline.Operation<Builder> invalidMailboxOwner(CreationMessageEntry create) {
        return builder -> {
            LOG.error("Appending message in an unknown mailbox");
            return builder.notCreated(create.getCreationId(),
                SetError.builder()
                    .type("error")
                    .properties(MessageProperty.mailboxIds)
                    .description("MailboxId invalid")
                    .build());
        };
    }

    private Pipeline.Operation<Builder> invalidEmptyMailboxIds(CreationMessageEntry create) {
        return builder -> builder.notCreated(create.getCreationId(),
            SetError.builder()
                .type("invalidProperties")
                .properties(MessageProperty.mailboxIds)
                .description("Message needs to be in at least one mailbox")
                .build());
    }

    private Pipeline.Operation<Builder> invalidDraftFlag(CreationMessageEntry entry) {
        return responseBuilder -> responseBuilder.notCreated(entry.getCreationId(),
            SetError.builder()
                .type("invalidProperties")
                .properties(MessageProperty.keywords)
                .description("A draft message should be flagged as Draft")
                .build());
    }

    private Pipeline.Operation<Builder> invalidMailboxIds(CreationMessageEntry entry) {
        return responseBuilder -> responseBuilder.notCreated(entry.getCreationId(),
            SetError.builder()
                .type("invalidProperties")
                .properties(MessageProperty.mailboxIds)
                .description("Message creation is only supported in mailboxes with role Draft and Outbox")
                .build());
    }

    private Pipeline.ConditionalStep.Factory<Builder> when(boolean b) {
        return Pipeline.when(b);
    }

    private Pipeline.ConditionalStep.Factory<Builder> whenNot(boolean b) {
        return Pipeline.when(!b);
    }

    private Pipeline.ConditionalStep.Factory<Builder> when(MailboxConditionSupplier condition) {
        return Pipeline.when(condition);
    }

}
