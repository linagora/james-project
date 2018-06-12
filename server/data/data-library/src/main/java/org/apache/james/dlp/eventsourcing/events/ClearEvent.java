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

package org.apache.james.dlp.eventsourcing.events;

import java.util.Objects;

import org.apache.james.dlp.eventsourcing.aggregates.DLPRuleAggregateId;
import org.apache.james.eventsourcing.AggregateId;
import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.EventId;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public class ClearEvent implements Event {
    private final DLPRuleAggregateId aggregateId;
    private final EventId eventId;

    public ClearEvent(DLPRuleAggregateId aggregateId, EventId eventId) {
        Preconditions.checkNotNull(aggregateId);
        Preconditions.checkNotNull(eventId);

        this.aggregateId = aggregateId;
        this.eventId = eventId;
    }

    @Override
    public EventId eventId() {
        return eventId;
    }

    @Override
    public AggregateId getAggregateId() {
        return aggregateId;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof ClearEvent) {
            ClearEvent that = (ClearEvent) o;

            return Objects.equals(this.aggregateId, that.aggregateId)
                && Objects.equals(this.eventId, that.eventId);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(aggregateId, eventId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("aggregateId", aggregateId)
            .add("eventId", eventId)
            .toString();
    }
}
