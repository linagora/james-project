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

import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.FakeMail;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.github.fge.lambdas.Throwing;
import com.google.common.base.Stopwatch;

public interface DelayedMailQueueContract {

    ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(2);

    MailQueue getMailQueue();

    @AfterAll
    static void afterAllTests() {
        EXECUTOR_SERVICE.shutdownNow();
    }

    @Test
    default void enqueueShouldDelayEmailsWhenSpecified() throws Exception {
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name")
            .build(), 2L, TimeUnit.SECONDS);

        Future<?> future = EXECUTOR_SERVICE.submit(Throwing.runnable(() -> getMailQueue().deQueue()));
        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
            .isInstanceOf(TimeoutException.class);
    }

    @Test
    default void delayedEmailCanBeRetrievedFromTheQueue() throws Exception {
        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name")
            .build(), 1L, TimeUnit.SECONDS);

        MailQueue.MailQueueItem mailQueueItem = getMailQueue().deQueue();
        assertThat(mailQueueItem.getMail().getName()).isEqualTo("name");
    }

    @Test
    default void delayShouldAtLeastBeTheOneSpecified() throws Exception {
        long delay = 1L;
        TimeUnit unit = TimeUnit.SECONDS;
        Stopwatch started = Stopwatch.createStarted();

        getMailQueue().enQueue(FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date())
            .name("name")
            .build(), delay, unit);

        getMailQueue().deQueue();
        assertThat(started.elapsed(TimeUnit.MILLISECONDS))
            .isGreaterThan(unit.toMillis(delay));
    }

}
