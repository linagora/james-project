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

package org.apache.james.queue.api;

import static org.apache.james.queue.api.MailQueueFixture.createMimeMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;
import org.apache.mailet.PerRecipientHeaders;
import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.MimeMessageUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.github.fge.lambdas.Throwing;

public interface MailQueueContract {

    ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(2);

    MailQueue getMailQueue();

    @AfterAll
    static void afterAllTests() {
        EXECUTOR_SERVICE.shutdownNow();
    }

    @Test
    default void queueShouldPreserveMailRecipients() throws Exception {
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name")
            .build());

        MailQueue.MailQueueItem mailQueueItem = getMailQueue().deQueue();
        assertThat(mailQueueItem.getMail().getRecipients())
            .containsOnly(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES);
    }

    @Test
    default void queueShouldPreserveMailSender() throws Exception {
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name")
            .build());

        MailQueue.MailQueueItem mailQueueItem = getMailQueue().deQueue();
        assertThat(mailQueueItem.getMail().getSender())
            .isEqualTo(MailAddressFixture.OTHER_AT_LOCAL);
    }

    @Test
    default void queueShouldPreserveMimeMessage() throws Exception {
        MimeMessage originalMimeMessage = createMimeMessage();
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(originalMimeMessage)
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name")
            .build());

        MailQueue.MailQueueItem mailQueueItem = getMailQueue().deQueue();
        assertThat(MimeMessageUtil.asString(mailQueueItem.getMail().getMessage()))
            .isEqualTo(MimeMessageUtil.asString(originalMimeMessage));
    }

    @Test
    default void queueShouldPreserveMailAttribute() throws Exception {
        String attributeName = "any";
        String attributeValue = "value";
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .attribute(attributeName, attributeValue)
            .name("name")
            .build());

        MailQueue.MailQueueItem mailQueueItem = getMailQueue().deQueue();
        assertThat(mailQueueItem.getMail().getAttribute(attributeName))
            .isEqualTo(attributeValue);
    }

    @Test
    default void queueShouldPreserveErrorMessage() throws Exception {
        String errorMessage = "ErrorMessage";
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .errorMessage(errorMessage)
            .name("name")
            .build());

        MailQueue.MailQueueItem mailQueueItem = getMailQueue().deQueue();
        assertThat(mailQueueItem.getMail().getErrorMessage())
            .isEqualTo(errorMessage);
    }

    @Test
    default void queueShouldPreserveState() throws Exception {
        String state = "state";
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .state(state)
            .name("name")
            .build());

        MailQueue.MailQueueItem mailQueueItem = getMailQueue().deQueue();
        assertThat(mailQueueItem.getMail().getState())
            .isEqualTo(state);
    }

    @Test
    default void queueShouldPreserveRemoteAddress() throws Exception {
        String remoteAddress = "remote";
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .remoteAddr(remoteAddress)
            .name("name")
            .build());

        MailQueue.MailQueueItem mailQueueItem = getMailQueue().deQueue();
        assertThat(mailQueueItem.getMail().getRemoteAddr())
            .isEqualTo(remoteAddress);
    }

    @Test
    default void queueShouldPreserveRemoteHost() throws Exception {
        String remoteHost = "remote";
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .remoteHost(remoteHost)
            .name("name")
            .build());

        MailQueue.MailQueueItem mailQueueItem = getMailQueue().deQueue();
        assertThat(mailQueueItem.getMail().getRemoteHost())
            .isEqualTo(remoteHost);
    }

    @Test
    default void queueShouldPreserveLastUpdated() throws Exception {
        Date lastUpdated = new Date();
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(lastUpdated)
            .name("name")
            .build());

        MailQueue.MailQueueItem mailQueueItem = getMailQueue().deQueue();
        assertThat(mailQueueItem.getMail().getLastUpdated())
            .isEqualTo(lastUpdated);
    }

    @Test
    default void queueShouldPreserveName() throws Exception {
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name")
            .build());

        MailQueue.MailQueueItem mailQueueItem = getMailQueue().deQueue();
        assertThat(mailQueueItem.getMail().getName())
            .isEqualTo("name");
    }

    @Test
    default void queueShouldPreservePerRecipientHeaders() throws Exception {
        PerRecipientHeaders.Header header = PerRecipientHeaders.Header.builder()
            .name("any")
            .value("any")
            .build();
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name")
            .addHeaderForRecipient(header, MailAddressFixture.ANY_AT_JAMES)
            .build());

        MailQueue.MailQueueItem mailQueueItem = getMailQueue().deQueue();
        assertThat(mailQueueItem.getMail().getPerRecipientSpecificHeaders()
            .getHeadersForRecipient(MailAddressFixture.ANY_AT_JAMES))
            .containsOnly(header);
    }

    @Test
    default void dequeueShouldBeFifo() throws Exception {
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name1")
            .build());
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name2")
            .build());

        MailQueue.MailQueueItem mailQueueItem1 = getMailQueue().deQueue();
        mailQueueItem1.done(true);
        MailQueue.MailQueueItem mailQueueItem2 = getMailQueue().deQueue();
        mailQueueItem2.done(true);
        assertThat(mailQueueItem1.getMail().getName()).isEqualTo("name1");
        assertThat(mailQueueItem2.getMail().getName()).isEqualTo("name2");
    }

    @Test
    default void dequeueCouldBeInterleaving() throws Exception {
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name1")
            .build());
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name2")
            .build());

        MailQueue.MailQueueItem mailQueueItem1 = getMailQueue().deQueue();
        MailQueue.MailQueueItem mailQueueItem2 = getMailQueue().deQueue();
        mailQueueItem1.done(true);
        mailQueueItem2.done(true);
        assertThat(mailQueueItem1.getMail().getName()).isEqualTo("name1");
        assertThat(mailQueueItem2.getMail().getName()).isEqualTo("name2");
    }

    @Test
    default void dequeueShouldAllowRetrieveFailItems() throws Exception {
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name1")
            .build());
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name1")
            .build());

        MailQueue.MailQueueItem mailQueueItem1 = getMailQueue().deQueue();
        mailQueueItem1.done(false);
        MailQueue.MailQueueItem mailQueueItem2 = getMailQueue().deQueue();
        mailQueueItem2.done(true);
        assertThat(mailQueueItem1.getMail().getName()).isEqualTo("name1");
        assertThat(mailQueueItem2.getMail().getName()).isEqualTo("name1");
    }

    @Test
    default void dequeueShouldNotReturnInProcessingEmails() throws Exception {
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name")
            .build());

        getMailQueue().deQueue();

        Future<?> future = EXECUTOR_SERVICE.submit(Throwing.runnable(() -> getMailQueue().deQueue()));
        assertThatThrownBy(() -> future.get(2, TimeUnit.SECONDS))
            .isInstanceOf(TimeoutException.class);
    }

    @Test
    default void deQueueShouldFreezeWhenNoMail() throws Exception {
        Future<?> future = EXECUTOR_SERVICE.submit(Throwing.runnable(() -> getMailQueue().deQueue()));

        assertThatThrownBy(() -> future.get(2, TimeUnit.SECONDS))
            .isInstanceOf(TimeoutException.class);
    }

    @Test
    default void deQueueShouldWaitForAMailToBeEnqueued() throws Exception {
        Mail mail = FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name")
            .build();
        Future<MailQueue.MailQueueItem> tryDequeue = EXECUTOR_SERVICE.submit(() -> getMailQueue().deQueue());
        EXECUTOR_SERVICE.submit(Throwing.runnable(() -> getMailQueue().enQueue(mail)));

        assertThat(tryDequeue.get().getMail().getName()).isEqualTo("name");
    }

}
