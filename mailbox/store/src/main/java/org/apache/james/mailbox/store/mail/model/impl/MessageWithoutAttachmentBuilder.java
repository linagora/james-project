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

import java.util.Date;

import javax.mail.internet.SharedInputStream;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.MessageWithoutAttachment;

public class MessageWithoutAttachmentBuilder {

    private Optional<MessageId> messageId;
    private Optional<SharedInputStream> content;
    private Optional<Integer> bodyStartOctet;
    private Optional<Date> internalDate;
    private Optional<Long> size;
    private Optional<PropertyBuilder> propertyBuilder;

    MessageWithoutAttachmentBuilder() {
        messageId = Optional.absent();
        content = Optional.absent();
        bodyStartOctet = Optional.absent();
        internalDate = Optional.absent();
        size = Optional.absent();
        propertyBuilder = Optional.absent();
    }

    public MessageWithoutAttachmentBuilder messageId(MessageId messageId) {
        this.messageId = Optional.of(messageId);
        return this;
    }

    public MessageWithoutAttachmentBuilder content(SharedInputStream content) {
        this.content = Optional.of(content);
        return this;
    }

    public MessageWithoutAttachmentBuilder bodyStartOctet(int bodyStartOctet) {
        this.bodyStartOctet = Optional.of(bodyStartOctet);
        return this;
    }

    public MessageWithoutAttachmentBuilder internalDate(Date internalDate) {
        this.internalDate = Optional.of(internalDate);
        return this;
    }

    public MessageWithoutAttachmentBuilder size(long size) {
        this.size = Optional.of(size);
        return this;
    }
    public MessageWithoutAttachmentBuilder propertyBuilder(PropertyBuilder propertyBuilder) {
        this.propertyBuilder = Optional.of(propertyBuilder);
        return this;
    }

    public MessageWithoutAttachment build() {
        Preconditions.checkState(messageId.isPresent(), "messageId is required");
        Preconditions.checkState(content.isPresent(), "content is required");
        Preconditions.checkState(size.isPresent(), "size is required");
        Preconditions.checkState(internalDate.isPresent(), "internalDate is required");
        Preconditions.checkState(bodyStartOctet.isPresent(), "bodyStartOctet is required");
        Preconditions.checkState(propertyBuilder.isPresent(), "property builder is required");

        return new MessageWithoutAttachmentImpl(
            messageId.get(),
            content.get(),
            size.get(),
            internalDate.get(),
            propertyBuilder.get().getSubType(),
            propertyBuilder.get().getMediaType(),
            bodyStartOctet.get(),
            propertyBuilder.get().getTextualLineCount(),
            propertyBuilder.get().toProperties());
    }
}
