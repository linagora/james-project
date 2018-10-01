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

package org.apache.james.queue.rabbitmq.view.cassandra.model;

import java.util.Objects;

import org.apache.james.queue.rabbitmq.EnqueuedItem;

public class EnqueuedItemWithSlicingContext {
    public interface Builder {

        @FunctionalInterface
        interface RequireEnqueuedItem {
            RequireSlicingContext enqueuedItem(EnqueuedItem enqueuedItem);
        }

        @FunctionalInterface
        interface RequireSlicingContext {
            LastStage slicingContext(BucketedSlice slicingContext);
        }

        class LastStage {
            private final EnqueuedItem enqueuedItem;
            private final BucketedSlice slicingContext;

            public LastStage(EnqueuedItem enqueuedItem, BucketedSlice slicingContext) {
                this.enqueuedItem = enqueuedItem;
                this.slicingContext = slicingContext;
            }

            public EnqueuedItemWithSlicingContext build() {
                return new EnqueuedItemWithSlicingContext(enqueuedItem, slicingContext);
            }
        }
    }

    public static Builder.RequireEnqueuedItem builder() {
        return enqueuedItem -> slicingContext -> new Builder.LastStage(enqueuedItem, slicingContext);
    }

    private final EnqueuedItem enqueuedItem;
    private final BucketedSlice slicingContext;

    private EnqueuedItemWithSlicingContext(EnqueuedItem enqueuedItem, BucketedSlice slicingContext) {
        this.enqueuedItem = enqueuedItem;
        this.slicingContext = slicingContext;
    }

    public BucketedSlice getSlicingContext() {
        return slicingContext;
    }

    public EnqueuedItem getEnqueuedItem() {
        return enqueuedItem;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof EnqueuedItemWithSlicingContext) {
            EnqueuedItemWithSlicingContext that = (EnqueuedItemWithSlicingContext) o;

            return Objects.equals(this.slicingContext, that.slicingContext)
                    && Objects.equals(this.enqueuedItem, that.enqueuedItem);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(slicingContext, enqueuedItem);
    }
}
