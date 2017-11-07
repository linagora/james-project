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

import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.MailboxType;
import org.apache.james.imap.message.response.ListResponse;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.PathDelimiter;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.metrics.api.NoopMetricFactory;
import org.junit.Before;
import org.junit.Test;

public class ListProcessorTest  {
    private static final String USER_1 = "user1";
    private static final PathDelimiter PATH_DELIMITER = MailboxConstants.DEFAULT_DELIMITER;

    private ListProcessor processor;
    private ImapProcessor.Responder responder;
    private MailboxPath inboxPath;

    @Before
    public void setUp() throws Exception {
        StatusResponseFactory serverResponseFactory = mock(StatusResponseFactory.class);
        MailboxManager manager = mock(MailboxManager.class);
        responder = mock(ImapProcessor.Responder.class);
        processor = new ListProcessor(mock(ImapProcessor.class), manager, serverResponseFactory, new NoopMetricFactory());

        inboxPath = MailboxPath.forUser(USER_1, MailboxConstants.INBOX);
    }

    ListResponse createResponse(boolean noinferior, boolean noselect,
                                boolean marked, boolean unmarked, boolean hasChildren,
                                boolean hasNoChildren) {
        return new ListResponse(noinferior, noselect,
            marked, unmarked,
            hasChildren, hasNoChildren,
            MailboxConstants.INBOX, PATH_DELIMITER);
    }


    @Test
    public void testHasChildren() throws Exception {
        processor.processResult(responder,
            new ListProcessor.ListAnswer(PATH_DELIMITER,
                MailboxMetaData.Children.HAS_CHILDREN,
                MailboxMetaData.Selectability.NONE,
                inboxPath.getName(), inboxPath),
            MailboxType.OTHER);

        verify(responder).respond(
            createResponse(false,
                false,
                false,
                false,
                true,
                false));
    }

    @Test
    public void testHasNoChildren() throws Exception {
        processor.processResult(responder,
            new ListProcessor.ListAnswer(PATH_DELIMITER,
                MailboxMetaData.Children.HAS_NO_CHILDREN,
                MailboxMetaData.Selectability.NONE,
                inboxPath.getName(), inboxPath),
            MailboxType.OTHER);

        verify(responder).respond(
            createResponse(false,
                false,
                false,
                false,
                false,
                true));
    }
    
    @Test
    public void testNoInferiors() throws Exception {
        processor.processResult(responder,
            new ListProcessor.ListAnswer(PATH_DELIMITER,
                MailboxMetaData.Children.NO_INFERIORS,
                MailboxMetaData.Selectability.NONE,
                inboxPath.getName(), inboxPath),
            MailboxType.OTHER);

        verify(responder).respond(
            createResponse(true,
                false,
                false,
                false,
                false,
                false));
    }

    @Test
    public void testNoSelect() throws Exception {
        processor.processResult(responder,
            new ListProcessor.ListAnswer(PATH_DELIMITER,
                MailboxMetaData.Children.CHILDREN_ALLOWED_BUT_UNKNOWN,
                MailboxMetaData.Selectability.NOSELECT,
                inboxPath.getName(), inboxPath),
            MailboxType.OTHER);

        verify(responder).respond(
            createResponse(false,
                true,
                false,
                false,
                false,
                false));
    }

    @Test
    public void testUnMarked() throws Exception {
        processor.processResult(responder,
            new ListProcessor.ListAnswer(PATH_DELIMITER,
                MailboxMetaData.Children.CHILDREN_ALLOWED_BUT_UNKNOWN,
                MailboxMetaData.Selectability.UNMARKED,
                inboxPath.getName(), inboxPath),
            MailboxType.OTHER);

        verify(responder).respond(
            createResponse(false,
                false,
                false,
                true,
                false,
                false));
    }

    @Test
    public void testMarked() throws Exception {
        processor.processResult(responder,
            new ListProcessor.ListAnswer(PATH_DELIMITER,
                MailboxMetaData.Children.CHILDREN_ALLOWED_BUT_UNKNOWN,
                MailboxMetaData.Selectability.MARKED,
                inboxPath.getName(), inboxPath),
            MailboxType.OTHER);

        verify(responder).respond(
            createResponse(false,
                false,
                true,
                false,
                false,
                false));
    }
}
