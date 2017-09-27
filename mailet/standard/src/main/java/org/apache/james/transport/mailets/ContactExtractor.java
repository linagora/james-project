package org.apache.james.transport.mailets;

import static org.apache.james.transport.mailets.ContactExtractor.Configuration.extractAttributeTo;

import java.util.Optional;

import javax.mail.MessagingException;

import org.apache.james.core.MailAddress;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

public class ContactExtractor extends GenericMailet implements Mailet{

    public static class Message {
        private final String userEmail;
        private final ImmutableList<String> emails;

        public Message(ImmutableList<String> emails, String userEmail) {
            this.emails = emails;
            this.userEmail = userEmail;
        }

        public ImmutableList<String> getEmails() {
            return emails;
        }

        public String getUserEmail() {
            return userEmail;
        }
    }

    interface Configuration {
        String extractAttributeTo = "hardcodedValue";
    }

    public static Logger LOGGER = LoggerFactory.getLogger(ContactExtractor.class);

    private final ObjectMapper objectMapper;

    public ContactExtractor() {
        this.objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        try {
            LOGGER.debug("extracting recipients");
            Optional<String> payload = buildMessage(mail);
            LOGGER.debug("payload : {}", payload);
            payload.ifPresent(x -> mail.setAttribute(extractAttributeTo, x));
        } catch (JsonProcessingException e) {
            throw new MessagingException("json conversion failure", e);
        }
    }

    @VisibleForTesting
    Optional<String> buildMessage(Mail mail) throws JsonProcessingException {
        ImmutableList<String> emails = mail.getRecipients().stream().map(recipient -> recipient.asString()).collect(Guavate.toImmutableList());
        Optional<String> userEmail = Optional.ofNullable(mail.getSender()).map(MailAddress::asString);
        return userEmail.map(Throwing.function(x -> objectMapper.writeValueAsString(new Message(emails, x))));
    }
}
