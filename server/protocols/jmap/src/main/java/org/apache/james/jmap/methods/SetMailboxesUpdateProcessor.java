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
import static org.apache.james.jmap.methods.Pipeline.nested;

import java.util.Optional;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.apache.james.jmap.exceptions.MailboxParentNotFoundException;
import org.apache.james.jmap.model.MailboxFactory;
import org.apache.james.jmap.model.SetError;
import org.apache.james.jmap.model.SetMailboxesRequest;
import org.apache.james.jmap.model.SetMailboxesResponse;
import org.apache.james.jmap.model.SetMailboxesResponse.Builder;
import org.apache.james.jmap.model.mailbox.Mailbox;
import org.apache.james.jmap.model.mailbox.MailboxUpdateRequest;
import org.apache.james.jmap.model.mailbox.Rights.Username;
import org.apache.james.jmap.model.mailbox.Role;
import org.apache.james.jmap.utils.MailboxUtils;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.exception.DifferentDomainException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxExistsException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.exception.TooLongMailboxNameException;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.metrics.api.TimeMetric;
import org.apache.james.util.OptionalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.functions.ThrowingFunction;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;

public class SetMailboxesUpdateProcessor implements SetMailboxesProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetMailboxesUpdateProcessor.class);
    private final MailboxUtils mailboxUtils;
    private final MailboxManager mailboxManager;
    private final MailboxFactory mailboxFactory;
    private final MetricFactory metricFactory;
    private final SubscriptionManager subscriptionManager;

    @Inject
    @VisibleForTesting
    SetMailboxesUpdateProcessor(MailboxUtils mailboxUtils, MailboxManager mailboxManager, SubscriptionManager subscriptionManager, MailboxFactory mailboxFactory, MetricFactory metricFactory) {
        this.mailboxUtils = mailboxUtils;
        this.mailboxManager = mailboxManager;
        this.subscriptionManager = subscriptionManager;
        this.mailboxFactory = mailboxFactory;
        this.metricFactory = metricFactory;
    }

    @Override
    public SetMailboxesResponse process(SetMailboxesRequest request, MailboxSession mailboxSession) {
        TimeMetric timeMetric = metricFactory.timer(JMAP_PREFIX + "mailboxUpdateProcessor");

        SetMailboxesResponse.Builder responseBuilder = SetMailboxesResponse.builder();
        request.getUpdate()
            .forEach((key, value) -> handleUpdate(key, value, responseBuilder, mailboxSession));
        timeMetric.stopAndPublish();
        return responseBuilder.build();
    }

    private Builder handleUpdate(MailboxId mailboxId, MailboxUpdateRequest updateRequest, Builder responseBuilder, MailboxSession mailboxSession) {
        try {
            MessageManager messageManager = mailboxManager.getMailbox(mailboxId, mailboxSession);
            Mailbox mailbox = mailboxFactory.fromMessageManager(messageManager, mailboxSession);
            char pathDelimiter = mailboxSession.getPathDelimiter();
            MailboxPath originMailboxPath = messageManager.getMailboxPath();

            return Pipeline.forOperations(
                when(nameContainsPathDelimiter(updateRequest, pathDelimiter))
                    .then(invalidMailboxNameContainsDelimiter(mailboxId, updateRequest, pathDelimiter)),
                when(nameMatchesSystemMailbox(updateRequest))
                    .then(invalidSystemMailboxName(mailboxId, updateRequest)),
                when(sharingOutbox(mailbox, updateRequest))
                    .then(invalidArgument(mailboxId, "Sharing 'Outbox' is forbidden")),
                when(sharingDraft(mailbox, updateRequest))
                    .then(invalidArgument(mailboxId, "Sharing 'Drafts' is forbidden")),
                when(isForbiddenSystemMailboxUpdate(mailbox, updateRequest))
                    .then(invalidSystemMailboxUpdate(mailboxId)),
                validateParentStep(mailbox, updateRequest, mailboxSession),
                endWith(nested(
                    updateMailboxPipeline(mailbox, updateRequest, originMailboxPath, mailboxSession))))
                .executeFirst(responseBuilder);
        } catch (TooLongMailboxNameException e) {
            return responseBuilder.notUpdated(mailboxId, SetError.builder()
                .type("invalidArguments")
                .description("The mailbox name length is too long")
                .build());
        } catch (MailboxNotFoundException e) {
            return responseBuilder.notUpdated(mailboxId, SetError.builder()
                    .type("notFound")
                    .description(String.format("The mailbox '%s' was not found", mailboxId.serialize()))
                    .build());
        } catch (MailboxParentNotFoundException e) {
            return responseBuilder.notUpdated(mailboxId, SetError.builder()
                    .type("notFound")
                    .description(String.format("The parent mailbox '%s' was not found.", e.getParentId()))
                    .build());
        } catch (MailboxExistsException e) {
            return responseBuilder.notUpdated(mailboxId, SetError.builder()
                    .type("invalidArguments")
                    .description("Cannot rename a mailbox to an already existing mailbox.")
                    .build());
        } catch (DifferentDomainException e) {
            return responseBuilder.notUpdated(mailboxId, SetError.builder()
                .type("invalidArguments")
                .description("Cannot share a mailbox to another domain")
                .build());
        } catch (IllegalArgumentException e) {
            return responseBuilder.notUpdated(mailboxId, SetError.builder()
                .type("invalidArguments")
                .description(e.getMessage())
                .build());
        } catch (MailboxException e) {
            LOGGER.error("Error while updating mailbox", e);
            return responseBuilder.notUpdated(mailboxId, SetError.builder()
                    .type( "anErrorOccurred")
                    .description("An error occurred when updating the mailbox")
                    .build());
        } catch (MessagingException e) {
            throw Throwables.propagate(e);
        }
    }

    private Pipeline.Step<Builder> updateMailboxPipeline(Mailbox mailbox, MailboxUpdateRequest updateRequest, MailboxPath originMailboxPath, MailboxSession mailboxSession) throws MailboxException, MessagingException {
        MailboxPath destinationMailboxPath = computeNewMailboxPath(mailbox, originMailboxPath, updateRequest, mailboxSession);

        Pipeline.Operation<Builder> finalOperation = builder -> builder.updated(mailbox.getId());
        return Pipeline
            .forOperations(
                when(updateRequest.getSharedWith().isPresent())
                    .then(setRights(updateRequest, mailboxSession, originMailboxPath)),
                whenNot(originMailboxPath.equals(destinationMailboxPath))
                    .then(rename(mailboxSession, originMailboxPath, destinationMailboxPath)),
                endWith(finalOperation))
            .executeAll();
    }

    private boolean sharingDraft(Mailbox mailbox, MailboxUpdateRequest updateRequest) {
        return sharingMailboxWithRole(mailbox, updateRequest, Role.DRAFTS);
    }

    private boolean sharingOutbox(Mailbox mailbox, MailboxUpdateRequest updateRequest) {
        return sharingMailboxWithRole(mailbox, updateRequest, Role.OUTBOX);
    }

    private boolean sharingMailboxWithRole(Mailbox mailbox, MailboxUpdateRequest updateRequest, Role role) {
        return !updateRequest.getSharedWith().isPresent() || !mailbox.hasRole(role);
    }

    private boolean isForbiddenSystemMailboxUpdate(Mailbox mailbox, MailboxUpdateRequest updateRequest) {
        return mailbox.hasSystemRole() && hasForbiddenSystemMailboxUpdate(mailbox, updateRequest);
    }

    private boolean hasForbiddenSystemMailboxUpdate(Mailbox mailbox, MailboxUpdateRequest updateRequest) {
        return OptionalUtils.containsDifferent(updateRequest.getName(), mailbox.getName())
            || requestChanged(updateRequest.getParentId(), mailbox.getParentId())
            || requestChanged(updateRequest.getRole(), mailbox.getRole())
            || OptionalUtils.containsDifferent(updateRequest.getSortOrder(), mailbox.getSortOrder());
    }

    @VisibleForTesting
    <T> boolean requestChanged(Optional<T> requestValue, Optional<T> storeValue) {
        return requestValue
            .filter(value -> !requestValue.equals(storeValue))
            .isPresent();
    }

    private boolean nameMatchesSystemMailbox(MailboxUpdateRequest updateRequest) {
        return updateRequest.getName()
                .flatMap(Role::from)
                .filter(Role::isSystemRole)
                .isPresent();
    }

    private boolean nameContainsPathDelimiter(MailboxUpdateRequest updateRequest, char pathDelimiter) {
        return updateRequest.getName()
                .filter(name -> name.contains(String.valueOf(pathDelimiter)))
                .isPresent() ;
    }

    private Pipeline.Step<Builder> validateParentStep(Mailbox mailbox, MailboxUpdateRequest updateRequest, MailboxSession mailboxSession) {
        return builder -> {
            if (isParentIdInRequest(updateRequest)) {
                MailboxId newParentId = updateRequest.getParentId().get();
                MessageManager parent = retrieveParent(mailboxSession, newParentId);
                if (mustChangeParent(mailbox.getParentId(), newParentId)) {
                    if (hasChildren(mailbox, mailboxSession)) {
                        return Optional.of(builder.notUpdated(mailbox.getId(), SetError.builder()
                            .type("invalidArguments")
                            .description("Cannot update a parent mailbox.")
                            .build()));
                    }
                    if (belongsToOtherUser(mailboxSession, parent)) {
                        return Optional.of(builder.notUpdated(mailbox.getId(), SetError.builder()
                            .type("invalidArguments")
                            .description("Parent mailbox is not owned.")
                            .build()));
                    }
                }
            }
            return Optional.empty();
        };
    }

    private boolean belongsToOtherUser(MailboxSession mailboxSession, MessageManager parent) throws MailboxException {
        return !parent.getMailboxPath().belongsTo(mailboxSession);
    }

    private boolean hasChildren(Mailbox mailbox, MailboxSession mailboxSession) throws MailboxException {
        return mailboxUtils.hasChildren(mailbox.getId(), mailboxSession);
    }

    private MessageManager retrieveParent(MailboxSession mailboxSession, MailboxId newParentId) throws MailboxException {
        try {
            return mailboxManager.getMailbox(newParentId, mailboxSession);
        } catch (MailboxNotFoundException e) {
            throw new MailboxParentNotFoundException(newParentId);
        }
    }

    private boolean isParentIdInRequest(MailboxUpdateRequest updateRequest) {
        return updateRequest.getParentId() != null
                && updateRequest.getParentId().isPresent();
    }

    private boolean mustChangeParent(Optional<MailboxId> currentParentId, MailboxId newParentId) {
        return currentParentId
                .map(x -> ! x.equals(newParentId))
                .orElse(true);
    }

    private Pipeline.Operation<Builder> rename(MailboxSession mailboxSession, MailboxPath originMailboxPath, MailboxPath destinationMailboxPath) throws MailboxException {
        return builder -> {
            mailboxManager.renameMailbox(originMailboxPath, destinationMailboxPath, mailboxSession);

            subscriptionManager.unsubscribe(mailboxSession, originMailboxPath.getName());
            subscriptionManager.subscribe(mailboxSession, destinationMailboxPath.getName());
            return builder;
        };
    }

    private Pipeline.Operation<Builder> setRights(MailboxUpdateRequest updateRequest, MailboxSession mailboxSession, MailboxPath originMailboxPath) throws MailboxException {
        return builder -> {
            mailboxManager.setRights(originMailboxPath,
                updateRequest.getSharedWith()
                    .get()
                    .removeEntriesFor(Username.forMailboxPath(originMailboxPath))
                    .toMailboxAcl(),
                mailboxSession);
            return builder;
        };
    }

    private MailboxPath computeNewMailboxPath(Mailbox mailbox, MailboxPath originMailboxPath, MailboxUpdateRequest updateRequest, MailboxSession mailboxSession) throws MailboxException {
        Optional<MailboxId> parentId = updateRequest.getParentId();
        if (parentId == null) {
            return MailboxPath.forUser(
                mailboxSession.getUser().getUserName(),
                updateRequest.getName().orElse(mailbox.getName()));
        }

        MailboxPath modifiedMailboxPath = updateRequest.getName()
                .map(newName -> computeMailboxPathWithNewName(originMailboxPath, newName))
                .orElse(originMailboxPath);
        ThrowingFunction<MailboxId, MailboxPath> computeNewMailboxPath = parentMailboxId -> computeMailboxPathWithNewParentId(modifiedMailboxPath, parentMailboxId, mailboxSession);
        return parentId
                .map(Throwing.function(computeNewMailboxPath).sneakyThrow())
                .orElse(modifiedMailboxPath);
    }

    private MailboxPath computeMailboxPathWithNewName(MailboxPath originMailboxPath, String newName) {
        return new MailboxPath(originMailboxPath, newName);
    }

    private MailboxPath computeMailboxPathWithNewParentId(MailboxPath originMailboxPath, MailboxId parentMailboxId, MailboxSession mailboxSession) throws MailboxException {
        MailboxPath newParentMailboxPath = mailboxManager.getMailbox(parentMailboxId, mailboxSession).getMailboxPath();
        String lastName = getCurrentMailboxName(originMailboxPath, mailboxSession);
        return new MailboxPath(originMailboxPath, newParentMailboxPath.getName() + mailboxSession.getPathDelimiter() + lastName);
    }

    private String getCurrentMailboxName(MailboxPath originMailboxPath, MailboxSession mailboxSession) {
        return Iterables.getLast(
                Splitter.on(mailboxSession.getPathDelimiter())
                    .splitToList(originMailboxPath.getName()));
    }

    private Pipeline.Operation<Builder> invalidSystemMailboxUpdate(MailboxId mailboxId) {
        return responseBuilder -> responseBuilder.notUpdated(mailboxId, SetError.builder()
            .type("invalidArguments")
            .description("Cannot update a system mailbox.")
            .build());
    }

    private Pipeline.Operation<Builder> invalidArgument(MailboxId mailboxId, String message) {
        return builder -> builder.notUpdated(mailboxId, SetError.builder()
            .type("invalidArguments")
            .description(message)
            .build());
    }

    private Pipeline.Operation<Builder> invalidMailboxNameContainsDelimiter(MailboxId mailboxId, MailboxUpdateRequest updateRequest, char pathDelimiter) {
        return responseBuilder -> responseBuilder.notUpdated(mailboxId, SetError.builder()
            .type("invalidArguments")
            .description(String.format("The mailbox '%s' contains an illegal character: '%c'", updateRequest.getName().get(), pathDelimiter))
            .build());
    }

    private Pipeline.Operation<Builder> invalidSystemMailboxName(MailboxId mailboxId, MailboxUpdateRequest updateRequest) {
        return responseBuilder -> responseBuilder.notUpdated(mailboxId, SetError.builder()
            .type("invalidArguments")
            .description(String.format("The mailbox '%s' is a system mailbox.", updateRequest.getName().get()))
            .build());
    }

    private Pipeline.ConditionalStep.Factory<Builder> when(boolean b) {
        return Pipeline.when(b);
    }

    private Pipeline.ConditionalStep.Factory<Builder> whenNot(boolean b) {
        return Pipeline.when(!b);
    }

}
