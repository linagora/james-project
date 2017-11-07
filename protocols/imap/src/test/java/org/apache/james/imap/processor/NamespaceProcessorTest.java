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
package org.apache.james.imap.processor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapSessionState;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapProcessor.Responder;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.message.request.NamespaceRequest;
import org.apache.james.imap.message.response.NamespaceResponse;
import org.apache.james.imap.message.response.UnpooledStatusResponseFactory;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.metrics.api.NoopMetricFactory;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class NamespaceProcessorTest {

    private static final String SHARED_PREFIX = "SharedPrefix";
    private static final String USERS_PREFIX = "UsersPrefix";
    private static final String PERSONAL_PREFIX = "PersonalPrefix";
    
    NamespaceProcessor subject;
    StatusResponseFactory statusResponseStub;
    ImapSession imapSessionStub;
    MailboxSession mailboxSessionStub;
    NamespaceRequest namespaceRequest;
    Collection<String> sharedSpaces;
    MailboxManager mailboxManagerStub;

    @Before
    public void setUp() throws Exception {
        sharedSpaces = new ArrayList<>();
        statusResponseStub = new UnpooledStatusResponseFactory();
        mailboxManagerStub = mock(MailboxManager.class);
        imapSessionStub = mock(ImapSession.class);
        mailboxSessionStub = mock(MailboxSession.class);

        when(imapSessionStub.getState()).thenReturn(ImapSessionState.AUTHENTICATED);
        when(imapSessionStub.getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY))
            .thenReturn(mailboxSessionStub);
        when(mailboxSessionStub.getPathDelimiter())
            .thenReturn(MailboxConstants.DEFAULT_DELIMITER);
     
        namespaceRequest = new NamespaceRequest(ImapCommand.anyStateCommand("Name"), "TAG");
    }
    

    
    @Test
    public void testNamespaceResponseShouldContainPersonalAndUserSpaces() throws Exception {
        when(imapSessionStub.getNamespaceConfiguration())
            .thenReturn(new ImapSession.NamespaceConfiguration() {
                @Override
                public String personalNamespace() {
                    return PERSONAL_PREFIX;
                }

                @Override
                public String otherUsersNamespace() {
                    return USERS_PREFIX;
                }

                @Override
                public List<String> sharedNamespacesNamespaces() {
                    return ImmutableList.of();
                }
            });
        subject = new NamespaceProcessor(mock(ImapProcessor.class),
            mailboxManagerStub,
            statusResponseStub,
            new NoopMetricFactory());

        Responder responder = mock(Responder.class);
        subject.doProcess(namespaceRequest, responder, imapSessionStub);

        NamespaceResponse response = buildResponse(ImmutableList.of());
        verify(responder).respond(response);
    }
    
    @Test
    public void testNamespaceResponseShouldContainSharedSpaces() throws Exception {
        when(imapSessionStub.getNamespaceConfiguration())
            .thenReturn(new ImapSession.NamespaceConfiguration() {
                @Override
                public String personalNamespace() {
                    return PERSONAL_PREFIX;
                }

                @Override
                public String otherUsersNamespace() {
                    return USERS_PREFIX;
                }

                @Override
                public List<String> sharedNamespacesNamespaces() {
                    return ImmutableList.of(SHARED_PREFIX);
                }
            });
        subject = new NamespaceProcessor(mock(ImapProcessor.class),
            mailboxManagerStub,
            statusResponseStub,
            new NoopMetricFactory());

        Responder responder = mock(Responder.class);
        subject.doProcess(namespaceRequest, responder, imapSessionStub);

        NamespaceResponse response = buildResponse(ImmutableList.of(new NamespaceResponse.Namespace(SHARED_PREFIX, MailboxConstants.DEFAULT_DELIMITER)));
        verify(responder).respond(response);

    }

    private NamespaceResponse buildResponse(List<NamespaceResponse.Namespace> sharedSpaces) {
       
        final List<NamespaceResponse.Namespace> personalSpaces = new ArrayList<>();
        personalSpaces.add(new NamespaceResponse.Namespace(PERSONAL_PREFIX, MailboxConstants.DEFAULT_DELIMITER));
        final List<NamespaceResponse.Namespace> otherUsersSpaces = new ArrayList<>();
        otherUsersSpaces.add(new NamespaceResponse.Namespace(USERS_PREFIX, MailboxConstants.DEFAULT_DELIMITER));

        return new NamespaceResponse(personalSpaces, otherUsersSpaces, sharedSpaces);
    }
}

