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

import static org.apache.james.jmap.api.filtering.Rule.Condition.Comparator.CONTAINS;
import static org.apache.james.jmap.api.filtering.Rule.Condition.Comparator.EXACTLY_EQUALS;
import static org.apache.james.jmap.api.filtering.Rule.Condition.Comparator.NOT_CONTAINS;
import static org.apache.james.jmap.api.filtering.Rule.Condition.Comparator.NOT_EXACTLY_EQUALS;
import static org.apache.james.jmap.api.filtering.Rule.Condition.Field.CC;
import static org.apache.james.jmap.api.filtering.Rule.Condition.Field.FROM;
import static org.apache.james.jmap.api.filtering.Rule.Condition.Field.RECIPIENT;
import static org.apache.james.jmap.api.filtering.Rule.Condition.Field.SUBJECT;
import static org.apache.james.jmap.api.filtering.Rule.Condition.Field.TO;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.mail.Address;
import javax.mail.Message;

import org.apache.commons.lang3.StringUtils;
import org.apache.james.javax.AddressHelper;
import org.apache.james.jmap.api.filtering.Rule;
import org.apache.james.jmap.api.filtering.Rule.Condition.Field;
import org.apache.mailet.Mail;

import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.functions.ThrowingFunction;
import com.google.common.collect.ImmutableMap;

public interface MailMatcher {

    interface HeaderExtractor extends ThrowingFunction<Mail, Stream<String>> {}

    class HeaderMatcher implements MailMatcher {
        private final ContentMatcher contentMatcher;
        private final String ruleValue;
        private final HeaderExtractor headerExtractor;

        HeaderMatcher(ContentMatcher contentMatcher, String ruleValue, HeaderExtractor headerExtractor) {
            this.contentMatcher = contentMatcher;
            this.ruleValue = ruleValue;
            this.headerExtractor = headerExtractor;
        }

        @Override
        public boolean match(Mail mail) {
            return Throwing.function(headerExtractor)
                .apply(mail)
                .anyMatch(headerLine -> contentMatcher.match(headerLine, ruleValue));
        }
    }

    interface ContentMatcher {
        ContentMatcher CONTAINS_MATCHER = StringUtils::contains;
        ContentMatcher NOT_CONTAINS_MATCHER = negate(StringUtils::contains);
        ContentMatcher EXACTLY_EQUALS_MATCHER = StringUtils::equals;
        ContentMatcher NOT_EXACTLY_EQUALS_MATCHER = negate(StringUtils::equals);

        Map<Rule.Condition.Comparator, ContentMatcher> MAPPER = ImmutableMap.<Rule.Condition.Comparator, ContentMatcher>builder()
                .put(CONTAINS, CONTAINS_MATCHER)
                .put(NOT_CONTAINS, NOT_CONTAINS_MATCHER)
                .put(EXACTLY_EQUALS, EXACTLY_EQUALS_MATCHER)
                .put(NOT_EXACTLY_EQUALS, NOT_EXACTLY_EQUALS_MATCHER)
                .build();

        static ContentMatcher negate(ContentMatcher contentMatcher) {
            return (String fieldContent, String filterRuleValue) ->
                    !contentMatcher.match(fieldContent, filterRuleValue);
        }

        static ContentMatcher getContentMatcher(Rule.Condition.Comparator comparator) {
            return Optional
                .ofNullable(MAPPER.get(comparator))
                .orElseThrow(() -> new IllegalArgumentException("unexpected comparator " + comparator.asString()));
        }

        boolean match(String fieldContent, String filterRuleValue);
    }

    HeaderExtractor SUBJECT_EXTRACTOR = mail ->
            Optional.ofNullable(mail.getMessage().getSubject())
                    .map(Stream::of)
                    .orElse(Stream.empty());
    HeaderExtractor RECIPIENT_EXTRACTOR =  mail -> addressExtractor(mail.getMessage().getAllRecipients());
    HeaderExtractor FROM_EXTRACTOR = mail -> addressExtractor(mail.getMessage().getFrom());
    HeaderExtractor CC_EXTRACTOR = recipientExtractor(Message.RecipientType.CC);
    HeaderExtractor TO_EXTRACTOR = recipientExtractor(Message.RecipientType.TO);

    Map<Field, HeaderExtractor> MAPPER = ImmutableMap.<Field, HeaderExtractor>builder()
            .put(SUBJECT, SUBJECT_EXTRACTOR)
            .put(RECIPIENT, RECIPIENT_EXTRACTOR)
            .put(FROM, FROM_EXTRACTOR)
            .put(CC, CC_EXTRACTOR)
            .put(TO, TO_EXTRACTOR)
            .build();

    static MailMatcher from(Rule rule) {
        ContentMatcher contentMatcher = ContentMatcher.getContentMatcher(rule.getCondition().getComparator());
        HeaderExtractor headerExtractor = getHeaderExtractor(rule.getCondition().getField());

        return new HeaderMatcher(contentMatcher, rule.getCondition().getValue(), headerExtractor);
    }

    static HeaderExtractor recipientExtractor(Message.RecipientType type) {
        return mail -> addressExtractor(mail.getMessage().getRecipients(type));
    }

    static Stream<String> addressExtractor(Address[] addresses) {
        return Optional.ofNullable(addresses)
                .map(AddressHelper::asStringStream)
                .orElse(Stream.empty());
    }

    static HeaderExtractor getHeaderExtractor(Field field) {
        return Optional
            .ofNullable(MAPPER.get(field))
            .orElseThrow(() -> new IllegalArgumentException("unexpected field " + field.asString()));
    }

    boolean match(Mail mail);
}
