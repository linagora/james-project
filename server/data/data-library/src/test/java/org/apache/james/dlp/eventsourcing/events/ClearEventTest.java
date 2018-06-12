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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.james.core.Domain;
import org.apache.james.dlp.eventsourcing.aggregates.DLPRuleAggregateId;
import org.apache.james.eventsourcing.EventId;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ClearEventTest {

    @Test
    public void shouldMatchBeanContract() {
        EqualsVerifier.forClass(ClearEvent.class)
            .allFieldsShouldBeUsed()
            .verify();
    }

    @Test
    public void constructorShouldThrowWhenNullAggregateId() {
        assertThatThrownBy(() -> new ClearEvent(null, EventId.first()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void constructorShouldThrowWhenNullEventId() {
        assertThatThrownBy(() -> new ClearEvent(new DLPRuleAggregateId(Domain.LOCALHOST), null))
            .isInstanceOf(NullPointerException.class);
    }

}