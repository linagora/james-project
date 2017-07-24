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

import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.apache.james.mailbox.cassandra.mail.migration.MigrationTracking.State;
import org.apache.james.util.FluentFutureStream;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationTrackingTest {

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private MigrationTracking migrationTracking;

    @Before
    public void setUp() {
        migrationTracking = new MigrationTracking();
    }

    @Test
    public void setMigrationTrackingConstructorShouldInitWith0ErrorAndSuccess() {
        State state = migrationTracking.getState();
        softly.assertThat(state.getNumOfSuccess()).isZero();
        softly.assertThat(state.getNumOfError()).isZero();
    }

    @Test
    public void reportSuccessShouldReturnNewState() {
        State state = migrationTracking.reportSuccess();

        softly.assertThat(state.getNumOfSuccess()).isEqualTo(1);
        softly.assertThat(state.getNumOfError()).isZero();
    }

    @Test
    public void reportSuccessShouldAffectState() {
        migrationTracking.reportSuccess();

        State state = migrationTracking.getState();

        softly.assertThat(state.getNumOfSuccess()).isEqualTo(1);
        softly.assertThat(state.getNumOfError()).isZero();
    }

    @Test
    public void reportErrorShouldReturnNewState() {
        State state = migrationTracking.reportError();

        softly.assertThat(state.getNumOfError()).isEqualTo(1);
        softly.assertThat(state.getNumOfSuccess()).isZero();
    }

    @Test
    public void reportErrorShouldAffectState() {
        migrationTracking.reportError();

        State state = migrationTracking.getState();

        softly.assertThat(state.getNumOfError()).isEqualTo(1);
        softly.assertThat(state.getNumOfSuccess()).isZero();
    }

    @Test
    public void reportErrorShouldBeThreadSafe() {
        int expected = 42;
        runInParrallel(42, migrationTracking::reportError);

        State state = migrationTracking.getState();
        assertThat(state.getNumOfError()).isEqualTo(expected);
    }

    @Test
    public void reportSuccessShouldBeThreadSafe() {
        int expected = 42;
        runInParrallel(42, migrationTracking::reportSuccess);

        State state = migrationTracking.getState();
        assertThat(state.getNumOfSuccess()).isEqualTo(expected);
    }

    private void runInParrallel(int numOfTime, Runnable runnable) {
        FluentFutureStream
            .of(
                IntStream
                    .range(0, numOfTime)
                    .parallel()
                    .mapToObj(i -> CompletableFuture.runAsync(runnable)))
            .join();
    }
}