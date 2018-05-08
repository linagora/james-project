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

import java.time.Clock;

import org.apache.james.core.User;
import org.apache.james.mailbox.Event;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.quota.HistoryEvolution;
import org.apache.james.mailbox.quota.QuotaThresholdChangedEvent;
import org.apache.james.mailbox.quota.QuotaThresholdHistoryStore;
import org.apache.james.mailbox.quota.mailing.QuotaMailingListenerConfiguration;
import org.apache.james.mailbox.quota.model.QuotaThreshold;
import org.apache.james.mailbox.store.event.MailboxEventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuotaThresholdCrossingListener implements MailboxListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaThresholdCrossingListener.class);

    private final MailboxEventDispatcher dispatcher;
    private final QuotaThresholdHistoryStore quotaThresholdHistoryStore;
    private final Clock clock;
    private QuotaMailingListenerConfiguration configuration;

    public QuotaThresholdCrossingListener(MailboxEventDispatcher dispatcher, QuotaThresholdHistoryStore quotaThresholdHistoryStore, Clock clock) {
        this.dispatcher = dispatcher;
        this.quotaThresholdHistoryStore = quotaThresholdHistoryStore;
        this.clock = clock;
        this.configuration = QuotaMailingListenerConfiguration.DEFAULT;
    }

    public QuotaThresholdCrossingListener(MailboxEventDispatcher dispatcher, QuotaThresholdHistoryStore quotaThresholdHistoryStore) {
        this(dispatcher, quotaThresholdHistoryStore, Clock.systemUTC());
    }

    public void configure(QuotaMailingListenerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ListenerType getType() {
        return ListenerType.ONCE;
    }

    @Override
    public ExecutionMode getExecutionMode() {
        return ExecutionMode.SYNCHRONOUS;
    }

    @Override
    public void event(Event event) {
        try {
            if (event instanceof QuotaUsageUpdatedEvent) {
                handleEvent(getUser(event), (QuotaUsageUpdatedEvent) event);
            }
        } catch (Exception e) {
            LOGGER.error("Can not re-emmit quota threshold events", e);
        }
    }

    public void handleEvent(User user, QuotaUsageUpdatedEvent event) {
        QuotaThreshold countThreshold = configuration.getThresholds().highestExceededThreshold(event.getCountQuota());
        QuotaThreshold sizeThreshold = configuration.getThresholds().highestExceededThreshold(event.getSizeQuota());

        HistoryEvolution countHistoryEvolution = quotaThresholdHistoryStore
            .retrieveQuotaCountThresholdChanges(user)
            .compareWithCurrentThreshold(countThreshold, configuration.getGracePeriod(), clock);
        HistoryEvolution sizeHistoryEvolution = quotaThresholdHistoryStore
            .retrieveQuotaSizeThresholdChanges(user)
            .compareWithCurrentThreshold(sizeThreshold, configuration.getGracePeriod(), clock);

        if (countHistoryEvolution.isChange() || sizeHistoryEvolution.isChange()) {
            dispatcher.event(
                new QuotaThresholdChangedEvent(
                    sizeHistoryEvolution,
                    countHistoryEvolution,
                    event.getSizeQuota(),
                    event.getCountQuota(),
                    event.getSession()));
        }
    }

    private User getUser(Event event) {
        return User.fromUsername(
            event.getSession()
                .getUser()
                .getUserName());
    }
}
