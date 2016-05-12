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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.james.jmap.exceptions.MailboxRoleNotFoundException;
import org.apache.james.jmap.model.CreationMessage;
import org.apache.james.jmap.model.CreationMessage.DraftEmailer;
import org.apache.james.jmap.model.CreationMessageId;
import org.apache.james.jmap.model.SetError;
import org.apache.james.jmap.model.SetMessagesRequest;
import org.apache.james.jmap.model.SetMessagesResponse;
import org.apache.james.jmap.send.MailFactory;
import org.apache.james.jmap.send.MailMetadata;
import org.apache.james.jmap.send.MailSpool;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.inmemory.InMemoryId;
import org.apache.james.mailbox.inmemory.InMemoryMailboxSessionMapperFactory;
import org.apache.james.mailbox.inmemory.manager.InMemoryIntegrationResources;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.mailet.Mail;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

@SuppressWarnings("unchecked")
public class SetMessagesCreationProcessorTest {

    private static final String OUTBOX_ID = "1";
    private static final long DRAFTS_ID = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(SetMessagesCreationProcessorTest.class);

    private MessageMapper<InMemoryId> mockMapper;
    private MailboxSessionMapperFactory<InMemoryId> stubSessionMapperFactory;
    private MailSpool mockedMailSpool;
    private MailFactory<InMemoryId> mockedMailFactory;
    private MailboxManager inMemoryMailboxManager;
    private MailboxSession inMemoryMailboxSession;
    private InMemoryMailboxSessionMapperFactory inMemoryMailboxMapperFactory;
    private InMemoryIntegrationResources inMemoryIntegrationResources;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        mockMapper = mock(MessageMapper.class);
        stubSessionMapperFactory = mock(MailboxSessionMapperFactory.class);
        when(stubSessionMapperFactory.createMessageMapper(any(MailboxSession.class)))
                .thenReturn(mockMapper);
        mockedMailSpool = mock(MailSpool.class);
        mockedMailFactory = mock(MailFactory.class);

        inMemoryIntegrationResources = new InMemoryIntegrationResources();
        inMemoryMailboxManager = inMemoryIntegrationResources.createMailboxManager(inMemoryIntegrationResources.createGroupMembershipResolver());
        inMemoryMailboxSession = inMemoryMailboxManager.login("user@domain.org", "pass", LOGGER);
        inMemoryMailboxManager.createMailbox(new MailboxPath("#private", "user@domain.org", "outbox"), inMemoryMailboxSession);
        inMemoryMailboxManager.createMailbox(new MailboxPath("#private", "user@domain.org", "drafts"), inMemoryMailboxSession);
        inMemoryMailboxMapperFactory = new InMemoryMailboxSessionMapperFactory();
    }

    @Test
    public void processShouldReturnEmptyCreatedWhenRequestHasEmptyCreate() {
        SetMessagesCreationProcessor<InMemoryId> sut = new SetMessagesCreationProcessor<>(inMemoryMailboxMapperFactory, inMemoryMailboxManager, null, null, null, null);
        SetMessagesRequest requestWithEmptyCreate = SetMessagesRequest.builder().build();

        SetMessagesResponse result = sut.process(requestWithEmptyCreate, inMemoryMailboxSession);

        assertThat(result.getCreated()).isEmpty();
        assertThat(result.getNotCreated()).isEmpty();
    }

    @Test
    public void processShouldReturnNonEmptyCreatedWhenRequestHasNonEmptyCreate() throws MailboxException {
        // Given
        SetMessagesCreationProcessor<InMemoryId> sut = new SetMessagesCreationProcessor<>(inMemoryMailboxMapperFactory, inMemoryMailboxManager, stubSessionMapperFactory, new MIMEMessageConverter(), mockedMailSpool, mockedMailFactory);
        // When
        SetMessagesResponse result = sut.process(buildFakeCreationRequest(), inMemoryMailboxSession);

        // Then
        assertThat(result.getCreated()).isNotEmpty();
        assertThat(result.getNotCreated()).isEmpty();
    }

    @Test(expected = MailboxRoleNotFoundException.class)
    public void processShouldThrowWhenOutboxNotFound() throws Exception {
        // Given
        MailboxManager mailboxManagerWithMissingOutbox = inMemoryIntegrationResources.createMailboxManager(inMemoryIntegrationResources.createGroupMembershipResolver());
        inMemoryMailboxSession = mailboxManagerWithMissingOutbox .login("user@domain.org", "pass", LOGGER);
        mailboxManagerWithMissingOutbox .createMailbox(new MailboxPath("#private", "user@domain.org", "drafts"), inMemoryMailboxSession);
        SetMessagesCreationProcessor<InMemoryId> sut = new SetMessagesCreationProcessor<>(inMemoryMailboxMapperFactory, mailboxManagerWithMissingOutbox, null, null, null, null);
        // When
        sut.process(buildFakeCreationRequest(), inMemoryMailboxSession);
    }

    @Test
    public void processShouldCallMessageMapperWhenRequestHasNonEmptyCreate() throws MailboxException {
        // Given
        SetMessagesCreationProcessor<InMemoryId> sut = new SetMessagesCreationProcessor<>(inMemoryMailboxMapperFactory, inMemoryMailboxManager,
                stubSessionMapperFactory, new MIMEMessageConverter(), mockedMailSpool, mockedMailFactory);
        // When
        sut.process(buildFakeCreationRequest(), inMemoryMailboxSession);

        // Then
        verify(mockMapper).add(any(Mailbox.class), any(MailboxMessage.class));
    }

    @Test
    public void processShouldSendMailWhenRequestHasNonEmptyCreate() throws Exception {
        // Given
        SetMessagesCreationProcessor<InMemoryId> sut = new SetMessagesCreationProcessor<>(inMemoryMailboxMapperFactory, inMemoryMailboxManager,
                stubSessionMapperFactory, new MIMEMessageConverter(), mockedMailSpool, mockedMailFactory);
        // When
        sut.process(buildFakeCreationRequest(), inMemoryMailboxSession);

        // Then
        verify(mockedMailSpool).send(any(Mail.class), any(MailMetadata.class));
    }

    private SetMessagesRequest buildFakeCreationRequest() {
        return SetMessagesRequest.builder()
                .create(ImmutableMap.of(CreationMessageId.of("anything-really"), CreationMessage.builder()
                        .from(DraftEmailer.builder().name("alice").email("alice@example.com").build())
                        .to(ImmutableList.of(DraftEmailer.builder().name("bob").email("bob@example.com").build()))
                        .subject("Hey! ")
                        .mailboxIds(ImmutableList.of(OUTBOX_ID))
                        .build()
                ))
                .build();
    }

    @Test
    public void processShouldNotSpoolMailWhenNotSavingToOutbox() throws Exception {
        // Given
        SetMessagesCreationProcessor<InMemoryId> sut = new SetMessagesCreationProcessor<>(inMemoryMailboxMapperFactory, inMemoryMailboxManager,
                stubSessionMapperFactory, new MIMEMessageConverter(), mockedMailSpool, mockedMailFactory);
        // When
        sut.process(buildCreationRequestNotForSending(), inMemoryMailboxSession);

        // Then
        verify(mockedMailSpool, never()).send(any(Mail.class), any(MailMetadata.class));
    }

    private SetMessagesRequest buildCreationRequestNotForSending() {
        return SetMessagesRequest.builder()
                .create(ImmutableMap.of(CreationMessageId.of("anything-really"), CreationMessage.builder()
                        .from(DraftEmailer.builder().name("alice").email("alice@example.com").build())
                        .to(ImmutableList.of(DraftEmailer.builder().name("bob").email("bob@example.com").build()))
                        .subject("Hey! ")
                        .mailboxIds(ImmutableList.of("any-id-but-outbox-id"))
                        .build()
                ))
                .build();
    }

    @Test
    public void processShouldReturnNotImplementedErrorWhenSavingToDrafts() {
        // Given
        SetMessagesCreationProcessor<InMemoryId> sut = new SetMessagesCreationProcessor<>(inMemoryMailboxMapperFactory, inMemoryMailboxManager,
                stubSessionMapperFactory, new MIMEMessageConverter(), mockedMailSpool, mockedMailFactory);
        // When
        CreationMessageId creationMessageId = CreationMessageId.of("anything-really");
        SetMessagesResponse actual = sut.process(buildSaveToDraftsRequest(InMemoryId.of(DRAFTS_ID), creationMessageId), inMemoryMailboxSession);

        // Then
        assertThat(actual.getNotCreated()).containsExactly(Maps.immutableEntry(creationMessageId, SetError.builder()
                .type("error")
                .description("Not yet implemented")
                .build()));
    }

    private SetMessagesRequest buildSaveToDraftsRequest(InMemoryId draftsId, CreationMessageId creationMessageId) {
        return SetMessagesRequest.builder()
                .create(ImmutableMap.of(creationMessageId, CreationMessage.builder()
                        .from(DraftEmailer.builder().name("alice").email("alice@example.com").build())
                        .to(ImmutableList.of(DraftEmailer.builder().name("bob").email("bob@example.com").build()))
                        .subject("Hey! ")
                        .mailboxIds(ImmutableList.of(draftsId.serialize()))
                        .build()
                )).build();
    }

    @Test
    public void processShouldNotSendWhenSavingToDrafts() throws Exception {
        // Given
        SetMessagesCreationProcessor<InMemoryId> sut = new SetMessagesCreationProcessor<>(inMemoryMailboxMapperFactory, inMemoryMailboxManager,
                stubSessionMapperFactory, new MIMEMessageConverter(), mockedMailSpool, mockedMailFactory);
        // When
        CreationMessageId creationMessageId = CreationMessageId.of("anything-really");
        sut.process(buildSaveToDraftsRequest(InMemoryId.of(DRAFTS_ID), creationMessageId), inMemoryMailboxSession);

        // Then
        verify(mockedMailSpool, never()).send(any(Mail.class), any(MailMetadata.class));
    }
}