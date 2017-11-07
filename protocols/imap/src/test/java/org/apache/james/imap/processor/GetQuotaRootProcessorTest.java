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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapSessionState;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.message.response.ImapResponseMessage;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.message.request.GetQuotaRootRequest;
import org.apache.james.imap.message.response.QuotaResponse;
import org.apache.james.imap.message.response.QuotaRootResponse;
import org.apache.james.imap.message.response.UnpooledStatusResponseFactory;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.quota.QuotaManager;
import org.apache.james.mailbox.quota.QuotaRootResolver;
import org.apache.james.mailbox.store.quota.QuotaImpl;
import org.apache.james.mailbox.store.quota.QuotaRootImpl;
import org.apache.james.metrics.api.NoopMetricFactory;
import org.apache.james.protocols.imap.DefaultNamespaceConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class GetQuotaRootProcessorTest {

    private static final QuotaRoot QUOTA_ROOT = QuotaRootImpl.quotaRoot("plop");
    public static final MailboxPath MAILBOX_PATH = MailboxPath.forUser("plop", "INBOX");
    public static final Quota MESSAGE_QUOTA = QuotaImpl.quota(24, 1589);
    public static final Quota STORAGE_QUOTA = QuotaImpl.quota(240, 15890);

    private GetQuotaRootProcessor testee;
    private ImapSession mockedImapSession;
    private ImapProcessor.Responder mockedResponder;
    private MailboxManager mockedMailboxManager;
    private MailboxSession mailboxSession;
    private ArgumentCaptor<ImapResponseMessage> argumentCaptor;

    @Before
    public void setUp() throws MailboxException {
        mailboxSession = new MockMailboxSession("plop");
        UnpooledStatusResponseFactory statusResponseFactory = new UnpooledStatusResponseFactory();
        mockedImapSession = mock(ImapSession.class);
        QuotaManager mockedQuotaManager = mock(QuotaManager.class);
        QuotaRootResolver mockedQuotaRootResolver = mock(QuotaRootResolver.class);
        mockedResponder = mock(ImapProcessor.Responder.class);
        mockedMailboxManager = mock(MailboxManager.class);

        when(mockedImapSession.getNamespaceConfiguration()).thenReturn(new DefaultNamespaceConfiguration());
        when(mockedImapSession.getState()).thenReturn(ImapSessionState.AUTHENTICATED);
        when(mockedImapSession.getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY))
            .thenReturn(mailboxSession);
        when(mockedQuotaRootResolver.getQuotaRoot(MAILBOX_PATH))
            .thenReturn(QUOTA_ROOT);
        when(mockedMailboxManager.hasRight(MAILBOX_PATH, MailboxACL.Right.Read, mailboxSession))
            .thenReturn(true);
        when(mockedQuotaManager.getMessageQuota(QUOTA_ROOT))
            .thenReturn(MESSAGE_QUOTA);
        when(mockedQuotaManager.getStorageQuota(QUOTA_ROOT))
            .thenReturn(STORAGE_QUOTA);

        argumentCaptor = ArgumentCaptor.forClass(ImapResponseMessage.class);

        testee = new GetQuotaRootProcessor(mock(ImapProcessor.class), mockedMailboxManager,
            statusResponseFactory, mockedQuotaRootResolver, mockedQuotaManager, new NoopMetricFactory());
    }

    @Test
    public void processorShouldWorkOnValidRights() throws Exception {
        GetQuotaRootRequest getQuotaRootRequest = new GetQuotaRootRequest("A004", ImapCommand.anyStateCommand("Name"), "INBOX");

        QuotaResponse storageQuotaResponse = new QuotaResponse("STORAGE", "plop", STORAGE_QUOTA);
        QuotaResponse messageQuotaResponse = new QuotaResponse("MESSAGE", "plop", MESSAGE_QUOTA);
        QuotaRootResponse quotaRootResponse = new QuotaRootResponse("INBOX", "plop");

        testee.doProcess(getQuotaRootRequest, mockedResponder, mockedImapSession);

        verify(mockedResponder, times(4)).respond(argumentCaptor.capture());
        assertThat(argumentCaptor.getAllValues().get(0)).isEqualTo(quotaRootResponse);
        assertThat(argumentCaptor.getAllValues().get(1)).isEqualTo(messageQuotaResponse);
        assertThat(argumentCaptor.getAllValues().get(2)).isEqualTo(storageQuotaResponse);
        assertThat(argumentCaptor.getAllValues().get(3)).matches(StatusResponseTypeMatcher.OK_RESPONSE_MATCHER::matches);
        verifyNoMoreInteractions(mockedResponder);
    }

    @Test
    public void processorShouldWorkOnErrorThrown() throws Exception {
        GetQuotaRootRequest getQuotaRootRequest = new GetQuotaRootRequest("A004", ImapCommand.anyStateCommand("Name"), "INBOX");

        when(mockedMailboxManager.hasRight(MAILBOX_PATH, MailboxACL.Right.Read, mailboxSession))
            .thenThrow(new MailboxException());

        testee.doProcess(getQuotaRootRequest, mockedResponder, mockedImapSession);


        verify(mockedResponder).respond(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue())
            .matches(StatusResponseTypeMatcher.BAD_RESPONSE_MATCHER::matches);
        verifyNoMoreInteractions(mockedResponder);
    }

    @Test
    public void processorShouldWorkOnNonValidRights() throws Exception {
        GetQuotaRootRequest getQuotaRootRequest = new GetQuotaRootRequest("A004", ImapCommand.anyStateCommand("Name"), "INBOX");

        when(mockedMailboxManager.hasRight(MAILBOX_PATH, MailboxACL.Right.Read, mailboxSession))
            .thenReturn(false);

        testee.doProcess(getQuotaRootRequest, mockedResponder, mockedImapSession);

        verify(mockedResponder).respond(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue())
            .matches(StatusResponseTypeMatcher.NO_RESPONSE_MATCHER::matches);
        verifyNoMoreInteractions(mockedResponder);
    }
}
