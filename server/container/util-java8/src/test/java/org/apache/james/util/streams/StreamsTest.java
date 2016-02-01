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

package org.apache.james.util.streams;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class StreamsTest {

    @Test(expected=NullPointerException.class)
    public void omitEmptyShouldThrowWhenNull() {
        Streams.omitEmpty(null);
    }

    @Test
    public void omitEmptyShouldReturnEmptyStreamWhenEmptyList() {
        Stream<Object> sut = Streams.omitEmpty(ImmutableList.of());
        assertThat(sut.collect(Collectors.toImmutableList())).hasSize(0);
    }

    @Test
    public void omitEmptyShouldReturnOneWhenOne() {
        Stream<String> sut = Streams.omitEmpty(ImmutableList.of(Optional.of("test")));
        assertThat(sut.collect(Collectors.toImmutableList())).hasSize(1);
    }

    @Test
    public void omitEmptyShouldReturnEmptyWhenOneEmpty() {
        Stream<String> sut = Streams.omitEmpty(ImmutableList.of(Optional.empty()));
        assertThat(sut.collect(Collectors.toImmutableList())).hasSize(0);
    }

    @Test
    public void omitEmptyShouldReturnTwoWhenTwoPresentAndTwoEmpty() {
        Stream<String> sut = Streams.omitEmpty(ImmutableList.of(Optional.of("test1"), Optional.empty(), Optional.of("test2"), Optional.empty()));
        assertThat(sut.collect(Collectors.toImmutableList())).hasSize(2);
    }
}
