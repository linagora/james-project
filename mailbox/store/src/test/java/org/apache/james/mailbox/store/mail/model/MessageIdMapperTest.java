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

package org.apache.james.mailbox.store.mail.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import javax.mail.Flags;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageIdMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.xenei.junit.contract.Contract;
import org.xenei.junit.contract.ContractTest;
import org.xenei.junit.contract.IProducer;

import com.google.common.collect.ImmutableList;

/**
 * Generic purpose tests for your implementation MailboxMapper.
 * 
 * You then just need to instantiate your mailbox mapper and an IdGenerator.
 */
@Contract(MapperProvider.class)
public class MessageIdMapperTest<T extends MapperProvider> {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final static char DELIMITER = ':';
    private static final int BODY_START = 16;
    private final static long UID_VALIDITY = 42;

    private IProducer<T> producer;
    private MessageMapper messageMapper;
    private MailboxMapper mailboxMapper;
    private MessageIdMapper sut;

    private SimpleMailbox benwaInboxMailbox;
    private SimpleMailbox benwaWorkMailbox;
    
    private SimpleMailboxMessage message1;
    private SimpleMailboxMessage message2;
    private SimpleMailboxMessage message3;
    private SimpleMailboxMessage message4;

    @Rule
    public ExpectedException expected = ExpectedException.none();
    private T mapperProvider;

    @Contract.Inject
    public final void setProducer(IProducer<T> producer) throws MailboxException {
        this.producer = producer;
        this.mapperProvider = producer.newInstance();
        this.mapperProvider.ensureMapperPrepared();
        this.sut = mapperProvider.createMessageIdMapper();
        this.messageMapper = mapperProvider.createMessageMapper();
        this.mailboxMapper = mapperProvider.createMailboxMapper();

        benwaInboxMailbox = createMailbox(new MailboxPath("#private", "benwa", "INBOX"));
        benwaWorkMailbox = createMailbox( new MailboxPath("#private", "benwa", "INBOX"+DELIMITER+"work"));

        message1 = createMessage(benwaInboxMailbox, "Subject: Test1 \n\nBody1\n.\n", BODY_START, new PropertyBuilder());
        message2 = createMessage(benwaInboxMailbox, "Subject: Test2 \n\nBody2\n.\n", BODY_START, new PropertyBuilder());
        message3 = createMessage(benwaInboxMailbox, "Subject: Test3 \n\nBody3\n.\n", BODY_START, new PropertyBuilder());
        message4 = createMessage(benwaWorkMailbox, "Subject: Test4 \n\nBody4\n.\n", BODY_START, new PropertyBuilder());
    }

    @After
    public void tearDown() throws MailboxException {
        producer.cleanUp();
    }

    @ContractTest
    public void findShouldReturnEmptyWhenIdListIsEmpty() throws MailboxException {
        assertThat(sut.find(ImmutableList.<MessageId> of(), FetchType.Full)).isEmpty();
    }

    @ContractTest
    public void findShouldReturnOneMessageWhenIdListContainsOne() throws MailboxException {
        saveMessages();
        List<Message> messages = sut.find(ImmutableList.of(message1.getMessageId()), FetchType.Full);
        assertThat(messages).containsOnly(message1);
    }

    @ContractTest
    public void findShouldReturnMultipleMessagesWhenIdContainsMultiple() throws MailboxException {
        saveMessages();
        List<Message> messages = sut.find(ImmutableList.of(message1.getMessageId(), message2.getMessageId(), message3.getMessageId()), FetchType.Full);
        assertThat(messages).containsOnly(message1, message2, message3);
    }

    @ContractTest
    public void findShouldReturnMultipleMessagesWhenIdContainsMultipleInDifferentMailboxes() throws MailboxException {
        saveMessages();
        List<Message> messages = sut.find(ImmutableList.of(message1.getMessageId(), message4.getMessageId(), message3.getMessageId()), FetchType.Full);
        assertThat(messages).containsOnly(message1, message4, message3);
    }

    @ContractTest
    public void findMailboxesShouldReturnEmptyWhenMessageDoesntExist() throws MailboxException {
        assertThat(sut.findMailboxes(mapperProvider.getMessageIdFactory().generate())).isEmpty();
    }

    @ContractTest
    public void findMailboxesShouldOneMailboxWhenMessageExistsInOneMailbox() throws MailboxException {
        saveMessages();
        List<MailboxId> mailboxes = sut.findMailboxes(message1.getMessageId());
        assertThat(mailboxes).containsOnly(benwaInboxMailbox.getMailboxId());
    }

    @ContractTest
    public void findMailboxesShouldTwoMailboxesWhenMessageExistsInTwoMailboxes() throws MailboxException {
        saveMessages();

        SimpleMailboxMessage message1InOtherMailbox = SimpleMailboxMessage.copy(benwaWorkMailbox.getMailboxId(), message1);
        message1InOtherMailbox.setUid(mapperProvider.generateMessageUid());
        sut.save(message1InOtherMailbox);

        List<MailboxId> mailboxes = sut.findMailboxes(message1.getMessageId());
        assertThat(mailboxes).containsOnly(benwaInboxMailbox.getMailboxId(), benwaWorkMailbox.getMailboxId());
    }

    @ContractTest
    public void saveShouldSaveAMessage() throws Exception {
        message1.setUid(mapperProvider.generateMessageUid());
        sut.save(message1);
        List<Message> messages = sut.find(ImmutableList.of(message1.getMessageId()), FetchType.Full);
        assertThat(messages).containsOnly(message1);
    }

    @ContractTest
    public void saveShouldThrowWhenMailboxDoesntExist() throws Exception {
        SimpleMailbox notPersistedMailbox = new SimpleMailbox(new MailboxPath("#private", "benwa", "mybox"), UID_VALIDITY);
        notPersistedMailbox.setMailboxId(mapperProvider.generateId());
        SimpleMailboxMessage message = createMessage(notPersistedMailbox, "Subject: Test \n\nBody\n.\n", BODY_START, new PropertyBuilder());
        message.setUid(mapperProvider.generateMessageUid());

        expectedException.expect(MailboxNotFoundException.class);
        sut.save(message);
    }

    @ContractTest
    public void saveShouldSaveMessageAnotherIndicesWhenMessageAlreadyInMailbox() throws Exception {
        message1.setUid(mapperProvider.generateMessageUid());
        sut.save(message1);

        SimpleMailboxMessage message1InOtherMailbox = SimpleMailboxMessage.copy(benwaWorkMailbox.getMailboxId(), message1);
        message1InOtherMailbox.setUid(mapperProvider.generateMessageUid());
        sut.save(message1InOtherMailbox);

        List<MailboxId> mailboxes = sut.findMailboxes(message1.getMessageId());
        assertThat(mailboxes).containsOnly(benwaInboxMailbox.getMailboxId(), benwaWorkMailbox.getMailboxId());
    }

    @ContractTest
    public void deleteShouldNotThrowWhenUnknownMessage() {
        sut.delete(message1.getMessageId());
    }

    @ContractTest
    public void deleteShouldDeleteAMessage() throws Exception {
        message1.setUid(mapperProvider.generateMessageUid());
        sut.save(message1);

        MessageId messageId = message1.getMessageId();
        sut.delete(messageId);

        List<Message> messages = sut.find(ImmutableList.of(messageId), FetchType.Full);
        assertThat(messages).isEmpty();
    }

    @ContractTest
    public void deleteShouldDeleteMessageIndicesWhenStoredInTwoMailboxes() throws Exception {
        message1.setUid(mapperProvider.generateMessageUid());
        sut.save(message1);

        SimpleMailboxMessage message1InOtherMailbox = SimpleMailboxMessage.copy(benwaWorkMailbox.getMailboxId(), message1);
        message1InOtherMailbox.setUid(mapperProvider.generateMessageUid());
        sut.save(message1InOtherMailbox);

        MessageId messageId = message1.getMessageId();
        sut.delete(messageId);

        List<MailboxId> mailboxes = sut.findMailboxes(messageId);
        assertThat(mailboxes).isEmpty();
    }

    private SimpleMailbox createMailbox(MailboxPath mailboxPath) throws MailboxException {
        SimpleMailbox mailbox = new SimpleMailbox(mailboxPath, UID_VALIDITY);
        mailbox.setMailboxId(mapperProvider.generateId());
        mailboxMapper.save(mailbox);
        return mailbox;
    }
    
    private void saveMessages() throws MailboxException {
        addMessageAndSetModSeq(benwaInboxMailbox, message1);
        addMessageAndSetModSeq(benwaInboxMailbox, message2);
        addMessageAndSetModSeq(benwaInboxMailbox, message3);
        addMessageAndSetModSeq(benwaWorkMailbox, message4);
    }

    private void addMessageAndSetModSeq(Mailbox mailbox, MailboxMessage message) throws MailboxException {
        messageMapper.add(mailbox, message);
        message1.setModSeq(messageMapper.getHighestModSeq(mailbox));
    }

    private SimpleMailboxMessage createMessage(Mailbox mailbox, String content, int bodyStart, PropertyBuilder propertyBuilder) {
        return new SimpleMailboxMessage(mapperProvider.getMessageIdFactory().generate(), 
                new Date(), 
                content.length(), 
                bodyStart, 
                new SharedByteArrayInputStream(content.getBytes()), 
                new Flags(), 
                propertyBuilder, 
                mailbox.getMailboxId());
    }
}
