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
package org.apache.james.jmap.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.apache.james.jmap.json.ObjectMapperFactory;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

public class FilterTest {

    private ObjectMapper parser;

    @Before
    public void setup() {
        parser = new ObjectMapperFactory().forParsing();
    }

    @Test
    public void emptyFilterConditionShouldBeDeserialized() throws Exception {
        String json = "{}";
        Filter expected = FilterCondition.builder()
                .build();
        Filter actual = parser.readValue(json, Filter.class);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void singleFilterConditionShouldBeDeserialized() throws Exception {
        String json = "{\"inMailboxes\": [\"12\",\"34\"]}";
        Filter expected = FilterCondition.builder()
                .inMailboxes(Optional.of(ImmutableList.of("12","34")))
                .build();
        Filter actual = parser.readValue(json, Filter.class);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void doubleFilterConditionShouldBeDeserialized() throws Exception {
        String json = "{\"inMailboxes\": [\"12\",\"34\"], \"notInMailboxes\": [\"45\",\"67\"]}";
        Filter expected = FilterCondition.builder()
                .inMailboxes(Optional.of(ImmutableList.of("12","34")))
                .notInMailboxes(Optional.of(ImmutableList.of("45","67")))
                .build();
        Filter actual = parser.readValue(json, Filter.class);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void operatorWithSingleConditionShouldBeDeserialized() throws Exception {
        String json = "{\"operator\": \"AND\", \"conditions\": [{\"inMailboxes\": [\"12\",\"34\"]}]}";
        Filter expected = FilterOperator.builder()
                .operator(Operator.AND)
                .conditions(ImmutableList.of(FilterCondition.builder()
                        .inMailboxes(Optional.of(ImmutableList.of("12","34")))
                        .build()))
                .build();
        Filter actual = parser.readValue(json, Filter.class);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void complexFilterShouldBeDeserialized() throws Exception {
        String json = "{\"operator\": \"AND\", \"conditions\": ["
                + "         {\"inMailboxes\": [\"12\",\"34\"]},"
                + "         {\"operator\": \"OR\", \"conditions\": ["
                + "                 {\"operator\": \"NOT\", \"conditions\": ["
                + "                         {\"notInMailboxes\": [\"45\"]}]},"
                + "                 {}]}]}";
        Filter expected = FilterOperator.builder()
                .operator(Operator.AND)
                .conditions(ImmutableList.of(
                        FilterCondition.builder()
                            .inMailboxes(Optional.of(ImmutableList.of("12","34")))
                            .build(),
                        FilterOperator.builder()
                            .operator(Operator.OR)
                            .conditions(ImmutableList.of(
                                    FilterOperator.builder()
                                        .operator(Operator.NOT)
                                        .conditions(ImmutableList.of(
                                                FilterCondition.builder()
                                                    .notInMailboxes(Optional.of(ImmutableList.of("45")))
                                                    .build()))
                                        .build(),
                                    FilterCondition.builder()
                                        .build()
                                )).build()))
                .build();
        Filter actual = parser.readValue(json, Filter.class);
        assertThat(actual).isEqualTo(expected);
    }
}
