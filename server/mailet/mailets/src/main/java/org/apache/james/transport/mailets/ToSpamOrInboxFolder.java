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

import static org.apache.james.transport.mailets.LocalDelivery.LOCAL_DELIVERED_MAILS_METRIC_NAME;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;

import org.apache.james.core.MailAddress;
import org.apache.james.mailbox.DefaultMailboxes;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.server.core.MailImpl;
import org.apache.james.transport.mailets.delivery.MailDispatcher;
import org.apache.james.transport.mailets.delivery.MailboxAppender;
import org.apache.james.transport.mailets.delivery.SimpleMailStore;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.util.scanner.SpamAssassinResult;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;

import com.google.common.collect.ImmutableList;

/**
 * Receives a Mail from the Queue and takes care to deliver the message
 * to a spam folder or INBOX of the recipient(s) depending on the per recipient headers.
 * 
 * You may define the Spam folder name of the recipient(s).
 * The mail will be consumed if all recipients are delivered in the Spam folder.
 * 
 * <pre>
 * &lt;mailet match="RecipientIsLocal" class="ToSpamOrInboxFolder"&gt;
 *    &lt;folder&gt; <i>Junk</i> &lt;/folder&gt;
 * &lt;/mailet&gt;
 * </pre>
 * 
 */
public class ToSpamOrInboxFolder extends GenericMailet {

    private static final String YES = "yes";

    public static final String FOLDER_PARAMETER = "folder";
    public static final String CONSUME_PARAMETER = "consume";

    private final MailboxManager mailboxManager;
    private final UsersRepository usersRepository;
    private final MetricFactory metricFactory;
    private MailDispatcher mailDispatcher;

    @Inject
    public ToSpamOrInboxFolder(@Named("mailboxmanager")MailboxManager mailboxManager, UsersRepository usersRepository,
                             MetricFactory metricFactory) {
        this.metricFactory = metricFactory;
        this.mailboxManager = mailboxManager;
        this.usersRepository = usersRepository;
    }

    @Override
    public String getMailetInfo() {
        return ToSpamOrInboxFolder.class.getSimpleName() + " Mailet";
    }

    @Override
    public void init() throws MessagingException {
        mailDispatcher = MailDispatcher.builder()
            .mailStore(SimpleMailStore.builder()
                .mailboxAppender(new MailboxAppender(mailboxManager))
                .usersRepository(usersRepository)
                .folder(getInitParameter(FOLDER_PARAMETER, DefaultMailboxes.SPAM))
                .metric(metricFactory.generate(LOCAL_DELIVERED_MAILS_METRIC_NAME))
                .build())
            .mailetContext(getMailetContext())
            .build();
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        ImmutableList.Builder<MailAddress> toInboxRecipientsBuilder = ImmutableList.builder();
        for (MailAddress recipient : mail.getRecipients()) {
            if (isMarkedAsSpam(mail, recipient)) {
                sendInSpam(mail, recipient);
            } else {
                toInboxRecipientsBuilder.add(recipient);
            }
        }
        
        consumeOrSendInInbox(mail, toInboxRecipientsBuilder.build());
    }

    private void consumeOrSendInInbox(Mail mail, List<MailAddress> toInboxRecipients) {
        if (toInboxRecipients.isEmpty()) {
            mail.setState(Mail.GHOST);
        } else {
            mail.setRecipients(toInboxRecipients);
        }
    }

    private void sendInSpam(Mail mail, MailAddress recipient) throws MessagingException {
        MailImpl spamMail = MailImpl.duplicate(mail);
        spamMail.setRecipients(ImmutableList.of(recipient));
        mailDispatcher.dispatch(spamMail);
    }

    private boolean isMarkedAsSpam(Mail mail, MailAddress recipient) {
        return mail.getPerRecipientSpecificHeaders().getHeadersForRecipient(recipient)
            .stream()
            .filter(header -> header.getName().equals(SpamAssassinResult.STATUS_MAIL_ATTRIBUTE_NAME))
            .anyMatch(header -> header.getValue().toLowerCase(Locale.US).startsWith(YES));
    }

}
