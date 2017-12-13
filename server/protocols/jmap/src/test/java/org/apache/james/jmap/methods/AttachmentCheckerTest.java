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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.jmap.model.Attachment;
import org.apache.james.jmap.model.BlobId;
import org.apache.james.jmap.model.CreationMessage;
import org.apache.james.jmap.model.CreationMessageId;
import org.apache.james.mailbox.AttachmentManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.AttachmentId;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class AttachmentCheckerTest {

    private final CreationMessageId creationMessageId = CreationMessageId.of("dlkja");
    private final CreationMessage.Builder creationMessageBuilder = CreationMessage.builder()
        .from(CreationMessage.DraftEmailer.builder().name("alice").email("alice@example.com").build())
        .to(ImmutableList.of(CreationMessage.DraftEmailer.builder().name("bob").email("bob@example.com").build()))
        .mailboxId("id")
        .subject("Hey! ");

    private AttachmentManager attachmentManager;
    private MailboxSession session;

    private AttachmentChecker sut;

    @Before
    public void setUp() {
        session = new MockMailboxSession("Jonhy");
        attachmentManager = mock(AttachmentManager.class);

        sut = new AttachmentChecker(attachmentManager);
    }

    @Test
    public void listAttachmentNotFoundsShouldReturnBlobIdsWhenUnknown() throws MailboxException {
        BlobId unknownBlobId = BlobId.of("unknownBlobId");
        AttachmentId unknownAttachmentId = AttachmentId.from(unknownBlobId.getRawValue());
        when(attachmentManager.exists(unknownAttachmentId, session)).thenReturn(false);

        assertThat(sut.listAttachmentNotFounds(
            new ValueWithId.CreationMessageEntry(
                creationMessageId,
                creationMessageBuilder.attachments(
                    Attachment.builder().size(12L).type("image/jpeg").blobId(unknownBlobId).build())
                    .build()
            ),
            session))
            .containsOnly(unknownBlobId);
    }

    @Test
    public void listAttachmentNotFoundsShouldReturnEmptyWhenKnown() throws Exception {
        BlobId blobId = BlobId.of("unknownBlobId");
        AttachmentId attachmentId = AttachmentId.from(blobId.getRawValue());
        when(attachmentManager.exists(attachmentId, session)).thenReturn(true);

        assertThat(
            sut.listAttachmentNotFounds(
                new ValueWithId.CreationMessageEntry(
                    creationMessageId,
                    creationMessageBuilder.attachments(
                        Attachment.builder().size(12L).type("image/jpeg").blobId(blobId).build())
                        .build()),
                session))
            .isEmpty();
    }

    @Test
    public void listAttachmentNotFoundsShouldReturnAllBlobIdsWhenAllUnknown() throws MailboxException {
        BlobId unknownBlobId1 = BlobId.of("unknownBlobId1");
        BlobId unknownBlobId2 = BlobId.of("unknownBlobId2");
        AttachmentId unknownAttachmentId1 = AttachmentId.from(unknownBlobId1.getRawValue());
        AttachmentId unknownAttachmentId2 = AttachmentId.from(unknownBlobId2.getRawValue());

        when(attachmentManager.exists(unknownAttachmentId1, session)).thenReturn(false);
        when(attachmentManager.exists(unknownAttachmentId2, session)).thenReturn(false);

        assertThat(
            sut.listAttachmentNotFounds(
                new ValueWithId.CreationMessageEntry(
                    creationMessageId,
                    creationMessageBuilder.attachments(
                        Attachment.builder().size(12L).type("image/jpeg").blobId(unknownBlobId1).build(),
                        Attachment.builder().size(23L).type("image/git").blobId(unknownBlobId2).build())
                        .build()),
                session))
            .containsOnly(unknownBlobId1, unknownBlobId2);
    }

    @Test
    public void listAttachmentNotFoundsShouldReturnEmptyWhenAllKnown() throws Exception {
        BlobId blobId1 = BlobId.of("unknownBlobId1");
        BlobId blobId2 = BlobId.of("unknownBlobId2");
        AttachmentId attachmentId1 = AttachmentId.from(blobId1.getRawValue());
        AttachmentId attachmentId2 = AttachmentId.from(blobId2.getRawValue());

        when(attachmentManager.exists(attachmentId1, session)).thenReturn(true);
        when(attachmentManager.exists(attachmentId2, session)).thenReturn(true);

        assertThat(
            sut.listAttachmentNotFounds(
                new ValueWithId.CreationMessageEntry(
                    creationMessageId,
                    creationMessageBuilder.attachments(
                        Attachment.builder().size(12L).type("image/jpeg").blobId(blobId1).build(),
                        Attachment.builder().size(23L).type("image/git").blobId(blobId2).build())
                        .build()),
                session))
            .isEmpty();
    }

    @Test
    public void listAttachmentNotFoundsShouldReturnEmptyWhenEmpty() throws Exception {
        BlobId blobId1 = BlobId.of("unknownBlobId1");
        BlobId blobId2 = BlobId.of("unknownBlobId2");
        AttachmentId attachmentId1 = AttachmentId.from(blobId1.getRawValue());
        AttachmentId attachmentId2 = AttachmentId.from(blobId2.getRawValue());

        when(attachmentManager.exists(attachmentId1, session)).thenReturn(true);
        when(attachmentManager.exists(attachmentId2, session)).thenReturn(true);

        assertThat(
            sut.listAttachmentNotFounds(
                new ValueWithId.CreationMessageEntry(
                    creationMessageId,
                    creationMessageBuilder.attachments()
                        .build()),
                session))
            .isEmpty();
    }

    @Test
    public void listAttachmentNotFoundsShouldReturnOnlyUnknownBlobIds() throws MailboxException {
        BlobId blobId1 = BlobId.of("unknownBlobId1");
        BlobId unknownBlobId2 = BlobId.of("unknownBlobId2");
        AttachmentId attachmentId1 = AttachmentId.from(blobId1.getRawValue());
        AttachmentId unknownAttachmentId2 = AttachmentId.from(unknownBlobId2.getRawValue());

        when(attachmentManager.exists(attachmentId1, session)).thenReturn(true);
        when(attachmentManager.exists(unknownAttachmentId2, session)).thenReturn(false);

        assertThat(
            sut.listAttachmentNotFounds(
                new ValueWithId.CreationMessageEntry(
                    creationMessageId,
                    creationMessageBuilder.attachments(
                        Attachment.builder().size(12L).type("image/jpeg").blobId(blobId1).build(),
                        Attachment.builder().size(23L).type("image/git").blobId(unknownBlobId2).build())
                        .build()),
                session))
            .containsOnly(unknownBlobId2);
    }

}