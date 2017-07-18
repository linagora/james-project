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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Flags;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.TestId;
import org.apache.james.mailbox.store.SimpleMessageMetaData;
import org.apache.james.mailbox.store.mail.model.impl.MessageUtil;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

public class MetadataMapAssertTest {

    private static final MessageUid UID = MessageUid.of(18);
    private static final MessageId MESSAGE_ID = new DefaultMessageId();
    private static final Long MODSEQ = 24L;
    private static final Date DATE = new Date();
    private static final String HEADER_STRING = "name: headerName\n\n";
    private static final String BODY_STRING = "body\\n.\\n";
    private static final TestId MAILBOX_ID = TestId.of(12L);

    private MutableMailboxMessage message1;

    @Before
    public void setUp() {
        message1 = MessageUtil.buildMutableMailboxMessage()
            .messageId(MESSAGE_ID)
            .internalDate(DATE)
            .size(HEADER_STRING.length() + BODY_STRING.length())
            .bodyStartOctet(HEADER_STRING.length())
            .content(new SharedByteArrayInputStream((HEADER_STRING + BODY_STRING).getBytes(Charsets.UTF_8)))
            .flags(new Flags())
            .propertyBuilder(new PropertyBuilder())
            .mailboxId(MAILBOX_ID)
            .modSeq(MODSEQ)
            .uid(UID)
            .attachments(ImmutableList.<MessageAttachment>of())
            .build();
    }

    @Test
    public void metadataMapAssertShouldSucceedWhenContainingRightMetadata() {
        Map<MessageUid, MessageMetaData> metaDataMap = new HashMap<MessageUid, MessageMetaData>();
        metaDataMap.put(UID, new SimpleMessageMetaData(UID, MODSEQ, new Flags(), HEADER_STRING.length() + BODY_STRING.length(), DATE, MESSAGE_ID));
        MetadataMapAssert.assertThat(metaDataMap).containsMetadataForMessages(message1);
    }

    @Test(expected = AssertionError.class)
    public void metadataMapAssertShouldFailWhenUidMismatch() {
        Map<MessageUid, MessageMetaData> metaDataMap = new HashMap<MessageUid, MessageMetaData>();
        metaDataMap.put(UID, new SimpleMessageMetaData(UID.next(), MODSEQ, new Flags(), HEADER_STRING.length() + BODY_STRING.length(), DATE, MESSAGE_ID));
        MetadataMapAssert.assertThat(metaDataMap).containsMetadataForMessages(message1);
    }

    @Test(expected = AssertionError.class)
    public void metadataMapAssertShouldFailWhenDateMismatch() {
        Map<MessageUid, MessageMetaData> metaDataMap = new HashMap<MessageUid, MessageMetaData>();
        Date date = new Date();
        date.setTime(DATE.getTime() + 100L);
        metaDataMap.put(UID, new SimpleMessageMetaData(UID, MODSEQ, new Flags(), HEADER_STRING.length() + BODY_STRING.length(), date, MESSAGE_ID));
        MetadataMapAssert.assertThat(metaDataMap).containsMetadataForMessages(message1);
    }

    @Test(expected = AssertionError.class)
    public void metadataMapAssertShouldFailWhenSizeMismatch() {
        Map<MessageUid, MessageMetaData> metaDataMap = new HashMap<MessageUid, MessageMetaData>();
        metaDataMap.put(UID, new SimpleMessageMetaData(UID , MODSEQ, new Flags(), HEADER_STRING.length() + BODY_STRING.length() + 1, DATE, MESSAGE_ID));
        MetadataMapAssert.assertThat(metaDataMap).containsMetadataForMessages(message1);
    }
}
