package org.apache.james.transport.mailets;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.core.MailAddress;
import org.apache.james.transport.mailets.ContactExtractor;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.MimeMessageBuilder;
import org.junit.Test;

public class ContactExtractorTest {

    @Test
    public void buildMessageShouldGenerateJsonPayload() throws Exception {
        FakeMail mail = FakeMail.builder().mimeMessage(MimeMessageBuilder.defaultMimeMessage())
            .sender(new MailAddress("matthieu@op.lng.com"))
            .recipient(new MailAddress("michael@op.lng.com"))
            .build();
        assertThat(new ContactExtractor().buildMessage(mail).get()).isEqualToIgnoringWhitespace("{\"userEmail\" : \"matthieu@op.lng.com\", \"emails\" : [ \"michael@op.lng.com\" ]}");

    }


}