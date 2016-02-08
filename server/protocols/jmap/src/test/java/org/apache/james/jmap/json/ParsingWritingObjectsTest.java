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

package org.apache.james.jmap.json;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.apache.commons.io.IOUtils;
import org.apache.james.jmap.methods.GetMessagesMethod;
import org.apache.james.jmap.methods.JmapResponseWriterImpl;
import org.apache.james.jmap.model.Emailer;
import org.apache.james.jmap.model.Message;
import org.apache.james.jmap.model.MessageId;
import org.apache.james.jmap.model.SubMessage;
import org.junit.Test;

import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ParsingWritingObjectsTest {


    private static final MessageId MESSAGE_ID = MessageId.of("username|mailbox|1");
    private static final String BLOB_ID = "myBlobId";
    private static final String THREAD_ID = "myThreadId";
    private static final ImmutableList<String> MAILBOX_IDS = ImmutableList.of("mailboxId1", "mailboxId2");
    private static final String IN_REPLY_TO_MESSAGE_ID = "myInReplyToMessageId";
    private static final boolean IS_UNREAD = true;
    private static final boolean IS_FLAGGED = true;
    private static final boolean IS_ANSWERED = true;
    private static final boolean IS_DRAFT = true;
    private static final boolean HAS_ATTACHMENT = true;
    private static final ImmutableMap<String, String> HEADERS = ImmutableMap.of("h1", "h1Value", "h2", "h2Value");
    private static final Emailer FROM = Emailer.builder().name("myName").email("myEmail@james.org").build();
    private static final ImmutableList<Emailer> TO = ImmutableList.of(Emailer.builder().name("to1").email("to1@james.org").build(),
            Emailer.builder().name("to2").email("to2@james.org").build());
    private static final ImmutableList<Emailer> CC = ImmutableList.of(Emailer.builder().name("cc1").email("cc1@james.org").build(),
            Emailer.builder().name("cc2").email("cc2@james.org").build());
    private static final ImmutableList<Emailer> BCC = ImmutableList.of(Emailer.builder().name("bcc1").email("bcc1@james.org").build(),
            Emailer.builder().name("bcc2").email("bcc2@james.org").build());
    private static final ImmutableList<Emailer> REPLY_TO = ImmutableList.of(Emailer.builder().name("replyTo1").email("replyTo1@james.org").build(),
            Emailer.builder().name("replyTo2").email("replyTo2@james.org").build());
    private static final String SUBJECT = "mySubject";
    private static final ZonedDateTime DATE = ZonedDateTime.parse("2014-10-30T14:12:00Z").withZoneSameLocal(ZoneId.of("GMT"));
    private static final int SIZE = 1024;
    private static final String PREVIEW = "myPreview";
    private static final String TEXT_BODY = "myTextBody";
    private static final String HTML_BODY = "<h1>myHtmlBody</h1>";

    private static final Message MESSAGE = Message.builder()
            .id(MESSAGE_ID)
            .blobId(BLOB_ID)
            .threadId(THREAD_ID)
            .mailboxIds(MAILBOX_IDS)
            .inReplyToMessageId(IN_REPLY_TO_MESSAGE_ID)
            .isUnread(IS_UNREAD)
            .isFlagged(IS_FLAGGED)
            .isAnswered(IS_ANSWERED)
            .isDraft(IS_DRAFT)
            .hasAttachment(HAS_ATTACHMENT)
            .headers(HEADERS)
            .from(FROM)
            .to(TO)
            .cc(CC)
            .bcc(BCC)
            .replyTo(REPLY_TO)
            .subject(SUBJECT)
            .date(DATE)
            .size(SIZE)
            .preview(PREVIEW)
            .textBody(TEXT_BODY)
            .htmlBody(HTML_BODY)
            .build();

    private static final SubMessage SUB_MESSAGE = SubMessage.builder()
            .headers(HEADERS)
            .from(FROM)
            .to(TO)
            .cc(CC)
            .bcc(BCC)
            .replyTo(REPLY_TO)
            .subject(SUBJECT)
            .date(DATE)
            .textBody(TEXT_BODY)
            .htmlBody(HTML_BODY)
            .build();

    @Test
    public void parsingJsonShouldWorkOnSubMessage() throws Exception {
        SubMessage expected = SUB_MESSAGE;

        SubMessage subMessage = new ObjectMapperFactory().forParsing()
            .readValue(IOUtils.toString(ClassLoader.getSystemResource("json/subMessage.json")), SubMessage.class);

        assertThat(subMessage).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void writingJsonShouldWorkOnSubMessage() throws Exception {
        String expected = IOUtils.toString(ClassLoader.getSystemResource("json/subMessage.json"));

        String json = new ObjectMapperFactory().forWriting()
                .writeValueAsString(SUB_MESSAGE);

        assertThatJson(json)
            .when(IGNORING_ARRAY_ORDER)
            .isEqualTo(expected);

    }

    @Test
    public void parsingJsonShouldWorkOnMessage() throws Exception {
        Message expected = MESSAGE;

        Message message = new ObjectMapperFactory().forParsing()
            .readValue(IOUtils.toString(ClassLoader.getSystemResource("json/message.json")), Message.class);

        assertThat(message).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void writingJsonShouldWorkOnMessage() throws Exception {
        String expected = IOUtils.toString(ClassLoader.getSystemResource("json/message.json"));

        SimpleFilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter(JmapResponseWriterImpl.PROPERTIES_FILTER, SimpleBeanPropertyFilter.serializeAll())
                .addFilter(GetMessagesMethod.HEADERS_FILTER, SimpleBeanPropertyFilter.serializeAll());

        String json = new ObjectMapperFactory().forWriting()
                .setFilterProvider(filterProvider)
                .writeValueAsString(MESSAGE);

        assertThatJson(json)
            .when(IGNORING_ARRAY_ORDER)
            .isEqualTo(expected);

    }
}
