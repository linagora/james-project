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
package org.apache.james.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.PerRecipientHeaders;
import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.MailUtil;
import org.apache.mailet.base.test.MimeMessageBuilder;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

public class MailImplTest {

    private static final Session NO_SESSION = null;

    @Test
    public void constructor1ShouldBuildEmailWithExpectedFields() throws MessagingException {
        MailImpl mail = new MailImpl();

        helperTestInitialState(mail);
        assertThat(mail.getMessageSize()).isEqualTo(0);

        assertThat(mail.getMessage()).as("Checking there is no initial message").isNull();
        assertThat(mail.getSender()).as("Checking there is an initial sender").isNull();
        assertThat(mail.getName()).as("Checking there is an initial name").isNull();
    }

    @Test
    public void constructor2ShouldBuildEmailWithExpectedFields() throws MessagingException {
        ArrayList<MailAddress> recipients = new ArrayList<MailAddress>();
        String name = MailUtil.newId();
        String sender = "sender@localhost";
        MailAddress senderMailAddress = new MailAddress(sender);
        MailImpl mail = new MailImpl(name, senderMailAddress, recipients);

        helperTestInitialState(mail); // MimeMessageWrapper default is 0
        assertThat(mail.getMessageSize()).isEqualTo(0);
        assertThat(mail.getMessage()).as("No initial message when not specified").isNull();
        assertThat(mail.getSender().asString()).as("Sender should match original value").isEqualTo(sender);
        assertThat(mail.getName()).as("Name should be the specified one").isEqualTo(name);
    }

    @Test
    public void setMessageShouldAddAMessage() throws MessagingException {
        ArrayList<MailAddress> recipients = new ArrayList<MailAddress>();
        String name = MailUtil.newId();
        String sender = "sender@localhost";
        MailAddress senderMailAddress = new MailAddress(sender);
        MailImpl mail = new MailImpl(name, senderMailAddress, recipients);

        mail.setMessage(new MimeMessage(NO_SESSION));

        assertThat(mail.getMessage()).as("Message should not be null when specified").isNotNull();
    }

    @Test
    public void constructor3ShouldBuildEmailWithExpectedFields() throws MessagingException {
        ArrayList<MailAddress> recipients = new ArrayList<MailAddress>();
        String name = MailUtil.newId();
        String sender = "sender@localhost";
        MailAddress senderMailAddress = new MailAddress(sender);
        MimeMessage mimeMessage = new MimeMessage(NO_SESSION, new ByteArrayInputStream(new byte[0]));
        MailImpl mail = new MailImpl(name, senderMailAddress, recipients, mimeMessage);

        helperTestInitialState(mail);
        assertThat(mail.getMessageSize()).isEqualTo(0);
        assertThat(mail.getMessage().getMessageID()).isEqualTo(mimeMessage.getMessageID());
        assertThat(mail.getSender().asString()).isEqualTo(sender);
        assertThat(mail.getName()).isEqualTo(name);
        mail.dispose();
    }

    @Test
    public void duplicateShouldCreateANewJavaObject() throws MessagingException {
        MailImpl mail = new MailImpl();
        MailImpl duplicate = (MailImpl) mail.duplicate();

        assertThat(mail).as("duplicate method should return different objects").isNotSameAs(duplicate);

        helperTestInitialState(duplicate);
        assertThat(mail.getMessageSize()).isEqualTo(0);
    }

    @Test
    public void duplicateShouldNotChangeOriginalMailName() throws MessagingException {
        String newName = "aNewName";
        MailImpl mail = new MailImpl();

        mail.duplicate(newName);

        assertThat(mail.getName()).as("new name should be set").isNotEqualTo(newName);
    }

    @Test
    public void hasAttributesShouldBeFalseByDefault() {
        Mail mail = new MailImpl();

        assertThat(mail.hasAttributes()).isFalse();
    }

    @Test
    public void getAttributeNamesShouldBeEmptyByDefault() {
        Mail mail = new MailImpl();

        assertThat(mail.getAttributeNames()).isEmpty();
    }

    @Test
    public void getAttributeShouldReturnNullWhenAttributeAbsent() {
        Mail mail = new MailImpl();

        assertThat(mail.getAttribute("test")).isNull();
    }

    @Test
    public void setAttributeShouldReturnNullWhenNoPreviousValue() {
        Mail mail = new MailImpl();

        assertThat(mail.setAttribute("test", "value")).isNull();
    }

    @Test
    public void getAttributeShouldReturnStoredValue() {
        Mail mail = new MailImpl();

        String value = "value";
        String key = "test";
        mail.setAttribute(key, value);

        assertThat(mail.getAttribute(key)).isEqualTo(value);
    }

    @Test
    public void setAttributeShouldReturnPreviouslyStoredValue() {
        Mail mail = new MailImpl();

        String value = "value";
        String key = "test";
        mail.setAttribute(key, value);

        assertThat(mail.setAttribute(key, "newValue")).isEqualTo(value);
    }

    @Test
    public void removeAttributeShouldReturnNullWhenAbsent() {
        Mail mail = new MailImpl();

        assertThat(mail.removeAttribute("test")).isNull();
    }

    @Test
    public void removeAttributeShouldReturnPreviouslyStoredValue() {
        Mail mail = new MailImpl();

        String value = "value";
        String key = "test";
        mail.setAttribute(key, value);

        assertThat(mail.removeAttribute(key)).isEqualTo(value);
    }

    @Test
    public void hasAttributesShouldReturnTrueWhenHasAttribute() {
        Mail mail = new MailImpl();

        String value = "value";
        String key = "test";
        mail.setAttribute(key, value);

        assertThat(mail.hasAttributes()).isTrue();
    }

    @Test
    public void getAttributeNamesShouldReturnStoredAttributesName() {
        Mail mail = new MailImpl();

        String value = "value";
        String key = "test";
        mail.setAttribute(key, value);

        assertThat(mail.getAttributeNames()).containsExactly(key);
    }

    @Test
    public void removeAttributeShouldRemoveSpecificAttribute() {
        Mail mail = new MailImpl();

        String value = "value";
        String key1 = "test1";
        String key2 = "test2";
        mail.setAttribute(key1, value);
        mail.setAttribute(key2, value);
        mail.removeAttribute(key1);

        assertThat(mail.getAttributeNames()).containsExactly(key2);
    }

    @Test
    public void removeAllAttributesShouldClearMailAttributes() {
        Mail mail = new MailImpl();

        String value = "value";
        String key1 = "test1";
        String key2 = "test2";
        mail.setAttribute(key1, value);
        mail.setAttribute(key2, value);
        mail.removeAllAttributes();

        assertThat(mail.getAttributeNames()).isEmpty();
    }

    protected void helperTestInitialState(Mail mail) {
        assertThat(mail.hasAttributes()).as("Expecting no initial attributes").isFalse();
        assertThat(mail.getErrorMessage()).as("Expecting no initial error").isNull();
        assertThat(mail.getLastUpdated()).as("Expecting initial last update set").isNotNull();
        assertThat(mail.getRecipients()).isEmpty();
        assertThat(mail.getRemoteAddr()).as("initial remote address should be localhost ip").isEqualTo("127.0.0.1");
        assertThat(mail.getRemoteHost()).as("initial remote host should be localhost").isEqualTo("localhost");
        assertThat(mail.getState()).as("Expecting default initial state").isEqualTo(Mail.DEFAULT);
    }

    @Test
    public void mailImplShouldBeSerializable() throws Exception {
        MailAddress sender = MailAddressFixture.ANY_AT_JAMES;
        ImmutableList<MailAddress> recipients = ImmutableList.of(MailAddressFixture.ANY_AT_LOCAL);
        MimeMessage message = MimeMessageBuilder.mimeMessageFromStream(
            new ByteArrayInputStream("subject: default mail\r\n\r\nAwesome body"
                .getBytes(Charsets.UTF_8)));
        MailImpl initialMail = new MailImpl("name", sender, recipients, message);
        initialMail.setRecipients(ImmutableList.of(MailAddressFixture.ANY_AT_JAMES));
        initialMail.addSpecificHeaderForRecipient(
            PerRecipientHeaders.Header.builder()
                .name("name")
                .value("value")
                .build(),
            MailAddressFixture.ANY_AT_JAMES);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new ObjectOutputStream(outputStream).writeObject(initialMail);
        MailImpl readObject = (MailImpl) new ObjectInputStream(new ByteArrayInputStream(outputStream.toByteArray())).readObject();

        assertThat(getMessageAsString(readObject.getMessage()))
            .isEqualTo(getMessageAsString(message));
    }

    private String getMessageAsString(MimeMessage message) throws IOException, MessagingException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        message.writeTo(os);
        return IOUtils.toString(new ByteArrayInputStream(os.toByteArray()), Charsets.UTF_8);
    }

    @Test
    public void mailImplShouldDeduplicateRecipientsInEnvelope() throws Exception {
        MailImpl mail = new MailImpl();
        mail.setRecipients(ImmutableList.of(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.ANY_AT_JAMES));

        assertThat(mail.getRecipients()).containsExactly(MailAddressFixture.ANY_AT_JAMES);
    }

}
