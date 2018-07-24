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

package org.apache.james.mailbox.cassandra.mail.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.mail.Flags;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.james.backends.cassandra.migration.Migration;
import org.apache.james.blob.cassandra.CassandraBlobsDAO;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.cassandra.ids.CassandraMessageId;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentMessageIdDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageDAO;
import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.AttachmentId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public interface AttachmentMessageIdCreationContract {
    CassandraMessageId.Factory messageIdFactory = new CassandraMessageId.Factory();
    CassandraMessageId messageId = messageIdFactory.generate();

    class Testee {
        private final CassandraBlobsDAO blobsDAO;
        private final CassandraMessageDAO cassandraMessageDAO;
        private final CassandraAttachmentMessageIdDAO attachmentMessageIdDAO;
        private final AttachmentMessageIdCreation migration;

        public Testee(CassandraBlobsDAO blobsDAO, CassandraMessageDAO cassandraMessageDAO,
                      CassandraAttachmentMessageIdDAO attachmentMessageIdDAO, AttachmentMessageIdCreation migration) {
            this.blobsDAO = blobsDAO;
            this.cassandraMessageDAO = cassandraMessageDAO;
            this.attachmentMessageIdDAO = attachmentMessageIdDAO;
            this.migration = migration;
        }
    }

    Testee attachmentMessageIdCreationTestee();

    @Test
    default void emptyMigrationShouldSucceed() {
        assertThat(attachmentMessageIdCreationTestee().migration.run())
            .isEqualTo(Migration.Result.COMPLETED);
    }

    @Test
    default void migrationShouldSucceedWhenNoAttachment() throws Exception {
        List<MessageAttachment> noAttachment = ImmutableList.of();
        SimpleMailboxMessage message = createMessage(messageId, noAttachment);

        attachmentMessageIdCreationTestee().cassandraMessageDAO.save(message).join();

        assertThat(attachmentMessageIdCreationTestee().migration.run())
            .isEqualTo(Migration.Result.COMPLETED);
    }

    @Test
    default void migrationShouldSucceedWhenAttachment() throws Exception {
        MessageAttachment attachment = createAttachment();
        SimpleMailboxMessage message = createMessage(messageId, ImmutableList.of(attachment));

        attachmentMessageIdCreationTestee().cassandraMessageDAO.save(message).join();

        assertThat(attachmentMessageIdCreationTestee().migration.run())
            .isEqualTo(Migration.Result.COMPLETED);
    }

    @Test
    default void migrationShouldCreateAttachmentIdOnAttachmentMessageIdTableFromMessage() throws Exception {
        MessageAttachment attachment = createAttachment();
        SimpleMailboxMessage message = createMessage(messageId, ImmutableList.of(attachment));

        attachmentMessageIdCreationTestee().cassandraMessageDAO.save(message).join();

        attachmentMessageIdCreationTestee().migration.run();

        assertThat(attachmentMessageIdCreationTestee().attachmentMessageIdDAO.getOwnerMessageIds(attachment.getAttachmentId()).join())
            .containsExactly(messageId);
    }

    @Test
    default void migrationShouldReturnPartialWhenRetrieveAllAttachmentIdFromMessageFail() throws Exception {
        CassandraMessageDAO cassandraMessageDAO = mock(CassandraMessageDAO.class);
        CassandraAttachmentMessageIdDAO attachmentMessageIdDAO = mock(CassandraAttachmentMessageIdDAO.class);
        AttachmentMessageIdCreation migration = new AttachmentMessageIdCreation(cassandraMessageDAO, attachmentMessageIdDAO);

        when(cassandraMessageDAO.retrieveAllMessageIdAttachmentIds()).thenThrow(new RuntimeException());

        assertThat(migration.run()).isEqualTo(Migration.Result.PARTIAL);
    }

    @Test
    default void migrationShouldReturnPartialWhenSavingAttachmentIdForMessageIdFail() throws Exception {
        CassandraMessageDAO cassandraMessageDAO = mock(CassandraMessageDAO.class);
        CassandraAttachmentMessageIdDAO attachmentMessageIdDAO = mock(CassandraAttachmentMessageIdDAO.class);
        CassandraMessageDAO.MessageIdAttachmentIds messageIdAttachmentIds = mock(CassandraMessageDAO.MessageIdAttachmentIds.class);

        AttachmentMessageIdCreation migration = new AttachmentMessageIdCreation(cassandraMessageDAO, attachmentMessageIdDAO);

        when(messageIdAttachmentIds.getAttachmentId()).thenReturn(ImmutableSet.of(AttachmentId.from("any")));
        when(cassandraMessageDAO.retrieveAllMessageIdAttachmentIds())
            .thenReturn(CompletableFuture.completedFuture(Stream.of(messageIdAttachmentIds)));
        when(attachmentMessageIdDAO.storeAttachmentForMessageId(any(AttachmentId.class), any(MessageId.class)))
            .thenThrow(new RuntimeException());

        assertThat(migration.run()).isEqualTo(Migration.Result.PARTIAL);
    }

    default SimpleMailboxMessage createMessage(MessageId messageId, Collection<MessageAttachment> attachments) {
        MessageUid messageUid = MessageUid.of(1);
        CassandraId mailboxId = CassandraId.timeBased();
        String content = "Subject: Any subject \n\nThis is the body\n.\n";
        int bodyStart = 22;

        return SimpleMailboxMessage.builder()
            .messageId(messageId)
            .mailboxId(mailboxId)
            .uid(messageUid)
            .internalDate(new Date())
            .bodyStartOctet(bodyStart)
            .size(content.length())
            .content(new SharedByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
            .flags(new Flags())
            .propertyBuilder(new PropertyBuilder())
            .addAttachments(attachments)
            .build();
    }

    default MessageAttachment createAttachment() {
        return MessageAttachment.builder()
            .attachment(Attachment.builder()
                .bytes("content".getBytes(StandardCharsets.UTF_8))
                .type("type")
                .build())
            .build();
    }
}