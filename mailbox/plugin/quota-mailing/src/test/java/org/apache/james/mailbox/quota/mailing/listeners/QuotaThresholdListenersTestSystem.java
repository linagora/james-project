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

import org.apache.james.mailbox.Event;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.quota.QuotaThresholdHistoryStore;
import org.apache.james.mailbox.quota.mailing.QuotaMailingListenerConfiguration;
import org.apache.james.mailbox.store.event.DefaultDelegatingMailboxListener;
import org.apache.james.mailbox.store.event.MailboxEventDispatcher;
import org.apache.james.user.memory.MemoryUsersRepository;
import org.apache.mailet.MailetContext;

public class QuotaThresholdListenersTestSystem {
    private final MailboxEventDispatcher dispatcher;
    private final DefaultDelegatingMailboxListener delegatingListener;
    private final QuotaThresholdCrossingListener thresholdEmitter;
    private final QuotaThresholdHistoryUpdater historyUpdater;
    private final QuotaThresholdMailer thresholdMailer;

    public QuotaThresholdListenersTestSystem(QuotaThresholdHistoryStore store, MailetContext mailetContext, Clock clock) throws MailboxException {
        delegatingListener = new DefaultDelegatingMailboxListener();
        dispatcher = new MailboxEventDispatcher(delegatingListener);

        thresholdEmitter = new QuotaThresholdCrossingListener(dispatcher, store, clock);
        historyUpdater = new QuotaThresholdHistoryUpdater(store, clock);
        thresholdMailer = new QuotaThresholdMailer(mailetContext, MemoryUsersRepository.withVirtualHosting());

        MockMailboxSession mailboxSession = new MockMailboxSession("system");
        delegatingListener.addGlobalListener(thresholdEmitter, mailboxSession);
        delegatingListener.addGlobalListener(historyUpdater, mailboxSession);
        delegatingListener.addGlobalListener(thresholdMailer, mailboxSession);
    }

    public QuotaThresholdListenersTestSystem(QuotaThresholdHistoryStore store, MailetContext mailetContext) throws MailboxException {
        this(store, mailetContext, Clock.systemUTC());
    }

    public void configure(QuotaMailingListenerConfiguration configuration) {
        thresholdEmitter.configure(configuration);
    }

    public void event(Event event) {
        delegatingListener.event(event);
    }
}
