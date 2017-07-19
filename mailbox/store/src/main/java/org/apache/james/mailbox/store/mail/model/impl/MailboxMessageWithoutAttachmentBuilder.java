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

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.MailboxMessageWithoutAttachment;

import javax.mail.Flags;
import javax.mail.internet.SharedInputStream;
import java.util.Date;

public class MailboxMessageWithoutAttachmentBuilder {
    private MessageWithoutAttachmentBuilder messageWithoutAttachmentBuilder;
    private MailboxContextBuilder mailboxContextBuilder;

    MailboxMessageWithoutAttachmentBuilder() {
        messageWithoutAttachmentBuilder = new MessageWithoutAttachmentBuilder();
        mailboxContextBuilder = new MailboxContextBuilder();
    }

    public MailboxMessageWithoutAttachmentBuilder messageId(MessageId messageId) {
        messageWithoutAttachmentBuilder.messageId(messageId);
        return this;
    }

    public MailboxMessageWithoutAttachmentBuilder content(SharedInputStream content) {
        messageWithoutAttachmentBuilder.content(content);
        return this;
    }

    public MailboxMessageWithoutAttachmentBuilder bodyStartOctet(int bodyStartOctet) {
        messageWithoutAttachmentBuilder.bodyStartOctet(bodyStartOctet);
        return this;
    }

    public MailboxMessageWithoutAttachmentBuilder internalDate(Date internalDate) {
        messageWithoutAttachmentBuilder.internalDate(internalDate);
        return this;
    }

    public MailboxMessageWithoutAttachmentBuilder size(long size) {
        messageWithoutAttachmentBuilder.size(size);
        return this;
    }

    public MailboxMessageWithoutAttachmentBuilder propertyBuilder(PropertyBuilder propertyBuilder) {
        messageWithoutAttachmentBuilder.propertyBuilder(propertyBuilder);
        return this;
    }

    public MailboxMessageWithoutAttachmentBuilder mailboxId(MailboxId mailboxId) {
        mailboxContextBuilder.mailboxId(mailboxId);
        return this;
    }

    public MailboxMessageWithoutAttachmentBuilder uid(MessageUid uid) {
        mailboxContextBuilder.uid(uid);
        return this;
    }

    public MailboxMessageWithoutAttachmentBuilder modSeq(long modSeq) {
        mailboxContextBuilder.modSeq(modSeq);
        return this;
    }

    public MailboxMessageWithoutAttachmentBuilder flags(Flags flags) {
        mailboxContextBuilder.flags(flags);
        return this;
    }

    public MailboxMessageWithoutAttachmentBuilder idWithMetatData(ComposedMessageIdWithMetaData idWithMetaData) {
        this.messageId(idWithMetaData.getComposedMessageId().getMessageId());
        mailboxContextBuilder.idWithMetatData(idWithMetaData);
        return this;
    }

    public MailboxMessageWithoutAttachment build() {
        return MessageUtil.addMailboxContext(
            messageWithoutAttachmentBuilder.build(),
            mailboxContextBuilder.build());
    }
}
