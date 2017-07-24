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
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.HasMailboxContext;
import org.apache.james.mailbox.store.mail.model.ImmutableMailboxMessage;
import org.apache.james.mailbox.store.mail.model.MutableMailboxMessage;
import org.apache.james.mailbox.store.mail.model.Property;

import javax.mail.Flags;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

public class FakeMutableMailboxMessage implements MutableMailboxMessage {
    private final ImmutableMailboxMessage immutableMailboxMessage;

    public FakeMutableMailboxMessage(ImmutableMailboxMessage immutableMailboxMessage) {
        this.immutableMailboxMessage = immutableMailboxMessage;
    }

    @Override
    public MessageId getMessageId() {
        return immutableMailboxMessage.getMessageId();
    }

    @Override
    public List<MessageAttachment> getAttachments() {
        return immutableMailboxMessage.getAttachments();
    }

    @Override
    public Date getInternalDate() {
        return immutableMailboxMessage.getInternalDate();
    }

    @Override
    public MailboxId getMailboxId() {
        return immutableMailboxMessage.getMailboxId();
    }

    @Override
    public MessageUid getUid() {
        return immutableMailboxMessage.getUid();
    }

    @Override
    public long getModSeq() {
        return immutableMailboxMessage.getModSeq();
    }

    @Override
    public InputStream getBodyContent() throws IOException {
        return immutableMailboxMessage.getBodyContent();
    }

    @Override
    public boolean isAnswered() {
        return immutableMailboxMessage.isAnswered();
    }

    @Override
    public boolean isDeleted() {
        return immutableMailboxMessage.isDeleted();
    }

    @Override
    public String getMediaType() {
        return immutableMailboxMessage.getMediaType();
    }

    @Override
    public boolean isDraft() {
        return immutableMailboxMessage.isDraft();
    }

    @Override
    public boolean isFlagged() {
        return immutableMailboxMessage.isFlagged();
    }

    @Override
    public String getSubType() {
        return immutableMailboxMessage.getSubType();
    }

    @Override
    public boolean isRecent() {
        return immutableMailboxMessage.isRecent();
    }

    @Override
    public long getBodyOctets() {
        return immutableMailboxMessage.getBodyOctets();
    }

    @Override
    public boolean isSeen() {
        return immutableMailboxMessage.isSeen();
    }

    @Override
    public long getFullContentOctets() {
        return immutableMailboxMessage.getFullContentOctets();
    }

    @Override
    public Flags createFlags() {
        return immutableMailboxMessage.createFlags();
    }

    @Override
    public long getHeaderOctets() {
        return immutableMailboxMessage.getHeaderOctets();
    }

    @Override
    public Long getTextualLineCount() {
        return immutableMailboxMessage.getTextualLineCount();
    }

    @Override
    public InputStream getHeaderContent() throws IOException {
        return immutableMailboxMessage.getHeaderContent();
    }

    @Override
    public InputStream getFullContent() throws IOException {
        return immutableMailboxMessage.getFullContent();
    }

    @Override
    public List<Property> getProperties() {
        return immutableMailboxMessage.getProperties();
    }

    @Override
    public int compareTo(HasMailboxContext o) {
        return immutableMailboxMessage.compareTo(o);
    }

    @Override
    public void setUid(MessageUid uid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setModSeq(long modSeq) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFlags(Flags flags) {
        throw new UnsupportedOperationException();
    }
}
