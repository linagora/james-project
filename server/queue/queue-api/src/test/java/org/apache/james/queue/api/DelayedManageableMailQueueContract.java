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

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.mailet.Mail;
import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.FakeMail;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public interface DelayedManageableMailQueueContract {

    ManageableMailQueue getManageableMailQueue();

    ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(2);

    @AfterAll
    static void afterAllTests() {
        EXECUTOR_SERVICE.shutdownNow();
    }

    @Test
    default void flushShouldRemoveDelays() throws Exception {
        getManageableMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name")
            .build(), 30L, TimeUnit.SECONDS);

        getManageableMailQueue().flush();

        Future<MailQueue.MailQueueItem> tryDequeue = EXECUTOR_SERVICE.submit(() -> getManageableMailQueue().deQueue());
        assertThat(tryDequeue.get(1, TimeUnit.SECONDS).getMail().getName())
            .isEqualTo("name");
    }

    @Test
    default void flushShouldPreserveBrowseOrder() throws Exception {
        getManageableMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name1")
            .build());

        getManageableMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name2")
            .build(), 30L, TimeUnit.SECONDS);

        getManageableMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name3")
            .build(), 2L, TimeUnit.SECONDS);

        getManageableMailQueue().flush();

        assertThat(getManageableMailQueue().browse())
            .extracting(ManageableMailQueue.MailQueueItemView::getMail)
            .extracting(Mail::getName)
            .containsExactly("name1", "name2", "name3");
    }

}
