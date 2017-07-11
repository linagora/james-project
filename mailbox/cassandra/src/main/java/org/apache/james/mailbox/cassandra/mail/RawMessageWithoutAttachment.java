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

package org.apache.james.mailbox.cassandra.mail;

import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import javax.mail.Flags;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMessage;

public class RawMessageWithoutAttachment {
    private final MessageId messageId;
    private final Date internalDate;
    private final Long size;
    private final Integer bodySize;
    private final SharedByteArrayInputStream content;
    private final PropertyBuilder propertyBuilder;

    public RawMessageWithoutAttachment(
        MessageId messageId,
        Date internalDate,
        Long size,
        Integer bodySize,
        SharedByteArrayInputStream content,
        PropertyBuilder propertyBuilder
    ) {
        this.messageId = messageId;
        this.internalDate = internalDate;
        this.size = size;
        this.bodySize = bodySize;
        this.content = content;
        this.propertyBuilder = propertyBuilder;
    }

    public SimpleMailboxMessage toMailboxMessage(MailboxId mailboxId, Flags flags, List<MessageAttachment> attachments) {
        return SimpleMailboxMessage.builder()
            .messageId(messageId)
            .mailboxId(mailboxId)
            .internalDate(internalDate)
            .bodyStartOctet(bodySize)
            .size(size)
            .content(content)
            .propertyBuilder(propertyBuilder)
            .addAttachments(attachments)
            .flags(flags)
            .build();
    }

    public MessageWithoutAttachment toMessageWithoutAttachment(ComposedMessageIdWithMetaData composedMessageIdWithMetaData) {
        ComposedMessageId composedMessageId = composedMessageIdWithMetaData.getComposedMessageId();

        return new MessageWithoutAttachment(
            composedMessageId.getMessageId(),
            internalDate,
            size,
            bodySize,
            content,
            composedMessageIdWithMetaData.getFlags(),
            propertyBuilder,
            composedMessageId.getMailboxId(),
            composedMessageId.getUid(),
            composedMessageIdWithMetaData.getModSeq()
        );
    }

    public MessageId getMessageId() {
        return messageId;
    }

    public SharedByteArrayInputStream getContent() {
        return content;
    }

    public PropertyBuilder getPropertyBuilder() {
        return propertyBuilder;
    }

    public SimpleMessage toMessage(List<MessageAttachment> attachments) {
        return new SimpleMessage(
                messageId,
                content,
                size,
                internalDate,
                propertyBuilder.getSubType(),
                propertyBuilder.getMediaType(),
                bodySize,
                propertyBuilder.getTextualLineCount(),
                propertyBuilder.toProperties(),
                attachments);
    }
}
