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

import java.net.InetAddress;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.transport.mailets.AbstractRedirect.SpecialAddress;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.RFC822DateFormat;
import org.apache.mailet.base.mail.MimeMultipartReport;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.base.test.FakeMailContext.SentMail;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DSNBounceTest {

    private static final String MAILET_NAME = "mailetName";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private DSNBounce dsnBounce;
    private FakeMailContext fakeMailContext;

    @Before
    public void setUp() throws Exception {
        dsnBounce = new DSNBounce();
        DNSService dnsService = mock(DNSService.class);
        dsnBounce.setDNSService(dnsService);
        fakeMailContext = FakeMailContext.defaultContext();

        InetAddress localHost = InetAddress.getLocalHost();
        when(dnsService.getLocalHost())
            .thenReturn(localHost);
        when(dnsService.getHostName(localHost))
            .thenReturn("My host");
    }

    @Test
    public void getMailetInfoShouldReturnValue() {
        assertThat(dsnBounce.getMailetInfo()).isEqualTo("DSNBounce Mailet");
    }

    @Test
    public void getAllowedInitParametersShouldReturnTheParameters() {
        assertThat(dsnBounce.getAllowedInitParameters()).containsOnly("debug", "passThrough", "messageString", "attachment", "sender", "prefix");
    }

    @Test
    public void initShouldFailWhenUnknownParameterIsConfigured() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("unknwon", "value");
        expectedException.expect(MessagingException.class);

        dsnBounce.init(mailetConfig);
    }

    @Test
    public void getRecipientsShouldReturnReversePathOnly() {
        assertThat(dsnBounce.getRecipients()).containsOnly(SpecialAddress.REVERSE_PATH);
    }

    @Test
    public void getToShouldReturnReversePathOnly() {
        assertThat(dsnBounce.getTo()).containsOnly(SpecialAddress.REVERSE_PATH.toInternetAddress());
    }

    @Test
    public void getReversePathShouldReturnNullSpecialAddress() {
        Mail mail = null;
        assertThat(dsnBounce.getReversePath(mail)).isEqualTo(SpecialAddress.NULL);
    }

    @Test
    public void serviceShouldSendMultipartMailToTheSender() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        dsnBounce.init(mailetConfig);

        MailAddress senderMailAddress = new MailAddress("sender@domain.com");
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mimeMessage.setText("My content");
        FakeMail mail = FakeMail.builder()
                .sender(senderMailAddress)
                .mimeMessage(mimeMessage)
                .name(MAILET_NAME)
                .recipient(new MailAddress("recipient@domain.com"))
                .lastUpdated(new Date())
                .build();

        dsnBounce.service(mail);

        List<SentMail> sentMails = fakeMailContext.getSentMails();
        assertThat(sentMails).hasSize(1);
        SentMail sentMail = sentMails.get(0);
        assertThat(sentMail.getSender()).isNull();
        assertThat(sentMail.getRecipients()).containsOnly(senderMailAddress);
        MimeMessage sentMessage = sentMail.getMsg();
        assertThat(sentMessage.getContentType()).startsWith("multipart/report; report-type=delivery-status;");
    }

    @Test
    public void serviceShouldSendMultipartMailWithTextPart() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        dsnBounce.init(mailetConfig);

        MailAddress senderMailAddress = new MailAddress("sender@domain.com");
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mimeMessage.setText("My content");
        FakeMail mail = FakeMail.builder()
                .sender(senderMailAddress)
                .attribute("delivery-error", "Delivery error")
                .mimeMessage(mimeMessage)
                .name(MAILET_NAME)
                .recipient(new MailAddress("recipient@domain.com"))
                .lastUpdated(new Date())
                .build();

        dsnBounce.service(mail);

        List<SentMail> sentMails = fakeMailContext.getSentMails();
        assertThat(sentMails).hasSize(1);
        SentMail sentMail = sentMails.get(0);
        MimeMessage sentMessage = sentMail.getMsg();
        MimeMultipartReport content = (MimeMultipartReport) sentMessage.getContent();
        String hostname = InetAddress.getLocalHost().getHostName();
        assertThat(content.getBodyPart(0).getContent()).isEqualTo("Hi. This is the James mail server at " + hostname + ".\nI'm afraid I wasn't able to deliver your message to the following addresses.\nThis is a permanent error; I've given up. Sorry it didn't work out.  Below\nI include the list of recipients and the reason why I was unable to deliver\nyour message.\n\n" +
                "Failed recipient(s):\n" + 
                "recipient@domain.com\n" +
                "\n" +
                "Error message:\n" +
                "Delivery error\n" + 
                "\n");
    }

    @Test
    public void serviceShouldSendMultipartMailWithTextPartWhenCustomMessageIsConfigured() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("messageString", "My custom message\n");
        dsnBounce.init(mailetConfig);

        MailAddress senderMailAddress = new MailAddress("sender@domain.com");
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mimeMessage.setText("My content");
        FakeMail mail = FakeMail.builder()
                .sender(senderMailAddress)
                .attribute("delivery-error", "Delivery error")
                .mimeMessage(mimeMessage)
                .name(MAILET_NAME)
                .recipient(new MailAddress("recipient@domain.com"))
                .lastUpdated(new Date())
                .build();

        dsnBounce.service(mail);

        List<SentMail> sentMails = fakeMailContext.getSentMails();
        assertThat(sentMails).hasSize(1);
        SentMail sentMail = sentMails.get(0);
        MimeMessage sentMessage = sentMail.getMsg();
        MimeMultipartReport content = (MimeMultipartReport) sentMessage.getContent();
        assertThat(content.getBodyPart(0).getContent()).isEqualTo("My custom message\n\n" +
                "Failed recipient(s):\n" + 
                "recipient@domain.com\n" +
                "\n" +
                "Error message:\n" +
                "Delivery error\n" + 
                "\n");
    }

    @Test
    public void serviceShouldSendMultipartMailWithDSNPart() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        dsnBounce.init(mailetConfig);

        MailAddress senderMailAddress = new MailAddress("sender@domain.com");
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mimeMessage.setText("My content");
        Date lastUpdated = new Date();
        FakeMail mail = FakeMail.builder()
                .sender(senderMailAddress)
                .attribute("delivery-error", "Delivery error")
                .mimeMessage(mimeMessage)
                .name(MAILET_NAME)
                .recipient(new MailAddress("recipient@domain.com"))
                .lastUpdated(lastUpdated)
                .remoteAddr("remoteHost")
                .build();

        dsnBounce.service(mail);

        List<SentMail> sentMails = fakeMailContext.getSentMails();
        assertThat(sentMails).hasSize(1);
        SentMail sentMail = sentMails.get(0);
        MimeMessage sentMessage = sentMail.getMsg();
        MimeMultipartReport content = (MimeMultipartReport) sentMessage.getContent();
        assertThat(content.getBodyPart(1).getContent()).isEqualTo("Reporting-MTA: dns; My host\n" +
                "Received-From-MTA: dns; 111.222.333.444\n" +
                "\n" +
                "Final-Recipient: rfc822; recipient@domain.com\n" +
                "Action: failed\n" +
                "Status: Delivery error\n" +
                "Diagnostic-Code: X-James; Delivery error\n" +
                "Last-Attempt-Date: " + new RFC822DateFormat().format(lastUpdated) + "\n");
    }

    @Test
    public void serviceShouldUpdateTheMailStateWhenNoSenderAndPassThroughIsFalse() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("passThrough", "false");
        dsnBounce.init(mailetConfig);

        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mimeMessage.setText("My content");
        Date lastUpdated = new Date();
        FakeMail mail = FakeMail.builder()
                .attribute("delivery-error", "Delivery error")
                .mimeMessage(mimeMessage)
                .name(MAILET_NAME)
                .recipient(new MailAddress("recipient@domain.com"))
                .lastUpdated(lastUpdated)
                .remoteAddr("remoteHost")
                .build();

        dsnBounce.service(mail);

        assertThat(mail.getState()).isEqualTo(Mail.GHOST);
    }

    @Test
    public void serviceShouldNotUpdateTheMailStateWhenNoSenderPassThroughHasDefaultValue() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        dsnBounce.init(mailetConfig);

        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mimeMessage.setText("My content");
        Date lastUpdated = new Date();
        FakeMail mail = FakeMail.builder()
                .attribute("delivery-error", "Delivery error")
                .mimeMessage(mimeMessage)
                .name(MAILET_NAME)
                .recipient(new MailAddress("recipient@domain.com"))
                .lastUpdated(lastUpdated)
                .remoteAddr("remoteHost")
                .build();

        dsnBounce.service(mail);

        assertThat(mail.getState()).isNull();
    }

    @Test
    public void serviceShouldNotUpdateTheMailStateWhenNoSenderPassThroughIsTrue() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("passThrough", "true");
        dsnBounce.init(mailetConfig);

        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mimeMessage.setText("My content");
        Date lastUpdated = new Date();
        FakeMail mail = FakeMail.builder()
                .attribute("delivery-error", "Delivery error")
                .mimeMessage(mimeMessage)
                .name(MAILET_NAME)
                .recipient(new MailAddress("recipient@domain.com"))
                .lastUpdated(lastUpdated)
                .remoteAddr("remoteHost")
                .build();

        dsnBounce.service(mail);

        assertThat(mail.getState()).isNull();
    }

    @Test
    public void serviceShouldNotAttachedTheOriginalMailWhenAttachmentIsEqualToNone() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("attachment", "none");
        dsnBounce.init(mailetConfig);

        MailAddress senderMailAddress = new MailAddress("sender@domain.com");
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mimeMessage.setText("My content");
        FakeMail mail = FakeMail.builder()
                .sender(senderMailAddress)
                .mimeMessage(mimeMessage)
                .name(MAILET_NAME)
                .recipient(new MailAddress("recipient@domain.com"))
                .lastUpdated(new Date())
                .build();

        dsnBounce.service(mail);

        List<SentMail> sentMails = fakeMailContext.getSentMails();
        assertThat(sentMails).hasSize(1);
        SentMail sentMail = sentMails.get(0);
        assertThat(sentMail.getSender()).isNull();
        assertThat(sentMail.getRecipients()).containsOnly(senderMailAddress);
        MimeMessage sentMessage = sentMail.getMsg();
        MimeMultipartReport content = (MimeMultipartReport) sentMessage.getContent();

        // Should fail to retrieve the original mail part
        expectedException.expect(ArrayIndexOutOfBoundsException.class);
        content.getBodyPart(2);
    }

    @Test
    public void serviceShouldAttachedTheOriginalMailWhenAttachmentIsEqualToAll() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("attachment", "all");
        dsnBounce.init(mailetConfig);

        MailAddress senderMailAddress = new MailAddress("sender@domain.com");
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mimeMessage.setText("My content");
        FakeMail mail = FakeMail.builder()
                .sender(senderMailAddress)
                .mimeMessage(mimeMessage)
                .name(MAILET_NAME)
                .recipient(new MailAddress("recipient@domain.com"))
                .lastUpdated(new Date())
                .build();

        dsnBounce.service(mail);

        List<SentMail> sentMails = fakeMailContext.getSentMails();
        assertThat(sentMails).hasSize(1);
        SentMail sentMail = sentMails.get(0);
        assertThat(sentMail.getSender()).isNull();
        assertThat(sentMail.getRecipients()).containsOnly(senderMailAddress);
        MimeMessage sentMessage = sentMail.getMsg();
        MimeMultipartReport content = (MimeMultipartReport) sentMessage.getContent();
        assertThat(content.getBodyPart(2).getContent()).isEqualTo(mimeMessage);
    }

    @Test
    public void serviceShouldAttachedTheOriginalMailHeadersOnlyWhenAttachmentIsEqualToHeads() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("attachment", "heads");
        dsnBounce.init(mailetConfig);

        MailAddress senderMailAddress = new MailAddress("sender@domain.com");
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mimeMessage.setText("My content");
        mimeMessage.setHeader("myHeader", "myValue");
        mimeMessage.setSubject("mySubject");
        FakeMail mail = FakeMail.builder()
                .sender(senderMailAddress)
                .mimeMessage(mimeMessage)
                .name(MAILET_NAME)
                .recipient(new MailAddress("recipient@domain.com"))
                .lastUpdated(new Date())
                .build();

        dsnBounce.service(mail);

        List<SentMail> sentMails = fakeMailContext.getSentMails();
        assertThat(sentMails).hasSize(1);
        SentMail sentMail = sentMails.get(0);
        assertThat(sentMail.getSender()).isNull();
        assertThat(sentMail.getRecipients()).containsOnly(senderMailAddress);
        MimeMessage sentMessage = sentMail.getMsg();
        MimeMultipartReport content = (MimeMultipartReport) sentMessage.getContent();
        BodyPart bodyPart = content.getBodyPart(2);
        assertThat(bodyPart.getContent()).isEqualTo("Subject: mySubject\r\n" +
                "myHeader: myValue\r\n");
        assertThat(bodyPart.getContentType()).isEqualTo("text/rfc822-headers; name=mySubject");
    }
}
