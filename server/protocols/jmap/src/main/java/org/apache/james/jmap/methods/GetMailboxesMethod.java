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

package org.apache.james.jmap.methods;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.NotImplementedException;
import org.apache.james.jmap.model.AuthenticatedProtocolRequest;
import org.apache.james.jmap.model.GetMailboxesRequest;
import org.apache.james.jmap.model.GetMailboxesResponse;
import org.apache.james.jmap.model.Mailbox;
import org.apache.james.jmap.model.ProtocolRequest;
import org.apache.james.jmap.model.ProtocolResponse;
import org.apache.james.jmap.model.Role;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageManager.MetaData;
import org.apache.james.mailbox.MessageManager.MetaData.FetchGroup;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.MailboxMapperFactory;
import org.apache.james.mailbox.store.mail.model.MailboxId;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class GetMailboxesMethod<Id extends MailboxId> implements Method {
    
    private ProtocolArgumentsManager protocolArgumentsManager;
    private MailboxManager mailboxManager; 
    private MailboxMapperFactory<Id> mailboxMapperFactory;

    @Inject
    @VisibleForTesting public GetMailboxesMethod(ProtocolArgumentsManager protocolArgumentsManager, MailboxManager mailboxManager, MailboxMapperFactory<Id> mailboxMapperFactory) {
        this.protocolArgumentsManager = protocolArgumentsManager;
        this.mailboxManager = mailboxManager;
        this.mailboxMapperFactory = mailboxMapperFactory;
    }

    public String methodName() {
        return "getMailboxes";
    }

    public ProtocolResponse process(ProtocolRequest request) {
        try {
            protocolArgumentsManager.extractJmapRequest(request, GetMailboxesRequest.class);
        } catch (IOException e) {
            if (e.getCause() instanceof NotImplementedException) {
                return protocolArgumentsManager.formatErrorResponse(request, "Not yet implemented");
            } else {
                return protocolArgumentsManager.formatErrorResponse(request, "invalidArguments");
            }
        }
        try {
            MailboxSession mailboxSession = ((AuthenticatedProtocolRequest)request).getMailboxSession();
            List<MailboxPath> list = mailboxManager.list(mailboxSession);
            Builder<Mailbox> mailboxes = ImmutableList.builder();
            for (MailboxPath mailboxPath: list) {
                mailboxes.add(mailboxFromMailboxPath(mailboxPath, mailboxSession));
            }
            GetMailboxesResponse getMailboxesResponse = GetMailboxesResponse.builder()
                    .list(mailboxes.build())
                    .build();
            return protocolArgumentsManager.formatMethodResponse(request, getMailboxesResponse);
        } catch (MailboxException e) {
            return protocolArgumentsManager.formatErrorResponse(request);
        }
    }
    
    private Mailbox mailboxFromMailboxPath(MailboxPath mailboxPath, MailboxSession mailboxSession) throws MailboxException {
        org.apache.james.mailbox.store.mail.model.Mailbox<Id> mailbox = mailboxMapperFactory.getMailboxMapper(mailboxSession).findMailboxByPath(mailboxPath);
        MessageManager messageManager = mailboxManager.getMailbox(mailboxPath, mailboxSession);
        MetaData metaData = messageManager.getMetaData(false, mailboxSession, FetchGroup.UNSEEN_COUNT);
        return Mailbox.builder()
                .id(mailbox.getMailboxId().serialize())
                .name(mailboxPath.getName())
                .role(Role.from(mailboxPath.getName()))
                .unreadMessages(metaData.getUnseenCount())
                .build();
    }

}
