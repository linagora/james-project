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

package org.apache.james.mailbox.quota.mailing.listeners;

import static org.apache.james.mailbox.quota.model.QuotaThresholdFixture._50;
import static org.apache.james.mailbox.quota.model.QuotaThresholdFixture._80;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.james.core.User;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.quota.QuotaCount;
import org.apache.james.mailbox.quota.QuotaSize;
import org.apache.james.mailbox.quota.QuotaThresholdHistoryStore;
import org.apache.james.mailbox.quota.mailing.QuotaMailingListenerConfiguration;
import org.apache.james.mailbox.quota.model.QuotaThreshold;
import org.apache.james.mailbox.quota.model.QuotaThresholdChange;
import org.apache.james.mailbox.quota.model.QuotaThresholdHistory;
import org.apache.james.mailbox.quota.model.QuotaThresholds;
import org.apache.james.util.concurrency.ConcurrentTestRunner;
import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.FakeMailContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

public interface QuotaMailingListenersIntegrationTest {

    Duration GRACE_PERIOD = Duration.ofDays(1);
    QuotaThresholds SINGLE_THRESHOLD = new QuotaThresholds(ImmutableList.of(_50));
    String BOB = "bob@domain";
    User BOB_USER = User.fromUsername(BOB);
    Instant BASE_INSTANT = Instant.now();

    @Test
    default void shouldNotSendMailWhenUnderAllThresholds(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }

    @Test
    default void shouldNotSendMailWhenNoThresholdUpdate(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        store.persistQuotaSizeThresholdChange(BOB_USER,
            new QuotaThresholdChange(_50,
                BASE_INSTANT.minus(Duration.ofDays(2))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }

    @Test
    default void shouldNotSendMailWhenThresholdOverPassedRecently(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        store.persistQuotaSizeThresholdChange(BOB_USER,
            new QuotaThresholdChange(_50,
                BASE_INSTANT.minus(Duration.ofHours(12))));
        store.persistQuotaSizeThresholdChange(BOB_USER,
            new QuotaThresholdChange(QuotaThreshold.ZERO,
                BASE_INSTANT.minus(Duration.ofHours(6))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }

    @Test
    default void shouldSendMailWhenThresholdOverPassed(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldNotSendDuplicates(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(60))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldNotifySeparatelyCountAndSize(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(52))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(60))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(2);
    }

    @Test
    default void shouldGroupSizeAndCountNotificationsWhenTriggeredByASingleEvent(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(52))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldSendMailWhenThresholdOverPassedOverGracePeriod(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        store.persistQuotaSizeThresholdChange(BOB_USER,
            new QuotaThresholdChange(_50,
                BASE_INSTANT.minus(Duration.ofDays(12))));
        store.persistQuotaSizeThresholdChange(BOB_USER,
            new QuotaThresholdChange(QuotaThreshold.ZERO,
                BASE_INSTANT.minus(Duration.ofDays(6))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldNotSendMailWhenNoThresholdUpdateForCount(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        store.persistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(_50,
                BASE_INSTANT.minus(Duration.ofDays(2))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }

    @Test
    default void shouldNotSendMailWhenThresholdOverPassedRecentlyForCount(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        store.persistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(_50,
                BASE_INSTANT.minus(Duration.ofHours(12))));
        store.persistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(QuotaThreshold.ZERO,
                BASE_INSTANT.minus(Duration.ofHours(6))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }

    @Test
    default void shouldSendMailWhenThresholdOverPassedForCount(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldSendMailWhenThresholdOverPassedOverGracePeriodForCount(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);

        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        store.persistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(_50,
                BASE_INSTANT.minus(Duration.ofDays(12))));
        store.persistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(QuotaThreshold.ZERO,
                BASE_INSTANT.minus(Duration.ofDays(6))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    default FakeMailContext mailetContext() {
        return FakeMailContext.builder()
            .postmaster(MailAddressFixture.POSTMASTER_AT_JAMES)
            .build();
    }

    @Test
    default void shouldUpdateSizeChangesWhenOverPassingLimit(QuotaThresholdHistoryStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext(), Clock.fixed(BASE_INSTANT, ZoneId.systemDefault()));
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(
                new QuotaThresholdChange(
                    _50,
                    BASE_INSTANT)));
    }

    @Test
    default void shouldNotUpdateSizeChangesWhenNoChanges(QuotaThresholdHistoryStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext(), Clock.fixed(BASE_INSTANT, ZoneId.systemDefault()));
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory());
    }

    @Test
    default void shouldNotUpdateSizeChangesWhenNoChange(QuotaThresholdHistoryStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext(), Clock.fixed(BASE_INSTANT, ZoneId.systemDefault()));
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange = new QuotaThresholdChange(_50,
            BASE_INSTANT.minus(Duration.ofDays(6)));
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange));
    }

    @Test
    default void shouldUpdateSizeChangesWhenBelow(QuotaThresholdHistoryStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext(), Clock.fixed(BASE_INSTANT, ZoneId.systemDefault()));
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange = new QuotaThresholdChange(_50,
            BASE_INSTANT.minus(Duration.ofDays(6)));
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange,
                new QuotaThresholdChange(QuotaThreshold.ZERO, BASE_INSTANT)));
    }

    @Test
    default void shouldUpdateSizeChangesWhenAbove(QuotaThresholdHistoryStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext(), Clock.fixed(BASE_INSTANT, ZoneId.systemDefault()));
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange = new QuotaThresholdChange(QuotaThreshold.ZERO,
            BASE_INSTANT.minus(Duration.ofDays(6)));
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange,
                new QuotaThresholdChange(_50, BASE_INSTANT)));
    }

    @Test
    default void shouldUpdateSizeChangesWhenAboveButRecentlyOverpasses(QuotaThresholdHistoryStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext(), Clock.fixed(BASE_INSTANT, ZoneId.systemDefault()));
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange1 = new QuotaThresholdChange(_50,
            BASE_INSTANT.minus(Duration.ofHours(12)));
        QuotaThresholdChange oldChange2 = new QuotaThresholdChange(QuotaThreshold.ZERO,
            BASE_INSTANT.minus(Duration.ofHours(6)));
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange1);
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange2);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange1, oldChange2,
                new QuotaThresholdChange(_50, BASE_INSTANT)));
    }

    @Test
    default void shouldUpdateCountChangesWhenOverPassingLimit(QuotaThresholdHistoryStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext(), Clock.fixed(BASE_INSTANT, ZoneId.systemDefault()));
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(
                new QuotaThresholdChange(
                    _50,
                    BASE_INSTANT)));
    }

    @Test
    default void shouldNotUpdateCountChangesWhenNoChanges(QuotaThresholdHistoryStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext(), Clock.fixed(BASE_INSTANT, ZoneId.systemDefault()));
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory());
    }

    @Test
    default void shouldNotUpdateCountChangesWhenNoChange(QuotaThresholdHistoryStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext(), Clock.fixed(BASE_INSTANT, ZoneId.systemDefault()));
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange = new QuotaThresholdChange(_50,
            BASE_INSTANT.minus(Duration.ofDays(6)));
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange));
    }

    @Test
    default void shouldUpdateCountChangesWhenBelow(QuotaThresholdHistoryStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext(), Clock.fixed(BASE_INSTANT, ZoneId.systemDefault()));
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange = new QuotaThresholdChange(_50,
            BASE_INSTANT.minus(Duration.ofDays(6)));
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange,
                new QuotaThresholdChange(QuotaThreshold.ZERO, BASE_INSTANT)));
    }

    @Test
    default void shouldUpdateCountChangesWhenAbove(QuotaThresholdHistoryStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext(), Clock.fixed(BASE_INSTANT, ZoneId.systemDefault()));
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange = new QuotaThresholdChange(QuotaThreshold.ZERO,
            BASE_INSTANT.minus(Duration.ofDays(6)));
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange,
                new QuotaThresholdChange(_50, BASE_INSTANT)));
    }

    @Test
    default void shouldUpdateCountChangesWhenAboveButRecentlyOverpasses(QuotaThresholdHistoryStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext(), Clock.fixed(BASE_INSTANT, ZoneId.systemDefault()));
        testee.configure(new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD));


        QuotaThresholdChange oldChange1 = new QuotaThresholdChange(_50,
            BASE_INSTANT.minus(Duration.ofHours(12)));
        QuotaThresholdChange oldChange2 = new QuotaThresholdChange(QuotaThreshold.ZERO,
            BASE_INSTANT.minus(Duration.ofHours(6)));
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange1);
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange2);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange1, oldChange2,
                new QuotaThresholdChange(_50, BASE_INSTANT)));
    }

    @Test
    default void shouldSendOneNoticePerThreshold(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);
        testee.configure(new QuotaMailingListenerConfiguration(new QuotaThresholds(_50, _80), GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));
        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(85))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(42))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails())
            .hasSize(2);
    }

    @Disabled
    @Test
    default void shouldSendOneMailUponConcurrentEvents(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);
        testee.configure(new QuotaMailingListenerConfiguration(new QuotaThresholds(_50, _80), GRACE_PERIOD));

        new ConcurrentTestRunner(10, 1, (threadNb, step) ->
            testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
                QuotaRoot.quotaRoot("any", Optional.empty()),
                Quota.<QuotaCount>builder()
                    .used(QuotaCount.count(55))
                    .computedLimit(QuotaCount.count(100))
                    .build(),
                Quota.<QuotaSize>builder()
                    .used(QuotaSize.size(40))
                    .computedLimit(QuotaSize.size(100))
                    .build())))
            .run()
            .awaitTermination(1, TimeUnit.MINUTES);

        assertThat(mailetContext.getSentMails())
            .hasSize(1);
    }

    @Disabled
    @Test
    default void storeShouldOnlyStoreOneOverPassedThresholdOnceWhenConcurrentEvents(QuotaThresholdHistoryStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(store, mailetContext);
        testee.configure(new QuotaMailingListenerConfiguration(new QuotaThresholds(_50, _80), GRACE_PERIOD));

        int threadCount = 5;
        new ConcurrentTestRunner(threadCount, 1, (threadNb, step) ->
            testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
                QuotaRoot.quotaRoot("any", Optional.empty()),
                Quota.<QuotaCount>builder()
                    .used(QuotaCount.count(60 + threadNb))
                    .computedLimit(QuotaCount.count(100))
                    .build(),
                Quota.<QuotaSize>builder()
                    .used(QuotaSize.size(40))
                    .computedLimit(QuotaSize.size(100))
                    .build())))
            .run()
            .awaitTermination(1, TimeUnit.MINUTES);

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER)
            .getChanges())
            .hasSize(1);
    }
}