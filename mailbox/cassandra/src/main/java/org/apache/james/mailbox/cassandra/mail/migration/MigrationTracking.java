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

package org.apache.james.mailbox.cassandra.mail.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class MigrationTracking {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationTracking.class);

    public static class State {

        private final int numOfSuccess;
        private final int numOfError;

        private State(int numOfSuccess, int numOfError) {
            this.numOfSuccess = numOfSuccess;
            this.numOfError = numOfError;
        }

        public int getNumOfSuccess() {
            return numOfSuccess;
        }

        public int getNumOfError() {
            return numOfError;
        }
    }

    public final AtomicInteger numOfSuccess;
    public final AtomicInteger numOfError;


    public MigrationTracking() {
        this.numOfSuccess = new AtomicInteger(0);
        this.numOfError = new AtomicInteger(0);
    }

    public State reportSuccess() {
        int success = numOfSuccess.incrementAndGet();
        logProcess();

        return new State(success, numOfError.get());
    }

    public State reportError() {
        int error = numOfError.incrementAndGet();
        logProcess();

        return new State(numOfSuccess.get(), error);
    }

    public State getState() {
        return new State(numOfSuccess.get(), numOfError.get());
    }

    private void logProcess() {
        State state = getState();
        LOGGER.info("Since application start, {} messages has been migrated and {} migration messages has failed",
            state.getNumOfSuccess(),
            state.getNumOfError());
    }
}
