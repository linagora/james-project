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

package org.apache.james.transport.matchers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import javax.mail.MessagingException;

import org.apache.mailet.MailAddress;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMatcherConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LimitRecipientsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private LimitRecipients testee;

    @Before
    public void setUp() {
        testee = new LimitRecipients();
    }

    @Test(expected = MessagingException.class)
    public void initShouldThrowOnAbsentCondition() throws Exception {
        testee.init(FakeMatcherConfig.builder()
            .matcherName("name")
            .build());
    }

    @Test(expected = MessagingException.class)
    public void initShouldThrowOnInvalidCondition()throws Exception {
        testee.init(FakeMatcherConfig.builder()
            .matcherName("name")
            .condition("q")
            .build());
    }

    @Test(expected = MessagingException.class)
    public void initShouldThrowOnEmptyCondition() throws MessagingException {
        testee.init(FakeMatcherConfig.builder()
            .matcherName("name")
            .condition("")
            .build());
    }

    @Test(expected = MessagingException.class)
    public void initShouldThrowOnZeroCondition() throws MessagingException {
        testee.init(FakeMatcherConfig
            .builder()
            .matcherName("name")
            .condition("0")
            .build());
    }

    @Test(expected = MessagingException.class)
    public void initShouldThrowOnNegativeCondition() throws MessagingException {
        testee.init(FakeMatcherConfig
            .builder()
            .matcherName("name")
            .condition("-1")
            .build());
    }

    @Test
    public void matchShouldReturnNoRecipientWhenMailHaveNoRecipient() throws MessagingException {
        testee.init(FakeMatcherConfig
            .builder()
            .matcherName("name")
            .condition("3")
            .build());


        Collection<MailAddress> result = testee.match(FakeMail.builder().recipients().build());

        assertThat(result).isEmpty();
    }

    @Test
    public void matchShouldNotModifyMailsUnderLimit() throws MessagingException {
        testee.init(FakeMatcherConfig
            .builder()
            .matcherName("name")
            .condition("3")
            .build());

        MailAddress mailAddress = new MailAddress("mailai@gmail.com");

        Collection<MailAddress> result = testee.match(
            FakeMail.builder()
                .recipients(mailAddress)
                .build());

        assertThat(result).containsOnly(mailAddress);
    }


    @Test
    public void matchShouldNotModifyMailsAtLimit() throws MessagingException {
        testee.init(FakeMatcherConfig
            .builder()
            .matcherName("name")
            .condition("3")
            .build());

        MailAddress mailAddress = new MailAddress("mailai@gmail.com");
        MailAddress mailAddress1 = new MailAddress("cuc@gmail.com");
        MailAddress mailAddress2 = new MailAddress("lun@gmail.com");

        Collection<MailAddress> result = testee.match(
            FakeMail.builder()
            .recipients(mailAddress, mailAddress1, mailAddress2)
                .build());

        assertThat(result).containsOnly(mailAddress,mailAddress1,mailAddress2);
    }
    
    @Test
    public void matchShouldLimitRecipientCountWhenMailsOverLimit() throws MessagingException {
        testee.init(FakeMatcherConfig.builder()
            .matcherName("name")
            .condition("3")
            .build());

        MailAddress mailAddress = new MailAddress("mailai@gmail.com");
        MailAddress mailAddress1 = new MailAddress("cuc@gmail.com");
        MailAddress mailAddress2 = new MailAddress("lun@gmail.com");
        MailAddress mailAddress3 = new MailAddress("bonoit@gmail.com");

        Collection<MailAddress> result = testee.match(
            FakeMail.builder()
                .recipients(mailAddress, mailAddress1, mailAddress2, mailAddress3)
                .build());

        assertThat(result).containsOnly(mailAddress, mailAddress1, mailAddress2);
    }

}
