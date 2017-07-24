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
import org.apache.james.mailbox.store.mail.model.MutableMailboxMessageWithoutAttachment;

import javax.mail.Flags;
import javax.mail.internet.SharedInputStream;
import java.util.Date;

public class MutableMailboxMessageWithoutAttachmentBuilder {
    private MessageWithoutAttachmentBuilder messageWithoutAttachmentBuilder;
    private MutableMailboxContextBuilder mailboxContextBuilder;

    MutableMailboxMessageWithoutAttachmentBuilder() {
        messageWithoutAttachmentBuilder = new MessageWithoutAttachmentBuilder();
        mailboxContextBuilder = new MutableMailboxContextBuilder();
    }

    public MutableMailboxMessageWithoutAttachmentBuilder messageId(MessageId messageId) {
        messageWithoutAttachmentBuilder.messageId(messageId);
        return this;
    }

    public MutableMailboxMessageWithoutAttachmentBuilder content(SharedInputStream content) {
        messageWithoutAttachmentBuilder.content(content);
        return this;
    }

    public MutableMailboxMessageWithoutAttachmentBuilder bodyStartOctet(int bodyStartOctet) {
        messageWithoutAttachmentBuilder.bodyStartOctet(bodyStartOctet);
        return this;
    }

    public MutableMailboxMessageWithoutAttachmentBuilder internalDate(Date internalDate) {
        messageWithoutAttachmentBuilder.internalDate(internalDate);
        return this;
    }

    public MutableMailboxMessageWithoutAttachmentBuilder size(long size) {
        messageWithoutAttachmentBuilder.size(size);
        return this;
    }

    public MutableMailboxMessageWithoutAttachmentBuilder propertyBuilder(PropertyBuilder propertyBuilder) {
        messageWithoutAttachmentBuilder.propertyBuilder(propertyBuilder);
        return this;
    }

    public MutableMailboxMessageWithoutAttachmentBuilder mailboxId(MailboxId mailboxId) {
        mailboxContextBuilder.mailboxId(mailboxId);
        return this;
    }

    public MutableMailboxMessageWithoutAttachmentBuilder uid(MessageUid uid) {
        mailboxContextBuilder.uid(uid);
        return this;
    }

    public MutableMailboxMessageWithoutAttachmentBuilder modSeq(long modSeq) {
        mailboxContextBuilder.modSeq(modSeq);
        return this;
    }

    public MutableMailboxMessageWithoutAttachmentBuilder flags(Flags flags) {
        mailboxContextBuilder.flags(flags);
        return this;
    }

    public MutableMailboxMessageWithoutAttachmentBuilder idWithMetatData(ComposedMessageIdWithMetaData idWithMetaData) {
        this.messageId(idWithMetaData.getComposedMessageId().getMessageId());
        mailboxContextBuilder.idWithMetatData(idWithMetaData);
        return this;
    }

    public MutableMailboxMessageWithoutAttachment build() {
        return MessageUtil.addMutableMailboxContext(
            messageWithoutAttachmentBuilder.build(),
            mailboxContextBuilder.build());
    }
}
