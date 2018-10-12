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

package org.apache.mailbox.tools.indexer;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.james.core.User;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.search.MailboxQuery;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.search.ListeningMessageSearchIndex;
import org.apache.james.task.Task;
import org.apache.james.util.OptionalUtils;
import org.apache.james.util.streams.Iterators;
import org.apache.mailbox.tools.indexer.events.ImpactingEventType;
import org.apache.mailbox.tools.indexer.events.ImpactingMessageEvent;
import org.apache.mailbox.tools.indexer.registrations.GlobalRegistration;
import org.apache.mailbox.tools.indexer.registrations.MailboxRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ReIndexerPerformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReIndexerPerformer.class);

    private static final int NO_LIMIT = 0;
    private static final int SINGLE_MESSAGE = 1;

    private final MailboxManager mailboxManager;
    private final ListeningMessageSearchIndex messageSearchIndex;
    private final MailboxSessionMapperFactory mailboxSessionMapperFactory;

    @Inject
    public ReIndexerPerformer(MailboxManager mailboxManager,
                              ListeningMessageSearchIndex messageSearchIndex,
                              MailboxSessionMapperFactory mailboxSessionMapperFactory) {
        this.mailboxManager = mailboxManager;
        this.messageSearchIndex = messageSearchIndex;
        this.mailboxSessionMapperFactory = mailboxSessionMapperFactory;
    }

    public Task.Result reIndex(MailboxPath path, ReprocessingContext reprocessingContext) throws MailboxException {
        MailboxSession mailboxSession = mailboxManager.createSystemSession(path.getUser());
        return reIndex(path, mailboxSession, reprocessingContext);
    }

    public Task.Result reIndex(ReprocessingContext reprocessingContext) throws MailboxException {
        MailboxSession mailboxSession = mailboxManager.createSystemSession("re-indexing");
        LOGGER.info("Starting a full reindex");
        List<MailboxPath> mailboxPaths = mailboxManager.list(mailboxSession);

        try {
            return reIndex(mailboxPaths, mailboxSession, reprocessingContext);
        } finally {
            LOGGER.info("Full reindex finished");
        }
    }

    public Task.Result reIndex(User user, ReprocessingContext reprocessingContext) throws MailboxException {
        MailboxSession mailboxSession = mailboxManager.createSystemSession(user.asString());
        LOGGER.info("Starting a reindex for user {}", user.asString());
        List<MailboxPath> mailboxPaths = mailboxManager.search(MailboxQuery.privateMailboxesBuilder(mailboxSession)
            .build(), mailboxSession)
            .stream()
            .map(MailboxMetaData::getPath)
            .collect(ImmutableList.toImmutableList());

        try {
            return reIndex(mailboxPaths, mailboxSession, reprocessingContext);
        } finally {
            LOGGER.info("User {} reindex finished", user.asString());
        }
    }

    public Task.Result handleMessageReIndexing(MailboxPath path, MessageUid uid) throws MailboxException {
        MailboxSession mailboxSession = mailboxManager.createSystemSession(path.getUser());
        Mailbox mailbox = mailboxSessionMapperFactory.getMailboxMapper(mailboxSession)
            .findMailboxByPath(path);

        return wrapInMailboxRegistration(path, mailboxSession,
            mailboxRegistration -> handleMessageReIndexing(mailboxSession, mailboxRegistration, mailbox, uid));
    }

    private Task.Result reIndex(List<MailboxPath> mailboxPaths, MailboxSession mailboxSession, ReprocessingContext reprocessingContext) throws MailboxException {
        return wrapInGlobalRegistration(mailboxSession,
            globalRegistration -> handleMultiMailboxesReindexingIterations(mailboxPaths, globalRegistration, reprocessingContext));
    }

    private Task.Result handleMultiMailboxesReindexingIterations(List<MailboxPath> mailboxPaths, GlobalRegistration globalRegistration,
                                                                 ReprocessingContext reprocessingContext) {
        return mailboxPaths.stream()
            .map(globalRegistration::getPathToIndex)
            .flatMap(OptionalUtils::toStream)
            .map(path -> {
                try {
                    return reIndex(path, reprocessingContext);
                } catch (Throwable e) {
                    LOGGER.error("Error while proceeding to full reindexing on {}", path.asString(), e);
                    return Task.Result.PARTIAL;
                }
            })
            .reduce(Task::combine)
            .orElse(Task.Result.COMPLETED);
    }

    private Task.Result reIndex(MailboxPath path, MailboxSession mailboxSession, ReprocessingContext reprocessingContext) throws MailboxException {
        LOGGER.info("Intend to reindex {}", path.asString());
        Mailbox mailbox = mailboxSessionMapperFactory.getMailboxMapper(mailboxSession).findMailboxByPath(path);
        messageSearchIndex.deleteAll(mailboxSession, mailbox);

        return wrapInMailboxRegistration(path, mailboxSession,
            Throwing.function((MailboxRegistration mailboxRegistration) -> {
                try {
                    return Iterators.toStream(
                        mailboxSessionMapperFactory.getMessageMapper(mailboxSession)
                            .findInMailbox(mailbox, MessageRange.all(), MessageMapper.FetchType.Metadata, NO_LIMIT))
                        .map(MailboxMessage::getUid)
                        .map(uid -> handleMessageReIndexing(mailboxSession, mailboxRegistration, mailbox, uid))
                        .peek(reprocessingContext::updateAccordingToReprocessingResult)
                        .reduce(Task::combine)
                        .orElse(Task.Result.COMPLETED);
                } finally {
                    LOGGER.info("Finish to reindex {}", path.asString());
                }
            }).sneakyThrow());
    }

    private Task.Result handleMessageReIndexing(MailboxSession mailboxSession, MailboxRegistration mailboxRegistration, Mailbox mailbox, MessageUid uid) {
        try {
            Optional<ImpactingMessageEvent> impactingMessageEvent = findMostRelevant(mailboxRegistration.getImpactingEvents(uid));

            Optional.of(uid)
                .filter(x -> !wasDeleted(impactingMessageEvent))
                .flatMap(Throwing.function(mUid -> fullyReadMessage(mailboxSession, mailbox, mUid)))
                .map(message -> messageUpdateRegardingEvents(message, impactingMessageEvent))
                .ifPresent(Throwing.consumer(message -> messageSearchIndex.add(mailboxSession, mailbox, message)));
            return Task.Result.COMPLETED;
        } catch (Exception e) {
            LOGGER.warn("ReIndexing failed for {} {}", mailbox.generateAssociatedPath(), uid, e);
            return Task.Result.PARTIAL;
        }
    }

    private Optional<ImpactingMessageEvent> findMostRelevant(List<ImpactingMessageEvent> messageEvents) {
        for (ImpactingMessageEvent impactingMessageEvent : messageEvents) {
            if (impactingMessageEvent.getType().equals(ImpactingEventType.Deletion)) {
                return Optional.of(impactingMessageEvent);
            }
        }
        return Lists.reverse(messageEvents).stream().findFirst();
    }

    private boolean wasDeleted(Optional<ImpactingMessageEvent> impactingMessageEvent) {
        return impactingMessageEvent.map(ImpactingMessageEvent::wasDeleted).orElse(false);
    }

    private MailboxMessage messageUpdateRegardingEvents(MailboxMessage message, Optional<ImpactingMessageEvent> impactingMessageEvent) {
        impactingMessageEvent.flatMap(ImpactingMessageEvent::newFlags).ifPresent(message::setFlags);
        return message;
    }

    private Optional<MailboxMessage> fullyReadMessage(MailboxSession mailboxSession, Mailbox mailbox, MessageUid mUid) throws MailboxException {
        return Iterators.toStream(mailboxSessionMapperFactory.getMessageMapper(mailboxSession)
            .findInMailbox(mailbox, MessageRange.one(mUid), MessageMapper.FetchType.Full, SINGLE_MESSAGE))
            .findFirst();
    }

    private <T> T wrapInMailboxRegistration(MailboxPath path, MailboxSession session, Function<MailboxRegistration, T> function) throws MailboxException {
        MailboxRegistration mailboxRegistration = new MailboxRegistration(path);
        mailboxManager.addListener(path, mailboxRegistration, session);

        try {
            return function.apply(mailboxRegistration);
        } finally {
            mailboxManager.removeGlobalListener(mailboxRegistration, session);
        }
    }

    private <T> T wrapInGlobalRegistration(MailboxSession session, Function<GlobalRegistration, T> function) throws MailboxException {
        GlobalRegistration globalRegistration = new GlobalRegistration();
        mailboxManager.addGlobalListener(globalRegistration, session);
        try {
            return function.apply(globalRegistration);
        } finally {
            mailboxManager.removeGlobalListener(globalRegistration, session);
        }
    }

}
