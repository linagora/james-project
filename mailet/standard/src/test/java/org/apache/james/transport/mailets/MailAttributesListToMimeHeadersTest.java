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

import javax.mail.MessagingException;

import org.apache.mailet.Mailet;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.apache.mailet.base.test.MailUtil;
import org.apache.mailet.base.test.MimeMessageBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class MailAttributesListToMimeHeadersTest {

    @Rule public ExpectedException expectedException = ExpectedException.none();

    private Mailet mailet;

    private final String HEADER_NAME1 = "JUNIT";
    private final String HEADER_NAME2 = "JUNIT2";

    private final ImmutableList<String> MAIL_ATTRIBUTE_VALUE1 = ImmutableList.of("test1.1", "test1.2");
    private final ImmutableList<String> MAIL_ATTRIBUTE_VALUE2 = ImmutableList.of("test2.1", "test2.2");

    private final String[] MAIL_ATTRIBUTE_VALUE1_AS_ARRAY = MAIL_ATTRIBUTE_VALUE1.toArray(new String[MAIL_ATTRIBUTE_VALUE1.size()]);
    private final String[] MAIL_ATTRIBUTE_VALUE2_AS_ARRAY = MAIL_ATTRIBUTE_VALUE2.toArray(new String[MAIL_ATTRIBUTE_VALUE1.size()]);

    private final String MAIL_ATTRIBUTE_NAME1 = "org.apache.james.test";
    private final String MAIL_ATTRIBUTE_NAME2 = "org.apache.james.test2";

    @Before
    public void setup() {
        mailet = new MailAttributesListToMimeHeaders();
    }

    @Test
    public void shouldThrowMessagingExceptionIfMappingIsNotGiven() throws MessagingException {
        expectedException.expect(MessagingException.class);
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
            .mailetName("Test")
            .build();

        mailet.init(mailetConfig);
    }

    @Test
    public void shouldThrowMessagingExceptionIfMappingIsEmpty() throws MessagingException {
        expectedException.expect(MessagingException.class);
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty("simplemmapping", "")
            .build();

        mailet.init(mailetConfig);
    }

    @Test
    public void shouldIgnoreAttributeOfMappingThatDoesNotExistOnTheMessage() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty("simplemapping",
                MAIL_ATTRIBUTE_NAME1 + "; " + HEADER_NAME1 +
                    "," + MAIL_ATTRIBUTE_NAME2 + "; " + HEADER_NAME2 +
                    "," + "another.attribute" + "; " + "Another-Header")
            .build();

        mailet.init(mailetConfig);

        FakeMail mockedMail = MailUtil.createMockMail2Recipients(MailUtil.createMimeMessage());
        mockedMail.setAttribute(MAIL_ATTRIBUTE_NAME1, MAIL_ATTRIBUTE_VALUE1);
        mockedMail.setAttribute(MAIL_ATTRIBUTE_NAME2, MAIL_ATTRIBUTE_VALUE2);

        mailet.service(mockedMail);
        assertThat(mockedMail.getMessage().getHeader("another.attribute")).isNull();
    }

    @Test
    public void shouldWorkWithMappingWithASingleBinding() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty("simplemapping",
                MAIL_ATTRIBUTE_NAME1 + "; " + HEADER_NAME1)
            .build();

        mailet.init(mailetConfig);

        FakeMail mockedMail = MailUtil.createMockMail2Recipients(MailUtil.createMimeMessage());
        mockedMail.setAttribute(MAIL_ATTRIBUTE_NAME1, MAIL_ATTRIBUTE_VALUE1);

        mailet.service(mockedMail);

        assertThat(mockedMail.getMessage().getHeader(HEADER_NAME1))
            .containsExactly(MAIL_ATTRIBUTE_VALUE1_AS_ARRAY);
    }

    @Test
    public void shouldIgnoreNullValueInsideList() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty("simplemapping",
                MAIL_ATTRIBUTE_NAME1 + "; " + HEADER_NAME1)
            .build();

        mailet.init(mailetConfig);

        FakeMail mockedMail = MailUtil.createMockMail2Recipients(MailUtil.createMimeMessage());

        ArrayList<String> listWithNull = new ArrayList<String>();
        listWithNull.add("1");
        listWithNull.add(null);
        listWithNull.add("2");
        mockedMail.setAttribute(MAIL_ATTRIBUTE_NAME1, listWithNull);

        mailet.service(mockedMail);

        assertThat(mockedMail.getMessage().getHeader(HEADER_NAME1))
            .containsExactly("1", "2");
    }

    @Test
    public void shouldPutAttributesIntoHeadersWhenMappingDefined() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("simplemapping",
                        MAIL_ATTRIBUTE_NAME1 + "; " + HEADER_NAME1 +
                        "," + MAIL_ATTRIBUTE_NAME2 + "; " + HEADER_NAME2 +
                        "," + "another.attribute" + "; " + "Another-Header")
                .build();
        mailet.init(mailetConfig);

        FakeMail mockedMail = MailUtil.createMockMail2Recipients(MailUtil.createMimeMessage());
        mockedMail.setAttribute(MAIL_ATTRIBUTE_NAME1, MAIL_ATTRIBUTE_VALUE1);
        mockedMail.setAttribute(MAIL_ATTRIBUTE_NAME2, MAIL_ATTRIBUTE_VALUE2);
        mockedMail.setAttribute("unmatched.attribute", "value");

        mailet.service(mockedMail);

        assertThat(mockedMail.getMessage().getHeader(HEADER_NAME1))
            .containsExactly(MAIL_ATTRIBUTE_VALUE1_AS_ARRAY);

        assertThat(mockedMail.getMessage().getHeader(HEADER_NAME2))
            .containsExactly(MAIL_ATTRIBUTE_VALUE2_AS_ARRAY);
    }

    @Test
    public void shouldNotRemovePreviousAttributeValueWhenAttributeAlreadyPresent() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("simplemapping", MAIL_ATTRIBUTE_NAME1 + "; " + HEADER_NAME1)
                .build();
        mailet.init(mailetConfig);

        FakeMail mockedMail = MailUtil.createMockMail2Recipients(MimeMessageBuilder.mimeMessageBuilder()
            .addHeader(HEADER_NAME1, "first value")
            .build());
        mockedMail.setAttribute(MAIL_ATTRIBUTE_NAME1, MAIL_ATTRIBUTE_VALUE1);

        mailet.service(mockedMail);

        List<String> expectedHeaderValues = ImmutableList
            .<String>builder()
            .addAll(MAIL_ATTRIBUTE_VALUE1)
            .add("first value" )
            .build();

        assertThat(mockedMail.getMessage().getHeader(HEADER_NAME1))
            .containsOnly(expectedHeaderValues.toArray(new String[expectedHeaderValues.size()]));
    }

    @Test
    public void shouldThrowAtInitWhenNoSemicolumnInConfigurationEntry() throws MessagingException {
        expectedException.expect(MessagingException.class);

        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("simplemapping", "invalidConfigEntry")
                .build();

        mailet.init(mailetConfig);
    }

    @Test
    public void shouldThrowAtInitWhenTwoSemicolumnsInConfigurationEntry() throws MessagingException {
        expectedException.expect(MessagingException.class);

        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("simplemapping", "first;second;third")
                .build();

        mailet.init(mailetConfig);
    }

    @Test
    public void shouldThrowAtInitWhenNoConfigurationEntry() throws MessagingException {
        expectedException.expect(MessagingException.class);

        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .build();

        mailet.init(mailetConfig);
    }
}
