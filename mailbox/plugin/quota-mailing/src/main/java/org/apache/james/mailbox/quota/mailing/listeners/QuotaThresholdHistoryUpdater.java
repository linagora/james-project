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
import java.time.Instant;

import org.apache.james.core.User;
import org.apache.james.mailbox.Event;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.quota.QuotaThresholdChangedEvent;
import org.apache.james.mailbox.quota.QuotaThresholdHistoryStore;
import org.apache.james.mailbox.quota.model.QuotaThresholdChange;

public class QuotaThresholdHistoryUpdater implements MailboxListener {

    private final QuotaThresholdHistoryStore quotaThresholdHistoryStore;
    private final Clock clock;

    public QuotaThresholdHistoryUpdater(QuotaThresholdHistoryStore quotaThresholdHistoryStore) {
        this(quotaThresholdHistoryStore, Clock.systemUTC());
    }

    public QuotaThresholdHistoryUpdater(QuotaThresholdHistoryStore quotaThresholdHistoryStore, Clock clock) {
        this.quotaThresholdHistoryStore = quotaThresholdHistoryStore;
        this.clock = clock;
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
        if (event instanceof QuotaThresholdChangedEvent) {
            QuotaThresholdChangedEvent changeEvent = (QuotaThresholdChangedEvent) event;
            User user = getUser(event);

            updateCountHistory(changeEvent, user);
            updateSizeHistory(changeEvent, user);
        }
    }

    public void updateSizeHistory(QuotaThresholdChangedEvent changeEvent, User user) {
        if (changeEvent.getSizeHistoryEvolution().isChange()) {
            quotaThresholdHistoryStore.persistQuotaSizeThresholdChange(user,
                new QuotaThresholdChange(changeEvent.getSizeHistoryEvolution().getThreshold(), Instant.now(clock)));
        }
    }

    public void updateCountHistory(QuotaThresholdChangedEvent changeEvent, User user) {
        if (changeEvent.getCountHistoryEvolution().isChange()) {
            quotaThresholdHistoryStore.persistQuotaCountThresholdChange(user,
                new QuotaThresholdChange(changeEvent.getCountHistoryEvolution().getThreshold(),
                    Instant.now(clock)));
        }
    }

    private User getUser(Event event) {
        return User.fromUsername(
            event.getSession()
                .getUser()
                .getUserName());
    }
}
