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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.HasMailboxContext;
import org.apache.james.mailbox.store.mail.model.ImmutableMailboxMessage;
import org.apache.james.mailbox.store.mail.model.ImmutableMailboxMessageWithoutAttachment;
import org.apache.james.mailbox.store.mail.model.MutableMailboxMessage;
import org.apache.james.mailbox.store.mail.model.MutableMailboxMessageWithoutAttachment;
import org.apache.james.mailbox.store.mail.model.Property;

import com.google.common.collect.ImmutableList;

class ImmutableMailboxMessageImpl implements ImmutableMailboxMessage {

    private final ImmutableMailboxMessageWithoutAttachment message;
    private final ImmutableList<MessageAttachment> attachments;

    public ImmutableMailboxMessageImpl(ImmutableMailboxMessageWithoutAttachment message, Collection<MessageAttachment> attachments) {
        this.message = message;
        this.attachments = ImmutableList.copyOf(attachments);
    }

    @Override
    public int compareTo(HasMailboxContext o) {
        return message.compareTo(o);
    }

    @Override
    public MailboxId getMailboxId() {
        return message.getMailboxId();
    }

    @Override
    public MessageUid getUid() {
        return message.getUid();
    }

    @Override
    public long getModSeq() {
        return message.getModSeq();
    }

    @Override
    public boolean isAnswered() {
        return message.isAnswered();
    }

    @Override
    public boolean isDeleted() {
        return message.isDeleted();
    }

    @Override
    public boolean isDraft() {
        return message.isDraft();
    }

    @Override
    public boolean isFlagged() {
        return message.isFlagged();
    }

    @Override
    public boolean isRecent() {
        return message.isRecent();
    }

    @Override
    public boolean isSeen() {
        return message.isSeen();
    }

    @Override
    public Flags createFlags() {
        return message.createFlags();
    }

    @Override
    public MessageId getMessageId() {
        return message.getMessageId();
    }

    @Override
    public Date getInternalDate() {
        return message.getInternalDate();
    }

    @Override
    public InputStream getBodyContent() throws IOException {
        return message.getBodyContent();
    }

    @Override
    public String getMediaType() {
        return message.getMediaType();
    }

    @Override
    public String getSubType() {
        return message.getSubType();
    }

    @Override
    public long getBodyOctets() {
        return message.getBodyOctets();
    }

    @Override
    public long getFullContentOctets() {
        return message.getFullContentOctets();
    }

    @Override
    public long getHeaderOctets() {
        return message.getHeaderOctets();
    }

    @Override
    public Long getTextualLineCount() {
        return message.getTextualLineCount();
    }

    @Override
    public InputStream getHeaderContent() throws IOException {
        return message.getHeaderContent();
    }

    @Override
    public InputStream getFullContent() throws IOException {
        return message.getFullContent();
    }

    @Override
    public List<Property> getProperties() {
        return message.getProperties();
    }

    @Override
    public final boolean equals(Object other) {
        if (other instanceof HasMailboxContext) {
            return this.getUid().compareTo(((HasMailboxContext) other).getUid()) == 0;
        }

        return false;
    }

    @Override
    public List<MessageAttachment> getAttachments() {
        return attachments;
    }
}
