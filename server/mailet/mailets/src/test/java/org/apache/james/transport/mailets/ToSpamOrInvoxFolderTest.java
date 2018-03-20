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
package org.apache.james.transport.mailets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.metrics.api.Metric;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.util.MimeMessageUtil;
import org.apache.james.util.scanner.SpamAssassinResult;
import org.apache.mailet.Mail;
import org.apache.mailet.PerRecipientHeaders.Header;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableList;

public class ToSpamOrInvoxFolderTest {

    @Rule public ExpectedException expectedException = ExpectedException.none();

    private ToSpamOrInboxFolder mailet;
    private MailboxManager mailboxManager;
    private UsersRepository usersRepository;
    private MetricFactory metricFactory;


    @Before
    public void setup() throws Exception {
        mailboxManager = mock(MailboxManager.class);
        usersRepository = mock(UsersRepository.class);
        metricFactory = mock(MetricFactory.class);
        mailet = new ToSpamOrInboxFolder(mailboxManager, usersRepository, metricFactory);
    }

    @Test
    public void getMailetInfoShouldReturnExpectedContent() {
        String expected = "ToSpamOrInboxFolder Mailet";

        String actual = mailet.getMailetInfo();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void serviceShouldNotConsumeMailWhenNotSpam() throws Exception {
        Metric metric = mock(Metric.class);
        when(metricFactory.generate(any(String.class))).thenReturn(metric);

        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .build();
        mailet.init(mailetConfig);

        FakeMail message = FakeMail.defaultFakeMail();
        message.setRecipients(ImmutableList.of(new MailAddress("user@james.org")));
        message.setState(Mail.DEFAULT);
        mailet.service(message);

        assertThat(message.getState()).isEqualTo(Mail.DEFAULT);
    }

    @Test
    public void serviceShouldConsumeMailWhenSpam() throws Exception {
        Metric metric = mock(Metric.class);
        when(metricFactory.generate(any(String.class))).thenReturn(metric);

        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .build();
        mailet.init(mailetConfig);

        MimeMessage mimeMessage = MimeMessageUtil.mimeMessageFromString(
                "Subject: subject\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "\r\n" +
                    "This is a fake mail");
        mimeMessage.setFlag(Flag.RECENT, true);
        MailAddress recipient = new MailAddress("user@james.org");
        FakeMail message = FakeMail.builder()
                .name("message")
                .mimeMessage(mimeMessage)
                .recipient(recipient)
                .state(Mail.DEFAULT)
                .addHeaderForRecipient(
                        Header.builder()
                            .name(SpamAssassinResult.STATUS_MAIL_ATTRIBUTE_NAME)
                            .value("YES")
                            .build(),
                        recipient)
                .build();
        mailet.service(message);

        assertThat(message.getState()).isEqualTo(Mail.GHOST);
    }

    @Test
    public void serviceShouldModifyRecipientsWhenSomeAreDeliveredInSpam() throws Exception {
        Metric metric = mock(Metric.class);
        when(metricFactory.generate(any(String.class))).thenReturn(metric);

        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .build();
        mailet.init(mailetConfig);

        MimeMessage mimeMessage = MimeMessageUtil.mimeMessageFromString(
                "Subject: subject\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "\r\n" +
                    "This is a fake mail");
        mimeMessage.setFlag(Flag.RECENT, true);
        MailAddress toSpam = new MailAddress("user@james.org");
        MailAddress toSpam2 = new MailAddress("user2@james.org");
        MailAddress toInbox = new MailAddress("user3@james.org");
        MailAddress toInbox2 = new MailAddress("user4@james.org");
        FakeMail message = FakeMail.builder()
                .name("message")
                .mimeMessage(mimeMessage)
                .recipients(toSpam, toSpam2, toInbox, toInbox2)
                .state(Mail.DEFAULT)
                .addHeaderForRecipient(
                        Header.builder()
                            .name(SpamAssassinResult.STATUS_MAIL_ATTRIBUTE_NAME)
                            .value("YES")
                            .build(),
                        toSpam)
                .addHeaderForRecipient(
                        Header.builder()
                            .name(SpamAssassinResult.STATUS_MAIL_ATTRIBUTE_NAME)
                            .value("YES")
                            .build(),
                        toSpam2)
                .addHeaderForRecipient(
                        Header.builder()
                            .name(SpamAssassinResult.STATUS_MAIL_ATTRIBUTE_NAME)
                            .value("NO")
                            .build(),
                            toInbox)
                .build();
        mailet.service(message);

        assertThat(message.getRecipients()).containsOnly(toInbox, toInbox2);
    }
}
