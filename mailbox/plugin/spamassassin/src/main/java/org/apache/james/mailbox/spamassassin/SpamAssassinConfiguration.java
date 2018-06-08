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

package org.apache.james.mailbox.spamassassin;

import java.util.Objects;
import java.util.Optional;

import org.apache.james.util.Host;

import com.google.common.base.MoreObjects;

public class SpamAssassinConfiguration {

    public static class Builder {
        public static final boolean DEFAULT_ASYNCHRONOUS = true;
        public static final int DEFAULT_THREAD_COUNT = 8;
        private Optional<Host> host;
        private Optional<Boolean> isAsynchronous;
        private Optional<Integer> threadCount;

        public Builder() {
            this.host = Optional.empty();
            this.isAsynchronous = Optional.empty();
            this.threadCount = Optional.empty();
        }

        public Builder host(Host host) {
            this.host = Optional.of(host);
            return this;
        }

        public Builder host(Optional<Host> host) {
            host.ifPresent(this::host);
            return this;
        }

        public Builder isAsynchronous(Boolean isAsynchronous) {
            this.isAsynchronous = Optional.of(isAsynchronous);
            return this;
        }

        public Builder isAsynchronous(Optional<Boolean> isAsynchronous) {
            isAsynchronous.ifPresent(this::isAsynchronous);
            return this;
        }

        public Builder threadCount(int threadCount) {
            this.threadCount = Optional.of(threadCount);
            return this;
        }

        public Builder threadCount(Optional<Integer> threadCount) {
            threadCount.ifPresent(this::threadCount);
            return this;
        }

        public SpamAssassinConfiguration build() {
            return new SpamAssassinConfiguration(
                host,
                isAsynchronous.orElse(DEFAULT_ASYNCHRONOUS),
                threadCount.orElse(DEFAULT_THREAD_COUNT));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SpamAssassinConfiguration disabled() {
        return builder().build();
    }

    private final Optional<Host> host;
    private final boolean isAsynchronous;
    private final int threadCount;

    public SpamAssassinConfiguration(Optional<Host> host, boolean isAsynchronous, int threadCount) {
        this.host = host;
        this.isAsynchronous = isAsynchronous;
        this.threadCount = threadCount;
    }

    public boolean isEnable() {
        return host.isPresent();
    }

    public Optional<Host> getHost() {
        return host;
    }

    public boolean isAsynchronous() {
        return isAsynchronous;
    }

    public int getThreadCount() {
        return threadCount;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof SpamAssassinConfiguration) {
            SpamAssassinConfiguration that = (SpamAssassinConfiguration) o;

            return Objects.equals(this.host, that.host)
                && Objects.equals(this.isAsynchronous, that.isAsynchronous)
                && Objects.equals(this.threadCount, that.threadCount);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(host, isAsynchronous, threadCount);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("host", host)
                .add("isAsynchronous", isAsynchronous)
                .add("threadCount", threadCount)
                .toString();
    }
}
