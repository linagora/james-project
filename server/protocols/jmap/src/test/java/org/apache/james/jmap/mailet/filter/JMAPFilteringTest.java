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

package org.apache.james.jmap.mailet.filter;

import static org.apache.james.core.builder.MimeMessageBuilder.mimeMessageBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.james.core.User;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.james.jmap.api.filtering.Rule;
import org.apache.james.jmap.mailet.filter.JMAPFilteringExtension.JMAPFilteringTestSystem;
import org.apache.james.mailbox.inmemory.InMemoryMailboxManager;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.mailet.base.test.FakeMail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;

@ExtendWith(JMAPFilteringExtension.class)
class JMAPFilteringTest {

    private static final String DELIVERY_PATH_PREFIX = "DeliveryPath_";

    private static final String SENDER_1_FULL_ADDRESS = "sender1 <sender1@james.org>";
    private static final String SENDER_1_ADDRESS = "sender1@james.org";
    private static final String SENDER_1_USERNAME = "sender1";

    private static final String SENDER_2_FULL_ADDRESS = "sender2 <sender2@james.org>";
    private static final String SENDER_2_ADDRESS = "sender2@james.org";
    private static final String SENDER_2_USERNAME = "sender2";

    private static final String SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS = "sender1 <sender1@james.org>, \r\nunfolded\r\n_user\r\n <unfolded_user@james.org>";

    private static final String RECIPIENT_TO_1_FULL_ADDRESS = "recipient_to_1 <recipient_to_1@james.org>";
    private static final String RECIPIENT_TO_1_ADDRESS = "recipient_to_1@james.org";
    private static final String RECIPIENT_TO_1_USERNAME = "recipient_to_1";

    private static final String RECIPIENT_TO_2_FULL_ADDRESS = "recipient_to_2 <recipient_to_2@james.org>";
    private static final String RECIPIENT_TO_2_ADDRESS = "recipient_to_2@james.org";
    private static final String RECIPIENT_TO_2_USERNAME = "recipient_to_2";

    private static final String RECIPIENT_CC_1_FULL_ADDRESS = "recipient_cc_1 <recipient_cc_1@james.org>";
    private static final String RECIPIENT_CC_1_ADDRESS = "recipient_cc_1@james.org";
    private static final String RECIPIENT_CC_1_USERNAME = "recipient_cc_1";

    private static final String RECIPIENT_CC_2_FULL_ADDRESS = "recipient_cc_2 <recipient_cc_2@james.org>";

    private static final String SCRAMBLED_SUBJECT = "this is the subject =?UTF-8?B?RnLDqWTDqXJpYyBNQVJUSU4=?= of the mail";
    private static final String UNSCRAMBLED_SUBJECT = "this is the subject Frédéric MARTIN of the mail";
    private static final String SHOULD_NOT_MATCH = "should not match";

    private static final String RECIPIENT_TO_3_FULL_ADDRESS = "recipient_to_3 <recipient_to_3@james.org>";
    private static final String RECIPIENT_TO_3_USERNAME = "recipient_to_3";
    private static final String RECIPIENT_TO_4_FULL_ADDRESS = "recipient_to_4 <recipient_to_4@james.org>";

    private static final String RECIPIENT_1 = "recipient1@james.org";
    static final String RECIPIENT_1_USERNAME = "recipient1";
    static final String RECIPIENT_1_MAILBOX_1 = "recipient1_maibox1";

    private static final String FRED_MARTIN_FULLNAME = "Frédéric MARTIN";
    private static final String FRED_MARTIN_FULL_SCRAMBLED_ADDRESS = "=?UTF-8?B?RnLDqWTDqXJpYyBNQVJUSU4=?= <fred.martin@linagora.com>";
    private static final String FRED_MARTIN_FULL_ADDRESS = "Frédéric MARTIN <fred.martin@linagora.com>";

    private static final String UNFOLDED_USERNAME = "unfolded_user";

    private static final String SUFFIX = "suffix";

    static Stream<Arguments> mailDirectiveShouldSetWhenContainsRuleValueParamsProvider() throws Exception {
        return Stream.of(
            Arguments.of("normal content", Rule.Condition.Field.FROM, mimeMessageBuilder().addFrom(SENDER_1_FULL_ADDRESS), SENDER_1_USERNAME),
            Arguments.of("multiple headers", Rule.Condition.Field.FROM, mimeMessageBuilder().addFrom(SENDER_1_FULL_ADDRESS).addFrom(SENDER_2_FULL_ADDRESS), SENDER_2_USERNAME),
            Arguments.of("scrambled content", Rule.Condition.Field.FROM, mimeMessageBuilder().addHeader("from", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), FRED_MARTIN_FULLNAME),
            Arguments.of("folded content", Rule.Condition.Field.FROM, mimeMessageBuilder().addHeader("from", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), UNFOLDED_USERNAME),

            Arguments.of("normal content", Rule.Condition.Field.TO, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS), RECIPIENT_TO_1_USERNAME),
            Arguments.of("multiple headers", Rule.Condition.Field.TO, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS).addToRecipient(RECIPIENT_TO_2_FULL_ADDRESS), RECIPIENT_TO_2_USERNAME),
            Arguments.of("scrambled content", Rule.Condition.Field.TO, mimeMessageBuilder().addHeader("to", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), FRED_MARTIN_FULLNAME),
            Arguments.of("folded content", Rule.Condition.Field.TO, mimeMessageBuilder().addHeader("to", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), UNFOLDED_USERNAME),

            Arguments.of("normal content", Rule.Condition.Field.CC, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS), "recipient_cc_1"),
            Arguments.of("multiple headers", Rule.Condition.Field.CC, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS).addCcRecipient(RECIPIENT_CC_2_FULL_ADDRESS), "recipient_cc_2"),
            Arguments.of("scrambled content", Rule.Condition.Field.CC, mimeMessageBuilder().addHeader("cc", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), FRED_MARTIN_FULLNAME),
            Arguments.of("folded content", Rule.Condition.Field.CC, mimeMessageBuilder().addHeader("cc", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), UNFOLDED_USERNAME),

            Arguments.of("normal content", Rule.Condition.Field.SUBJECT, mimeMessageBuilder().setSubject(SCRAMBLED_SUBJECT), "subject"),
            Arguments.of("scrambled content", Rule.Condition.Field.SUBJECT, mimeMessageBuilder().setSubject(UNSCRAMBLED_SUBJECT), "subject Frédéric MARTIN")
        );
    }

    @ParameterizedTest(name = "mailDirectiveShouldSetWhenContainsRuleValue when matching header field {1}, with {0}")
    @MethodSource("mailDirectiveShouldSetWhenContainsRuleValueParamsProvider")
    void mailDirectiveShouldSetWhenContainsRuleValue(String testDescription,
                                                     Rule.Condition.Field fieldToMatch,
                                                     MimeMessageBuilder mimeMessageBuilder,
                                                     String valueToMatch,
                                                     JMAPFilteringTestSystem testSystem) throws Exception {

        testSystem.defineRuleForRecipient1(fieldToMatch, Rule.Condition.Comparator.CONTAINS, valueToMatch);

        FakeMail mail = FakeMail.builder()
                .sender(SENDER_1_ADDRESS)
                .recipients(RECIPIENT_1)
                .mimeMessage(mimeMessageBuilder)
                .build();

        testSystem.getJmapFiltering().service(mail);

        assertThat(mail.getAttribute(DELIVERY_PATH_PREFIX + RECIPIENT_1_USERNAME))
                .isEqualTo(RECIPIENT_1_MAILBOX_1);
    }

    @Test
    void mailDirectiveShouldNotBeSetWhenNoneRulesValueIsContained(JMAPFilteringTestSystem testSystem) throws Exception {

        testSystem.defineRulesForRecipient1(
            Rule.Condition.of(Rule.Condition.Field.FROM, Rule.Condition.Comparator.CONTAINS, SHOULD_NOT_MATCH),
            Rule.Condition.of(Rule.Condition.Field.TO, Rule.Condition.Comparator.CONTAINS, SHOULD_NOT_MATCH),
            Rule.Condition.of(Rule.Condition.Field.CC, Rule.Condition.Comparator.CONTAINS, SHOULD_NOT_MATCH));

        FakeMail mail = FakeMail.builder()
                .sender(SENDER_1_ADDRESS)
                .recipients(RECIPIENT_1)
                .mimeMessage(mimeMessageBuilder()
                    .addFrom(SENDER_1_FULL_ADDRESS)
                    .addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS)
                    .addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS))
                .build();

        testSystem.getJmapFiltering().service(mail);

        assertThat(mail.getAttribute(DELIVERY_PATH_PREFIX + RECIPIENT_1_USERNAME))
                .isNull();
    }

    static Stream<Arguments> mailDirectiveShouldBeSetWhenDoesntContainsRuleValueParamsProvider() throws Exception {
        return Stream.of(
            Arguments.of("normal content", Rule.Condition.Field.FROM, mimeMessageBuilder().addFrom(SENDER_1_FULL_ADDRESS), SENDER_2_USERNAME),
            Arguments.of("multiple headers", Rule.Condition.Field.FROM, mimeMessageBuilder().addFrom(SENDER_1_FULL_ADDRESS).addFrom(SENDER_2_FULL_ADDRESS), RECIPIENT_TO_1_USERNAME),
            Arguments.of("scrambled content", Rule.Condition.Field.FROM, mimeMessageBuilder().addHeader("from", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), SENDER_2_USERNAME),
            Arguments.of("folded content", Rule.Condition.Field.FROM, mimeMessageBuilder().addHeader("from", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), SENDER_2_USERNAME),

            Arguments.of("normal content", Rule.Condition.Field.TO, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS), SENDER_1_USERNAME),
            Arguments.of("multiple headers", Rule.Condition.Field.TO, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS).addToRecipient(RECIPIENT_TO_2_FULL_ADDRESS), SENDER_1_USERNAME),
            Arguments.of("scrambled content", Rule.Condition.Field.TO, mimeMessageBuilder().addHeader("to", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), SENDER_1_USERNAME),
            Arguments.of("folded content", Rule.Condition.Field.TO, mimeMessageBuilder().addHeader("to", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), SENDER_2_USERNAME),

            Arguments.of("normal content", Rule.Condition.Field.CC, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS), RECIPIENT_TO_1_USERNAME),
            Arguments.of("multiple headers", Rule.Condition.Field.CC, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS).addToRecipient(RECIPIENT_CC_2_FULL_ADDRESS), RECIPIENT_TO_1_USERNAME),
            Arguments.of("scrambled content", Rule.Condition.Field.CC, mimeMessageBuilder().addHeader("cc", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), RECIPIENT_TO_1_USERNAME),
            Arguments.of("folded content", Rule.Condition.Field.CC, mimeMessageBuilder().addHeader("cc", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), SENDER_2_USERNAME),

            Arguments.of("normal content", Rule.Condition.Field.SUBJECT, mimeMessageBuilder().setSubject(SCRAMBLED_SUBJECT), SHOULD_NOT_MATCH),
            Arguments.of("scrambled content", Rule.Condition.Field.SUBJECT, mimeMessageBuilder().setSubject(UNSCRAMBLED_SUBJECT), SHOULD_NOT_MATCH)
        );
    }

    @ParameterizedTest(name = "mailDirectiveShouldBeSetWhenDoesntContainsRuleValue when matching header field {1}, with {0}")
    @MethodSource("mailDirectiveShouldBeSetWhenDoesntContainsRuleValueParamsProvider")
    void mailDirectiveShouldBeSetWhenDoesntContainsRuleValue(String testDescription,
                                                             Rule.Condition.Field fieldToMatch,
                                                             MimeMessageBuilder mimeMessageBuilder,
                                                             String valueToMatch,
                                                             JMAPFilteringTestSystem testSystem) throws Exception {
        testSystem.defineRuleForRecipient1(fieldToMatch, Rule.Condition.Comparator.NOT_CONTAINS, valueToMatch);

        FakeMail mail = FakeMail.builder()
                .sender(SENDER_2_ADDRESS)
                .recipients(RECIPIENT_1)
                .mimeMessage(mimeMessageBuilder)
                .build();

        testSystem.getJmapFiltering().service(mail);

        assertThat(mail.getAttribute(DELIVERY_PATH_PREFIX + RECIPIENT_1_USERNAME))
                .isEqualTo(RECIPIENT_1_MAILBOX_1);
    }

    static Stream<Arguments> mailDirectiveShouldNotBeSetWhenAtleastOneContentContainsRuleValueParamsProvider() throws Exception {
        return Stream.of(
            Arguments.of("one match", Rule.Condition.Field.FROM, mimeMessageBuilder()
                    .addFrom(SENDER_1_FULL_ADDRESS)
                    .addFrom("sender2 <sender1@james.org>"), SENDER_2_USERNAME),
            Arguments.of("two matches", Rule.Condition.Field.TO, mimeMessageBuilder()
                    .addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS)
                    .addToRecipient(RECIPIENT_TO_2_FULL_ADDRESS)
                    .addToRecipient(RECIPIENT_TO_3_FULL_ADDRESS), RECIPIENT_TO_3_USERNAME),
            Arguments.of("all matches", Rule.Condition.Field.CC, mimeMessageBuilder()
                    .addCcRecipient(RECIPIENT_TO_1_FULL_ADDRESS)
                    .addCcRecipient(RECIPIENT_TO_2_FULL_ADDRESS)
                    .addCcRecipient(RECIPIENT_TO_3_FULL_ADDRESS)
                    .addCcRecipient(RECIPIENT_TO_4_FULL_ADDRESS), "recipient_to")
        );
    }

    @ParameterizedTest(name = "mailDirectiveShouldNotBeSetWhenAtleastOneContentContainsRuleValue when {0}")
    @MethodSource("mailDirectiveShouldNotBeSetWhenAtleastOneContentContainsRuleValueParamsProvider")
    void mailDirectiveShouldNotBeSetWhenAtleastOneContentContainsRuleValue(
            String testDescription,
            Rule.Condition.Field fieldToMatch,
            MimeMessageBuilder mimeMessageBuilder,
            String valueToMatch,
            JMAPFilteringTestSystem testSystem) throws Exception {

        testSystem.defineRuleForRecipient1(fieldToMatch, Rule.Condition.Comparator.NOT_CONTAINS, valueToMatch);

        FakeMail mail = FakeMail.builder()
                .sender(SENDER_2_ADDRESS)
                .recipients(RECIPIENT_1)
                .mimeMessage(mimeMessageBuilder)
                .build();

        testSystem.getJmapFiltering().service(mail);

        assertThat(mail.getAttribute(DELIVERY_PATH_PREFIX + RECIPIENT_1_USERNAME))
                .isNull();
    }


    static Stream<Arguments> mailDirectiveShouldBeSetWhenExactlyEqualsRuleValueParamsProvider() throws Exception {
        return Stream.of(
            Arguments.of("full address header", Rule.Condition.Field.FROM, mimeMessageBuilder().addFrom(SENDER_1_FULL_ADDRESS), SENDER_1_FULL_ADDRESS),
            Arguments.of("address only header", Rule.Condition.Field.FROM, mimeMessageBuilder().addFrom(SENDER_1_FULL_ADDRESS), SENDER_1_USERNAME),
            Arguments.of("personal only header", Rule.Condition.Field.FROM, mimeMessageBuilder().addFrom(SENDER_1_FULL_ADDRESS), SENDER_1_ADDRESS),
            Arguments.of("multiple headers", Rule.Condition.Field.FROM, mimeMessageBuilder().addFrom(SENDER_1_FULL_ADDRESS).addFrom(SENDER_2_FULL_ADDRESS), SENDER_2_USERNAME),
            Arguments.of("scrambled content", Rule.Condition.Field.FROM, mimeMessageBuilder().addHeader("from", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), FRED_MARTIN_FULL_ADDRESS),
            Arguments.of("folded content", Rule.Condition.Field.FROM, mimeMessageBuilder().addHeader("from", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), UNFOLDED_USERNAME),

            Arguments.of("full address header", Rule.Condition.Field.TO, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS), RECIPIENT_TO_1_FULL_ADDRESS),
            Arguments.of("address only header", Rule.Condition.Field.TO, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS), RECIPIENT_TO_1_ADDRESS),
            Arguments.of("personal only header", Rule.Condition.Field.TO, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS), RECIPIENT_TO_1_USERNAME),
            Arguments.of("multiple headers", Rule.Condition.Field.TO, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS).addToRecipient(RECIPIENT_TO_2_FULL_ADDRESS), RECIPIENT_TO_1_FULL_ADDRESS),
            Arguments.of("scrambled content", Rule.Condition.Field.TO, mimeMessageBuilder().addHeader("to", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), FRED_MARTIN_FULL_ADDRESS),
            Arguments.of("folded content", Rule.Condition.Field.TO, mimeMessageBuilder().addHeader("to", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), UNFOLDED_USERNAME),

            Arguments.of("full address header", Rule.Condition.Field.CC, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS), RECIPIENT_CC_1_FULL_ADDRESS),
            Arguments.of("address only header", Rule.Condition.Field.CC, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS), RECIPIENT_CC_1_ADDRESS),
            Arguments.of("personal only header", Rule.Condition.Field.CC, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS), RECIPIENT_CC_1_USERNAME),
            Arguments.of("multiple headers", Rule.Condition.Field.CC, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS).addCcRecipient(RECIPIENT_CC_2_FULL_ADDRESS), RECIPIENT_CC_1_FULL_ADDRESS),
            Arguments.of("scrambled content", Rule.Condition.Field.CC, mimeMessageBuilder().addHeader("cc", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), FRED_MARTIN_FULL_ADDRESS),
            Arguments.of("folded content", Rule.Condition.Field.CC, mimeMessageBuilder().addHeader("cc", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), UNFOLDED_USERNAME),

            Arguments.of("full address to header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS), RECIPIENT_TO_1_FULL_ADDRESS),
            Arguments.of("address only to header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS), RECIPIENT_TO_1_ADDRESS),
            Arguments.of("personal only to header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS), RECIPIENT_TO_1_USERNAME),
            Arguments.of("scrambled content in to header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addHeader("to", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), FRED_MARTIN_FULL_ADDRESS),
            Arguments.of("folded content in to header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addHeader("to", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), UNFOLDED_USERNAME),

            Arguments.of("full address cc header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS), RECIPIENT_CC_1_FULL_ADDRESS),
            Arguments.of("address only cc header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS), RECIPIENT_CC_1_ADDRESS),
            Arguments.of("personal only cc header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS), RECIPIENT_CC_1_USERNAME),
            Arguments.of("scrambled content in cc header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addHeader("cc", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), FRED_MARTIN_FULL_ADDRESS),
            Arguments.of("folded content in cc header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addHeader("cc", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), UNFOLDED_USERNAME),

            Arguments.of("multiple to and cc headers", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder()
                    .addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS)
                    .addCcRecipient(RECIPIENT_TO_1_FULL_ADDRESS)
                    .addCcRecipient(RECIPIENT_CC_2_FULL_ADDRESS)
                    .addCcRecipient(RECIPIENT_TO_2_FULL_ADDRESS), RECIPIENT_CC_1_FULL_ADDRESS),

            Arguments.of("scrambled subject", Rule.Condition.Field.SUBJECT, mimeMessageBuilder().setSubject(SCRAMBLED_SUBJECT), UNSCRAMBLED_SUBJECT),
            Arguments.of("unscrambled subject", Rule.Condition.Field.SUBJECT, mimeMessageBuilder().setSubject(UNSCRAMBLED_SUBJECT), UNSCRAMBLED_SUBJECT)
        );
    }

    @ParameterizedTest(name = "mailDirectiveShouldBeSetWhenExactlyEqualsRuleValue when matching header field {1}, with {0}")
    @MethodSource("mailDirectiveShouldBeSetWhenExactlyEqualsRuleValueParamsProvider")
    void mailDirectiveShouldBeSetWhenExactlyEqualsRuleValue(
            String testDescription,
            Rule.Condition.Field fieldToMatch,
            MimeMessageBuilder mimeMessageBuilder,
            String valueToMatch,
            JMAPFilteringTestSystem testSystem) throws Exception {

        testSystem.defineRuleForRecipient1(fieldToMatch, Rule.Condition.Comparator.EXACTLY_EQUALS, valueToMatch);

        FakeMail mail = FakeMail.builder()
                .sender(SENDER_2_ADDRESS)
                .recipients(RECIPIENT_1)
                .mimeMessage(mimeMessageBuilder)
                .build();

        testSystem.getJmapFiltering().service(mail);

        assertThat(mail.getAttribute(DELIVERY_PATH_PREFIX + RECIPIENT_1_USERNAME))
                .isEqualTo(RECIPIENT_1_MAILBOX_1);
    }

    @Test
    void mailDirectiveShouldNotBeSetWhenAllDoNotExactlyEqualsRuleValue(JMAPFilteringTestSystem testSystem) throws Exception {

        testSystem.defineRulesForRecipient1(
            Rule.Condition.of(Rule.Condition.Field.FROM, Rule.Condition.Comparator.CONTAINS, SENDER_1_FULL_ADDRESS),
            Rule.Condition.of(Rule.Condition.Field.FROM, Rule.Condition.Comparator.EXACTLY_EQUALS, SENDER_1_FULL_ADDRESS),
            Rule.Condition.of(Rule.Condition.Field.TO, Rule.Condition.Comparator.CONTAINS, RECIPIENT_TO_1_ADDRESS),
            Rule.Condition.of(Rule.Condition.Field.TO, Rule.Condition.Comparator.EXACTLY_EQUALS, RECIPIENT_TO_1_ADDRESS),
            Rule.Condition.of(Rule.Condition.Field.CC, Rule.Condition.Comparator.CONTAINS, RECIPIENT_CC_1_ADDRESS),
            Rule.Condition.of(Rule.Condition.Field.CC, Rule.Condition.Comparator.EXACTLY_EQUALS, RECIPIENT_CC_1_ADDRESS),
            Rule.Condition.of(Rule.Condition.Field.RECIPIENT, Rule.Condition.Comparator.EXACTLY_EQUALS, RECIPIENT_TO_1_ADDRESS),
            Rule.Condition.of(Rule.Condition.Field.RECIPIENT, Rule.Condition.Comparator.EXACTLY_EQUALS, RECIPIENT_CC_1_ADDRESS),
            Rule.Condition.of(Rule.Condition.Field.SUBJECT, Rule.Condition.Comparator.CONTAINS, SCRAMBLED_SUBJECT),
            Rule.Condition.of(Rule.Condition.Field.SUBJECT, Rule.Condition.Comparator.EXACTLY_EQUALS, SCRAMBLED_SUBJECT)
        );

        FakeMail mail = FakeMail.builder()
                .sender(SENDER_2_ADDRESS)
                .recipients(RECIPIENT_1)
                .mimeMessage(mimeMessageBuilder())
                .build();

        testSystem.getJmapFiltering().service(mail);

        assertThat(mail.getAttribute(DELIVERY_PATH_PREFIX + RECIPIENT_1_USERNAME))
                .isNull();
    }

    static Stream<Arguments> mailDirectiveShouldBeSetWhenNotExactlyEqualsRuleValueParamsProvider() throws Exception {
        return Stream.of(
            Arguments.of("full address header", Rule.Condition.Field.FROM, mimeMessageBuilder().addFrom(SENDER_1_FULL_ADDRESS), SENDER_1_FULL_ADDRESS + SUFFIX),
            Arguments.of("address only header", Rule.Condition.Field.FROM, mimeMessageBuilder().addFrom(SENDER_1_FULL_ADDRESS), SENDER_1_USERNAME + SUFFIX),
            Arguments.of("personal only header", Rule.Condition.Field.FROM, mimeMessageBuilder().addFrom(SENDER_1_FULL_ADDRESS), SENDER_1_ADDRESS + SUFFIX),
            Arguments.of("multiple headers", Rule.Condition.Field.FROM, mimeMessageBuilder().addFrom(SENDER_1_FULL_ADDRESS).addFrom(SENDER_2_FULL_ADDRESS), SENDER_2_USERNAME + SUFFIX),
            Arguments.of("scrambled content", Rule.Condition.Field.FROM, mimeMessageBuilder().addHeader("from", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), FRED_MARTIN_FULL_ADDRESS + SUFFIX),
            Arguments.of("folded content", Rule.Condition.Field.FROM, mimeMessageBuilder().addHeader("from", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), UNFOLDED_USERNAME + SUFFIX),

            Arguments.of("full address header", Rule.Condition.Field.TO, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS), RECIPIENT_TO_1_FULL_ADDRESS + SUFFIX),
            Arguments.of("address only header", Rule.Condition.Field.TO, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS), RECIPIENT_TO_1_ADDRESS + SUFFIX),
            Arguments.of("personal only header", Rule.Condition.Field.TO, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS), RECIPIENT_TO_1_USERNAME + SUFFIX),
            Arguments.of("multiple headers", Rule.Condition.Field.TO, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS).addToRecipient(RECIPIENT_TO_2_FULL_ADDRESS), RECIPIENT_TO_1_FULL_ADDRESS + SUFFIX),
            Arguments.of("scrambled content", Rule.Condition.Field.TO, mimeMessageBuilder().addHeader("to", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), FRED_MARTIN_FULL_ADDRESS + SUFFIX),
            Arguments.of("folded content", Rule.Condition.Field.TO, mimeMessageBuilder().addHeader("to", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), UNFOLDED_USERNAME + SUFFIX),

            Arguments.of("full address header", Rule.Condition.Field.CC, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS), RECIPIENT_CC_1_FULL_ADDRESS + SUFFIX),
            Arguments.of("address only header", Rule.Condition.Field.CC, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS), RECIPIENT_CC_1_ADDRESS + SUFFIX),
            Arguments.of("personal only header", Rule.Condition.Field.CC, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS), RECIPIENT_CC_1_USERNAME + SUFFIX),
            Arguments.of("multiple headers", Rule.Condition.Field.CC, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS).addCcRecipient(RECIPIENT_CC_2_FULL_ADDRESS), RECIPIENT_CC_1_FULL_ADDRESS + SUFFIX),
            Arguments.of("scrambled content", Rule.Condition.Field.CC, mimeMessageBuilder().addHeader("cc", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), FRED_MARTIN_FULL_ADDRESS + SUFFIX),
            Arguments.of("folded content", Rule.Condition.Field.CC, mimeMessageBuilder().addHeader("cc", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), UNFOLDED_USERNAME + SUFFIX),

            Arguments.of("full address to header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS), RECIPIENT_TO_1_FULL_ADDRESS + SUFFIX),
            Arguments.of("address only to header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS), RECIPIENT_TO_1_ADDRESS + SUFFIX),
            Arguments.of("personal only to header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS), RECIPIENT_TO_1_USERNAME + SUFFIX),
            Arguments.of("scrambled content in to header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addHeader("to", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), FRED_MARTIN_FULL_ADDRESS + SUFFIX),
            Arguments.of("folded content in to header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addHeader("to", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), UNFOLDED_USERNAME + SUFFIX),

            Arguments.of("full address cc header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS), RECIPIENT_CC_1_FULL_ADDRESS + SUFFIX),
            Arguments.of("address only cc header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS), RECIPIENT_CC_1_ADDRESS + SUFFIX),
            Arguments.of("personal only cc header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS), RECIPIENT_CC_1_USERNAME + SUFFIX),
            Arguments.of("scrambled content in cc header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addHeader("cc", FRED_MARTIN_FULL_SCRAMBLED_ADDRESS), FRED_MARTIN_FULL_ADDRESS + SUFFIX),
            Arguments.of("folded content in cc header", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder().addHeader("cc", SENDER_1_AND_UNFOLDED_USER_FULL_ADDRESS), UNFOLDED_USERNAME + SUFFIX),

            Arguments.of("multiple to and cc headers", Rule.Condition.Field.RECIPIENT, mimeMessageBuilder()
                    .addCcRecipient(RECIPIENT_CC_1_FULL_ADDRESS)
                    .addCcRecipient(RECIPIENT_TO_1_FULL_ADDRESS)
                    .addCcRecipient(RECIPIENT_CC_2_FULL_ADDRESS)
                    .addCcRecipient(RECIPIENT_TO_2_FULL_ADDRESS), RECIPIENT_CC_1_FULL_ADDRESS + SUFFIX),

            Arguments.of("scrambled subject", Rule.Condition.Field.SUBJECT, mimeMessageBuilder().setSubject(SCRAMBLED_SUBJECT), SCRAMBLED_SUBJECT + SUFFIX),
            Arguments.of("unscrambled subject", Rule.Condition.Field.SUBJECT, mimeMessageBuilder().setSubject(UNSCRAMBLED_SUBJECT), SCRAMBLED_SUBJECT + SUFFIX)
        );
    }

    @ParameterizedTest(name = "mailDirectiveShouldBeSetWhenNotExactlyEqualsRuleValue when matching header field {1}, with {0}")
    @MethodSource("mailDirectiveShouldBeSetWhenNotExactlyEqualsRuleValueParamsProvider")
    void mailDirectiveShouldBeSetWhenNotExactlyEqualsRuleValue(
            String testDescription,
            Rule.Condition.Field fieldToMatch,
            MimeMessageBuilder mimeMessageBuilder,
            String valueToMatch,
            JMAPFilteringTestSystem testSystem) throws Exception {

        testSystem.defineRuleForRecipient1(fieldToMatch, Rule.Condition.Comparator.NOT_EXACTLY_EQUALS, valueToMatch);

        FakeMail mail = FakeMail.builder()
                .sender(SENDER_2_ADDRESS)
                .recipients(RECIPIENT_1)
                .mimeMessage(mimeMessageBuilder)
                .build();

        testSystem.getJmapFiltering().service(mail);

        assertThat(mail.getAttribute(DELIVERY_PATH_PREFIX + RECIPIENT_1_USERNAME))
                .isEqualTo(RECIPIENT_1_MAILBOX_1);
    }


    static Stream<Arguments> mailDirectiveShouldNotBeSetWhenAtLeastExactlyEqualsRuleValueParamsProvider() throws Exception {
        return Stream.of(
            Arguments.of("one match", Rule.Condition.Field.FROM, mimeMessageBuilder()
                    .addFrom(SENDER_1_FULL_ADDRESS)
                    .addFrom("sender2 <sender1@james.org>"), SENDER_2_USERNAME),
            Arguments.of("multiple matches", Rule.Condition.Field.TO, mimeMessageBuilder()
                    .addToRecipient(RECIPIENT_TO_1_FULL_ADDRESS)
                    .addToRecipient(RECIPIENT_TO_2_FULL_ADDRESS)
                    .addToRecipient(RECIPIENT_TO_3_FULL_ADDRESS), RECIPIENT_TO_3_USERNAME));
    }

    @ParameterizedTest(name = "mailDirectiveShouldNotBeSetWhenAtLeastExactlyEqualsRuleValue when {0}")
    @MethodSource("mailDirectiveShouldNotBeSetWhenAtLeastExactlyEqualsRuleValueParamsProvider")
    void mailDirectiveShouldNotBeSetWhenAtLeastExactlyEqualsRuleValue(
            String testDescription,
            Rule.Condition.Field fieldToMatch,
            MimeMessageBuilder mimeMessageBuilder,
            String valueToMatch,
            JMAPFilteringTestSystem testSystem) throws Exception {
        testSystem.defineRuleForRecipient1(fieldToMatch, Rule.Condition.Comparator.NOT_EXACTLY_EQUALS, valueToMatch);

        FakeMail mail = FakeMail.builder()
                .sender(SENDER_2_ADDRESS)
                .recipients(RECIPIENT_1)
                .mimeMessage(mimeMessageBuilder)
                .build();

        testSystem.getJmapFiltering().service(mail);

        assertThat(mail.getAttribute(DELIVERY_PATH_PREFIX + RECIPIENT_1_USERNAME))
                .isNull();
    }


    static Stream<Arguments> mailDirectiveShouldIgnoreBccHeadersParamsProvider() {
        return Stream.of(
            Arguments.of(Rule.Condition.Comparator.CONTAINS),
            Arguments.of(Rule.Condition.Comparator.EXACTLY_EQUALS));
    }

    @ParameterizedTest(name = "mailDirectiveShouldIgnoreBccHeaders with comparator {0}")
    @MethodSource("mailDirectiveShouldIgnoreBccHeadersParamsProvider")
    void mailDirectiveShouldIgnoreBccHeaders(
            Rule.Condition.Comparator comparator,
            JMAPFilteringTestSystem testSystem) throws Exception {

        String valueToMatch = RECIPIENT_TO_1_FULL_ADDRESS;
        testSystem.defineRuleForRecipient1(Rule.Condition.Field.RECIPIENT, comparator, valueToMatch);

        FakeMail mail = FakeMail.builder()
                .sender(SENDER_2_ADDRESS)
                .recipients(RECIPIENT_1)
                .mimeMessage(mimeMessageBuilder().addBccRecipient(RECIPIENT_TO_1_FULL_ADDRESS))
                .build();

        testSystem.getJmapFiltering().service(mail);

        assertThat(mail.getAttribute(DELIVERY_PATH_PREFIX + RECIPIENT_1_USERNAME))
                .isNull();
    }

    static Stream<Arguments> mailDirectiveShouldNotBeSetWhenHeaderContentIsNullParamsProvider() {
        return Arrays.stream(Rule.Condition.Field.values())
            .flatMap(field -> Stream.of(
                 Arguments.of(field, Rule.Condition.Comparator.CONTAINS),
                 Arguments.of(field, Rule.Condition.Comparator.EXACTLY_EQUALS)));
    }

    @ParameterizedTest(name = "mailDirectiveShouldNotBeSetWhenHeaderContentIsNull when matching header field {1}, with {0}")
    @MethodSource("mailDirectiveShouldNotBeSetWhenHeaderContentIsNullParamsProvider")
    void mailDirectiveShouldNotBeSetWhenHeaderContentIsNull(
            Rule.Condition.Field fieldToMatch,
            Rule.Condition.Comparator comparator,
            JMAPFilteringTestSystem testSystem) throws Exception {

        String valueToMatch = SENDER_1_USERNAME;
        testSystem.defineRuleForRecipient1(fieldToMatch, comparator, valueToMatch);

        FakeMail mail = FakeMail.builder()
                .sender(SENDER_1_ADDRESS)
                .recipients(RECIPIENT_1)
                .mimeMessage(mimeMessageBuilder())
                .build();

        testSystem.getJmapFiltering().service(mail);

        assertThat(mail.getAttribute(DELIVERY_PATH_PREFIX + RECIPIENT_1_USERNAME))
                .isNull();
    }

    static Stream<Arguments> mailDirectiveShouldBeSetWhenHeaderContentIsNullParamsProvider() {
        return Arrays.stream(Rule.Condition.Field.values())
            .flatMap(field -> Stream.of(
                Arguments.of(field, Rule.Condition.Comparator.NOT_CONTAINS),
                Arguments.of(field, Rule.Condition.Comparator.NOT_EXACTLY_EQUALS)));
    }

    @ParameterizedTest(name = "mailDirectiveShouldBeSetWhenHeaderContentIsNull when matching header field {1}, with {0}")
    @MethodSource("mailDirectiveShouldBeSetWhenHeaderContentIsNullParamsProvider")
    void mailDirectiveShouldBeSetWhenHeaderContentIsNull(
            Rule.Condition.Field fieldToMatch,
            Rule.Condition.Comparator comparator,
            JMAPFilteringTestSystem testSystem) throws Exception {

        String valueToMatch = RECIPIENT_TO_1_USERNAME;
        testSystem.defineRuleForRecipient1(fieldToMatch, comparator, valueToMatch);

        FakeMail mail = FakeMail.builder()
                .sender(SENDER_1_ADDRESS)
                .recipients(RECIPIENT_1)
                .mimeMessage(mimeMessageBuilder())
                .build();

        testSystem.getJmapFiltering().service(mail);

        assertThat(mail.getAttribute(DELIVERY_PATH_PREFIX + RECIPIENT_1_USERNAME))
                .isEqualTo(RECIPIENT_1_MAILBOX_1);
    }

    @Test
    void mailDirectiveShouldSetFirstMatchedRuleWhenMultipleRules(JMAPFilteringTestSystem testSystem) throws Exception {
        InMemoryMailboxManager mailboxManager = testSystem.getMailboxManager();
        MailboxId mailbox1Id = testSystem.createMailbox(mailboxManager, JMAPFilteringTest.RECIPIENT_1_USERNAME, "RECIPIENT_1_MAILBOX_1");
        MailboxId mailbox2Id = testSystem.createMailbox(mailboxManager, JMAPFilteringTest.RECIPIENT_1_USERNAME, "RECIPIENT_1_MAILBOX_2");
        MailboxId mailbox3Id = testSystem.createMailbox(mailboxManager, JMAPFilteringTest.RECIPIENT_1_USERNAME, "RECIPIENT_1_MAILBOX_3");

        testSystem.defineRulesForRecipient1();
        testSystem.getFilteringManagement().defineRulesForUser(User.fromUsername(RECIPIENT_1_USERNAME),
                ImmutableList.of(
                    Rule.builder()
                        .id(Rule.Id.of("1"))
                        .name("rule 1")
                        .condition(Rule.Condition.of(Rule.Condition.Field.SUBJECT, Rule.Condition.Comparator.CONTAINS, UNSCRAMBLED_SUBJECT))
                        .action(Rule.Action.of(Rule.Action.AppendInMailboxes.withMailboxIds(mailbox1Id.serialize())))
                        .build(),
                    Rule.builder()
                        .id(Rule.Id.of("2"))
                        .name("rule 2")
                        .condition(Rule.Condition.of(Rule.Condition.Field.FROM, Rule.Condition.Comparator.NOT_CONTAINS, SENDER_1_USERNAME))
                        .action(Rule.Action.of(Rule.Action.AppendInMailboxes.withMailboxIds(mailbox2Id.serialize())))
                        .build(),
                    Rule.builder()
                        .id(Rule.Id.of("3"))
                        .name("rule 3")
                        .condition(Rule.Condition.of(Rule.Condition.Field.TO, Rule.Condition.Comparator.EXACTLY_EQUALS, RECIPIENT_TO_1_ADDRESS))
                        .action(Rule.Action.of(Rule.Action.AppendInMailboxes.withMailboxIds(mailbox3Id.serialize())))
                        .build()));

        FakeMail mail = FakeMail.builder()
                .sender(SENDER_1_ADDRESS)
                .recipients(RECIPIENT_1)
                .mimeMessage(mimeMessageBuilder()
                    .addFrom(SENDER_2_ADDRESS)
                    .addToRecipient(RECIPIENT_TO_1_ADDRESS)
                    .setSubject(UNSCRAMBLED_SUBJECT))
                .build();

        testSystem.getJmapFiltering().service(mail);

        assertThat(mail.getAttribute(DELIVERY_PATH_PREFIX + RECIPIENT_1_USERNAME))
                .isEqualTo("RECIPIENT_1_MAILBOX_1");
    }

    @Test
    void mailDirectiveShouldSetFirstMatchedMailboxWhenMultipleMailboxes(JMAPFilteringTestSystem testSystem) throws Exception {
        InMemoryMailboxManager mailboxManager = testSystem.getMailboxManager();
        MailboxId mailbox1Id = testSystem.createMailbox(mailboxManager, JMAPFilteringTest.RECIPIENT_1_USERNAME, "RECIPIENT_1_MAILBOX_1");
        MailboxId mailbox2Id = testSystem.createMailbox(mailboxManager, JMAPFilteringTest.RECIPIENT_1_USERNAME, "RECIPIENT_1_MAILBOX_2");
        MailboxId mailbox3Id = testSystem.createMailbox(mailboxManager, JMAPFilteringTest.RECIPIENT_1_USERNAME, "RECIPIENT_1_MAILBOX_3");

        testSystem.getFilteringManagement().defineRulesForUser(User.fromUsername(RECIPIENT_1_USERNAME),
                ImmutableList.of(
                    Rule.builder()
                        .id(Rule.Id.of("1"))
                        .name("rule 1")
                        .condition(Rule.Condition.of(Rule.Condition.Field.SUBJECT, Rule.Condition.Comparator.CONTAINS, UNSCRAMBLED_SUBJECT))
                        .action(Rule.Action.of(Rule.Action.AppendInMailboxes.withMailboxIds(ImmutableList.of(
                                mailbox3Id.serialize(),
                                mailbox2Id.serialize(),
                                mailbox1Id.serialize()))))
                        .build()));

        FakeMail mail = FakeMail.builder()
                .sender(SENDER_1_ADDRESS)
                .recipients(RECIPIENT_1)
                .mimeMessage(mimeMessageBuilder()
                    .setSubject(UNSCRAMBLED_SUBJECT))
                .build();

        testSystem.getJmapFiltering().service(mail);

        assertThat(mail.getAttribute(DELIVERY_PATH_PREFIX + RECIPIENT_1_USERNAME))
                .isEqualTo("RECIPIENT_1_MAILBOX_3");
    }
}