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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.james.core.quota.QuotaCount;
import org.apache.james.core.quota.QuotaSize;
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
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.quota.QuotaManager;
import org.apache.james.mailbox.quota.QuotaRootResolver;
import org.apache.james.metrics.api.NoopMetricFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class GetQuotaRootProcessorTest {

    private static final QuotaRoot QUOTA_ROOT = QuotaRoot.quotaRoot("plop", Optional.empty());
    public static final MailboxPath MAILBOX_PATH = MailboxPath.forUser("plop", "INBOX");
    public static final Quota<QuotaCount> MESSAGE_QUOTA =
        Quota.<QuotaCount>builder().used(QuotaCount.count(24)).computedLimit(QuotaCount.count(1589)).build();
    public static final Quota<QuotaSize> STORAGE_QUOTA =
        Quota.<QuotaSize>builder().used(QuotaSize.size(240)).computedLimit(QuotaSize.size(15890)).build();

    private GetQuotaRootProcessor testee;
    private ImapSession mockedImapSession;
    private ImapProcessor.Responder mockedResponder;
    private QuotaManager mockedQuotaManager;
    private QuotaRootResolver mockedQuotaRootResolver;
    private MailboxManager mockedMailboxManager;
    private MailboxSession mailboxSession;

    @Before
    public void setUp() {
        mailboxSession = new MockMailboxSession("plop");
        UnpooledStatusResponseFactory statusResponseFactory = new UnpooledStatusResponseFactory();
        mockedImapSession = mock(ImapSession.class);
        mockedQuotaManager = mock(QuotaManager.class);
        mockedQuotaRootResolver = mock(QuotaRootResolver.class);
        mockedResponder = mock(ImapProcessor.Responder.class);
        mockedMailboxManager = mock(MailboxManager.class);
        testee = new GetQuotaRootProcessor(mock(ImapProcessor.class), mockedMailboxManager,
            statusResponseFactory, mockedQuotaRootResolver, mockedQuotaManager, new NoopMetricFactory());
    }

    @Test
    public void processorShouldWorkOnValidRights() throws Exception {
        GetQuotaRootRequest getQuotaRootRequest = new GetQuotaRootRequest("A004", ImapCommand.anyStateCommand("Name"), "INBOX");


        when(mockedImapSession.getState()).thenReturn(ImapSessionState.AUTHENTICATED);
        when(mockedImapSession.getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY))
            .thenReturn(mailboxSession);

        when(mockedQuotaRootResolver.getQuotaRoot(any()))
            .thenReturn(QUOTA_ROOT);
        when(mockedMailboxManager.hasRight(MAILBOX_PATH, MailboxACL.Right.Read, mailboxSession))
            .thenReturn(true);

        when(mockedQuotaManager.getMessageQuota(QUOTA_ROOT)).thenReturn(MESSAGE_QUOTA);
        when(mockedQuotaManager.getStorageQuota(QUOTA_ROOT)).thenReturn(STORAGE_QUOTA);

        testee.doProcess(getQuotaRootRequest, mockedResponder, mockedImapSession);

        QuotaResponse storageQuotaResponse = new QuotaResponse("STORAGE", "plop", STORAGE_QUOTA);
        QuotaResponse messageQuotaResponse = new QuotaResponse("MESSAGE", "plop", MESSAGE_QUOTA);
        QuotaRootResponse quotaRootResponse = new QuotaRootResponse("INBOX", "plop");

        ArgumentCaptor<ImapResponseMessage> argumentCaptor = ArgumentCaptor.forClass(ImapResponseMessage.class);
        verify(mockedResponder, times(4)).respond(argumentCaptor.capture());
        verifyNoMoreInteractions(mockedResponder);

        assertThat(argumentCaptor.getAllValues())
            .hasSize(4)
            .contains(quotaRootResponse, storageQuotaResponse, messageQuotaResponse);
        assertThat(argumentCaptor.getAllValues().get(2))
            .matches(StatusResponseTypeMatcher.OK_RESPONSE_MATCHER::matches);
    }
/*
    @Test
    public void processorShouldWorkOnErrorThrown() throws Exception {
        GetQuotaRootRequest getQuotaRootRequest = new GetQuotaRootRequest("A004", ImapCommand.anyStateCommand("Name"), "INBOX");
        Expectations expectations = new Expectations();

        expectations.allowing(mockedImapSession).getState();
        expectations.will(Expectations.returnValue(ImapSessionState.AUTHENTICATED));

        expectations.allowing(mockedImapSession).getAttribute(expectations.with(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY));
        expectations.will(Expectations.returnValue(mailboxSession));

        expectations.allowing(mockedMailboxManager).hasRight(expectations.with(MAILBOX_PATH),
            expectations.with(MailboxACL.Right.Read), expectations.with(mailboxSession));
        expectations.will(Expectations.throwException(new MailboxException()));

        expectations.allowing(mockedMailboxManager).startProcessingRequest(expectations.with(mailboxSession));

        expectations.allowing(mockedMailboxManager).endProcessingRequest(expectations.with(mailboxSession));


        mockery.checking(expectations);

        mockery.checking(new Expectations() {
            {
                oneOf(mockedResponder).respond(with(new StatusResponseTypeMatcher(StatusResponse.Type.BAD)));
            }
        });

        testee.doProcess(getQuotaRootRequest, mockedResponder, mockedImapSession);
    }

    @Test
    public void processorShouldWorkOnNonValidRights() throws Exception {
        GetQuotaRootRequest getQuotaRootRequest = new GetQuotaRootRequest("A004", ImapCommand.anyStateCommand("Name"), "INBOX");
        Expectations expectations = new Expectations();

        expectations.allowing(mockedImapSession).getState();
        expectations.will(Expectations.returnValue(ImapSessionState.AUTHENTICATED));

        expectations.allowing(mockedImapSession).getAttribute(expectations.with(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY));
        expectations.will(Expectations.returnValue(mailboxSession));

        expectations.allowing(mockedMailboxManager).hasRight(expectations.with(MAILBOX_PATH),
            expectations.with(MailboxACL.Right.Read), expectations.with(mailboxSession));
        expectations.will(Expectations.returnValue(false));

        expectations.allowing(mockedMailboxManager).startProcessingRequest(expectations.with(mailboxSession));

        expectations.allowing(mockedMailboxManager).endProcessingRequest(expectations.with(mailboxSession));

        mockery.checking(expectations);

        mockery.checking(new Expectations() {
            {
                oneOf(mockedResponder).respond(with(new StatusResponseTypeMatcher(StatusResponse.Type.NO)));
            }
        });

        testee.doProcess(getQuotaRootRequest, mockedResponder, mockedImapSession);
    }
*/
}
