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

import org.apache.commons.configuration.Configuration;

import com.google.common.base.Preconditions;

public class CassandraMailQueueViewConfiguration {
    interface Builder {
        @FunctionalInterface
        interface RequireBucketCount {
            RequireUpdateBrowseStartPace bucketCount(int bucketCount);
        }

        @FunctionalInterface
        interface RequireUpdateBrowseStartPace {
            RequireSliceWindow updateBrowseStartPace(int updateBrowseStartPace);
        }

        @FunctionalInterface
        interface RequireSliceWindow {
            ReadyToBuild sliceWindow(Duration sliceWindow);
        }

        class ReadyToBuild {
            private final int bucketCount;
            private final int updateBrowseStartPace;
            private final Duration sliceWindow;

            private ReadyToBuild(int bucketCount, int updateBrowseStartPace, Duration sliceWindow) {
                this.bucketCount = bucketCount;
                this.updateBrowseStartPace = updateBrowseStartPace;
                this.sliceWindow = sliceWindow;
            }

            public CassandraMailQueueViewConfiguration build() {
                Preconditions.checkNotNull(sliceWindow, "'sliceWindow' is compulsory");
                Preconditions.checkState(bucketCount > 0, "'bucketCount' needs to be a strictly positive integer");
                Preconditions.checkState(updateBrowseStartPace > 0, "'updateBrowseStartPace' needs to be a strictly positive integer");

                return new CassandraMailQueueViewConfiguration(bucketCount, updateBrowseStartPace, sliceWindow);
            }
        }
    }

    public static Builder.RequireBucketCount builder() {
        return bucketCount -> updateBrowseStartPace -> sliceWindow -> new Builder.ReadyToBuild(bucketCount, updateBrowseStartPace, sliceWindow);
    }

    public static CassandraMailQueueViewConfiguration from(Configuration configuration) {
        long sliceWindowInSecond = configuration.getLong(SLICE_WINDOW_PROPERTY_NAME);
        Preconditions.checkState(sliceWindowInSecond > 1, "slice window is a duration, then have to be positive");

        int bucketCount = configuration.getInt(BUCKET_COUNT_PROPERTY_NAME);
        Preconditions.checkState(bucketCount > 1, "bucket count have to be positive");

        int updateBrowseStartPace = configuration.getInt(UPDATE_BROWSE_START_PACE_PROPERTY_NAME);
        Preconditions.checkState(updateBrowseStartPace > 1, "update browse start pace have to be positive");

        return builder()
            .bucketCount(bucketCount)
            .updateBrowseStartPace(updateBrowseStartPace)
            .sliceWindow(Duration.ofSeconds(sliceWindowInSecond))
            .build();
    }

    public static final CassandraMailQueueViewConfiguration DEFAULT = builder()
        .bucketCount(1)
        .updateBrowseStartPace(1000)
        .sliceWindow(Duration.ofHours(1))
        .build();

    private static final String SLICE_WINDOW_PROPERTY_NAME = "sliceWindow";
    private static final String BUCKET_COUNT_PROPERTY_NAME = "bucketCount";
    private static final String UPDATE_BROWSE_START_PACE_PROPERTY_NAME = "updateBrowseStartPace";

    private final int bucketCount;
    private final int updateBrowseStartPace;
    private final Duration sliceWindow;

    private CassandraMailQueueViewConfiguration(int bucketCount,
                                                int updateBrowseStartPace,
                                                Duration sliceWindow) {
        this.bucketCount = bucketCount;
        this.updateBrowseStartPace = updateBrowseStartPace;
        this.sliceWindow = sliceWindow;
    }

    public int getUpdateBrowseStartPace() {
        return updateBrowseStartPace;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public Duration getSliceWindow() {
        return sliceWindow;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof CassandraMailQueueViewConfiguration) {
            CassandraMailQueueViewConfiguration that = (CassandraMailQueueViewConfiguration) o;

            return Objects.equals(this.bucketCount, that.bucketCount)
                && Objects.equals(this.updateBrowseStartPace, that.updateBrowseStartPace)
                && Objects.equals(this.sliceWindow, that.sliceWindow);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(bucketCount, updateBrowseStartPace, sliceWindow);
    }
}
