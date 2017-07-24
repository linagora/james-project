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

package org.apache.james.mailbox.store.mail.model.impl;

import java.util.Collection;

import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.store.mail.model.*;

public class MessageUtil {

    public static Message addAttachmentToMessage(
        MessageWithoutAttachment message,
        Collection<MessageAttachment> attachments
    ) {
        return new MessageImpl(message, attachments);
    }

    public static ImmutableMailboxMessage addAttachmentToImmutableMailboxMessage(
        ImmutableMailboxMessageWithoutAttachment message,
        Collection<MessageAttachment> attachments
    ) {
        return new ImmutableMailboxMessageImpl(message, attachments);
    }
    public static MutableMailboxMessage addAttachmentToMutableMailboxMessage(
        MutableMailboxMessageWithoutAttachment message,
        Collection<MessageAttachment> attachments
    ) {
        return new MutableMailboxMessageImpl(message, attachments);
    }

    public static MutableMailboxMessageWithoutAttachment addMutableMailboxContext(
        MessageWithoutAttachment message,
        ComposedMessageIdWithMetaData idAndMetaData
    ) {
        return new MutableMailboxMessageWithoutAttachmentImpl(message,
            buildMutableMailboxContext().
                idWithMetatData(idAndMetaData)
                .build());
    }

    public static MutableMailboxMessageWithoutAttachment addMutableMailboxContext(
        MessageWithoutAttachment message,
        MutableMailboxContext mailboxContext
    ) {
        return new MutableMailboxMessageWithoutAttachmentImpl(message, mailboxContext);
    }

    public static ImmutableMailboxMessageWithoutAttachment addImmutableMailboxContext(
        MessageWithoutAttachment message,
        ImmutableMailboxContext mailboxContext
    ) {
        return new ImmutableMailboxMessageWithoutAttachmentImpl(message, mailboxContext);
    }

    public static MessageWithoutAttachmentBuilder buildMessageWithoutAttachment() {
        return new MessageWithoutAttachmentBuilder();
    }

    public static MutableMailboxMessageWithoutAttachmentBuilder buildMutableMailboxMessageWithoutAttachment() {
        return new MutableMailboxMessageWithoutAttachmentBuilder();
    }

    public static MutableMailboxMessageBuilder buildMutableMailboxMessage() {
        return new MutableMailboxMessageBuilder();
    }


    public static ImmutableMailboxMessageBuilder buildImmutableMailboxMessage() {
        return new ImmutableMailboxMessageBuilder();
    }

    public static ImmutableMailboxMessageWithoutAttachmentBuilder buildImmutableMailboxMessageWithoutAttachment() {
        return new ImmutableMailboxMessageWithoutAttachmentBuilder();
    }

    public static MessageBuilder buildMessage() {
        return new MessageBuilder();
    }

    public static MutableMailboxMessage copyToMutable(MailboxMessage origin, MailboxId mailboxId, Collection<MessageAttachment> attachments) {
        return addAttachmentToMutableMailboxMessage(
            addMutableMailboxContext(
                origin,
                buildMutableMailboxContext()
                    .mailboxContext(origin)
                    .mailboxId(mailboxId).build()),
            attachments);
    }

    public static ImmutableMailboxMessage copyToImmutable(MailboxMessage origin, MailboxId mailboxId, Collection<MessageAttachment> attachments) {
        return addAttachmentToImmutableMailboxMessage(
            addImmutableMailboxContext(
                origin,
                buildImmutableMailboxContext()
                    .mailboxContext(origin)
                    .mailboxId(mailboxId).build()),
            attachments);
    }

    public static ImmutableMailboxMessage copyToImmutable(MailboxMessage origin, MailboxId mailboxId) {
        return copyToImmutable(origin, mailboxId, origin.getAttachments());
    }

    public static MutableMailboxMessage copyToMutable(MailboxMessage origin, MailboxId mailboxId) {
        return copyToMutable(origin, mailboxId, origin.getAttachments());
    }

    public static MutableMailboxContextBuilder buildMutableMailboxContext() {
        return new MutableMailboxContextBuilder();
    }

    public static ImmutableMailboxContextBuilder buildImmutableMailboxContext() {
        return new ImmutableMailboxContextBuilder();
    }

    public static <T extends HasMailboxContext & MessageWithoutAttachment> ComposedMessageIdWithMetaData getOnlyMetaData(T message) {
        return ComposedMessageIdWithMetaData.builder()
            .composedMessageId(
                new ComposedMessageId(
                    message.getMailboxId(),
                    message.getMessageId(),
                    message.getUid()))
            .flags(message.createFlags())
            .modSeq(message.getModSeq())
            .build();
    }

    public static MutableMailboxMessage mutableViewOfImmutable(ImmutableMailboxMessage immutableMailboxMessage) {
        return new FakeMutableMailboxMessage(immutableMailboxMessage);
    }
}
