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

import java.util.stream.IntStream;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.junit.jupiter.api.Test;

import com.github.steveash.guavate.Guavate;

public interface CassandraMailboxRecentDAOContract {
    MessageUid UID1 = MessageUid.of(36L);
    MessageUid UID2 = MessageUid.of(37L);
    CassandraId CASSANDRA_ID = CassandraId.timeBased();


    CassandraMailboxRecentsDAO testee();

    @Test
    default void getRecentMessageUidsInMailboxShouldBeEmptyByDefault() {
        assertThat(testee().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).isEmpty();
    }

    @Test
    default void addToRecentShouldAddUidWhenEmpty() {
        testee().addToRecent(CASSANDRA_ID, UID1).join();

        assertThat(testee().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).containsOnly(UID1);
    }

    @Test
    default void removeFromRecentShouldRemoveUidWhenOnlyOneUid() {
        testee().addToRecent(CASSANDRA_ID, UID1).join();

        testee().removeFromRecent(CASSANDRA_ID, UID1).join();

        assertThat(testee().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).isEmpty();
    }

    @Test
    default void removeFromRecentShouldNotFailIfNotExisting() {
        testee().removeFromRecent(CASSANDRA_ID, UID1).join();

        assertThat(testee().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).isEmpty();
    }

    @Test
    default void addToRecentShouldAddUidWhenNotEmpty() {
        testee().addToRecent(CASSANDRA_ID, UID1).join();

        testee().addToRecent(CASSANDRA_ID, UID2).join();

        assertThat(testee().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).containsOnly(UID1, UID2);
    }

    @Test
    default void removeFromRecentShouldOnlyRemoveUidWhenNotEmpty() {
        testee().addToRecent(CASSANDRA_ID, UID1).join();
        testee().addToRecent(CASSANDRA_ID, UID2).join();

        testee().removeFromRecent(CASSANDRA_ID, UID2).join();

        assertThat(testee().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).containsOnly(UID1);
    }

    @Test
    default void addToRecentShouldBeIdempotent() {
        testee().addToRecent(CASSANDRA_ID, UID1).join();
        testee().addToRecent(CASSANDRA_ID, UID1).join();

        assertThat(testee().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).containsOnly(UID1);
    }

    @Test
    default void getRecentMessageUidsInMailboxShouldNotTimeoutWhenOverPagingLimit() {
        int pageSize = 5000;
        int size = pageSize + 1000;
        IntStream.range(0, size)
            .parallel()
            .forEach(i -> testee().addToRecent(CASSANDRA_ID, MessageUid.of(i + 1)).join());

        assertThat(testee().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).hasSize(size);
    }
}
