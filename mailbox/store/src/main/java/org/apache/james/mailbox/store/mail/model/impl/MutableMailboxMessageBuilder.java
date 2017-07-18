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
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.MutableMailboxMessage;

import javax.mail.Flags;
import javax.mail.internet.SharedInputStream;
import java.util.Collection;
import java.util.Date;

public class MutableMailboxMessageBuilder {
    private final MutableMailboxMessageWithoutAttachmentBuilder mailboxMessageWithoutAttachmentBuilder;
    private Optional<Collection<MessageAttachment>> attachments;

    MutableMailboxMessageBuilder() {
        mailboxMessageWithoutAttachmentBuilder = new MutableMailboxMessageWithoutAttachmentBuilder();
        attachments = Optional.absent();
    }

    public MutableMailboxMessageBuilder attachments(Collection<MessageAttachment> attachments) {
        this.attachments = Optional.of(attachments);
        return this;
    }

    public MutableMailboxMessageBuilder messageId(MessageId messageId) {
        mailboxMessageWithoutAttachmentBuilder.messageId(messageId);
        return this;
    }

    public MutableMailboxMessageBuilder content(SharedInputStream content) {
        mailboxMessageWithoutAttachmentBuilder.content(content);
        return this;
    }

    public MutableMailboxMessageBuilder bodyStartOctet(int bodyStartOctet) {
        Preconditions.checkArgument(bodyStartOctet >= 0, "bodyStartOctet must be positive");
        mailboxMessageWithoutAttachmentBuilder.bodyStartOctet(bodyStartOctet);
        return this;
    }

    public MutableMailboxMessageBuilder internalDate(Date internalDate) {
        mailboxMessageWithoutAttachmentBuilder.internalDate(internalDate);
        return this;
    }

    public MutableMailboxMessageBuilder size(long size) {
        Preconditions.checkArgument(size >= 0, "size must be positive");
        mailboxMessageWithoutAttachmentBuilder.size(size);
        return this;
    }

    public MutableMailboxMessageBuilder propertyBuilder(PropertyBuilder propertyBuilder) {
        mailboxMessageWithoutAttachmentBuilder.propertyBuilder(propertyBuilder);
        return this;
    }

    public MutableMailboxMessageBuilder mailboxId(MailboxId mailboxId) {
        mailboxMessageWithoutAttachmentBuilder.mailboxId(mailboxId);
        return this;
    }

    public MutableMailboxMessageBuilder uid(MessageUid uid) {
        mailboxMessageWithoutAttachmentBuilder.uid(uid);
        return this;
    }

    public MutableMailboxMessageBuilder modSeq(long modSeq) {
        Preconditions.checkArgument(modSeq >= 0, "modSeq must be positive");
        mailboxMessageWithoutAttachmentBuilder.modSeq(modSeq);
        return this;
    }

    public MutableMailboxMessageBuilder flags(Flags flags) {
        mailboxMessageWithoutAttachmentBuilder.flags(flags);
        return this;
    }

    public MutableMailboxMessageBuilder idWithMetatData(ComposedMessageIdWithMetaData idWithMetaData) {
        mailboxMessageWithoutAttachmentBuilder.idWithMetatData(idWithMetaData);
        return this;
    }

    public MutableMailboxMessage build() {
        Preconditions.checkState(attachments.isPresent(), "attachments is required");
        return MessageUtil.addAttachmentToMutableMailboxMessage(
            mailboxMessageWithoutAttachmentBuilder.build(),
            attachments.get()
        );
    }
}
