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

import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.model.MailboxCounters;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.junit.jupiter.api.Test;

public interface CassandraMailboxCounterDAOContract {
    int UID_VALIDITY = 15;
    CassandraId MAILBOX_ID = CassandraId.timeBased();
    SimpleMailbox mailbox = new SimpleMailbox(MailboxPath.forUser("user", "name"), UID_VALIDITY, MAILBOX_ID);

    CassandraMailboxCounterDAO testee();

    @Test
    default void countMessagesInMailboxShouldReturnEmptyByDefault() throws Exception {
        assertThat(testee().countMessagesInMailbox(mailbox).join()).isEmpty();
    }

    @Test
    default void countUnseenMessagesInMailboxShouldReturnEmptyByDefault() throws Exception {
        assertThat(testee().countUnseenMessagesInMailbox(mailbox).join()).isEmpty();
    }

    @Test
    default void retrieveMailboxCounterShouldReturnEmptyByDefault() throws Exception {
        assertThat(testee().retrieveMailboxCounters(mailbox).join()).isEmpty();
    }

    @Test
    default void incrementCountShouldAddOneWhenAbsent() throws Exception {
        testee().incrementCount(MAILBOX_ID).join();

        assertThat(testee().countMessagesInMailbox(mailbox).join()).contains(1L);
    }

    @Test
    default void incrementUnseenShouldAddOneWhenAbsent() throws Exception {
        testee().incrementUnseen(MAILBOX_ID).join();

        assertThat(testee().countUnseenMessagesInMailbox(mailbox).join()).contains(1L);
    }

    @Test
    default void incrementUnseenShouldAddOneWhenAbsentOnMailboxCounters() throws Exception {
        testee().incrementUnseen(MAILBOX_ID).join();

        assertThat(testee().retrieveMailboxCounters(mailbox).join())
            .contains(MailboxCounters.builder()
                .count(0L)
                .unseen(1L)
                .build());
    }

    @Test
    default void incrementCountShouldAddOneWhenAbsentOnMailboxCounters() throws Exception {
        testee().incrementCount(MAILBOX_ID).join();

        assertThat(testee().retrieveMailboxCounters(mailbox).join())
            .contains(MailboxCounters.builder()
                .count(1L)
                .unseen(0L)
                .build());
    }

    @Test
    default void retrieveMailboxCounterShouldWorkWhenFullRow() throws Exception {
        testee().incrementCount(MAILBOX_ID).join();
        testee().incrementUnseen(MAILBOX_ID).join();

        assertThat(testee().retrieveMailboxCounters(mailbox).join())
            .contains(MailboxCounters.builder()
                .count(1L)
                .unseen(1L)
                .build());
    }

    @Test
    default void decrementCountShouldRemoveOne() throws Exception {
        testee().incrementCount(MAILBOX_ID).join();

        testee().decrementCount(MAILBOX_ID).join();

        assertThat(testee().countMessagesInMailbox(mailbox).join())
            .contains(0L);
    }

    @Test
    default void decrementUnseenShouldRemoveOne() throws Exception {
        testee().incrementUnseen(MAILBOX_ID).join();

        testee().decrementUnseen(MAILBOX_ID).join();

        assertThat(testee().countUnseenMessagesInMailbox(mailbox).join())
            .contains(0L);
    }

    @Test
    default void incrementUnseenShouldHaveNoImpactOnMessageCount() throws Exception {
        testee().incrementUnseen(MAILBOX_ID).join();

        assertThat(testee().countMessagesInMailbox(mailbox).join())
            .contains(0L);
    }

    @Test
    default void incrementCountShouldHaveNoEffectOnUnseenCount() throws Exception {
        testee().incrementCount(MAILBOX_ID).join();

        assertThat(testee().countUnseenMessagesInMailbox(mailbox).join())
            .contains(0L);
    }

    @Test
    default void decrementUnseenShouldHaveNoEffectOnMessageCount() throws Exception {
        testee().incrementCount(MAILBOX_ID).join();

        testee().decrementUnseen(MAILBOX_ID).join();

        assertThat(testee().countMessagesInMailbox(mailbox).join())
            .contains(1L);
    }

    @Test
    default void decrementCountShouldHaveNoEffectOnUnseenCount() throws Exception {
        testee().incrementUnseen(MAILBOX_ID).join();

        testee().decrementCount(MAILBOX_ID).join();

        assertThat(testee().countUnseenMessagesInMailbox(mailbox).join())
            .contains(1L);
    }

    @Test
    default void decrementCountCanLeadToNegativeValue() throws Exception {
        testee().decrementCount(MAILBOX_ID).join();

        assertThat(testee().countMessagesInMailbox(mailbox).join())
            .contains(-1L);
    }

    @Test
    default void decrementUnseenCanLeadToNegativeValue() throws Exception {
        testee().decrementUnseen(MAILBOX_ID).join();

        assertThat(testee().countUnseenMessagesInMailbox(mailbox).join())
            .contains(-1L);
    }
}
