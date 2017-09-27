package org.apache.james.transport.mailets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.james.jmap.mailet.VacationMailet;
import org.apache.james.mailets.TemporaryJamesServer;
import org.apache.james.mailets.configuration.CommonProcessors;
import org.apache.james.mailets.configuration.MailetConfiguration;
import org.apache.james.mailets.configuration.MailetContainer;
import org.apache.james.mailets.configuration.ProcessorConfiguration;
import org.apache.james.transport.mailets.AmqpForwardAttribute;
import org.apache.james.transport.mailets.ContactExtractor;
import org.apache.james.transport.mailets.LocalDelivery;
import org.apache.james.transport.mailets.RemoveMimeHeader;
import org.apache.james.transport.mailets.amqp.AmqpRule;
import org.apache.james.transport.matchers.All;
import org.apache.james.transport.matchers.RecipientIsLocal;
import org.apache.james.transport.matchers.SMTPAuthSuccessful;
import org.apache.james.util.streams.SwarmGenericContainer;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.IMAPMessageReader;
import org.apache.james.utils.SMTPMessageSender;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.MimeMessageBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;

public class ContactCollectionTest {


    public static final String MATTHIEU = "matthieu@op.lng.com";
    public static final String MICHAEL = "michael@op.lng.com";
    public static final String PASSWORD = "secret";
    public static final String OP_LNG_COM = "op.lng.com";
    public static final String EXCHANGE = "collector:email";
    public static final String ROUTING_KEY = "";

    public SwarmGenericContainer rabbit = new SwarmGenericContainer("rabbitmq:3");
    public AmqpRule amqpRule = new AmqpRule(rabbit, EXCHANGE, ROUTING_KEY);
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public RuleChain chain = RuleChain.outerRule(rabbit).around(amqpRule).around(folder);

    private TemporaryJamesServer jamesServer;

    @Before
    public void setup() throws Exception {
        MailetContainer mailets = MailetContainer
            .builder()
            .threads(5)
            .postmaster(MATTHIEU)
            .addProcessor(CommonProcessors.root())
            .addProcessor(CommonProcessors.error())
            .addProcessor(
                ProcessorConfiguration.builder()
                    .state("transport")
                    .addMailet(MailetConfiguration.builder()
                        .match(SMTPAuthSuccessful.class)
                        .clazz(ContactExtractor.class)
                        .build())
                    .addMailet(MailetConfiguration.builder()
                        .match(All.class)
                        .clazz(AmqpForwardAttribute.class)
                        .addProperty(AmqpForwardAttribute.URI_PARAMETER_NAME, amqpRule.getAmqpUri())
                        .addProperty(AmqpForwardAttribute.EXCHANGE_PARAMETER_NAME, EXCHANGE)
                        .addProperty(AmqpForwardAttribute.ATTRIBUTE_PARAMETER_NAME, ContactExtractor.Configuration.extractAttributeTo)
                        .build())
                    .addMailet(MailetConfiguration.builder()
                        .match(All.class)
                        .clazz(RemoveMimeHeader.class)
                        .addProperty("name", "bcc")
                        .build())
                    .addMailet(MailetConfiguration.builder()
                        .match(RecipientIsLocal.class)
                        .clazz(VacationMailet.class)
                        .build())
                    .addMailet(
                        MailetConfiguration.builder()
                        .match(RecipientIsLocal.class)
                        .clazz(LocalDelivery.class)
                        .build())
                    .build())
            .build();
        jamesServer = new TemporaryJamesServer(folder, mailets);
        DataProbeImpl probe = jamesServer.getProbe(DataProbeImpl.class);
        probe.addDomain(OP_LNG_COM);
        probe.addUser(MATTHIEU, PASSWORD);
        probe.addUser(MICHAEL, PASSWORD);
    }

    @After
    public void tearDown() {
        jamesServer.shutdown();
    }

    @Test
    public void recipientsShouldBePublishedToAmqpWhenSendingEmail() throws Exception {
        MimeMessage message = MimeMessageBuilder.mimeMessageBuilder()
            .addFrom(MATTHIEU)
            .addToRecipient(MICHAEL)
            .setSubject("Contact collection Rocks")
            .setText("This is my email")
            .build();
        FakeMail mail = FakeMail.builder()
            .mimeMessage(message)
            .sender(new MailAddress(MATTHIEU))
            .recipients(new MailAddress(MICHAEL))
            .build();
        SMTPMessageSender messageSender = SMTPMessageSender.authentication("localhost", 1025, OP_LNG_COM, MATTHIEU, PASSWORD);
        messageSender.sendMessage(mail);
        IMAPMessageReader imap = new IMAPMessageReader("localhost", 1143);
        Awaitility.await().pollDelay(Duration.FIVE_HUNDRED_MILLISECONDS)
            .atMost(Duration.ONE_MINUTE)
            .until(() -> imap.userReceivedMessage(MICHAEL, PASSWORD));
        Optional<String> actual = amqpRule.readContent();
        assertThat(actual).isNotEmpty();
        assertThat(actual.get()).isEqualToIgnoringWhitespace("{\"userEmail\" : \"matthieu@op.lng.com\", \"emails\" : [ \"michael@op.lng.com\" ]}");
    }

}
