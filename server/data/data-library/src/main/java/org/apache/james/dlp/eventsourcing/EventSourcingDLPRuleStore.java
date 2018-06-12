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

package org.apache.james.dlp.eventsourcing;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.james.core.Domain;
import org.apache.james.dlp.api.DLPRule;
import org.apache.james.dlp.api.DLPRulesStore;
import org.apache.james.dlp.eventsourcing.aggregates.DLPRuleAggregate;
import org.apache.james.dlp.eventsourcing.aggregates.DLPRuleAggregateId;
import org.apache.james.dlp.eventsourcing.commands.ClearCommand;
import org.apache.james.dlp.eventsourcing.commands.ClearCommandHandler;
import org.apache.james.dlp.eventsourcing.commands.StoreCommand;
import org.apache.james.dlp.eventsourcing.commands.StoreCommandHandler;
import org.apache.james.eventsourcing.EventSourcingSystem;
import org.apache.james.eventsourcing.Subscriber;
import org.apache.james.eventsourcing.eventstore.EventStore;

import com.google.common.collect.ImmutableSet;

public class EventSourcingDLPRuleStore implements DLPRulesStore {

    private static final ImmutableSet<Subscriber> NO_SUBSCRIBER = ImmutableSet.of();

    private final EventSourcingSystem eventSourcingSystem;
    private final EventStore eventStore;

    @Inject
    public EventSourcingDLPRuleStore(EventStore eventStore) {
        this.eventSourcingSystem = new EventSourcingSystem(
            ImmutableSet.of(
                new ClearCommandHandler(eventStore),
                new StoreCommandHandler(eventStore)),
            NO_SUBSCRIBER,
            eventStore);
        this.eventStore = eventStore;
    }

    @Override
    public Stream<DLPRule> retrieveRules(Domain domain) {

        DLPRuleAggregateId aggregateId = new DLPRuleAggregateId(domain);

        return DLPRuleAggregate.load(
                aggregateId,
                eventStore.getEventsOfAggregate(aggregateId))
            .retrieveRules();
    }

    @Override
    public void store(Domain domain, List<DLPRule> rules) {
        eventSourcingSystem.dispatch(new StoreCommand(domain, rules));
    }

    @Override
    public void clear(Domain domain) {
        eventSourcingSystem.dispatch(new ClearCommand(domain));
    }

}
