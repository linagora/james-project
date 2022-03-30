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

package org.apache.james.protocols.pop3.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.apache.james.metrics.tests.RecordingMetricFactory;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.pop3.POP3Response;
import org.apache.james.protocols.pop3.POP3Session;
import org.apache.james.protocols.pop3.POP3StreamResponse;
import org.apache.james.protocols.pop3.mailbox.Mailbox;
import org.apache.james.protocols.pop3.mailbox.MessageMetaData;
import org.junit.jupiter.api.Test;

class RetrCmdHandlerTest {

    @Test
    void onCommandHandlesMissingContent() throws IOException {
        POP3Session session = mock(POP3Session.class);
        when(session.getHandlerState()).thenReturn(POP3Session.TRANSACTION);

        Request request = mock(Request.class);
        when(request.getArgument()).thenReturn("1");

        MessageMetaData data = new MessageMetaData("1234", 567);
        when(session.getAttachment(POP3Session.UID_LIST, ProtocolSession.State.Transaction)).thenReturn(Optional.of(List.of(data)));

        Mailbox mailbox = mock(Mailbox.class);
        when(session.getUserMailbox()).thenReturn(mailbox);
        when(mailbox.getMessage(data.getUid())).thenThrow(new IOException("cannot retrieve message content"));

        Response response = new RetrCmdHandler(new RecordingMetricFactory()).onCommand(session, request);
        assertThat(response.getRetCode()).isEqualTo(POP3Response.ERR_RESPONSE);
        assertThat(response.getLines().get(0)).contains("does not exist");
    }

    @Test
    void onCommandRetrievesMessage() throws IOException {
        POP3Session session = mock(POP3Session.class);
        when(session.getHandlerState()).thenReturn(POP3Session.TRANSACTION);

        Request request = mock(Request.class);
        when(request.getArgument()).thenReturn("1");

        MessageMetaData data = new MessageMetaData("1234", 567);
        when(session.getAttachment(POP3Session.UID_LIST, ProtocolSession.State.Transaction)).thenReturn(Optional.of(List.of(data)));

        Mailbox mailbox = mock(Mailbox.class);
        when(session.getUserMailbox()).thenReturn(mailbox);
        when(mailbox.getMessage(data.getUid())).thenReturn(InputStream.nullInputStream());

        Response response = new RetrCmdHandler(new RecordingMetricFactory()).onCommand(session, request);
        assertThat(response.getRetCode()).isEqualTo(POP3Response.OK_RESPONSE);
        assertThat(response).isInstanceOf(POP3StreamResponse.class);
    }
}
