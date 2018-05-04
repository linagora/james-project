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

package org.apache.james.mailbox.quota.mailing;

import static org.apache.james.mailbox.quota.HistoryEvolution.HighestThresholdRecentness.NotAlreadyReachedDuringGracePeriod;
import static org.apache.james.mailbox.quota.HistoryEvolution.HighestThresholdRecentness.AlreadyReachedDuringGracePriod;
import static org.apache.james.mailbox.quota.model.QuotaThresholdFixture._80;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.quota.HistoryEvolution;
import org.apache.james.mailbox.quota.QuotaCount;
import org.apache.james.mailbox.quota.QuotaSize;
import org.apache.james.mailbox.quota.model.QuotaThreshold;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class QuotaThresholdNoticeTest {

    @Test
    public void shouldMatchBeanContract() {
        EqualsVerifier.forClass(QuotaThresholdNotice.class)
            .allFieldsShouldBeUsed()
            .verify();
    }

    @Test
    public void buildShouldReturnEmptyWhenNoThresholds() {
        assertThat(QuotaThresholdNotice.builder()
            .sizeQuota(Quota.<QuotaSize>builder()
                .used(QuotaSize.size(82))
                .computedLimit(QuotaSize.size(100))
                .build())
            .countQuota(Quota.<QuotaCount>builder()
                .used(QuotaCount.count(82))
                .computedLimit(QuotaCount.count(100))
                .build())
            .build())
            .isEmpty();
    }

    @Test
    public void buildShouldReturnEmptyWhenNoChanges() {
        assertThat(QuotaThresholdNotice.builder()
            .sizeQuota(Quota.<QuotaSize>builder()
                .used(QuotaSize.size(82))
                .computedLimit(QuotaSize.size(100))
                .build())
            .countQuota(Quota.<QuotaCount>builder()
                .used(QuotaCount.count(82))
                .computedLimit(QuotaCount.count(100))
                .build())
            .sizeThreshold(HistoryEvolution.noChanges(_80))
            .build())
            .isEmpty();
    }

    @Test
    public void buildShouldReturnEmptyWhenBelow() {
        assertThat(QuotaThresholdNotice.builder()
            .sizeQuota(Quota.<QuotaSize>builder()
                .used(QuotaSize.size(82))
                .computedLimit(QuotaSize.size(100))
                .build())
            .countQuota(Quota.<QuotaCount>builder()
                .used(QuotaCount.count(82))
                .computedLimit(QuotaCount.count(100))
                .build())
            .sizeThreshold(HistoryEvolution.lowerThresholdReached(_80))
            .build())
            .isEmpty();
    }

    @Test
    public void buildShouldReturnEmptyWhenAboveButRecentChanges() {
        assertThat(QuotaThresholdNotice.builder()
            .sizeQuota(Quota.<QuotaSize>builder()
                .used(QuotaSize.size(82))
                .computedLimit(QuotaSize.size(100))
                .build())
            .countQuota(Quota.<QuotaCount>builder()
                .used(QuotaCount.count(82))
                .computedLimit(QuotaCount.count(100))
                .build())
            .sizeThreshold(HistoryEvolution.higherThresholdReached(_80, AlreadyReachedDuringGracePriod))
            .build())
            .isEmpty();
    }

    @Test
    public void buildShouldReturnPresentWhenAbove() {
        Quota<QuotaSize> sizeQuota = Quota.<QuotaSize>builder()
            .used(QuotaSize.size(82))
            .computedLimit(QuotaSize.size(100))
            .build();
        Quota<QuotaCount> countQuota = Quota.<QuotaCount>builder()
            .used(QuotaCount.count(82))
            .computedLimit(QuotaCount.count(100))
            .build();
        QuotaThreshold sizeThreshold = _80;

        assertThat(QuotaThresholdNotice.builder()
            .sizeQuota(sizeQuota)
            .countQuota(countQuota)
            .sizeThreshold(HistoryEvolution.higherThresholdReached(sizeThreshold, NotAlreadyReachedDuringGracePeriod))
            .build())
            .isNotEmpty()
            .contains(new QuotaThresholdNotice(Optional.empty(), Optional.of(sizeThreshold), sizeQuota, countQuota));
    }

    @Test
    public void buildShouldFilterOutNotInterestingFields() {
        Quota<QuotaSize> sizeQuota = Quota.<QuotaSize>builder()
            .used(QuotaSize.size(82))
            .computedLimit(QuotaSize.size(100))
            .build();
        Quota<QuotaCount> countQuota = Quota.<QuotaCount>builder()
            .used(QuotaCount.count(82))
            .computedLimit(QuotaCount.count(100))
            .build();
        QuotaThreshold sizeThreshold = _80;
        QuotaThreshold countThreshold = _80;

        assertThat(QuotaThresholdNotice.builder()
            .sizeQuota(sizeQuota)
            .countQuota(countQuota)
            .sizeThreshold(HistoryEvolution.higherThresholdReached(sizeThreshold, NotAlreadyReachedDuringGracePeriod))
            .countThreshold(HistoryEvolution.lowerThresholdReached(countThreshold))
            .build())
            .isNotEmpty()
            .contains(new QuotaThresholdNotice(Optional.empty(), Optional.of(sizeThreshold), sizeQuota, countQuota));
    }

    @Test
    public void buildShouldKeepAllInterestingFields() {
        Quota<QuotaSize> sizeQuota = Quota.<QuotaSize>builder()
            .used(QuotaSize.size(82))
            .computedLimit(QuotaSize.size(100))
            .build();
        Quota<QuotaCount> countQuota = Quota.<QuotaCount>builder()
            .used(QuotaCount.count(82))
            .computedLimit(QuotaCount.count(100))
            .build();
        QuotaThreshold sizeThreshold = _80;
        QuotaThreshold countThreshold = _80;

        assertThat(QuotaThresholdNotice.builder()
            .sizeQuota(sizeQuota)
            .countQuota(countQuota)
            .sizeThreshold(HistoryEvolution.higherThresholdReached(sizeThreshold, NotAlreadyReachedDuringGracePeriod))
            .countThreshold(HistoryEvolution.higherThresholdReached(countThreshold, NotAlreadyReachedDuringGracePeriod))
            .build())
            .isNotEmpty()
            .contains(new QuotaThresholdNotice(Optional.of(countThreshold), Optional.of(sizeThreshold), sizeQuota, countQuota));
    }

    @Test
    public void generateReportShouldGenerateAHumanReadableMessage() {
        Quota<QuotaSize> sizeQuota = Quota.<QuotaSize>builder()
            .used(QuotaSize.size(82))
            .computedLimit(QuotaSize.size(100))
            .build();
        Quota<QuotaCount> countQuota = Quota.<QuotaCount>builder()
            .used(QuotaCount.count(92))
            .computedLimit(QuotaCount.count(100))
            .build();
        QuotaThreshold sizeThreshold = _80;
        QuotaThreshold countThreshold = _80;

        assertThat(QuotaThresholdNotice.builder()
            .sizeQuota(sizeQuota)
            .countQuota(countQuota)
            .sizeThreshold(HistoryEvolution.higherThresholdReached(sizeThreshold, NotAlreadyReachedDuringGracePeriod))
            .countThreshold(HistoryEvolution.higherThresholdReached(countThreshold, NotAlreadyReachedDuringGracePeriod))
            .build()
            .get()
            .generateReport())
            .isEqualTo("You receive this email because you recently exceeded a threshold related to the quotas of your email account.\n" +
                "\n" +
                "You currently occupy more than 80 % of the total size allocated to you.\n" +
                "You currently occupy 82 bytes on a total of 100 bytes allocated to you.\n" +
                "\n" +
                "You currently occupy more than 80 % of the total message count allocated to you.\n" +
                "You currently have 92 messages on a total of 100 allowed for you.\n" +
                "\n" +
                "You need to be aware that actions leading to exceeded quotas will be denied. This will result in a degraded service.\n" +
                "To mitigate this issue you might reach your administrator in order to increase your configured quota. You might also delete some non important emails.");
    }

    @Test
    public void generateReportShouldOmitCountPartWhenNone() {
        Quota<QuotaSize> sizeQuota = Quota.<QuotaSize>builder()
            .used(QuotaSize.size(82))
            .computedLimit(QuotaSize.size(100))
            .build();
        Quota<QuotaCount> countQuota = Quota.<QuotaCount>builder()
            .used(QuotaCount.count(72))
            .computedLimit(QuotaCount.count(100))
            .build();
        QuotaThreshold sizeThreshold = _80;

        assertThat(QuotaThresholdNotice.builder()
            .sizeQuota(sizeQuota)
            .countQuota(countQuota)
            .sizeThreshold(HistoryEvolution.higherThresholdReached(sizeThreshold, NotAlreadyReachedDuringGracePeriod))
            .build()
            .get()
            .generateReport())
            .isEqualTo("You receive this email because you recently exceeded a threshold related to the quotas of your email account.\n" +
                "\n" +
                "You currently occupy more than 80 % of the total size allocated to you.\n" +
                "You currently occupy 82 bytes on a total of 100 bytes allocated to you.\n" +
                "\n" +
                "You need to be aware that actions leading to exceeded quotas will be denied. This will result in a degraded service.\n" +
                "To mitigate this issue you might reach your administrator in order to increase your configured quota. You might also delete some non important emails.");
    }

    @Test
    public void generateReportShouldOmitSizePartWhenNone() {
        Quota<QuotaSize> sizeQuota = Quota.<QuotaSize>builder()
            .used(QuotaSize.size(82))
            .computedLimit(QuotaSize.size(100))
            .build();
        Quota<QuotaCount> countQuota = Quota.<QuotaCount>builder()
            .used(QuotaCount.count(92))
            .computedLimit(QuotaCount.count(100))
            .build();
        QuotaThreshold countThreshold = _80;

        assertThat(QuotaThresholdNotice.builder()
            .sizeQuota(sizeQuota)
            .countQuota(countQuota)
            .countThreshold(HistoryEvolution.higherThresholdReached(countThreshold, NotAlreadyReachedDuringGracePeriod))
            .build()
            .get()
            .generateReport())
            .isEqualTo("You receive this email because you recently exceeded a threshold related to the quotas of your email account.\n" +
                "\n" +
                "You currently occupy more than 80 % of the total message count allocated to you.\n" +
                "You currently have 92 messages on a total of 100 allowed for you.\n" +
                "\n" +
                "You need to be aware that actions leading to exceeded quotas will be denied. This will result in a degraded service.\n" +
                "To mitigate this issue you might reach your administrator in order to increase your configured quota. You might also delete some non important emails.");
    }

}