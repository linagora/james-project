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

import javax.mail.Flags;
import javax.mail.internet.SharedInputStream;
import java.util.Collection;
import java.util.Date;

public class MailboxMessageBuilder {
    private final MailboxMessageWithoutAttachmentBuilder mailboxMessageWithoutAttachmentBuilder;
    private Optional<Collection<MessageAttachment>> attachments;

    MailboxMessageBuilder() {
        mailboxMessageWithoutAttachmentBuilder = new MailboxMessageWithoutAttachmentBuilder();
        attachments = Optional.absent();
    }

    public MailboxMessageBuilder attachments(Collection<MessageAttachment> attachments) {
        this.attachments = Optional.of(attachments);
        return this;
    }

    public MailboxMessageBuilder messageId(MessageId messageId) {
        mailboxMessageWithoutAttachmentBuilder.messageId(messageId);
        return this;
    }

    public MailboxMessageBuilder content(SharedInputStream content) {
        mailboxMessageWithoutAttachmentBuilder.content(content);
        return this;
    }

    public MailboxMessageBuilder bodyStartOctet(int bodyStartOctet) {
        mailboxMessageWithoutAttachmentBuilder.bodyStartOctet(bodyStartOctet);
        return this;
    }

    public MailboxMessageBuilder internalDate(Date internalDate) {
        mailboxMessageWithoutAttachmentBuilder.internalDate(internalDate);
        return this;
    }

    public MailboxMessageBuilder size(long size) {
        mailboxMessageWithoutAttachmentBuilder.size(size);
        return this;
    }

    public MailboxMessageBuilder propertyBuilder(PropertyBuilder propertyBuilder) {
        mailboxMessageWithoutAttachmentBuilder.propertyBuilder(propertyBuilder);
        return this;
    }

    public MailboxMessageBuilder mailboxId(MailboxId mailboxId) {
        mailboxMessageWithoutAttachmentBuilder.mailboxId(mailboxId);
        return this;
    }

    public MailboxMessageBuilder uid(MessageUid uid) {
        mailboxMessageWithoutAttachmentBuilder.uid(uid);
        return this;
    }

    public MailboxMessageBuilder modSeq(long modSeq) {
        mailboxMessageWithoutAttachmentBuilder.modSeq(modSeq);
        return this;
    }

    public MailboxMessageBuilder flags(Flags flags) {
        mailboxMessageWithoutAttachmentBuilder.flags(flags);
        return this;
    }

    public MailboxMessageBuilder idWithMetatData(ComposedMessageIdWithMetaData idWithMetaData) {
        mailboxMessageWithoutAttachmentBuilder.idWithMetatData(idWithMetaData);
        return this;
    }

    public MailboxMessage build() {
        Preconditions.checkState(attachments.isPresent(), "attachments is required");
        return MessageUtil.addAttachmentToMailboxMessage(
            mailboxMessageWithoutAttachmentBuilder.build(),
            attachments.get()
        );
    }
}
