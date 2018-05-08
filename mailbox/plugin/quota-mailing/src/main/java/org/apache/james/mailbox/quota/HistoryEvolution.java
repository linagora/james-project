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

import java.util.Objects;
import java.util.Optional;

import org.apache.james.mailbox.quota.model.QuotaThreshold;

public class HistoryEvolution {

    public static HistoryEvolution noChanges(QuotaThreshold currentThreshold) {
        return new HistoryEvolution(ThresholdHistoryChange.NoChange,
            Optional.empty(),
            currentThreshold);
    }

    public static HistoryEvolution lowerThresholdReached(QuotaThreshold currentThreshold) {
        return new HistoryEvolution(ThresholdHistoryChange.LowerThresholdReached,
            Optional.empty(),
            currentThreshold);
    }

    public static HistoryEvolution higherThresholdReached(QuotaThreshold currentThreshold, HighestThresholdRecentness recentness) {
        return new HistoryEvolution(ThresholdHistoryChange.HigherThresholdReached,
            Optional.of(recentness),
            currentThreshold);
    }

    public enum ThresholdHistoryChange {
        HigherThresholdReached,
        NoChange,
        LowerThresholdReached
    }

    public enum HighestThresholdRecentness {
        AlreadyReachedDuringGracePriod,
        NotAlreadyReachedDuringGracePeriod
    }

    private final ThresholdHistoryChange thresholdHistoryChange;
    private final Optional<HighestThresholdRecentness> recentness;
    private final QuotaThreshold currentThreshold;

    private HistoryEvolution(ThresholdHistoryChange thresholdHistoryChange, Optional<HighestThresholdRecentness> recentness, QuotaThreshold currentThreshold) {
        this.thresholdHistoryChange = thresholdHistoryChange;
        this.recentness = recentness;
        this.currentThreshold = currentThreshold;
    }

    public boolean isChange() {
        return thresholdHistoryChange != ThresholdHistoryChange.NoChange;
    }

    public boolean currentThresholdNotRecentlyReached() {
        return recentness
            .map(value -> value == HighestThresholdRecentness.NotAlreadyReachedDuringGracePeriod)
            .orElse(false);
    }

    public QuotaThreshold getThreshold() {
        return currentThreshold;
    }

    public ThresholdHistoryChange getThresholdHistoryChange() {
        return thresholdHistoryChange;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof HistoryEvolution) {
            HistoryEvolution that = (HistoryEvolution) o;

            return Objects.equals(this.thresholdHistoryChange, that.thresholdHistoryChange)
                && Objects.equals(this.recentness, that.recentness)
                && Objects.equals(this.currentThreshold, that.currentThreshold);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(thresholdHistoryChange, recentness, currentThreshold);
    }


}
