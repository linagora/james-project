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
package org.apache.james.mailbox.store.mail.model.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;

import javax.mail.Flags;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.mutable.Mutable;
import org.apache.james.mailbox.FlagsBuilder;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.TestId;
import org.apache.james.mailbox.model.TestMessageId;
import org.apache.james.mailbox.store.mail.model.DefaultMessageId;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.MutableMailboxMessage;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.internal.FieldByFieldComparator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

public class MessageUtilTest {
    private static final Charset MESSAGE_CHARSET = Charsets.UTF_8;
    private static final String MESSAGE_CONTENT = "Simple message content without special characters";
    public static final SharedByteArrayInputStream CONTENT_STREAM = new SharedByteArrayInputStream(MESSAGE_CONTENT.getBytes(MESSAGE_CHARSET));
    private static final String MESSAGE_CONTENT_SPECIAL_CHAR = "Simple message content with special characters: \"'(§è!çà$*`";
    public static final TestId TEST_ID = TestId.of(1L);
    public static final int BODY_START_OCTET = 0;
    public static final MessageId MESSAGE_ID = new TestMessageId.Factory().generate();
    public static final int SIZE = 1000;
    private MutableMailboxMessage MESSAGE;
    private MutableMailboxMessage MESSAGE_SPECIAL_CHAR;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public final JUnitSoftAssertions soft = new JUnitSoftAssertions();

    @Before
    public void setUp() {
        MESSAGE = buildMessage(MESSAGE_CONTENT);
        MESSAGE_SPECIAL_CHAR = buildMessage(MESSAGE_CONTENT_SPECIAL_CHAR);
    }

    @Test
    public void testSize() {
        assertThat(MESSAGE.getFullContentOctets()).isEqualTo(MESSAGE_CONTENT.length());
    }

    @Test
    public void testInputStreamSize() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            byteArrayOutputStream.write(MESSAGE.getFullContent());
            assertThat(byteArrayOutputStream.size()).isEqualTo(MESSAGE_CONTENT.getBytes(MESSAGE_CHARSET).length);
        } finally {
            byteArrayOutputStream.close();
        }
    }

    @Test
    public void testInputStreamSizeSpecialCharacters() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            byteArrayOutputStream.write(MESSAGE_SPECIAL_CHAR.getFullContent());
            assertThat(byteArrayOutputStream.size()).isEqualTo(MESSAGE_CONTENT_SPECIAL_CHAR.getBytes(MESSAGE_CHARSET).length);
        } finally {
            byteArrayOutputStream.close();
        }
    }

    @Test
    public void testFullContent() throws IOException {
        assertThat(new String(IOUtils.toByteArray(MESSAGE.getFullContent()), MESSAGE_CHARSET)).isEqualTo(MESSAGE_CONTENT);
        assertThat(new String(IOUtils.toByteArray(MESSAGE_SPECIAL_CHAR.getFullContent()), MESSAGE_CHARSET)).isEqualTo(MESSAGE_CONTENT_SPECIAL_CHAR);
    }

    @Test
    public void simpleMessageShouldReturnTheSameUserFlagsThatThoseProvided() {
        MESSAGE.setFlags(new FlagsBuilder().add(Flags.Flag.DELETED, Flags.Flag.SEEN).add("mozzarela", "parmesan", "coppa", "limonchello").build());
        assertThat(MESSAGE.createFlags().getUserFlags()).containsOnly("mozzarela", "parmesan", "coppa", "limonchello");
    }

    @Test
    public void copyShouldReturnFieldByFieldEqualsObject() throws MailboxException {
        long textualLineCount = 42L;
        String text = "text";
        String plain = "plain";
        PropertyBuilder propertyBuilder = new PropertyBuilder();
        propertyBuilder.setTextualLineCount(textualLineCount);
        propertyBuilder.setMediaType(text);
        propertyBuilder.setSubType(plain);

        MailboxMessage original = MessageUtil.buildMutableMailboxMessage()
            .messageId(new TestMessageId.Factory().generate())
            .internalDate(new Date())
            .size(MESSAGE_CONTENT.length())
            .bodyStartOctet(BODY_START_OCTET)
            .content(CONTENT_STREAM)
            .flags(FlagsBuilder.builder().build())
            .propertyBuilder(propertyBuilder)
            .attachments(ImmutableList.<MessageAttachment>of())
            .mailboxId(TEST_ID)
            .build();

        MailboxMessage copy = MessageUtil.copyToMutable(original, TestId.of(1337));

        assertThat((Object)copy).isEqualToIgnoringGivenFields(original, "message", "mailboxId").isNotSameAs(original);
        assertThat(MessageUtil.copyToMutable(original, TEST_ID)).usingComparator(new FieldByFieldComparator()).isEqualTo(original);
        assertThat(MessageUtil.copyToMutable(original, TEST_ID).getTextualLineCount()).isEqualTo(textualLineCount);
        assertThat(MessageUtil.copyToMutable(original, TEST_ID).getMediaType()).isEqualTo(text);
        assertThat(MessageUtil.copyToMutable(original, TEST_ID).getSubType()).isEqualTo(plain);

    }

    private static MutableMailboxMessage buildMessage(String content) {
        return MessageUtil.buildMutableMailboxMessage()
            .messageId(new DefaultMessageId())
            .internalDate(Calendar.getInstance().getTime())
            .size(content.length())
            .bodyStartOctet(BODY_START_OCTET)
            .content(new SharedByteArrayInputStream(content.getBytes(MESSAGE_CHARSET)))
            .flags(FlagsBuilder.builder().build())
            .propertyBuilder(new PropertyBuilder())
            .mailboxId(TEST_ID)
            .attachments(ImmutableList.<MessageAttachment>of())
            .build();
    }

    @Test
    public void modseqShouldThrowWhenNegative() {
        expectedException.expect(IllegalArgumentException.class);

        MessageUtil.buildMutableMailboxMessage()
            .modSeq(-1);
    }

    @Test
    public void sizeShouldThrowWhenNegative() {
        expectedException.expect(IllegalArgumentException.class);

        MessageUtil.buildMutableMailboxMessage()
            .size(-1);
    }

    @Test
    public void bodyStartOctetShouldThrowWhenNegative() {
        expectedException.expect(IllegalArgumentException.class);

        MessageUtil.buildMutableMailboxMessage()
            .bodyStartOctet(-1);
    }

    @Test
    public void buildShouldWorkWithMinimalContent() {
        Date internalDate = new Date();
        MessageUtil.buildMutableMailboxMessage()
            .messageId(MESSAGE_ID)
            .mailboxId(TEST_ID)
            .internalDate(internalDate)
            .bodyStartOctet(BODY_START_OCTET)
            .size(SIZE)
            .content(CONTENT_STREAM)
            .flags(new Flags())
            .propertyBuilder(new PropertyBuilder())
            .attachments(ImmutableList.<MessageAttachment>of())
            .build();
    }



    @Test
    public void buildShouldCreateAMessageWithAllFields() throws IOException {
        Date internalDate = new Date();
        Flags flags = new Flags();
        PropertyBuilder propertyBuilder = new PropertyBuilder();
        int modseq = 145;
        MessageUid uid = MessageUid.of(45);
        MessageAttachment messageAttachment = MessageAttachment.builder()
            .attachment(Attachment.builder()
                .bytes("".getBytes(Charsets.UTF_8))
                .type("type")
                .build())
            .name("name")
            .isInline(false)
            .build();
        MailboxMessage message = MessageUtil.buildMutableMailboxMessage()
            .messageId(MESSAGE_ID)
            .mailboxId(TEST_ID)
            .modSeq(modseq)
            .uid(uid)
            .internalDate(internalDate)
            .bodyStartOctet(BODY_START_OCTET)
            .size(SIZE)
            .content(CONTENT_STREAM)
            .flags(flags)
            .propertyBuilder(propertyBuilder)
            .attachments(ImmutableList.of(messageAttachment))
            .build();

        soft.assertThat(message.getMessageId()).isEqualTo(MESSAGE_ID);
        soft.assertThat(message.getMailboxId()).isEqualTo(TEST_ID);
        soft.assertThat(message.getInternalDate()).isEqualTo(internalDate);
        soft.assertThat(message.getHeaderOctets()).isEqualTo(BODY_START_OCTET);
        soft.assertThat(message.getFullContentOctets()).isEqualTo(SIZE);
        soft.assertThat(IOUtils.toString(message.getFullContent(), Charsets.UTF_8)).isEqualTo(MESSAGE_CONTENT);
        soft.assertThat(message.createFlags()).isEqualTo(flags);
        soft.assertThat(message.getProperties()).isEqualTo(propertyBuilder.toProperties());
        soft.assertThat(message.getUid()).isEqualTo(uid);
        soft.assertThat(message.getModSeq()).isEqualTo(modseq);
        soft.assertThat(message.getAttachments()).containsOnly(messageAttachment);
    }

    @Test
    public void buildShouldThrowOnMissingMessageId() {
        expectedException.expect(IllegalStateException.class);

        Date internalDate = new Date();
        MessageUtil.buildMutableMailboxMessage()
            .mailboxId(TEST_ID)
            .internalDate(internalDate)
            .bodyStartOctet(BODY_START_OCTET)
            .size(SIZE)
            .content(CONTENT_STREAM)
            .flags(new Flags())
            .propertyBuilder(new PropertyBuilder())
            .attachments(ImmutableList.<MessageAttachment>of())
            .build();
    }

    @Test
    public void buildShouldThrowOnMissingMailboxId() {
        expectedException.expect(IllegalStateException.class);

        Date internalDate = new Date();
        MessageUtil.buildMutableMailboxMessage()
            .messageId(MESSAGE_ID)
            .internalDate(internalDate)
            .bodyStartOctet(BODY_START_OCTET)
            .size(SIZE)
            .content(CONTENT_STREAM)
            .flags(new Flags())
            .propertyBuilder(new PropertyBuilder())
            .attachments(ImmutableList.<MessageAttachment>of())
            .build();
    }

    @Test
    public void buildShouldThrowOnMissingInternalDate() {
        expectedException.expect(IllegalStateException.class);

        MessageUtil.buildMutableMailboxMessage()
            .messageId(MESSAGE_ID)
            .mailboxId(TEST_ID)
            .bodyStartOctet(BODY_START_OCTET)
            .size(SIZE)
            .content(CONTENT_STREAM)
            .flags(new Flags())
            .propertyBuilder(new PropertyBuilder())
            .attachments(ImmutableList.<MessageAttachment>of())
            .build();
    }

    @Test
    public void buildShouldThrowOnMissingBodyStartOctets() {
        expectedException.expect(IllegalStateException.class);

        Date internalDate = new Date();
        MessageUtil.buildMutableMailboxMessage()
            .messageId(MESSAGE_ID)
            .mailboxId(TEST_ID)
            .internalDate(internalDate)
            .size(SIZE)
            .content(CONTENT_STREAM)
            .flags(new Flags())
            .propertyBuilder(new PropertyBuilder())
            .attachments(ImmutableList.<MessageAttachment>of())
            .build();
    }

    @Test
    public void buildShouldThrowOnMissingSize() {
        expectedException.expect(IllegalStateException.class);

        Date internalDate = new Date();
        MessageUtil.buildMutableMailboxMessage()
            .messageId(MESSAGE_ID)
            .mailboxId(TEST_ID)
            .internalDate(internalDate)
            .bodyStartOctet(BODY_START_OCTET)
            .content(CONTENT_STREAM)
            .flags(new Flags())
            .propertyBuilder(new PropertyBuilder())
            .attachments(ImmutableList.<MessageAttachment>of())
            .build();
    }

    @Test
    public void buildShouldThrowOnMissingContent() {
        expectedException.expect(IllegalStateException.class);

        Date internalDate = new Date();
        MessageUtil.buildMutableMailboxMessage()
            .messageId(MESSAGE_ID)
            .mailboxId(TEST_ID)
            .internalDate(internalDate)
            .bodyStartOctet(BODY_START_OCTET)
            .size(SIZE)
            .flags(new Flags())
            .propertyBuilder(new PropertyBuilder())
            .attachments(ImmutableList.<MessageAttachment>of())
            .build();
    }

    @Test
    public void buildShouldThrowOnMissingFlags() {
        expectedException.expect(IllegalStateException.class);

        Date internalDate = new Date();
        MessageUtil.buildMutableMailboxMessage()
            .messageId(MESSAGE_ID)
            .mailboxId(TEST_ID)
            .internalDate(internalDate)
            .bodyStartOctet(BODY_START_OCTET)
            .size(SIZE)
            .content(CONTENT_STREAM)
            .propertyBuilder(new PropertyBuilder())
            .attachments(ImmutableList.<MessageAttachment>of())
            .build();
    }

    @Test
    public void buildShouldThrowOnMissingProperties() {
        expectedException.expect(IllegalStateException.class);

        Date internalDate = new Date();
        MessageUtil.buildMutableMailboxMessage()
            .messageId(MESSAGE_ID)
            .mailboxId(TEST_ID)
            .internalDate(internalDate)
            .bodyStartOctet(BODY_START_OCTET)
            .size(SIZE)
            .content(CONTENT_STREAM)
            .flags(new Flags())
            .attachments(ImmutableList.<MessageAttachment>of())
            .build();
    }

}
