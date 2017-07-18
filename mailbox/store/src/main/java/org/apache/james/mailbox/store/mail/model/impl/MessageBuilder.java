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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.MessageWithoutAttachment;

import javax.mail.internet.SharedInputStream;
import java.util.Collection;
import java.util.Date;

public class MessageBuilder {
    private final MessageWithoutAttachmentBuilder messageWithoutAttachmentBuilder;
    private Optional<Collection<MessageAttachment>> attachments;

    MessageBuilder() {
        messageWithoutAttachmentBuilder = new MessageWithoutAttachmentBuilder();
        attachments = Optional.absent();
    }

    public MessageBuilder attachments(Collection<MessageAttachment> attachments) {
        this.attachments = Optional.of(attachments);
        return this;
    }

    public MessageBuilder messageId(MessageId messageId) {
        messageWithoutAttachmentBuilder.messageId(messageId);
        return this;
    }

    public MessageBuilder content(SharedInputStream content) {
        messageWithoutAttachmentBuilder.content(content);
        return this;
    }

    public MessageBuilder bodyStartOctet(int bodyStartOctet) {
        messageWithoutAttachmentBuilder.bodyStartOctet(bodyStartOctet);
        return this;
    }

    public MessageBuilder internalDate(Date internalDate) {
        messageWithoutAttachmentBuilder.internalDate(internalDate);
        return this;
    }

    public MessageBuilder size(long size) {
        messageWithoutAttachmentBuilder.size(size);
        return this;
    }

    public MessageBuilder properties(PropertyBuilder propertyBuilder) {
        messageWithoutAttachmentBuilder.propertyBuilder(propertyBuilder);
        return this;
    }

    public Message build() {
        Preconditions.checkState(attachments.isPresent(), "attachments is required");
        MessageWithoutAttachment messageWithoutAttachment = messageWithoutAttachmentBuilder.build();

        return MessageUtil.addAttachmentToMessage(messageWithoutAttachment, attachments.get());
    }
}
