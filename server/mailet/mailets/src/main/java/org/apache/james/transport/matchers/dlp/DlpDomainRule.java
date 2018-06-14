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

package org.apache.james.transport.matchers.dlp;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.james.dlp.api.DLPConfigurationItem;
import org.apache.james.javax.MultipartUtil;
import org.apache.james.mime4j.util.MimeUtil;
import org.apache.james.util.OptionalUtils;
import org.apache.mailet.Mail;

import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.predicates.ThrowingPredicate;

public class DlpDomainRule {
    interface MatcherFunction extends ThrowingPredicate<Mail> { }

    public static class Factory {

        public DlpDomainRule recipientRule(DLPConfigurationItem.Id id, Pattern pattern) {
            return new DlpDomainRule(id,
                (Mail mail) -> listRecipientsAsString(mail).anyMatch(pattern.asPredicate()));
        }

        private Stream<String> listRecipientsAsString(Mail mail) throws MessagingException {
            return Stream.concat(
                listEnvelopRecipients(mail),
                listHeaderRecipients(mail));
        }

        private Stream<String> listEnvelopRecipients(Mail mail) {
            return mail.getRecipients().stream().map(MailAddress::asString);
        }

        private Stream<String> listHeaderRecipients(Mail mail) throws MessagingException {
            MimeMessage message = mail.getMessage();
            if (message != null) {
                return toStringStream(message.getAllRecipients());
            }
            return Stream.of();
        }

        private Stream<String> toStringStream(Address[] allRecipients) {
            return Optional.ofNullable(allRecipients)
                .map(Arrays::stream)
                .orElse(Stream.of())
                .map(this::asString);
        }

        private String asString(Address a) {
            return MimeUtil.unscrambleHeaderValue(a.toString());
        }

        public DlpDomainRule senderRule(DLPConfigurationItem.Id id, Pattern pattern) {
            return new DlpDomainRule(id,
                (Mail mail) -> listSenders(mail).anyMatch(pattern.asPredicate()));
        }

        private Stream<String> listSenders(Mail mail) throws MessagingException {
            return Stream.concat(listEnvelopSender(mail), listFromHeaders(mail));
        }

        private Stream<String> listEnvelopSender(Mail mail) {
            return OptionalUtils.toStream(Optional.ofNullable(mail.getSender()).map(MailAddress::asString));
        }

        private Stream<String> listFromHeaders(Mail mail) throws MessagingException {
            MimeMessage message = mail.getMessage();
            if (message != null) {
                return toStringStream(message.getFrom());
            }
            return Stream.of();
        }

        public DlpDomainRule contentRule(DLPConfigurationItem.Id id, Pattern pattern) {
            return new DlpDomainRule(
                id,
                (Mail mail) -> matchSubject(pattern, mail) || matchBody(pattern, mail));
        }

        private boolean matchSubject(Pattern pattern, Mail mail) throws MessagingException {
            return getMessageSubjects(mail).anyMatch(pattern.asPredicate());
        }

        private Stream<String> getMessageSubjects(Mail mail) throws MessagingException {
            MimeMessage message = mail.getMessage();
            if (message != null) {
                return Stream.of(message.getSubject());
            }
            return Stream.of();
        }

        private boolean matchBody(Pattern pattern, Mail mail) throws MessagingException, IOException {
            return getMessageBodies(mail.getMessage()).anyMatch(pattern.asPredicate());
        }

        private Stream<String> getMessageBodies(Message message) throws MessagingException, IOException {
            if (message != null) {
                return getMessageBodiesFromContent(message.getContent());
            }
            return Stream.of();
        }

        private Stream<String> getMessageBodiesFromContent(Object content) throws IOException, MessagingException {
            if (content instanceof String) {
                return Stream.of((String) content);
            }
            if (content instanceof Message) {
                Message message = (Message) content;
                return getMessageBodiesFromContent(message.getContent());
            }
            if (content instanceof Multipart) {
                return MultipartUtil.retrieveBodyParts((Multipart) content)
                    .stream()
                    .map(Throwing.function(BodyPart::getContent).sneakyThrow())
                    .flatMap(Throwing.function(this::getMessageBodiesFromContent).sneakyThrow());
            }
            return Stream.of();
        }
    }

    public static Factory factory() {
        return new Factory();
    }

    private final DLPConfigurationItem.Id id;
    private final MatcherFunction matcher;

    public DlpDomainRule(DLPConfigurationItem.Id id, MatcherFunction matcher) {
        this.id = id;
        this.matcher = matcher;
    }

    public DLPConfigurationItem.Id id() {
        return id;
    }

    public boolean match(Mail mail) {
        return matcher.test(mail);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DlpDomainRule) {
            DlpDomainRule other = (DlpDomainRule) o;
            return Objects.equals(id, other.id) &&
                Objects.equals(matcher, other.matcher);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, matcher);
    }
}
