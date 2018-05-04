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

package org.apache.james.mailbox.quota;

import static org.apache.james.mailbox.quota.model.QuotaThresholdFixture._75;
import static org.apache.james.mailbox.quota.model.QuotaThresholdFixture._90;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.apache.james.core.User;
import org.apache.james.mailbox.quota.model.QuotaThresholdChange;
import org.apache.james.mailbox.quota.model.QuotaThresholdHistory;
import org.junit.jupiter.api.Test;

public interface QuotaThresholdHistoryStoreTest {

    User USER = User.fromUsername("bob@domain");
    User OTHER_USER = User.fromUsername("alice@domain");

    @Test
    default void retrieveQuotaCountThresholdChangesShouldReturnEmptyByDefault(QuotaThresholdHistoryStore store) {
        assertThat(store.retrieveQuotaCountThresholdChanges(USER))
            .isEqualTo(new QuotaThresholdHistory());
    }

    @Test
    default void retrieveQuotaSizeThresholdChangesShouldReturnEmptyByDefault(QuotaThresholdHistoryStore store) {
        assertThat(store.retrieveQuotaSizeThresholdChanges(USER))
            .isEqualTo(new QuotaThresholdHistory());
    }

    @Test
    default void persistQuotaSizeThresholdChangeShouldAddTheChangeWhenNone(QuotaThresholdHistoryStore store) {
        QuotaThresholdChange change = new QuotaThresholdChange(_75,
            Instant.now().minus(Duration.ofDays(2)));

        store.persistQuotaSizeThresholdChange(USER, change);

        assertThat(store.retrieveQuotaSizeThresholdChanges(USER))
            .isEqualTo(new QuotaThresholdHistory(change));
    }

    @Test
    default void persistQuotaSizeThresholdChangeShouldAddTheChangeWhenAlreadySomeChanges(QuotaThresholdHistoryStore store) {
        QuotaThresholdChange change1 = new QuotaThresholdChange(_75,
            Instant.now().minus(Duration.ofDays(2)));
        QuotaThresholdChange change2 = new QuotaThresholdChange(_90,
            Instant.now().minus(Duration.ofDays(1)));

        store.persistQuotaSizeThresholdChange(USER, change1);
        store.persistQuotaSizeThresholdChange(USER, change2);

        assertThat(store.retrieveQuotaSizeThresholdChanges(USER))
            .isEqualTo(new QuotaThresholdHistory(change1, change2));
    }

    @Test
    default void persistQuotaCountThresholdChangeShouldAddTheChangeWhenNone(QuotaThresholdHistoryStore store) {
        QuotaThresholdChange change = new QuotaThresholdChange(_75,
            Instant.now().minus(Duration.ofDays(2)));

        store.persistQuotaCountThresholdChange(USER, change);

        assertThat(store.retrieveQuotaCountThresholdChanges(USER))
            .isEqualTo(new QuotaThresholdHistory(change));
    }

    @Test
    default void persistQuotaCountThresholdChangeShouldAddTheChangeWhenAlreadySomeChanges(QuotaThresholdHistoryStore store) {
        QuotaThresholdChange change1 = new QuotaThresholdChange(_75,
            Instant.now().minus(Duration.ofDays(2)));
        QuotaThresholdChange change2 = new QuotaThresholdChange(_90,
            Instant.now().minus(Duration.ofDays(1)));

        store.persistQuotaCountThresholdChange(USER, change1);
        store.persistQuotaCountThresholdChange(USER, change2);

        assertThat(store.retrieveQuotaCountThresholdChanges(USER))
            .isEqualTo(new QuotaThresholdHistory(change1, change2));
    }

    @Test
    default void sizeAndCountShouldBeIsolated(QuotaThresholdHistoryStore store) {
        QuotaThresholdChange change = new QuotaThresholdChange(_75,
            Instant.now().minus(Duration.ofDays(2)));

        store.persistQuotaCountThresholdChange(USER, change);

        assertThat(store.retrieveQuotaSizeThresholdChanges(USER))
            .isEqualTo(new QuotaThresholdHistory());
    }

    @Test
    default void usersShouldBeIsolated(QuotaThresholdHistoryStore store) {
        QuotaThresholdChange change = new QuotaThresholdChange(_75,
            Instant.now().minus(Duration.ofDays(2)));

        store.persistQuotaCountThresholdChange(USER, change);

        assertThat(store.retrieveQuotaCountThresholdChanges(OTHER_USER))
            .isEqualTo(new QuotaThresholdHistory());
    }

    @Test
    default void persistShouldReOrderHistory(QuotaThresholdHistoryStore store) {
        QuotaThresholdChange change1 = new QuotaThresholdChange(_75,
            Instant.now().minus(Duration.ofDays(2)));
        QuotaThresholdChange change2 = new QuotaThresholdChange(_90,
            Instant.now().minus(Duration.ofDays(1)));

        store.persistQuotaCountThresholdChange(USER, change2);
        store.persistQuotaCountThresholdChange(USER, change1);

        assertThat(store.retrieveQuotaCountThresholdChanges(USER))
            .isEqualTo(new QuotaThresholdHistory(change1, change2));
    }

}