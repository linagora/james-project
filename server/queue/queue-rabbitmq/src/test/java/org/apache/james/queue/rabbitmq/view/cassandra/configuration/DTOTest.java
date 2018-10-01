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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.apache.james.queue.rabbitmq.view.cassandra.configuration.CassandraMailQueueViewConfigurationModule.TYPE_NAME;
import static org.apache.james.queue.rabbitmq.view.cassandra.configuration.EventsourcingConfigurationManagement.CONFIGURATION_AGGREGATE_KEY;
import static org.apache.james.util.ClassLoaderUtils.getSystemResourceAsString;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class DTOTest {

    private static final int EVENT_ID = 0;
    private static final int BUCKET_COUNT = 1;
    private static final int UPDATE_PACE = 1000;
    private static final Duration SLICE_WINDOW = Duration.ofHours(1);

    private final ConfigurationChangedDTO CONFIGURATION_EDITED_DTO = new ConfigurationChangedDTO(
        EVENT_ID, CONFIGURATION_AGGREGATE_KEY, TYPE_NAME, BUCKET_COUNT, UPDATE_PACE, SLICE_WINDOW);

    private static final String CONFIGURATION_EDITED_DTO_JSON = getSystemResourceAsString(
        "json/mailqueueview/configuration/configuration_edited.json");

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
    }

    @Test
    void shouldSerializeConfigurationEditedDTO() throws Exception {
        assertThatJson(
            objectMapper.writeValueAsString(CONFIGURATION_EDITED_DTO))
            .isEqualTo(CONFIGURATION_EDITED_DTO_JSON);
    }

    @Test
    void shouldDeserializeConfigurationEditedDTO() throws Exception {
        assertThat(
            objectMapper.readValue(CONFIGURATION_EDITED_DTO_JSON, ConfigurationChangedDTO.class))
            .isEqualTo(CONFIGURATION_EDITED_DTO);
    }
}
