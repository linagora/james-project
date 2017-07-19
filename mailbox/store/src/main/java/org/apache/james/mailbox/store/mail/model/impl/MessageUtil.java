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
import org.apache.james.mailbox.store.mail.model.HasMailboxContext;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.MailboxMessageWithoutAttachment;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.MessageWithoutAttachment;

public class MessageUtil {

    public static Message addAttachmentToMessage(
        MessageWithoutAttachment message,
        Collection<MessageAttachment> attachments
    ) {
        return new MessageImpl(message, attachments);
    }

    public static MailboxMessage addAttachmentToMailboxMessage(
        MailboxMessageWithoutAttachment message,
        Collection<MessageAttachment> attachments
    ) {
        return new MailboxMessageImpl(message, attachments);
    }

    public static MailboxMessageWithoutAttachment addMailboxContext(
        MessageWithoutAttachment message,
        ComposedMessageIdWithMetaData idAndMetaData
    ) {
        return new MailboxMessageWithoutAttachmentImpl(message,
            buildMailboxContext().
                idWithMetatData(idAndMetaData)
                .build());
    }

    public static MailboxMessageWithoutAttachment addMailboxContext(
        MessageWithoutAttachment message,
        HasMailboxContext mailboxContext
    ) {
        return new MailboxMessageWithoutAttachmentImpl(message, mailboxContext);
    }

    public static MessageWithoutAttachmentBuilder buildMessageWithoutAttachment() {
        return new MessageWithoutAttachmentBuilder();
    }

    public static MailboxMessageWithoutAttachmentBuilder buildMailboxMessageWithoutAttachment() {
        return new MailboxMessageWithoutAttachmentBuilder();
    }


    public static MailboxMessageBuilder buildMailboxMessage() {
        return new MailboxMessageBuilder();
    }

    public static MessageBuilder buildMessage() {
        return new MessageBuilder();
    }

    public static MailboxMessage copy(MailboxMessage origin, MailboxId mailboxId, Collection<MessageAttachment> attachments) {
        return addAttachmentToMailboxMessage(
            addMailboxContext(
                origin,
                buildMailboxContext()
                    .mailboxContext(origin)
                    .mailboxId(mailboxId).build()),
            attachments);
    }

    public static MailboxMessage copy(MailboxMessage origin, MailboxId mailboxId) {
        return copy(origin, mailboxId, origin.getAttachments());
    }

    public static MailboxContextBuilder buildMailboxContext() {
        return new MailboxContextBuilder();
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
}
