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

import java.io.IOException;
import java.util.Date;

import javax.mail.Flags;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.TestId;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.impl.MessageUtil;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

public class MailboxMessageAssertTest {

    public static final TestId MAILBOX_ID = TestId.of(42L);
    public static final MessageUid UID = MessageUid.of(24);
    public static final MessageId MESSAGE_ID = new DefaultMessageId();
    private static final Date DATE = new Date();

    @Test
    public void messageAssertShouldSucceedWithTwoEqualsMessages() throws IOException {
        String headerString = "name: headerName\n\n";
        String bodyString = "body\n.\n";
        Date date = new Date();

        MailboxMessage message1 = createMessage(headerString, bodyString);

        MailboxMessage message2 = createMessage(headerString, bodyString);

        MessageAssert.assertThat(message1).isEqualTo(message2, MessageMapper.FetchType.Full);
    }

    @Test
    public void messageAssertShouldSucceedWhenBodyMismatchInFetchHeaderMode() throws IOException {
        String headerString = "name: headerName\n\n";
        String bodyString = "body\n.\n";
        Date date = new Date();

        MailboxMessage message1 = createMessage(headerString, bodyString);

        bodyString = "work\n.\n";

        MailboxMessage message2 = createMessage(headerString, bodyString);

        MessageAssert.assertThat(message1).isEqualTo(message2, MessageMapper.FetchType.Headers);
    }

    @Test(expected = AssertionError.class)
    public void messageAssertShouldFailWhenBodyMismatchInFetchBodyMode() throws IOException {
        String headerString = "name: headerName\n\n";
        String bodyString = "body\n.\n";
        Date date = new Date();

        MutableMailboxMessage message1 = createMessage(headerString, bodyString);

        message1.setUid(UID);
        bodyString = "work\n.\n";

        MutableMailboxMessage message2 = createMessage(headerString, bodyString);
        message2.setUid(UID);

        MessageAssert.assertThat(message1).isEqualTo(message2, MessageMapper.FetchType.Body);
    }

    private MutableMailboxMessage createMessage(String headerString, String bodyString) {
        return MessageUtil.buildMutableMailboxMessage()
            .messageId(MESSAGE_ID)
            .internalDate(DATE)
            .size(headerString.length() + bodyString.length())
            .bodyStartOctet(headerString.length())
            .content(new SharedByteArrayInputStream((headerString + bodyString).getBytes(Charsets.UTF_8)))
            .flags(new Flags())
            .propertyBuilder(new PropertyBuilder())
            .mailboxId(MAILBOX_ID)
            .uid(UID)
            .attachments(ImmutableList.<MessageAttachment>of())
            .build();
    }
}
