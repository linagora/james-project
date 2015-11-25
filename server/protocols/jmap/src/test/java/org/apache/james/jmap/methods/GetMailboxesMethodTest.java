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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.jmap.model.GetMailboxesRequest;
import org.apache.james.jmap.model.GetMailboxesResponse;
import org.apache.james.jmap.model.Mailbox;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageManager.MetaData.FetchGroup;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.MailboxMetaData;
import org.apache.james.mailbox.store.TestId;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MailboxMapperFactory;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

public class GetMailboxesMethodTest {

    private MailboxManager mockedMailboxManager;
    private MailboxMapperFactory<TestId> mockedMailboxMapperFactory;
    private MailboxSession mockedMailboxSession;
    private GetMailboxesMethod<TestId> getMailboxesMethod;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        String username = "username@domain.tld";

        mockedMailboxMapperFactory = mock(MailboxMapperFactory.class);
        mockedMailboxManager = mock(MailboxManager.class);
        mockedMailboxSession = mock(MailboxSession.class);

        getMailboxesMethod = new GetMailboxesMethod<>(mockedMailboxManager, mockedMailboxMapperFactory);

        when(mockedMailboxManager.createSystemSession(eq(username), any(Logger.class))).thenReturn(mockedMailboxSession);
    }

    @Test
    public void getMailboxesShouldReturnEmptyListWhenNoMailboxes() throws Exception {
        when(mockedMailboxManager.list(any()))
            .thenReturn(ImmutableList.<MailboxPath>of());
        GetMailboxesRequest getMailboxesRequest = GetMailboxesRequest.builder()
                .build();

        GetMailboxesResponse getMailboxesResponse = getMailboxesMethod.process(getMailboxesRequest, mockedMailboxSession);
        assertThat(getMailboxesResponse.getList()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getMailboxesShouldReturnMailboxesWhenAvailable() throws Exception {
        MailboxPath mailboxPath = new MailboxPath("namespace", "user", "name");
        when(mockedMailboxManager.list(eq(mockedMailboxSession)))
            .thenReturn(ImmutableList.<MailboxPath>of(mailboxPath));

        MessageManager mockedMessageManager = mock(MessageManager.class);
        when(mockedMailboxManager.getMailbox(eq(mailboxPath), eq(mockedMailboxSession)))
            .thenReturn(mockedMessageManager);

        MailboxMetaData mailboxMetaData = new MailboxMetaData(ImmutableList.of(), null, 123L, 5L, 10L, 3L, 2L, 1L, false, false, null);
        when(mockedMessageManager.getMetaData(eq(false), eq(mockedMailboxSession), eq(FetchGroup.UNSEEN_COUNT)))
            .thenReturn(mailboxMetaData);

        MailboxMapper<TestId> mockedMailboxMapper = mock(MailboxMapper.class);
        when(mockedMailboxMapperFactory.getMailboxMapper(eq(mockedMailboxSession)))
            .thenReturn(mockedMailboxMapper);

        long id = 23432L;
        SimpleMailbox<TestId> simpleMailbox = new SimpleMailbox<TestId>(mailboxPath, 5L);
        simpleMailbox.setMailboxId(TestId.of(id));
        when(mockedMailboxMapper.findMailboxByPath(mailboxPath))
            .thenReturn(simpleMailbox);

        GetMailboxesRequest getMailboxesRequest = GetMailboxesRequest.builder()
                .build();

        Mailbox expectedMailbox = Mailbox.builder()
                .id(String.valueOf(id))
                .name(mailboxPath.getName())
                .unreadMessages(2)
                .build();

        GetMailboxesResponse getMailboxesResponse = getMailboxesMethod.process(getMailboxesRequest, mockedMailboxSession);
        assertThat(getMailboxesResponse.getList()).containsOnly(expectedMailbox);
    }
}
