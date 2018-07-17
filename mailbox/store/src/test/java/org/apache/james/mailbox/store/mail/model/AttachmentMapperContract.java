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

package org.apache.james.mailbox.store.mail.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import org.apache.james.mailbox.exception.AttachmentNotFoundException;
import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.AttachmentId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

public interface AttachmentMapperContract {
    AttachmentId UNKNOWN_ATTACHMENT_ID = AttachmentId.from("unknown");
    Username OWNER = Username.fromRawValue("owner");
    Username ADDITIONAL_OWNER = Username.fromRawValue("additionalOwner");

    AttachmentMapper testee();

    MessageId generateMessageId();

    @Test
    default void getAttachmentShouldThrowWhenNullAttachmentId() {
        assertThatThrownBy(() -> testee().getAttachment(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    default void getAttachmentShouldThrowWhenNonReferencedAttachmentId() {
        assertThatThrownBy(() -> testee().getAttachment(UNKNOWN_ATTACHMENT_ID))
            .isInstanceOf(AttachmentNotFoundException.class);
    }

    @Test
    default void getAttachmentShouldReturnTheAttachmentWhenReferenced() throws Exception {
        //Given
        Attachment expected = Attachment.builder()
                .bytes("payload".getBytes(StandardCharsets.UTF_8))
                .type("content")
                .build();
        AttachmentId attachmentId = expected.getAttachmentId();
        testee().storeAttachmentForOwner(expected, OWNER);
        //When
        Attachment attachment = testee().getAttachment(attachmentId);
        //Then
        assertThat(attachment).isEqualTo(expected);
    }

    @Test
    default void getAttachmentShouldReturnTheAttachmentsWhenMultipleStored() throws Exception {
        //Given
        Attachment expected1 = Attachment.builder()
                .bytes("payload1".getBytes(StandardCharsets.UTF_8))
                .type("content1")
                .build();
        Attachment expected2 = Attachment.builder()
                .bytes("payload2".getBytes(StandardCharsets.UTF_8))
                .type("content2")
                .build();
        AttachmentId attachmentId1 = expected1.getAttachmentId();
        AttachmentId attachmentId2 = expected2.getAttachmentId();
        //When
        testee().storeAttachmentsForMessage(ImmutableList.of(expected1, expected2), generateMessageId());
        //Then
        Attachment attachment1 = testee().getAttachment(attachmentId1);
        Attachment attachment2 = testee().getAttachment(attachmentId2);
        assertThat(attachment1).isEqualTo(expected1);
        assertThat(attachment2).isEqualTo(expected2);
    }

    @Test
    default void getAttachmentsShouldThrowWhenNullAttachmentId() {
        assertThatThrownBy(() -> testee().getAttachments(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    default void getAttachmentsShouldReturnEmptyListWhenNonReferencedAttachmentId() {
        List<Attachment> attachments = testee().getAttachments(ImmutableList.of(UNKNOWN_ATTACHMENT_ID));

        assertThat(attachments).isEmpty();
    }

    @Test
    default void getAttachmentsShouldReturnTheAttachmentsWhenSome() throws Exception {
        //Given
        Attachment expected = Attachment.builder()
                .bytes("payload".getBytes(StandardCharsets.UTF_8))
                .type("content")
                .build();
        AttachmentId attachmentId = expected.getAttachmentId();
        testee().storeAttachmentForOwner(expected, OWNER);

        Attachment expected2 = Attachment.builder()
                .bytes("payload2".getBytes(StandardCharsets.UTF_8))
                .type("content")
                .build();
        AttachmentId attachmentId2 = expected2.getAttachmentId();
        testee().storeAttachmentForOwner(expected2, OWNER);

        //When
        List<Attachment> attachments = testee().getAttachments(ImmutableList.of(attachmentId, attachmentId2));
        //Then
        assertThat(attachments).contains(expected, expected2);
    }

    @Test
    default void getOwnerMessageIdsShouldReturnEmptyWhenNone() throws Exception {
        Collection<MessageId> messageIds = testee().getRelatedMessageIds(UNKNOWN_ATTACHMENT_ID);

        assertThat(messageIds).isEmpty();
    }

    @Test
    default void getOwnerMessageIdsShouldReturnEmptyWhenStoredWithoutMessageId() throws Exception {
        //Given
        Attachment attachment = Attachment.builder()
                .bytes("payload".getBytes(StandardCharsets.UTF_8))
                .type("content")
                .build();
        AttachmentId attachmentId = attachment.getAttachmentId();
        testee().storeAttachmentForOwner(attachment, OWNER);
        
        //When
        Collection<MessageId> messageIds = testee().getRelatedMessageIds(attachmentId);
        //Then
        assertThat(messageIds).isEmpty();
    }

    @Test
    default void getOwnerMessageIdsShouldReturnMessageIdWhenStoredWithMessageId() throws Exception {
        //Given
        Attachment attachment = Attachment.builder()
                .bytes("payload".getBytes(StandardCharsets.UTF_8))
                .type("content")
                .build();
        AttachmentId attachmentId = attachment.getAttachmentId();
        MessageId messageId = generateMessageId();
        testee().storeAttachmentsForMessage(ImmutableList.of(attachment), messageId);
        
        //When
        Collection<MessageId> messageIds = testee().getRelatedMessageIds(attachmentId);
        //Then
        assertThat(messageIds).containsOnly(messageId);
    }

    @Test
    default void getOwnerMessageIdsShouldReturnTwoMessageIdsWhenStoredTwice() throws Exception {
        //Given
        Attachment attachment = Attachment.builder()
                .bytes("payload".getBytes(StandardCharsets.UTF_8))
                .type("content")
                .build();
        AttachmentId attachmentId = attachment.getAttachmentId();
        MessageId messageId1 = generateMessageId();
        MessageId messageId2 = generateMessageId();
        testee().storeAttachmentsForMessage(ImmutableList.of(attachment), messageId1);
        testee().storeAttachmentsForMessage(ImmutableList.of(attachment), messageId2);
        
        //When
        Collection<MessageId> messageIds = testee().getRelatedMessageIds(attachmentId);
        //Then
        assertThat(messageIds).containsOnly(messageId1, messageId2);
    }

    @Test
    default void getOwnerMessageIdsShouldReturnOnlyMatchingMessageId() throws Exception {
        //Given
        Attachment attachment = Attachment.builder()
                .bytes("payload".getBytes(StandardCharsets.UTF_8))
                .type("content")
                .build();
        Attachment otherAttachment = Attachment.builder()
                .bytes("something different".getBytes(StandardCharsets.UTF_8))
                .type("content")
                .build();
        AttachmentId attachmentId = attachment.getAttachmentId();
        MessageId messageId1 = generateMessageId();
        MessageId messageId2 = generateMessageId();
        testee().storeAttachmentsForMessage(ImmutableList.of(attachment), messageId1);
        testee().storeAttachmentsForMessage(ImmutableList.of(otherAttachment), messageId2);
        
        //When
        Collection<MessageId> messageIds = testee().getRelatedMessageIds(attachmentId);
        //Then
        assertThat(messageIds).containsOnly(messageId1);
    }

    @Test
    default void getOwnerMessageIdsShouldReturnOnlyOneMessageIdWhenStoredTwice() throws Exception {
        //Given
        Attachment attachment = Attachment.builder()
                .bytes("payload".getBytes(StandardCharsets.UTF_8))
                .type("content")
                .build();
        AttachmentId attachmentId = attachment.getAttachmentId();
        MessageId messageId = generateMessageId();
        testee().storeAttachmentsForMessage(ImmutableList.of(attachment), messageId);
        testee().storeAttachmentsForMessage(ImmutableList.of(attachment), messageId);
        
        //When
        Collection<MessageId> messageIds = testee().getRelatedMessageIds(attachmentId);
        //Then
        assertThat(messageIds).containsOnly(messageId);
    }

    @Test
    default void getOwnerMessageIdsShouldReturnMessageIdForTwoAttachmentsWhenBothStoredAtTheSameTime() throws Exception {
        //Given
        Attachment attachment = Attachment.builder()
                .bytes("payload".getBytes(StandardCharsets.UTF_8))
                .type("content")
                .build();
        Attachment attachment2 = Attachment.builder()
                .bytes("other payload".getBytes(StandardCharsets.UTF_8))
                .type("content")
                .build();
        AttachmentId attachmentId = attachment.getAttachmentId();
        AttachmentId attachmentId2 = attachment2.getAttachmentId();
        MessageId messageId = generateMessageId();
        testee().storeAttachmentsForMessage(ImmutableList.of(attachment, attachment2), messageId);
        
        //When
        Collection<MessageId> messageIds = testee().getRelatedMessageIds(attachmentId);
        Collection<MessageId> messageIds2 = testee().getRelatedMessageIds(attachmentId2);
        //Then
        assertThat(messageIds).isEqualTo(messageIds2);
    }

    @Test
    default void getOwnersShouldBeRetrievedWhenExplicitlySpecified() throws Exception {
        //Given
        Attachment attachment = Attachment.builder()
            .bytes("payload".getBytes(StandardCharsets.UTF_8))
            .type("content")
            .build();

        AttachmentId attachmentId = attachment.getAttachmentId();
        testee().storeAttachmentForOwner(attachment, OWNER);

        //When
        Collection<Username> expectedOwners = ImmutableList.of(OWNER);
        Collection<Username> actualOwners = testee().getOwners(attachmentId);
        //Then
        assertThat(actualOwners).containsOnlyElementsOf(expectedOwners);
    }

    @Test
    default void getOwnersShouldReturnEmptyWhenMessageIdReferenced() throws Exception {
        //Given
        Attachment attachment = Attachment.builder()
            .bytes("payload".getBytes(StandardCharsets.UTF_8))
            .type("content")
            .build();

        AttachmentId attachmentId = attachment.getAttachmentId();
        testee().storeAttachmentsForMessage(ImmutableList.of(attachment), generateMessageId());

        //When
        Collection<Username> actualOwners = testee().getOwners(attachmentId);
        //Then
        assertThat(actualOwners).isEmpty();
    }

    @Test
    default void getOwnersShouldReturnAllOwners() throws Exception {
        //Given
        Attachment attachment = Attachment.builder()
            .bytes("payload".getBytes(StandardCharsets.UTF_8))
            .type("content")
            .build();

        AttachmentId attachmentId = attachment.getAttachmentId();
        testee().storeAttachmentForOwner(attachment, OWNER);
        testee().storeAttachmentForOwner(attachment, ADDITIONAL_OWNER);

        //When
        Collection<Username> expectedOwners = ImmutableList.of(OWNER, ADDITIONAL_OWNER);
        Collection<Username> actualOwners = testee().getOwners(attachmentId);
        //Then
        assertThat(actualOwners).containsOnlyElementsOf(expectedOwners);
    }

    @Test
    default void getOwnersShouldReturnEmptyWhenUnknownAttachmentId() throws Exception {
        Collection<Username> actualOwners = testee().getOwners(AttachmentId.from("any"));

        assertThat(actualOwners).isEmpty();
    }
}
