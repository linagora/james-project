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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.apache.james.eventsourcing.eventstore.EventStore;
import org.apache.james.eventsourcing.eventstore.cassandra.CassandraEventStoreExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class EventsourcingConfigurationManagementTest {

    @RegisterExtension
    static CassandraEventStoreExtension eventStoreExtension = new CassandraEventStoreExtension(
        CassandraMailQueueViewConfigurationModule.MAIL_QUEUE_VIEW_CONFIGURATION);

    private static final CassandraMailQueueViewConfiguration FIRST_CONFIGURATION = CassandraMailQueueViewConfiguration.builder()
        .bucketCount(1)
        .updateBrowseStartPace(1)
        .sliceWindow(Duration.ofHours(1))
        .build();
    private static final CassandraMailQueueViewConfiguration SECOND_CONFIGURATION = CassandraMailQueueViewConfiguration.builder()
        .bucketCount(2)
        .updateBrowseStartPace(2)
        .sliceWindow(Duration.ofHours(2))
        .build();
    private static final CassandraMailQueueViewConfiguration THIRD_CONFIGURATION = CassandraMailQueueViewConfiguration.builder()
        .bucketCount(3)
        .updateBrowseStartPace(3)
        .sliceWindow(Duration.ofHours(3))
        .build();

    private EventsourcingConfigurationManagement createConfigurationManagement(EventStore eventStore) {
        return new EventsourcingConfigurationManagement(eventStore);
    }

    @Test
    void loadShouldReturnEmptyIfNoConfigurationStored(EventStore eventStore) {
        EventsourcingConfigurationManagement testee = createConfigurationManagement(eventStore);

        assertThat(testee.load())
            .isEmpty();
    }

    @Test
    void loadShouldReturnTheLastConfiguration(EventStore eventStore) {
        EventsourcingConfigurationManagement testee = createConfigurationManagement(eventStore);
        testee.store(FIRST_CONFIGURATION);
        testee.store(SECOND_CONFIGURATION);
        testee.store(THIRD_CONFIGURATION);

        assertThat(testee.load())
            .contains(THIRD_CONFIGURATION);
    }

    @Test
    void storeShouldThrowWhenConfigurationIsNull(EventStore eventStore) {
        EventsourcingConfigurationManagement testee = createConfigurationManagement(eventStore);

        assertThatThrownBy(() -> testee.store(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void storeShouldThrowWhenBucketCountIsLessThanTheCurrentOne(EventStore eventStore) {
        EventsourcingConfigurationManagement testee = createConfigurationManagement(eventStore);
        testee.store(SECOND_CONFIGURATION);

        assertThatThrownBy(() -> testee.store(FIRST_CONFIGURATION))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void storeShouldStoreConfiguration(EventStore eventStore) {
        EventsourcingConfigurationManagement testee = createConfigurationManagement(eventStore);

        testee.store(FIRST_CONFIGURATION);

        assertThat(testee.load())
            .contains(FIRST_CONFIGURATION);
    }

    @Test
    void storeShouldReturnStoredConfiguration(EventStore eventStore) {
        EventsourcingConfigurationManagement testee = createConfigurationManagement(eventStore);

        CassandraMailQueueViewConfiguration storedConfiguration = testee.store(FIRST_CONFIGURATION);

        assertThat(storedConfiguration)
            .isEqualTo(FIRST_CONFIGURATION);
    }

    @Test
    void loadIfAbsentShouldLoadFromInitialIfEmptyHistory(EventStore eventStore) {
        EventsourcingConfigurationManagement testee = createConfigurationManagement(eventStore);

        assertThat(testee.loadIfAbsent(() -> FIRST_CONFIGURATION))
            .isEqualTo(FIRST_CONFIGURATION);
    }

    @Test
    void loadIfAbsentShouldLoadFromHistoryIfNotEmpty(EventStore eventStore) {
        EventsourcingConfigurationManagement testee = createConfigurationManagement(eventStore);
        testee.store(FIRST_CONFIGURATION);
        testee.store(SECOND_CONFIGURATION);

        assertThat(testee.loadIfAbsent(() -> THIRD_CONFIGURATION))
            .isEqualTo(SECOND_CONFIGURATION);
    }
}