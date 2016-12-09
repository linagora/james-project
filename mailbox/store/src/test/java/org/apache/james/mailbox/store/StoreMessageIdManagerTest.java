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

package org.apache.james.mailbox.store;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.exception.OverQuotaException;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.quota.QuotaManager;
import org.apache.james.mailbox.store.event.MailboxEventDispatcher;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageIdMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.mail.model.DefaultMessageId;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.quota.DefaultQuotaRootResolver;
import org.apache.james.mailbox.store.quota.QuotaImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableList;

public class StoreMessageIdManagerTest {

    private static final TestId TEST_ID = TestId.of(36);
    private static final TestId OTHER_TEST_ID = TestId.of(52);
    private static final MessageUid UID = MessageUid.of(28);
    private static final long MOD_SEQ = 18;
    private static final Quota OVER_QUOTA = QuotaImpl.quota(102, 100);
    public static final Flags FLAGS = new Flags();
    private static final ByteArrayInputStream ARRAY_INPUT_STREAM = new ByteArrayInputStream("".getBytes());

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private StoreMessageIdManager storeMessageIdManager;
    private MailboxEventDispatcher dispatcher;
    private DefaultMessageId.Factory messageIdFactory;
    private MailboxSession session;
    private MailboxMapper mailboxMapper;
    private Mailbox mailbox;
    private MessageIdMapper messageIdMapper;
    private DefaultMessageId messageId;
    private QuotaManager quotaManager;

    @Before
    public void setUp() throws Exception {
        MailboxSessionMapperFactory mailboxSessionMapperFactory = mock(MailboxSessionMapperFactory.class);
        dispatcher = mock(MailboxEventDispatcher.class);
        messageIdFactory = mock(DefaultMessageId.Factory.class);
        session = mock(MailboxSession.class);
        quotaManager = mock(QuotaManager.class);
        storeMessageIdManager = new StoreMessageIdManager(mailboxSessionMapperFactory, dispatcher, messageIdFactory,
            quotaManager, new DefaultQuotaRootResolver(mailboxSessionMapperFactory));

        messageId = new DefaultMessageId();
        mailboxMapper = mock(MailboxMapper.class);
        messageIdMapper = mock(MessageIdMapper.class);
        mailbox = mock(Mailbox.class);
        ModSeqProvider modSeqProvider = mock(ModSeqProvider.class);
        UidProvider uidProvider = mock(UidProvider.class);

        when(mailbox.getNamespace()).thenReturn("#private");
        when(mailbox.getUser()).thenReturn("user");
        when(mailbox.getName()).thenReturn("name");
        when(mailboxSessionMapperFactory.getMailboxMapper(any(MailboxSession.class))).thenReturn(mailboxMapper);
        when(mailboxSessionMapperFactory.getAttachmentMapper(any(MailboxSession.class))).thenReturn(mock(AttachmentMapper.class));
        when(mailboxMapper.findMailboxById(TEST_ID)).thenReturn(mailbox);
        when(mailboxSessionMapperFactory.getMessageIdMapper(any(MailboxSession.class))).thenReturn(messageIdMapper);
        when(quotaManager.getMessageQuota(any(QuotaRoot.class))).thenReturn(QuotaImpl.unlimited());
        when(quotaManager.getStorageQuota(any(QuotaRoot.class))).thenReturn(QuotaImpl.unlimited());
        when(mailboxSessionMapperFactory.getModSeqProvider()).thenReturn(modSeqProvider);
        when(mailboxSessionMapperFactory.getUidProvider()).thenReturn(uidProvider);
        when(uidProvider.nextUid(any(MailboxSession.class), any(MailboxId.class))).thenReturn(UID);
        when(modSeqProvider.nextModSeq(any(MailboxSession.class), any(MailboxId.class))).thenReturn(MOD_SEQ);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deleteShouldCallEventDispatcher() throws Exception {
        MailboxMessage mailboxMessage = mock(MailboxMessage.class);
        when(mailboxMessage.getUid()).thenReturn(MessageUid.of(28));
        when(mailboxMessage.getMailboxId()).thenReturn(TEST_ID);
        when(messageIdMapper.find(any(List.class), eq(MessageMapper.FetchType.Metadata)))
            .thenReturn(ImmutableList.of(mailboxMessage));

        storeMessageIdManager.delete(messageId, ImmutableList.<MailboxId>of(TEST_ID), session);

        verify(dispatcher).expunged(eq(session), any(MessageMetaData.class), eq(mailbox));
        verifyNoMoreInteractions(dispatcher);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deleteShouldNotCallEventDispatcherWhenMessageIsInWrongMailbox() throws Exception {
        MailboxMessage mailboxMessage = mock(MailboxMessage.class);
        when(mailboxMessage.getUid()).thenReturn(UID);
        when(mailboxMessage.getMailboxId()).thenReturn(OTHER_TEST_ID);
        when(messageIdMapper.find(any(List.class), eq(MessageMapper.FetchType.Metadata)))
            .thenReturn(ImmutableList.of(mailboxMessage));

        storeMessageIdManager.delete(messageId, ImmutableList.<MailboxId>of(TEST_ID), session);

        verifyNoMoreInteractions(dispatcher);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setInMailboxesShouldNotCallDispatcherWhenMessageAlreadyInMailbox() throws Exception {
        when(messageIdMapper.findMailboxes(messageId)).thenReturn(ImmutableList.<MailboxId>of(TEST_ID));
        MailboxMessage mailboxMessage = createMessage();
        when(messageIdMapper.find(any(List.class), eq(MessageMapper.FetchType.Full)))
            .thenReturn(ImmutableList.of(mailboxMessage));

        storeMessageIdManager.setInMailboxes(messageId, ImmutableList.<MailboxId>of(TEST_ID), session);

        verifyNoMoreInteractions(dispatcher);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setInMailboxesShouldCallDispatcher() throws Exception {
        when(messageIdMapper.findMailboxes(messageId)).thenReturn(ImmutableList.<MailboxId>of(OTHER_TEST_ID));
        MailboxMessage mailboxMessage = createMessage();
        when(messageIdMapper.find(any(List.class), eq(MessageMapper.FetchType.Full)))
            .thenReturn(ImmutableList.of(mailboxMessage));

        storeMessageIdManager.setInMailboxes(messageId, ImmutableList.<MailboxId>of(TEST_ID), session);

        verify(dispatcher).added(eq(session), any(MessageMetaData.class), any(Mailbox.class));
        verifyNoMoreInteractions(dispatcher);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setInMailboxesThrowWhenOverQuota() throws Exception {
        when(quotaManager.getMessageQuota(any(QuotaRoot.class))).thenReturn(OVER_QUOTA);
        when(messageIdMapper.findMailboxes(messageId)).thenReturn(ImmutableList.<MailboxId>of(OTHER_TEST_ID));
        MailboxMessage mailboxMessage = createMessage();
        when(messageIdMapper.find(any(List.class), eq(MessageMapper.FetchType.Full)))
            .thenReturn(ImmutableList.of(mailboxMessage));

        expectedException.expect(OverQuotaException.class);

        storeMessageIdManager.setInMailboxes(messageId, ImmutableList.<MailboxId>of(TEST_ID), session);
    }

    @Test
    public void setFlagsShouldNotDispatchWhenNoFlagsReturned() throws Exception {
        Flags newFlags = new Flags(Flags.Flag.SEEN);
        when(messageIdMapper.setFlags(newFlags, MessageManager.FlagsUpdateMode.ADD, messageId)).thenReturn(new TreeMap<MailboxId, UpdatedFlags>());

        storeMessageIdManager.setFlags(newFlags, MessageManager.FlagsUpdateMode.ADD, messageId, session);

        verifyNoMoreInteractions(dispatcher);
    }

    @Test
    public void setFlagsShouldNotDispatchWhenNoFlagsChanged() throws Exception {
        Flags newFlags = new Flags(Flags.Flag.SEEN);
        Map<MailboxId, UpdatedFlags> mailboxIdUpdatedFlags = new HashMap<MailboxId, UpdatedFlags>();
        mailboxIdUpdatedFlags.put(TEST_ID, new UpdatedFlags(UID, MOD_SEQ, newFlags, newFlags));
        when(messageIdMapper.setFlags(newFlags, MessageManager.FlagsUpdateMode.ADD, messageId))
            .thenReturn(mailboxIdUpdatedFlags);

        storeMessageIdManager.setFlags(newFlags, MessageManager.FlagsUpdateMode.ADD, messageId, session);

        verifyNoMoreInteractions(dispatcher);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setFlagsShouldDispatch() throws Exception {
        Flags newFlags = new Flags(Flags.Flag.SEEN);
        Map<MailboxId, UpdatedFlags> mailboxIdUpdatedFlags = new HashMap<MailboxId, UpdatedFlags>();
        UpdatedFlags updatedFlags = new UpdatedFlags(UID, MOD_SEQ, FLAGS, newFlags);
        mailboxIdUpdatedFlags.put(TEST_ID, updatedFlags);
        when(messageIdMapper.setFlags(newFlags, MessageManager.FlagsUpdateMode.ADD, messageId))
            .thenReturn(mailboxIdUpdatedFlags);

        storeMessageIdManager.setFlags(newFlags, MessageManager.FlagsUpdateMode.ADD, messageId, session);

        verify(dispatcher).flagsUpdated(session, UID, mailbox, updatedFlags);
        verifyNoMoreInteractions(dispatcher);
    }

    private MailboxMessage createMessage() throws IOException {
        MailboxMessage mailboxMessage = mock(MailboxMessage.class);
        when(mailboxMessage.getUid()).thenReturn(UID);
        when(mailboxMessage.getMailboxId()).thenReturn(OTHER_TEST_ID);
        when(mailboxMessage.getFullContent()).thenReturn(ARRAY_INPUT_STREAM);
        when(mailboxMessage.createFlags()).thenReturn(FLAGS);
        return mailboxMessage;
    }
}
