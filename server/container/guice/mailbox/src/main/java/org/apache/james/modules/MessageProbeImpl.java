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

package org.apache.james.modules;

import java.util.Collection;

import javax.inject.Inject;

import org.apache.james.mailbox.AttachmentManager;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.model.AttachmentId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.MailboxMapperFactory;
import org.apache.james.mailbox.store.probe.MessageProbe;
import org.apache.james.utils.GuiceProbe;

public class MessageProbeImpl implements GuiceProbe, MessageProbe {

    private final MailboxManager mailboxManager;
    private final AttachmentManager attachmentManager;

    @Inject
    private MessageProbeImpl(MailboxManager mailboxManager, AttachmentManager attachmentManager) {
        this.mailboxManager = mailboxManager;
        this.attachmentManager = attachmentManager;
    }

    @Override
    public Collection<MessageId> getRelatedMessageIds(AttachmentId attachmentId, String user) throws Exception {
        MailboxSession mailboxSession = mailboxManager.createSystemSession(user);
        return attachmentManager.getRelatedMessageIds(attachmentId, mailboxSession);
    }

}