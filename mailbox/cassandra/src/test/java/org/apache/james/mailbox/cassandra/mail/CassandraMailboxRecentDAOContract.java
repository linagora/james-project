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


    CassandraMailboxRecentsDAO mailboxRecentsDAO();

    @Test
    default void getRecentMessageUidsInMailboxShouldBeEmptyByDefault() {
        assertThat(mailboxRecentsDAO().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).isEmpty();
    }

    @Test
    default void addToRecentShouldAddUidWhenEmpty() {
        mailboxRecentsDAO().addToRecent(CASSANDRA_ID, UID1).join();

        assertThat(mailboxRecentsDAO().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).containsOnly(UID1);
    }

    @Test
    default void removeFromRecentShouldRemoveUidWhenOnlyOneUid() {
        mailboxRecentsDAO().addToRecent(CASSANDRA_ID, UID1).join();

        mailboxRecentsDAO().removeFromRecent(CASSANDRA_ID, UID1).join();

        assertThat(mailboxRecentsDAO().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).isEmpty();
    }

    @Test
    default void removeFromRecentShouldNotFailIfNotExisting() {
        mailboxRecentsDAO().removeFromRecent(CASSANDRA_ID, UID1).join();

        assertThat(mailboxRecentsDAO().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).isEmpty();
    }

    @Test
    default void addToRecentShouldAddUidWhenNotEmpty() {
        mailboxRecentsDAO().addToRecent(CASSANDRA_ID, UID1).join();

        mailboxRecentsDAO().addToRecent(CASSANDRA_ID, UID2).join();

        assertThat(mailboxRecentsDAO().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).containsOnly(UID1, UID2);
    }

    @Test
    default void removeFromRecentShouldOnlyRemoveUidWhenNotEmpty() {
        mailboxRecentsDAO().addToRecent(CASSANDRA_ID, UID1).join();
        mailboxRecentsDAO().addToRecent(CASSANDRA_ID, UID2).join();

        mailboxRecentsDAO().removeFromRecent(CASSANDRA_ID, UID2).join();

        assertThat(mailboxRecentsDAO().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).containsOnly(UID1);
    }

    @Test
    default void addToRecentShouldBeIdempotent() {
        mailboxRecentsDAO().addToRecent(CASSANDRA_ID, UID1).join();
        mailboxRecentsDAO().addToRecent(CASSANDRA_ID, UID1).join();

        assertThat(mailboxRecentsDAO().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).containsOnly(UID1);
    }

    @Test
    default void getRecentMessageUidsInMailboxShouldNotTimeoutWhenOverPagingLimit() {
        int pageSize = 5000;
        int size = pageSize + 1000;
        IntStream.range(0, size)
            .parallel()
            .forEach(i -> mailboxRecentsDAO().addToRecent(CASSANDRA_ID, MessageUid.of(i + 1)).join());

        assertThat(mailboxRecentsDAO().getRecentMessageUidsInMailbox(CASSANDRA_ID).join()
            .collect(Guavate.toImmutableList())).hasSize(size);
    }
}
