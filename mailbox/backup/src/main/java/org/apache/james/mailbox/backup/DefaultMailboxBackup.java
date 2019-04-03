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
package org.apache.james.mailbox.backup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.james.core.User;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.FetchGroupImpl;
import org.apache.james.mailbox.model.Mailbox;
import org.apache.james.mailbox.model.MailboxAnnotation;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.mailbox.model.search.MailboxQuery;
import org.apache.james.util.OptionalUtils;
import org.apache.james.util.streams.Iterators;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class DefaultMailboxBackup implements MailboxBackup {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMailboxBackup.class);

    @VisibleForTesting
    static class MailAccountContent {
        private final MailboxWithAnnotations mailboxWithAnnotations;
        private final Stream<MessageResult> messages;

        MailAccountContent(MailboxWithAnnotations mailboxWithAnnotations, Stream<MessageResult> messages) {
            this.mailboxWithAnnotations = mailboxWithAnnotations;
            this.messages = messages;
        }

        public MailboxWithAnnotations getMailboxWithAnnotations() {
            return mailboxWithAnnotations;
        }

        public Stream<MessageResult> getMessages() {
            return messages;
        }
    }

    private final MailboxManager mailboxManager;
    private final ArchiveService archiveService;
    private final MailArchivesLoader archiveLoader;

    public DefaultMailboxBackup(MailboxManager mailboxManager, ArchiveService archiveService, MailArchivesLoader archiveLoader) {
        this.mailboxManager = mailboxManager;
        this.archiveService = archiveService;
        this.archiveLoader = archiveLoader;
    }

    @Override
    public void backupAccount(User user, OutputStream destination) throws IOException, MailboxException {
        MailboxSession session = mailboxManager.createSystemSession(user.asString());
        List<MailAccountContent> accountContents = getAccountContentForUser(session);
        List<MailboxWithAnnotations> mailboxes = accountContents.stream()
            .map(MailAccountContent::getMailboxWithAnnotations)
            .collect(Guavate.toImmutableList());

        Stream<MessageResult> messages = allMessagesForUser(accountContents);
        archive(mailboxes, messages, destination);
    }

    @Override
    public Publisher<Void> restore(User user, InputStream source) {
        return Mono.fromRunnable(Throwing.runnable(() -> doRestore(user, source)))
            .subscribeOn(Schedulers.elastic())
            .doOnError(e -> LOGGER.error("Error during account restoration for user : " + user, e))
            .doOnTerminate(Throwing.runnable(source::close))
            .then();
    }

    private void doRestore(User user, InputStream source) throws MailboxException, IOException {
        MailboxSession session = mailboxManager.createSystemSession(user.asString());
        restoreEntries(source, session);
    }

    private void restoreEntries(InputStream source, MailboxSession session) throws IOException {
        try (MailArchiveIterator archiveIterator = archiveLoader.load(source)) {
            ImmutablePair<List<MailboxWithAnnotationsArchiveEntry>, Optional<MessageArchiveEntry>> mailboxesAndFirstMessage = readMailboxes(archiveIterator);

            Map<SerializedMailboxId, MessageManager> oldToNewMailbox = restoreMailboxes(session, mailboxesAndFirstMessage.left);
            Optional<MessageArchiveEntry> firstMessage = mailboxesAndFirstMessage.right;

            firstMessage.ifPresent(message -> restoreNonMailboxEntry(session, oldToNewMailbox, message));
            archiveIterator.forEachRemaining(entry -> restoreNonMailboxEntry(session, oldToNewMailbox, entry));
        }
    }

    private void restoreNonMailboxEntry(MailboxSession session, Map<SerializedMailboxId, MessageManager> oldToNewMailbox, MailArchiveEntry entry) {
        switch (entry.getType()) {
            case MESSAGE:
                MessageArchiveEntry messageArchiveEntry = (MessageArchiveEntry) entry;
                restoreMessageEntryWithErrorHandling(session, oldToNewMailbox, messageArchiveEntry);
                break;
            case MAILBOX:
                MailboxWithAnnotationsArchiveEntry mailboxArchiveEntry = ((MailboxWithAnnotationsArchiveEntry) entry);
                LOGGER.error(getMailboxEntryInWrongPositionErrorMessage(mailboxArchiveEntry));
                break;
            case UNKNOWN:
                String entryName = ((UnknownArchiveEntry) entry).getEntryName();
                LOGGER.error("unknow entry found in zip :" + entryName);
                break;
        }
    }

    private String getMailboxEntryInWrongPositionErrorMessage(MailboxWithAnnotationsArchiveEntry mailboxArchiveEntry) {
        return "mailbox entry found in wrong position in zip, should be before messages." + " id :"
            + mailboxArchiveEntry.getMailboxId().getValue()
            + " - name : " + mailboxArchiveEntry.getMailboxName();
    }

    private void restoreMessageEntryWithErrorHandling(MailboxSession session, Map<SerializedMailboxId, MessageManager> oldToNewMailbox, MessageArchiveEntry messageArchiveEntry) {
        try {
            restoreMessageEntry(session, oldToNewMailbox, messageArchiveEntry);
        } catch (IOException | MailboxException e) {
            LOGGER.error("Error when restoring message :" + messageArchiveEntry.getMessageId(), e);
        }
    }

    private void restoreMessageEntry(MailboxSession session, Map<SerializedMailboxId, MessageManager> oldToNewMailbox,
                                     MessageArchiveEntry messageArchiveEntry) throws IOException, MailboxException {
        try (InputStream content = messageArchiveEntry.getContent()) {
            MessageManager mailbox = oldToNewMailbox.get(messageArchiveEntry.getMailboxId());
            MessageManager.AppendCommand appendCommand = MessageManager.AppendCommand.builder()
                .withFlags(messageArchiveEntry.getFlags())
                .withInternalDate(messageArchiveEntry.getInternalDate())
                .build(content);
            mailbox.appendMessage(appendCommand, session);
        }
    }

    private Map<SerializedMailboxId, MessageManager> restoreMailboxes(MailboxSession session, List<MailboxWithAnnotationsArchiveEntry> mailboxes) {
        return mailboxes.stream()
            .flatMap(Throwing.function(mailboxEntry ->
                OptionalUtils.toStream(restoreMailboxEntry(session, mailboxEntry))))
            .collect(Guavate.entriesToImmutableMap());
    }

    private ImmutablePair<List<MailboxWithAnnotationsArchiveEntry>, Optional<MessageArchiveEntry>> readMailboxes(MailArchiveIterator iterator) {
        ImmutableList.Builder<MailboxWithAnnotationsArchiveEntry> mailboxes = ImmutableList.builder();
        while (iterator.hasNext()) {
            MailArchiveEntry entry = iterator.next();
            switch (entry.getType()) {
                case MAILBOX:
                    mailboxes.add((MailboxWithAnnotationsArchiveEntry) entry);
                    break;
                case MESSAGE:
                    return ImmutablePair.of(mailboxes.build(), Optional.of((MessageArchiveEntry) entry));
                case UNKNOWN:
                    String entryName = ((UnknownArchiveEntry) entry).getEntryName();
                    LOGGER.error("unknown entry found in zip :" + entryName);
                    break;
            }
        }
        return ImmutablePair.of(mailboxes.build(), Optional.empty());
    }

    private Optional<ImmutablePair<SerializedMailboxId, MessageManager>> restoreMailboxEntry(MailboxSession session,
                                                                                             MailboxWithAnnotationsArchiveEntry mailboxWithAnnotationsArchiveEntry) throws MailboxException {
        MailboxPath mailboxPath = MailboxPath.forUser(session.getUser().asString(), mailboxWithAnnotationsArchiveEntry.getMailboxName());
        Optional<MailboxId> newMailboxId = mailboxManager.createMailbox(mailboxPath, session);
        mailboxManager.updateAnnotations(mailboxPath, session, mailboxWithAnnotationsArchiveEntry.getAnnotations());
        return newMailboxId.map(Throwing.function(newId ->
            ImmutablePair.of(mailboxWithAnnotationsArchiveEntry.getMailboxId(), mailboxManager.getMailbox(newId, session))));
    }

    private Stream<MailAccountContent> getMailboxWithAnnotationsFromPath(MailboxSession session, MailboxPath path) {
        try {
            MessageManager messageManager =  mailboxManager.getMailbox(path, session);
            Mailbox mailbox = messageManager.getMailboxEntity();
            List<MailboxAnnotation> annotations = mailboxManager.getAllAnnotations(path, session);
            MailboxWithAnnotations mailboxWithAnnotations = new MailboxWithAnnotations(mailbox, annotations);
            Stream<MessageResult> messages = Iterators.toStream(messageManager.getMessages(MessageRange.all(), FetchGroupImpl.FULL_CONTENT, session));
            return Stream.of(new MailAccountContent(mailboxWithAnnotations, messages));
        } catch (MailboxException e) {
            LOGGER.error("Error while fetching Mailbox during backup", e);
            return Stream.empty();
        }
    }

    @VisibleForTesting
    List<MailAccountContent> getAccountContentForUser(MailboxSession session) throws MailboxException {
        MailboxQuery queryUser = MailboxQuery.builder().user(session.getUser()).build();
        Stream<MailboxPath> paths = mailboxManager.search(queryUser, session).stream()
            .map(MailboxMetaData::getPath);
        List<MailAccountContent> mailboxes = paths
            .flatMap(path -> getMailboxWithAnnotationsFromPath(session, path))
            .collect(Guavate.toImmutableList());

        return mailboxes;
    }

    private void archive(List<MailboxWithAnnotations> mailboxes, Stream<MessageResult> messages, OutputStream
        destination) throws IOException {
        archiveService.archive(mailboxes, messages, destination);
    }

    private Stream<MessageResult> allMessagesForUser(List<MailAccountContent> mailboxes) {
        return mailboxes.stream().flatMap(MailAccountContent::getMessages);
    }

}
