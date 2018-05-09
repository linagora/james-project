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

package org.apache.james.modules.mailbox;

import java.time.Duration;

import org.apache.james.evensourcing.cassandra.dto.EventDTOModule;
import org.apache.james.eventsourcing.CommandDispatcher;
import org.apache.james.eventsourcing.Subscriber;
import org.apache.james.mailbox.quota.cassandra.dto.QuotaThresholdChangedEventDTOModule;
import org.apache.james.mailbox.quota.mailing.QuotaMailingListenerConfiguration;
import org.apache.james.mailbox.quota.mailing.commands.DetectThresholdCrossingHandler;
import org.apache.james.mailbox.quota.mailing.subscribers.QuotaThresholdMailer;
import org.apache.james.mailbox.quota.model.QuotaThreshold;
import org.apache.james.mailbox.quota.model.QuotaThresholds;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class CassandraQuotaMailingModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), Subscriber.class)
            .addBinding()
            .to(QuotaThresholdMailer.class);

        Multibinder.newSetBinder(binder(), CommandDispatcher.CommandHandler.class)
            .addBinding()
            .to(DetectThresholdCrossingHandler.class);

        Multibinder.newSetBinder(binder(), EventDTOModule.class)
            .addBinding()
            .to(QuotaThresholdChangedEventDTOModule.class);

        bind(QuotaMailingListenerConfiguration.class)
            .toInstance(
                new QuotaMailingListenerConfiguration(
                    new QuotaThresholds(
                        new QuotaThreshold(0.80),
                        new QuotaThreshold(0.99)),
                    Duration.ofDays(1)));
    }
}
