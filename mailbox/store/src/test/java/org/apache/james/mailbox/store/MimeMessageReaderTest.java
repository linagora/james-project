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

import static org.apache.james.mailbox.store.mail.model.StandardNames.MIME_CONTENT_TRANSFER_ENCODING_NAME;
import static org.apache.james.mailbox.store.mail.model.StandardNames.MIME_CONTENT_TRANSFER_ENCODING_SPACE;
import static org.apache.james.mailbox.store.mail.model.StandardNames.MIME_CONTENT_TYPE_PARAMETER_BOUNDARY_NAME;
import static org.apache.james.mailbox.store.mail.model.StandardNames.MIME_CONTENT_TYPE_PARAMETER_CHARSET_NAME;
import static org.apache.james.mailbox.store.mail.model.StandardNames.MIME_CONTENT_TYPE_PARAMETER_SPACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.james.mailbox.store.mail.model.impl.SimpleProperty;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.DefaultMessageId;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

public class MimeMessageReaderTest {
    private static final MessageId MESSAGE_ID = new DefaultMessageId.Factory().generate();
    private static final Date SUN_SEP_9TH_2001 = new Date(1000000000000L);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MessageParser messageParser;

    @Before
    public void setUp() {
        messageParser = mock(MessageParser.class);
    }

    @Test
    public void initShouldThrowWithNullStream() throws Exception {
        expectedException.expect(NullPointerException.class);
        new MimeMessageReader(messageParser, null);
    }

    @Test
    public void readShouldCopyContent() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(IOUtils.toString(mimeMessageReader.read(MESSAGE_ID, SUN_SEP_9TH_2001).getFullContent(), Charsets.UTF_8))
            .isEqualTo(IOUtils.toString(ClassLoader.getSystemResourceAsStream("eml/mail.eml"), Charsets.UTF_8));
    }

    @Test
    public void readShouldCalculateSize() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(mimeMessageReader.read(MESSAGE_ID, SUN_SEP_9TH_2001).getFullContentOctets())
            .isEqualTo(5250);
    }

    @Test
    public void readShouldCalculateBodyOctets() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(mimeMessageReader.read(MESSAGE_ID, SUN_SEP_9TH_2001).getBodyOctets())
            .isEqualTo(465);
    }

    @Test
    public void readShouldCalculateLineCountOfTextParts() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(mimeMessageReader.read(MESSAGE_ID, SUN_SEP_9TH_2001).getTextualLineCount())
            .isEqualTo(10);
    }

    @Test
    public void readShouldRetrieveCharset() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(mimeMessageReader.read(MESSAGE_ID, SUN_SEP_9TH_2001).getProperties())
            .contains(new SimpleProperty(MIME_CONTENT_TYPE_PARAMETER_SPACE, MIME_CONTENT_TYPE_PARAMETER_CHARSET_NAME, "UTF-8"));
    }

    @Test
    public void readShouldRetrieveTransferEncoding() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(mimeMessageReader.read(MESSAGE_ID, SUN_SEP_9TH_2001).getProperties())
            .contains(new SimpleProperty(MIME_CONTENT_TRANSFER_ENCODING_SPACE, MIME_CONTENT_TRANSFER_ENCODING_NAME, "7bit"));
    }

    @Test
    public void readShouldRetrieveMediaType() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(mimeMessageReader.read(MESSAGE_ID, SUN_SEP_9TH_2001).getMediaType())
            .isEqualTo("text");
    }

    @Test
    public void readShouldRetrieveSubtype() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/htmlMail.eml"));

        assertThat(mimeMessageReader.read(MESSAGE_ID, SUN_SEP_9TH_2001).getSubType())
            .isEqualTo("alternative");
    }

    @Test
    public void readShouldRetrieveBoundary() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/htmlMail.eml"));

        assertThat(mimeMessageReader.read(MESSAGE_ID, SUN_SEP_9TH_2001).getProperties())
            .contains(new org.apache.james.mailbox.store.mail.model.impl.SimpleProperty(MIME_CONTENT_TYPE_PARAMETER_SPACE, MIME_CONTENT_TYPE_PARAMETER_BOUNDARY_NAME, "--==_mimepart_556fffe8c7e84_7ed0e0fe20445637"));
    }

    @Test
    public void readShouldReturnAttachments() throws Exception {
        ImmutableList<MessageAttachment> attachments = ImmutableList.of();
        when(messageParser.retrieveAttachments(any(InputStream.class))).thenReturn(attachments);

        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/htmlMail.eml"));

        assertThat(mimeMessageReader.read(MESSAGE_ID, SUN_SEP_9TH_2001).getAttachments()).isEmpty();
    }

}
