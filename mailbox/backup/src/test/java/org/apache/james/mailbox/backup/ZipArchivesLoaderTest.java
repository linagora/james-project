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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.mail.Flags;

import org.apache.commons.io.IOUtils;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.backup.zip.ZipArchivesLoader;
import org.apache.james.mailbox.backup.zip.Zipper;
import org.apache.james.mailbox.inmemory.manager.InMemoryIntegrationResources;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.fge.lambdas.Throwing;

public class ZipArchivesLoaderTest implements MailboxMessageFixture {
    private static final int BUFFER_SIZE = 4096;

    private final ArchiveService archiveService = new Zipper();
    private final MailArchivesLoader archiveLoader = new ZipArchivesLoader();

    private MailboxManager mailboxManager;
    private DefaultMailboxBackup backup;

    @BeforeEach
    void beforeEach() {
        mailboxManager = InMemoryIntegrationResources.defaultResources().getMailboxManager();
        backup = new DefaultMailboxBackup(mailboxManager, archiveService, archiveLoader);
    }

    private void createMailBoxWithMessage(MailboxSession session, MailboxPath mailboxPath, MailboxMessage... messages) throws Exception {
        MailboxId mailboxId = mailboxManager.createMailbox(mailboxPath, session).get();
        Arrays.stream(messages).forEach(Throwing.consumer(message ->
            {
                MessageManager.AppendCommand appendCommand = MessageManager.AppendCommand.builder()
                    .withFlags(message.createFlags())
                    .build(message.getFullContent());
                mailboxManager.getMailbox(mailboxId, session).appendMessage(appendCommand, session);
            }
            )
        );
    }

    @Test
    void mailAccountIteratorFromEmptyArchiveShouldBeEmpty() throws Exception {
        ByteArrayOutputStream destination = new ByteArrayOutputStream(BUFFER_SIZE);
        backup.backupAccount(USER1, destination);

        InputStream source = new ByteArrayInputStream(destination.toByteArray());
        MailArchiveIterator mailArchiveIterator = archiveLoader.load(source);

        assertThat(mailArchiveIterator.hasNext()).isEqualTo(false);
        assertThat(mailArchiveIterator.next()).isEqualTo(null);
    }

    @Test
    void callingNextSeveralTimeOnAnEmptyIteratorShouldReturnNull() throws Exception {
        ByteArrayOutputStream destination = new ByteArrayOutputStream(BUFFER_SIZE);
        backup.backupAccount(USER1, destination);

        InputStream source = new ByteArrayInputStream(destination.toByteArray());
        MailArchiveIterator mailArchiveIterator = archiveLoader.load(source);

        assertThat(mailArchiveIterator.hasNext()).isEqualTo(false);
        assertThat(mailArchiveIterator.next()).isEqualTo(null);
        assertThat(mailArchiveIterator.next()).isEqualTo(null);
        assertThat(mailArchiveIterator.next()).isEqualTo(null);
    }

    @Test
    void mailAccountIteratorFromArchiveWithOneMailboxShouldBeContainsOneMailbox() throws Exception {
        MailboxSession session = mailboxManager.createSystemSession(USER);
        createMailBoxWithMessage(session, MAILBOX_PATH_USER1_MAILBOX1);

        ByteArrayOutputStream destination = new ByteArrayOutputStream(BUFFER_SIZE);
        backup.backupAccount(USER1, destination);

        InputStream source = new ByteArrayInputStream(destination.toByteArray());
        MailArchiveIterator mailArchiveIterator = archiveLoader.load(source);
        assertThat(mailArchiveIterator.hasNext()).isEqualTo(true);

        MailboxWithAnnotationsArchiveEntry expectedMailbox = new MailboxWithAnnotationsArchiveEntry(MAILBOX_1_NAME, SERIALIZED_MAILBOX_ID_1, NO_ANNOTATION);
        MailboxWithAnnotationsArchiveEntry resultMailbox = (MailboxWithAnnotationsArchiveEntry) mailArchiveIterator.next();
        verifyMailboxArchiveEntry(mailArchiveIterator, expectedMailbox, resultMailbox, false);
    }

    @Test
    void mailAccountIteratorFromArchiveWithTwoMailboxShouldBeContainsTwoMailbox() throws Exception {
        MailboxSession session = mailboxManager.createSystemSession(USER);
        createMailBoxWithMessage(session, MAILBOX_PATH_USER1_MAILBOX1);
        createMailBoxWithMessage(session, MAILBOX_PATH_USER1_MAILBOX2);

        ByteArrayOutputStream destination = new ByteArrayOutputStream(BUFFER_SIZE);
        backup.backupAccount(USER1, destination);

        InputStream source = new ByteArrayInputStream(destination.toByteArray());
        MailArchiveIterator mailArchiveIterator = archiveLoader.load(source);
        assertThat(mailArchiveIterator.hasNext()).isEqualTo(true);

        MailboxWithAnnotationsArchiveEntry expectedMailbox = new MailboxWithAnnotationsArchiveEntry(MAILBOX_1_NAME, SERIALIZED_MAILBOX_ID_1, NO_ANNOTATION);
        MailboxWithAnnotationsArchiveEntry resultMailbox = (MailboxWithAnnotationsArchiveEntry) mailArchiveIterator.next();
        verifyMailboxArchiveEntry(mailArchiveIterator, expectedMailbox, resultMailbox, true);

        MailboxWithAnnotationsArchiveEntry expectedSecondMailbox = new MailboxWithAnnotationsArchiveEntry(MAILBOX_2_NAME, SERIALIZED_MAILBOX_ID_2, NO_ANNOTATION);
        MailboxWithAnnotationsArchiveEntry resultSecondMailbox = (MailboxWithAnnotationsArchiveEntry) mailArchiveIterator.next();
        verifyMailboxArchiveEntry(mailArchiveIterator, expectedSecondMailbox, resultSecondMailbox, false);
    }

    @Test
    void mailAccountIteratorFromArchiveWithOneMailboxAndOneAnnotationShouldContainsOneMailbox() throws Exception {
        MailboxSession session = mailboxManager.createSystemSession(USER);
        createMailBoxWithMessage(session, MAILBOX_PATH_USER1_MAILBOX1);
        mailboxManager.updateAnnotations(MAILBOX_PATH_USER1_MAILBOX1, session, WITH_ANNOTATION_1);

        ByteArrayOutputStream destination = new ByteArrayOutputStream(BUFFER_SIZE);
        backup.backupAccount(USER1, destination);

        InputStream source = new ByteArrayInputStream(destination.toByteArray());
        MailArchiveIterator mailArchiveIterator = archiveLoader.load(source);
        assertThat(mailArchiveIterator.hasNext()).isEqualTo(true);

        MailboxWithAnnotationsArchiveEntry expectedMailbox = new MailboxWithAnnotationsArchiveEntry(MAILBOX_1_NAME, SERIALIZED_MAILBOX_ID_1, WITH_ANNOTATION_1);
        MailboxWithAnnotationsArchiveEntry resultMailbox = (MailboxWithAnnotationsArchiveEntry) mailArchiveIterator.next();
        verifyMailboxArchiveEntry(mailArchiveIterator, expectedMailbox, resultMailbox, false);
    }

    @Test
    void mailAccountIteratorFromArchiveWithOneMailboxAndTwoAnnotationShouldContainsOneMailbox() throws Exception {
        MailboxSession session = mailboxManager.createSystemSession(USER);
        createMailBoxWithMessage(session, MAILBOX_PATH_USER1_MAILBOX1);
        mailboxManager.updateAnnotations(MAILBOX_PATH_USER1_MAILBOX1, session, WITH_ANNOTATION_1_AND_2);

        ByteArrayOutputStream destination = new ByteArrayOutputStream(BUFFER_SIZE);
        backup.backupAccount(USER1, destination);

        InputStream source = new ByteArrayInputStream(destination.toByteArray());
        MailArchiveIterator mailArchiveIterator = archiveLoader.load(source);
        assertThat(mailArchiveIterator.hasNext()).isEqualTo(true);

        MailboxWithAnnotationsArchiveEntry expectedMailbox = new MailboxWithAnnotationsArchiveEntry(MAILBOX_1_NAME, SERIALIZED_MAILBOX_ID_1, WITH_ANNOTATION_1_AND_2);
        MailboxWithAnnotationsArchiveEntry resultMailbox = (MailboxWithAnnotationsArchiveEntry) mailArchiveIterator.next();
        verifyMailboxArchiveEntry(mailArchiveIterator, expectedMailbox, resultMailbox, false);
    }

    @Test
    void mailAccountIteratorFromArchiveWithOneMailboxAndOneMessageShouldContainsOneMailboxAndOneMessage() throws Exception {
        MailboxSession session = mailboxManager.createSystemSession(USER);
        createMailBoxWithMessage(session, MAILBOX_PATH_USER1_MAILBOX1, MESSAGE_1);
        mailboxManager.updateAnnotations(MAILBOX_PATH_USER1_MAILBOX1, session, NO_ANNOTATION);

        ByteArrayOutputStream destination = new ByteArrayOutputStream(BUFFER_SIZE);
        backup.backupAccount(USER1, destination);

        InputStream source = new ByteArrayInputStream(destination.toByteArray());
        MailArchiveIterator mailArchiveIterator = archiveLoader.load(source);
        assertThat(mailArchiveIterator.hasNext()).isEqualTo(true);

        MailboxWithAnnotationsArchiveEntry expectedMailbox = new MailboxWithAnnotationsArchiveEntry(MAILBOX_1_NAME, SERIALIZED_MAILBOX_ID_1, NO_ANNOTATION);
        MailboxWithAnnotationsArchiveEntry resultMailbox = (MailboxWithAnnotationsArchiveEntry) mailArchiveIterator.next();
        verifyMailboxArchiveEntry(mailArchiveIterator, expectedMailbox, resultMailbox, true);

        try (InputStream expectedContentStream = CONTENT_STREAM_1.newStream(0, -1)) {
            MessageArchiveEntry expectedMessage = new MessageArchiveEntry(SERIALIZED_MESSAGE_ID_1, SERIALIZED_MAILBOX_ID_1,
                SIZE_1, MESSAGE_1.getInternalDate(), flags1, expectedContentStream);
            MessageArchiveEntry resultMessage = (MessageArchiveEntry) mailArchiveIterator.next();
            verifyMessageEntry(mailArchiveIterator, expectedMessage, resultMessage, MESSAGE_CONTENT_BYTES_1, false);
        }
    }

    @Test
    void mailAccountIteratorFromArchiveWithOneMailboxAndTwoMessageShouldContainsOneMailboxAndTwoMessage() throws Exception {
        MailboxSession session = mailboxManager.createSystemSession(USER);
        createMailBoxWithMessage(session, MAILBOX_PATH_USER1_MAILBOX1, MESSAGE_1, MESSAGE_2);
        mailboxManager.updateAnnotations(MAILBOX_PATH_USER1_MAILBOX1, session, NO_ANNOTATION);

        ByteArrayOutputStream destination = new ByteArrayOutputStream(BUFFER_SIZE);
        backup.backupAccount(USER1, destination);

        InputStream source = new ByteArrayInputStream(destination.toByteArray());
        MailArchiveIterator mailArchiveIterator = archiveLoader.load(source);
        assertThat(mailArchiveIterator.hasNext()).isEqualTo(true);

        MailboxWithAnnotationsArchiveEntry expectedMailbox = new MailboxWithAnnotationsArchiveEntry(MAILBOX_1_NAME, SERIALIZED_MAILBOX_ID_1, NO_ANNOTATION);
        MailboxWithAnnotationsArchiveEntry resultMailbox = (MailboxWithAnnotationsArchiveEntry) mailArchiveIterator.next();
        verifyMailboxArchiveEntry(mailArchiveIterator, expectedMailbox, resultMailbox, true);

        try (InputStream expectedContentStream = CONTENT_STREAM_1.newStream(0, -1)) {
            MessageArchiveEntry expectedMessage = new MessageArchiveEntry(SERIALIZED_MESSAGE_ID_1, SERIALIZED_MAILBOX_ID_1,
                SIZE_1, MESSAGE_1.getInternalDate(), flags1, expectedContentStream);
            MessageArchiveEntry resultMessage = (MessageArchiveEntry) mailArchiveIterator.next();
            verifyMessageEntry(mailArchiveIterator, expectedMessage, resultMessage, MESSAGE_CONTENT_BYTES_1, true);
        }
        try (InputStream expectedContentStream = CONTENT_STREAM_2.newStream(0, -1)) {
            MessageArchiveEntry expectedMessage2 = new MessageArchiveEntry(SERIALIZED_MESSAGE_ID_2, SERIALIZED_MAILBOX_ID_1,
                SIZE_2, MESSAGE_2.getInternalDate(), new Flags(), expectedContentStream);
            MessageArchiveEntry resultMessage2 = (MessageArchiveEntry) mailArchiveIterator.next();
            verifyMessageEntry(mailArchiveIterator, expectedMessage2, resultMessage2, MESSAGE_CONTENT_BYTES_2, false);
        }
    }

    @Test
    void mailAccountIteratorFromArchiveWithTwoMailboxAndOneMessageShouldContainsTwoMailboxAndOneMessage() throws Exception {
        MailboxSession session = mailboxManager.createSystemSession(USER);
        createMailBoxWithMessage(session, MAILBOX_PATH_USER1_MAILBOX1, MESSAGE_1);
        mailboxManager.updateAnnotations(MAILBOX_PATH_USER1_MAILBOX1, session, NO_ANNOTATION);
        createMailBoxWithMessage(session, MAILBOX_PATH_USER1_MAILBOX2);

        ByteArrayOutputStream destination = new ByteArrayOutputStream(BUFFER_SIZE);
        backup.backupAccount(USER1, destination);

        InputStream source = new ByteArrayInputStream(destination.toByteArray());
        MailArchiveIterator mailArchiveIterator = archiveLoader.load(source);
        assertThat(mailArchiveIterator.hasNext()).isEqualTo(true);

        MailboxWithAnnotationsArchiveEntry expectedMailbox = new MailboxWithAnnotationsArchiveEntry(MAILBOX_1_NAME, SERIALIZED_MAILBOX_ID_1, NO_ANNOTATION);
        MailboxWithAnnotationsArchiveEntry resultMailbox = (MailboxWithAnnotationsArchiveEntry) mailArchiveIterator.next();
        verifyMailboxArchiveEntry(mailArchiveIterator, expectedMailbox, resultMailbox, true);

        MailboxWithAnnotationsArchiveEntry expectedSecondMailbox = new MailboxWithAnnotationsArchiveEntry(MAILBOX_2_NAME, SERIALIZED_MAILBOX_ID_2, NO_ANNOTATION);
        MailboxWithAnnotationsArchiveEntry resultSecondMailbox = (MailboxWithAnnotationsArchiveEntry) mailArchiveIterator.next();
        verifyMailboxArchiveEntry(mailArchiveIterator, expectedSecondMailbox, resultSecondMailbox, true);

        try (InputStream expectedContentStream = CONTENT_STREAM_1.newStream(0, -1)) {
            MessageArchiveEntry expectedMessage = new MessageArchiveEntry(SERIALIZED_MESSAGE_ID_1, SERIALIZED_MAILBOX_ID_1,
                SIZE_1, MESSAGE_1.getInternalDate(), flags1, expectedContentStream);
            MessageArchiveEntry resultMessage = (MessageArchiveEntry) mailArchiveIterator.next();
            verifyMessageEntry(mailArchiveIterator, expectedMessage, resultMessage, MESSAGE_CONTENT_BYTES_1, false);
        }
    }

    private void verifyMessageEntry(MailArchiveIterator mailArchiveIterator, MessageArchiveEntry expectedMessage, MessageArchiveEntry resultMessage,
                                    byte[] expectedMessageContent, boolean iteratorHasNextElement) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
        try (InputStream inputStream = resultMessage.getContent()) {
            IOUtils.copy(inputStream, byteArrayOutputStream);
        }
        byte[] resultMessageContent = byteArrayOutputStream.toByteArray();

        assertThat(resultMessage.getMailboxId()).isEqualTo(expectedMessage.getMailboxId());
        assertThat(resultMessage.getMessageId()).isEqualTo(expectedMessage.getMessageId());
        assertThat(resultMessage.getSize()).isEqualTo(expectedMessage.getSize());

        assertThat(resultMessageContent).isEqualTo(expectedMessageContent);
        assertThat(mailArchiveIterator.hasNext()).isEqualTo(iteratorHasNextElement);
    }

    private void verifyMailboxArchiveEntry(MailArchiveIterator mailArchiveIterator, MailboxWithAnnotationsArchiveEntry expectedMailbox,
                                           MailboxWithAnnotationsArchiveEntry resultMailbox, boolean iteratorHasNextElement) {
        assertThat(resultMailbox.getMailboxId()).isEqualTo(expectedMailbox.getMailboxId());
        assertThat(resultMailbox.getMailboxName()).isEqualTo(expectedMailbox.getMailboxName());
        assertThat(resultMailbox.getAnnotations()).isEqualTo(expectedMailbox.getAnnotations());
        assertThat(resultMailbox).isEqualTo(expectedMailbox);
        assertThat(mailArchiveIterator.hasNext()).isEqualTo(iteratorHasNextElement);
    }

}
