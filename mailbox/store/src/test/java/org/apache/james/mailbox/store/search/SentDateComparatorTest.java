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

package org.apache.james.mailbox.store.search;

import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.search.comparator.SentDateComparator;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.when;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.assertj.core.api.Assertions.assertThat;

public class SentDateComparatorTest {

    @Mock
    MailboxMessage message1;

    @Mock
    MailboxMessage message2;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    public Date smallestDate = new GregorianCalendar(2017, 0, 21).getTime();
    public Date biggestDate = new GregorianCalendar(2017, 0, 22).getTime();

    @Test
    public void sentDateComparatorTestShouldCompareOnSentDateWhenPresentAndNotOnInternalDate() throws IOException {
        when(message1.getFullContent()).then(getInputStreamAnswer("Date: Fri, 2 Jun 2017 10:37:47 -0400\r\n"));
        when(message1.getInternalDate()).thenReturn(biggestDate);

        when(message2.getFullContent()).then(getInputStreamAnswer("Date: Fri, 2 Jun 2017 11:37:47 -0400\r\n"));
        when(message2.getInternalDate()).thenReturn(smallestDate);

        assertThat(SentDateComparator.SENTDATE.compare(message1, message2)).isNegative();
        assertThat(SentDateComparator.SENTDATE.compare(message2, message1)).isPositive();
    }

    @Test
    public void sentDateComparatorTestShouldUseInternalDateWhenNoSentDate() throws IOException {
        when(message1.getFullContent()).then(getInputStreamAnswer(""));
        when(message1.getInternalDate()).thenReturn(smallestDate);

        when(message2.getFullContent()).thenReturn(new ByteArrayInputStream("".getBytes(Charset.forName("utf-8"))));
        when(message2.getInternalDate()).then(getInputStreamAnswer(""));

        assertThat(SentDateComparator.SENTDATE.compare(message1, message2)).isNegative();
        assertThat(SentDateComparator.SENTDATE.compare(message2, message1)).isPositive();
    }

    @Test
    public void sentDateComparatorTestShouldCompareInternalDateAgainstSentDateWhenOneMailOverTwoHasNoSentDate() throws IOException {
        System.out.println(message1.getHeaderContent());

        when(message1.getFullContent()).then(getInputStreamAnswer(""));
        when(message1.getInternalDate()).thenReturn(biggestDate);
        when(message2.getFullContent()).then(getInputStreamAnswer("Date: Fri, 2 Jun 2017 10:37:47 -0400\r\n"));
        when(message2.getInternalDate()).thenReturn(smallestDate);

        assertThat(SentDateComparator.SENTDATE.compare(message1, message2)).isNegative();
        assertThat(SentDateComparator.SENTDATE.compare(message2, message1)).isPositive();
    }

    private Answer<InputStream> getInputStreamAnswer(final String string) {
        return new Answer<InputStream>() {
            @Override
            public InputStream answer(InvocationOnMock invocationOnMock) throws Throwable {
                return new ByteArrayInputStream(string.getBytes());
            }
        };
    }
}
