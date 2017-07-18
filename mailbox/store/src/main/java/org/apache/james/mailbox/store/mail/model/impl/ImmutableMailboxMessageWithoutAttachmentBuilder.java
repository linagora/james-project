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

import javax.mail.Flags;
import javax.mail.internet.SharedInputStream;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.ImmutableMailboxMessageWithoutAttachment;
import org.apache.james.mailbox.store.mail.model.MutableMailboxMessageWithoutAttachment;

public class ImmutableMailboxMessageWithoutAttachmentBuilder {
    private MessageWithoutAttachmentBuilder messageWithoutAttachmentBuilder;
    private ImmutableMailboxContextBuilder mailboxContextBuilder;

    ImmutableMailboxMessageWithoutAttachmentBuilder() {
        messageWithoutAttachmentBuilder = new MessageWithoutAttachmentBuilder();
        mailboxContextBuilder = new ImmutableMailboxContextBuilder();
    }

    public ImmutableMailboxMessageWithoutAttachmentBuilder messageId(MessageId messageId) {
        messageWithoutAttachmentBuilder.messageId(messageId);
        return this;
    }

    public ImmutableMailboxMessageWithoutAttachmentBuilder content(SharedInputStream content) {
        messageWithoutAttachmentBuilder.content(content);
        return this;
    }

    public ImmutableMailboxMessageWithoutAttachmentBuilder bodyStartOctet(int bodyStartOctet) {
        messageWithoutAttachmentBuilder.bodyStartOctet(bodyStartOctet);
        return this;
    }

    public ImmutableMailboxMessageWithoutAttachmentBuilder internalDate(Date internalDate) {
        messageWithoutAttachmentBuilder.internalDate(internalDate);
        return this;
    }

    public ImmutableMailboxMessageWithoutAttachmentBuilder size(long size) {
        messageWithoutAttachmentBuilder.size(size);
        return this;
    }

    public ImmutableMailboxMessageWithoutAttachmentBuilder propertyBuilder(PropertyBuilder propertyBuilder) {
        messageWithoutAttachmentBuilder.propertyBuilder(propertyBuilder);
        return this;
    }

    public ImmutableMailboxMessageWithoutAttachmentBuilder mailboxId(MailboxId mailboxId) {
        mailboxContextBuilder.mailboxId(mailboxId);
        return this;
    }

    public ImmutableMailboxMessageWithoutAttachmentBuilder uid(MessageUid uid) {
        mailboxContextBuilder.uid(uid);
        return this;
    }

    public ImmutableMailboxMessageWithoutAttachmentBuilder modSeq(long modSeq) {
        mailboxContextBuilder.modSeq(modSeq);
        return this;
    }

    public ImmutableMailboxMessageWithoutAttachmentBuilder flags(Flags flags) {
        mailboxContextBuilder.flags(flags);
        return this;
    }

    public ImmutableMailboxMessageWithoutAttachmentBuilder idWithMetatData(ComposedMessageIdWithMetaData idWithMetaData) {
        this.messageId(idWithMetaData.getComposedMessageId().getMessageId());
        mailboxContextBuilder.idWithMetatData(idWithMetaData);
        return this;
    }

    public ImmutableMailboxMessageWithoutAttachment build() {
        return MessageUtil.addImmutableMailboxContext(
            messageWithoutAttachmentBuilder.build(),
            mailboxContextBuilder.build());
    }
}
