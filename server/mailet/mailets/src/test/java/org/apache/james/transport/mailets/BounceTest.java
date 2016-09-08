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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.james.dnsservice.api.DNSService;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class BounceTest {

    private static final String MAILET_NAME = "mailetName";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Bounce bounce;
    private FakeMailContext fakeMailContext;

    @Before
    public void setUp() throws Exception {
        bounce = new Bounce();
        DNSService dnsService = mock(DNSService.class);
        bounce.setDNSService(dnsService);
        fakeMailContext = FakeMailContext.defaultContext();

        when(dnsService.getLocalHost()).thenThrow(new UnknownHostException());
    }

    @Test
    public void getMailetInfoShouldReturnValue() {
        assertThat(bounce.getMailetInfo()).isEqualTo("Bounce Mailet");
    }

    @Test
    public void initShouldThrowWhenUnkownParameter() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("unknown", "error");
        expectedException.expect(MessagingException.class);

        bounce.init(mailetConfig);
    }

    @Test
    public void initShouldNotThrowWhenEveryParameters() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("debug", "true");
        mailetConfig.setProperty("passThrough", "false");
        mailetConfig.setProperty("fakeDomainCheck", "false");
        mailetConfig.setProperty("inline", "all");
        mailetConfig.setProperty("attachment", "none");
        mailetConfig.setProperty("message", "custom message");
        mailetConfig.setProperty("notice", "");
        mailetConfig.setProperty("sender", "sender@domain.org");
        mailetConfig.setProperty("sendingAddress", "sender@domain.org");
        mailetConfig.setProperty("prefix", "my prefix");
        mailetConfig.setProperty("attachError", "true");

        bounce.init(mailetConfig);
    }

    @Test
    public void bounceShouldReturnAMailToTheSenderWithoutAttributes() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        bounce.init(mailetConfig);

        MailAddress senderMailAddress = new MailAddress("sender@domain.com");
        FakeMail mail = FakeMail.builder()
                .sender(senderMailAddress)
                .name(MAILET_NAME)
                .recipient(new MailAddress("recipient@domain.com"))
                .build();
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mimeMessage.setText("My content");
        mail.setMessage(mimeMessage);

        bounce.service(mail);

        MailAddress sender = null;
        Collection<MailAddress> recipients = Lists.newArrayList(senderMailAddress);
        MimeMessage message = null;
        Map<String, Serializable> attributes = ImmutableMap.<String, Serializable> of();
        FakeMailContext.SentMail expected = new FakeMailContext.SentMail(sender, recipients, message, attributes);
        assertThat(fakeMailContext.getSentMails()).containsOnly(expected);
    }

    @Test
    public void bounceShouldChangeTheStateWhenNoSenderAndFalse() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("passThrough", "false");
        bounce.init(mailetConfig);

        FakeMail mail = FakeMail.builder()
                .name(MAILET_NAME)
                .recipient(new MailAddress("recipient@domain.com"))
                .build();

        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mimeMessage.setText("My content");
        mail.setMessage(mimeMessage);

        bounce.service(mail);

        assertThat(mail.getState()).isEqualTo(Mail.GHOST);
    }

    @Test
    public void bounceShouldNotChandeTheStateWhenNoSenderAndTrue() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("passThrough", "true");
        bounce.init(mailetConfig);

        String initialState = "initial";
        FakeMail mail = FakeMail.builder()
                .state(initialState)
                .name(MAILET_NAME)
                .recipient(new MailAddress("recipient@domain.com"))
                .build();

        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mimeMessage.setText("My content");
        mail.setMessage(mimeMessage);

        bounce.service(mail);

        assertThat(mail.getState()).isEqualTo(initialState);
    }
}
