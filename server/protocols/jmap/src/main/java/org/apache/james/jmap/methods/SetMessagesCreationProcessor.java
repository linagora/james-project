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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.SharedInputStream;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.james.jmap.exceptions.MailboxRoleNotFoundException;
import org.apache.james.jmap.methods.ValueWithId.CreationMessageEntry;
import org.apache.james.jmap.methods.ValueWithId.ErrorWithId;
import org.apache.james.jmap.methods.ValueWithId.MessageWithId;
import org.apache.james.jmap.model.CreationMessage;
import org.apache.james.jmap.model.CreationMessageId;
import org.apache.james.jmap.model.Message;
import org.apache.james.jmap.model.MessageId;
import org.apache.james.jmap.model.MessageProperties;
import org.apache.james.jmap.model.MessageProperties.MessageProperty;
import org.apache.james.jmap.model.SetError;
import org.apache.james.jmap.model.SetMessagesRequest;
import org.apache.james.jmap.model.SetMessagesResponse;
import org.apache.james.jmap.model.SetMessagesResponse.Builder;
import org.apache.james.jmap.model.mailbox.Role;
import org.apache.james.jmap.send.MailFactory;
import org.apache.james.jmap.send.MailMetadata;
import org.apache.james.jmap.send.MailSpool;
import org.apache.james.jmap.utils.SystemMailboxesProvider;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;
import org.apache.mailet.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import no.finntech.lambdacompanion.Either;

public class SetMessagesCreationProcessor implements SetMessagesProcessor {


    private static final SetError UNEXPECTED_ERROR = SetError.builder().description("unexpected error").type("error").build();
    private static final Logger LOGGER = LoggerFactory.getLogger(SetMessagesCreationProcessor.class);

    private final MailboxSessionMapperFactory mailboxSessionMapperFactory;
    private final MIMEMessageConverter mimeMessageConverter;
    private final MailSpool mailSpool;
    private final MailFactory mailFactory;
    private final SystemMailboxesProvider systemMailboxesProvider;

    
    @VisibleForTesting @Inject
    SetMessagesCreationProcessor(MailboxSessionMapperFactory mailboxSessionMapperFactory,
                                 MIMEMessageConverter mimeMessageConverter,
                                 MailSpool mailSpool,
                                 MailFactory mailFactory,
                                 SystemMailboxesProvider systemMailboxesProvider) {
        this.mailboxSessionMapperFactory = mailboxSessionMapperFactory;
        this.mimeMessageConverter = mimeMessageConverter;
        this.mailSpool = mailSpool;
        this.mailFactory = mailFactory;
        this.systemMailboxesProvider = systemMailboxesProvider;
    }

    @Override
    public SetMessagesResponse process(SetMessagesRequest request, MailboxSession mailboxSession) {
        List<CreationMessageEntry> creationRequestEntries = request.getCreate();
        
        Builder responseBuilder = SetMessagesResponse.builder();
        creationRequestEntries.stream()
            .map(entry -> processEntry(entry, mailboxSession))
            .forEach(either -> either.fold(
                    error -> responseBuilder.notCreated(error.getCreationId(), error.getValue()),
                    result -> responseBuilder.created(result.getCreationId(), result.getValue())));
        return responseBuilder.build();
    }

    private Either<ErrorWithId, MessageWithId> processEntry(CreationMessageEntry entry, MailboxSession mailboxSession) {
        return Either.<ErrorWithId, CreationMessageEntry>right(entry)
            .joinRight(x -> notImplementedErrors(x, mailboxSession))
            .joinRight(x -> getInvalidArgumentsErrors(x))
            .joinRight(x -> notAllowedErrors(x, mailboxSession))
            .joinRight(x -> handleOutboxMessages(x, mailboxSession));
    }

    private Either<ErrorWithId, CreationMessageEntry> notImplementedErrors(CreationMessageEntry entry, MailboxSession session) {
        if (!isMessageSetIn(Role.DRAFTS, entry.getValue(), session)) {
            return Either.right(entry);
        } else {
            return Either.left(
                    new ErrorWithId(
                            entry.getCreationId(), 
                            SetError.builder().type("error").description("Not yet implemented").build()));
        }
    }
    
    private Either<ErrorWithId, CreationMessageEntry> getInvalidArgumentsErrors(CreationMessageEntry entry) {
        CreationMessage message = entry.getValue();
        if (!message.isValid()) {
            return Either.left(new ErrorWithId(entry.getCreationId(), buildSetErrorFromValidationResult(message.validate())));
        }
        return Either.right(entry);
    }
    
    private Either<ErrorWithId, CreationMessageEntry> notAllowedErrors(CreationMessageEntry entry, MailboxSession session) {
        List<String> allowedSenders = ImmutableList.of(session.getUser().getUserName());
        if (isAllowedFromAddress(entry.getValue(), allowedSenders)) {
            return Either.right(entry);
        } else {
            return Either.left(
                    new ErrorWithId(
                            entry.getCreationId(), 
                            SetError.builder()
                                .type("invalidProperties")
                                .properties(ImmutableSet.of(MessageProperty.from))
                                .description("Invalid 'from' field. Must be one of " + allowedSenders)
                                .build()));
        }
    }
    
    private boolean isAllowedFromAddress(CreationMessage creationMessage, List<String> allowedFromMailAddresses) {
        return creationMessage.getFrom()
                .map(draftEmailer -> draftEmailer.getEmail()
                        .map(allowedFromMailAddresses::contains)
                        .orElse(false))
                .orElse(false);
    }

    
    private Either<ErrorWithId, MessageWithId> handleOutboxMessages(CreationMessageEntry entry, MailboxSession session) {
        CreationMessageId creationId = entry.getCreationId();

        return getMailboxWithRole(session, Role.OUTBOX)
            .map(outbox -> {
                if (isRequestForSending(entry.getValue(), session)) {
                    Function<Long, MessageId> idGenerator = uid -> generateMessageId(session, outbox, uid);
                    return createMessageInOutboxAndSend(entry, session, outbox, idGenerator);
                }
                return Either.<ErrorWithId, MessageWithId>left(
                        new ErrorWithId(creationId, 
                                SetError.builder().description("unhandled request").type("unhandled").build()));
                
            }).orElse(Either.left(new ErrorWithId(creationId, UNEXPECTED_ERROR)));
    }
    
    @VisibleForTesting
    protected Either<ErrorWithId, MessageWithId> createMessageInOutboxAndSend(CreationMessageEntry createdEntry,
                                                           MailboxSession session,
                                                           Mailbox outbox, Function<Long, MessageId> buildMessageIdFromUid) {
        CreationMessageId creationId = createdEntry.getCreationId();
        try {
            MessageMapper messageMapper = mailboxSessionMapperFactory.createMessageMapper(session);
            MailboxMessage newMailboxMessage = buildMailboxMessage(createdEntry, outbox);
            messageMapper.add(outbox, newMailboxMessage);
            Message jmapMessage = Message.fromMailboxMessage(newMailboxMessage, ImmutableList.of(), buildMessageIdFromUid);
            sendMessage(newMailboxMessage, jmapMessage, session);
            return Either.right(new MessageWithId(creationId, jmapMessage));
            
        } catch (MessagingException | MailboxException e) {
            return Either.left(new ErrorWithId(creationId, UNEXPECTED_ERROR));
            
        } catch (MailboxRoleNotFoundException e) {
            LOGGER.error("Could not find mailbox '%s' while trying to save message.", e.getRole().serialize());
            return Either.left(new ErrorWithId(creationId, UNEXPECTED_ERROR));
        }
    }
    
    private boolean isMessageSetIn(Role role, CreationMessage entry, MailboxSession mailboxSession) {
        return getMailboxWithRole(mailboxSession, role)
                .map(box -> entry.isIn(box))
                .orElse(false);
    }

    private Optional<Mailbox> getMailboxWithRole(MailboxSession mailboxSession, Role role) {
        return systemMailboxesProvider.listMailboxes(role, mailboxSession).findFirst();
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

    private Set<MessageProperties.MessageProperty> collectMessageProperties(List<ValidationResult> validationErrors) {
        Splitter propertiesSplitter = Splitter.on(',').trimResults().omitEmptyStrings();
        return validationErrors.stream()
                .flatMap(err -> propertiesSplitter.splitToList(err.getProperty()).stream())
                .flatMap(MessageProperty::find)
                .collect(Collectors.toSet());
    }

    private boolean isRequestForSending(CreationMessage creationMessage, MailboxSession session) {
        return isMessageSetIn(Role.OUTBOX, creationMessage, session);
    }
    
    private MessageId generateMessageId(MailboxSession session, Mailbox outbox, long uid) {
        MailboxPath outboxPath = new MailboxPath(session.getPersonalSpace(), session.getUser().getUserName(), outbox.getName());
        return new MessageId(session.getUser(), outboxPath, uid);
    }

    private MailboxMessage buildMailboxMessage(MessageWithId.CreationMessageEntry createdEntry, Mailbox outbox) {
        byte[] messageContent = mimeMessageConverter.convert(createdEntry);
        SharedInputStream content = new SharedByteArrayInputStream(messageContent);
        long size = messageContent.length;
        int bodyStartOctet = 0;

        Flags flags = getMessageFlags(createdEntry.getValue());
        PropertyBuilder propertyBuilder = buildPropertyBuilder();
        MailboxId mailboxId = outbox.getMailboxId();
        Date internalDate = Date.from(createdEntry.getValue().getDate().toInstant());

        return new SimpleMailboxMessage(internalDate, size,
                bodyStartOctet, content, flags, propertyBuilder, mailboxId);
    }

    private PropertyBuilder buildPropertyBuilder() {
        return new PropertyBuilder();
    }

    private Flags getMessageFlags(CreationMessage message) {
        Flags result = new Flags();
        if (!message.isIsUnread()) {
            result.add(Flags.Flag.SEEN);
        }
        if (message.isIsFlagged()) {
            result.add(Flags.Flag.FLAGGED);
        }
        if (message.isIsAnswered() || message.getInReplyToMessageId().isPresent()) {
            result.add(Flags.Flag.ANSWERED);
        }
        if (message.isIsDraft()) {
            result.add(Flags.Flag.DRAFT);
        }
        return result;
    }

    private void sendMessage(MailboxMessage mailboxMessage, Message jmapMessage, MailboxSession session) throws MessagingException {
        try {
            Mail mail = mailFactory.build(mailboxMessage, jmapMessage);
            MailMetadata metadata = new MailMetadata(jmapMessage.getId(), session.getUser().getUserName());
            mailSpool.send(mail, metadata);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }
}
