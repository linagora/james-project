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

package org.apache.james.mailbox.quota.model;

import static org.apache.james.mailbox.quota.model.QuotaThresholdFixture._75;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class QuotaThresholdChangeTest {

    public static final Clock CLOCK = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    @Test
    public void shouldMatchBeanContract() {
        EqualsVerifier.forClass(QuotaThresholdChange.class)
            .allFieldsShouldBeUsed()
            .verify();
    }

    @Test
    public void isNotOlderThanShouldReturnTrueWhenRecent() {
        QuotaThresholdChange change = new QuotaThresholdChange(_75,
            Instant.now(CLOCK).minus(Duration.ofHours(2)));

        assertThat(change.isNotOlderThan(Duration.ofHours(3), CLOCK))
            .isTrue();
    }

    @Test
    public void isNotOlderThanShouldReturnFalseWhenOld() {
        QuotaThresholdChange change = new QuotaThresholdChange(_75,
            Instant.now().minus(Duration.ofHours(2)));

        assertThat(change.isNotOlderThan(Duration.ofHours(1), CLOCK))
            .isFalse();
    }

    @Test
    public void isNotOlderThanShouldReturnTrueWhenExactlyOldOfGracePeriod() {
        QuotaThresholdChange change = new QuotaThresholdChange(_75,
            Instant.now().minus(Duration.ofHours(2)));

        assertThat(change.isNotOlderThan(Duration.ofHours(2), CLOCK))
            .isTrue();
    }

}