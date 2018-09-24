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

package org.apache.james.queue.rabbitmq.view.cassandra.configuration;

import java.time.Duration;
import java.util.Objects;

import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.EventId;
import org.apache.james.eventsourcing.eventstore.cassandra.dto.EventDTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

class ConfigurationAddedDTO implements EventDTO {

    static ConfigurationAddedDTO from(ConfigurationAdded configurationAdded, String type) {
        Preconditions.checkNotNull(configurationAdded);

        CassandraMailQueueViewConfiguration configuration = configurationAdded.getConfiguration();
        return new ConfigurationAddedDTO(
            configurationAdded.eventId().serialize(),
            type, configuration.getBucketCount(),
            configuration.getUpdateBrowseStartPace(),
            configuration.getSliceWindow());
    }

    static ConfigurationAddedDTO from(ConfigurationAdded configurationAdded) {
        return from(configurationAdded, CassandraMailQueueViewConfigurationModule.TYPE);
    }

    private final int eventId;
    private final String type;
    private final int bucketCount;
    private final int updateBrowseStartPace;
    private final Duration sliceWindow;

    @JsonCreator
    ConfigurationAddedDTO(
        @JsonProperty("eventId") int eventId,
        @JsonProperty("type") String type,
        @JsonProperty("bucketCount") int bucketCount,
        @JsonProperty("updateBrowseStartPace") int updateBrowseStartPace,
        @JsonProperty("sliceWindow") Duration sliceWindow) {

        this.eventId = eventId;
        this.type = type;
        this.bucketCount = bucketCount;
        this.updateBrowseStartPace = updateBrowseStartPace;
        this.sliceWindow = sliceWindow;
    }

    @JsonIgnore
    @Override
    public Event toEvent() {
        return new ConfigurationAdded(
            EventId.fromSerialized(eventId),
            CassandraMailQueueViewConfiguration.builder()
                .bucketCount(bucketCount)
                .updateBrowseStartPace(updateBrowseStartPace)
                .sliceWindow(sliceWindow)
                .build());
    }

    public int getEventId() {
        return eventId;
    }

    public String getType() {
        return type;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public int getUpdateBrowseStartPace() {
        return updateBrowseStartPace;
    }

    public Duration getSliceWindow() {
        return sliceWindow;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof ConfigurationAddedDTO) {
            ConfigurationAddedDTO that = (ConfigurationAddedDTO) o;

            return Objects.equals(this.eventId, that.eventId)
                && Objects.equals(this.type, that.type)
                && Objects.equals(this.bucketCount, that.bucketCount)
                && Objects.equals(this.updateBrowseStartPace, that.updateBrowseStartPace)
                && Objects.equals(this.sliceWindow, that.sliceWindow);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(eventId, type, bucketCount, updateBrowseStartPace, sliceWindow);
    }
}
