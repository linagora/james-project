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

package org.apache.james.dlp.eventsourcing.aggregates;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.james.dlp.api.DLPRule;
import org.apache.james.dlp.eventsourcing.events.ClearEvent;
import org.apache.james.dlp.eventsourcing.events.StoreEvent;
import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.eventstore.History;

import com.google.common.collect.ImmutableList;

public class DLPRuleAggregate {

    public static DLPRuleAggregate load(DLPRuleAggregateId aggregateId, History history) {
        return new DLPRuleAggregate(aggregateId, history);
    }

    private final DLPRuleAggregateId aggregateId;
    private final History history;

    private DLPRuleAggregate(DLPRuleAggregateId aggregateId, History history) {
        this.aggregateId = aggregateId;
        this.history = history;
    }

    public Stream<DLPRule> retrieveRules() {
        return getLastEvent()
            .filter(event -> event instanceof StoreEvent)
            .map(event -> (StoreEvent) event)
            .map(StoreEvent::getRules)
            .map(List::stream)
            .orElse(Stream.of());
    }

    public List<Event> clear() {
        return ImmutableList.of(new ClearEvent(aggregateId, history.getNextEventId()));
    }

    public List<Event> store(List<DLPRule> rules) {
        return ImmutableList.of(new StoreEvent(aggregateId, history.getNextEventId(), rules));
    }

    private Optional<Event> getLastEvent() {
        return history.getEvents()
            .stream()
            .max(Comparator.naturalOrder());
    }

}
