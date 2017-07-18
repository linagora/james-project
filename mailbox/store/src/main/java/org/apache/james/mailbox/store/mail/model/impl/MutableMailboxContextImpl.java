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

import org.apache.james.mailbox.FlagsBuilder;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.store.mail.model.HasMailboxContext;
import org.apache.james.mailbox.store.mail.model.MutableMailboxContext;

import javax.mail.Flags;

class MutableMailboxContextImpl implements MutableMailboxContext {
    private final MailboxId mailboxId;
    private MessageUid uid;
    private long modSeq;
    private Flags flags;

    MutableMailboxContextImpl(MailboxId mailboxId, MessageUid uid, long modSeq, Flags flags) {
        this.mailboxId = mailboxId;
        this.uid = uid;
        this.modSeq = modSeq;
        this.flags = flags;
    }

    @Override
    public MailboxId getMailboxId() {
        return mailboxId;
    }

    @Override
    public MessageUid getUid() {
        return uid;
    }

    @Override
    public void setUid(MessageUid uid) {
        this.uid = uid;
    }

    @Override
    public void setModSeq(long modSeq) {
        this.modSeq = modSeq;
    }

    @Override
    public long getModSeq() {
        return modSeq;
    }

    @Override
    public boolean isAnswered() {
        return flags.contains(Flags.Flag.ANSWERED);
    }

    @Override
    public boolean isDeleted() {
        return flags.contains(Flags.Flag.DELETED);
    }

    @Override
    public boolean isDraft() {
        return flags.contains(Flags.Flag.DRAFT);
    }

    @Override
    public boolean isFlagged() {
        return flags.contains(Flags.Flag.FLAGGED);
    }

    @Override
    public boolean isRecent() {
        return flags.contains(Flags.Flag.RECENT);
    }

    @Override
    public boolean isSeen() {
        return flags.contains(Flags.Flag.SEEN);
    }

    @Override
    public void setFlags(Flags flags) {
        this.flags = FlagsBuilder.builder().add(flags).build();
    }

    @Override
    public Flags createFlags() {
        return FlagsBuilder.builder().add(flags).build();
    }

    @Override
    public int compareTo(HasMailboxContext o) {
        return this.uid.compareTo(o.getUid());
    }
}
