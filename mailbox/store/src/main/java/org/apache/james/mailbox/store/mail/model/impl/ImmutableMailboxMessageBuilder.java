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
import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.SharedInputStream;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.ImmutableMailboxMessage;
import org.apache.james.mailbox.store.mail.model.MutableMailboxMessage;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class ImmutableMailboxMessageBuilder {
    private final ImmutableMailboxMessageWithoutAttachmentBuilder mailboxMessageWithoutAttachmentBuilder;
    private Optional<Collection<MessageAttachment>> attachments;

    ImmutableMailboxMessageBuilder() {
        mailboxMessageWithoutAttachmentBuilder = new ImmutableMailboxMessageWithoutAttachmentBuilder();
        attachments = Optional.absent();
    }

    public ImmutableMailboxMessageBuilder attachments(Collection<MessageAttachment> attachments) {
        this.attachments = Optional.of(attachments);
        return this;
    }

    public ImmutableMailboxMessageBuilder messageId(MessageId messageId) {
        mailboxMessageWithoutAttachmentBuilder.messageId(messageId);
        return this;
    }

    public ImmutableMailboxMessageBuilder content(SharedInputStream content) {
        mailboxMessageWithoutAttachmentBuilder.content(content);
        return this;
    }

    public ImmutableMailboxMessageBuilder bodyStartOctet(int bodyStartOctet) {
        Preconditions.checkArgument(bodyStartOctet >= 0, "bodyStartOctet must be positive");
        mailboxMessageWithoutAttachmentBuilder.bodyStartOctet(bodyStartOctet);
        return this;
    }

    public ImmutableMailboxMessageBuilder internalDate(Date internalDate) {
        mailboxMessageWithoutAttachmentBuilder.internalDate(internalDate);
        return this;
    }

    public ImmutableMailboxMessageBuilder size(long size) {
        Preconditions.checkArgument(size >= 0, "size must be positive");
        mailboxMessageWithoutAttachmentBuilder.size(size);
        return this;
    }

    public ImmutableMailboxMessageBuilder propertyBuilder(PropertyBuilder propertyBuilder) {
        mailboxMessageWithoutAttachmentBuilder.propertyBuilder(propertyBuilder);
        return this;
    }

    public ImmutableMailboxMessageBuilder mailboxId(MailboxId mailboxId) {
        mailboxMessageWithoutAttachmentBuilder.mailboxId(mailboxId);
        return this;
    }

    public ImmutableMailboxMessageBuilder uid(MessageUid uid) {
        mailboxMessageWithoutAttachmentBuilder.uid(uid);
        return this;
    }

    public ImmutableMailboxMessageBuilder modSeq(long modSeq) {
        Preconditions.checkArgument(modSeq >= 0, "modSeq must be positive");
        mailboxMessageWithoutAttachmentBuilder.modSeq(modSeq);
        return this;
    }

    public ImmutableMailboxMessageBuilder flags(Flags flags) {
        mailboxMessageWithoutAttachmentBuilder.flags(flags);
        return this;
    }

    public ImmutableMailboxMessageBuilder idWithMetatData(ComposedMessageIdWithMetaData idWithMetaData) {
        mailboxMessageWithoutAttachmentBuilder.idWithMetatData(idWithMetaData);
        return this;
    }

    public ImmutableMailboxMessage build() {
        Preconditions.checkState(attachments.isPresent(), "attachments is required");
        return MessageUtil.addAttachmentToImmutableMailboxMessage(
            mailboxMessageWithoutAttachmentBuilder.build(),
            attachments.get()
        );
    }
}
