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

package org.apache.james.mailbox.cassandra.mail;

import static org.assertj.core.api.Assertions.assertThat;

import javax.mail.Flags;

import org.apache.james.mailbox.FlagsBuilder;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;

public interface CassandraApplicableFlagDAOContract {

    String USER_FLAG = "User Flag";
    String USER_FLAG2 = "User Flag 2";
    CassandraId CASSANDRA_ID = CassandraId.timeBased();

    CassandraApplicableFlagDAO applicableFlagsDAO();

    @Test
    default void updateApplicableFlagsShouldReturnEmptyByDefault() {
        assertThat(applicableFlagsDAO().retrieveApplicableFlag(CASSANDRA_ID).join())
            .isEmpty();
    }

    @Test
    default void updateApplicableFlagsShouldSupportEmptyUserFlags() {
        applicableFlagsDAO().updateApplicableFlags(CASSANDRA_ID, ImmutableSet.of()).join();

        assertThat(applicableFlagsDAO().retrieveApplicableFlag(CASSANDRA_ID).join())
            .isEmpty();
    }

    @Test
    default void updateApplicableFlagsShouldUpdateUserFlag() {
        applicableFlagsDAO().updateApplicableFlags(CASSANDRA_ID, ImmutableSet.of(USER_FLAG)).join();

        assertThat(applicableFlagsDAO().retrieveApplicableFlag(CASSANDRA_ID).join())
            .contains(new Flags(USER_FLAG));
    }

    @Test
    default void updateApplicableFlagsShouldUnionUserFlags() {
        applicableFlagsDAO().updateApplicableFlags(CASSANDRA_ID, ImmutableSet.of(USER_FLAG)).join();
        applicableFlagsDAO().updateApplicableFlags(CASSANDRA_ID, ImmutableSet.of(USER_FLAG2)).join();

        assertThat(applicableFlagsDAO().retrieveApplicableFlag(CASSANDRA_ID).join())
            .contains(FlagsBuilder.builder().add(USER_FLAG, USER_FLAG2).build());
    }

    @Test
    default void updateApplicableFlagsShouldBeIdempotent() {
        applicableFlagsDAO().updateApplicableFlags(CASSANDRA_ID, ImmutableSet.of(USER_FLAG)).join();
        applicableFlagsDAO().updateApplicableFlags(CASSANDRA_ID, ImmutableSet.of(USER_FLAG)).join();

        assertThat(applicableFlagsDAO().retrieveApplicableFlag(CASSANDRA_ID).join())
            .contains(new Flags(USER_FLAG));
    }

    @Test
    default void updateApplicableFlagsShouldSkipAlreadyStoredFlagsWhenAddingFlag() {
        applicableFlagsDAO().updateApplicableFlags(CASSANDRA_ID, ImmutableSet.of(USER_FLAG)).join();
        applicableFlagsDAO().updateApplicableFlags(CASSANDRA_ID, ImmutableSet.of(USER_FLAG, USER_FLAG2)).join();

        assertThat(applicableFlagsDAO().retrieveApplicableFlag(CASSANDRA_ID).join())
            .contains(FlagsBuilder.builder().add(USER_FLAG, USER_FLAG2).build());
    }

    @Test
    default void updateApplicableFlagsShouldUpdateMultiFlags() {
        applicableFlagsDAO().updateApplicableFlags(CASSANDRA_ID, ImmutableSet.of(USER_FLAG, USER_FLAG2)).join();

        assertThat(applicableFlagsDAO().retrieveApplicableFlag(CASSANDRA_ID).join())
            .contains(FlagsBuilder.builder().add(USER_FLAG, USER_FLAG2).build());
    }

}