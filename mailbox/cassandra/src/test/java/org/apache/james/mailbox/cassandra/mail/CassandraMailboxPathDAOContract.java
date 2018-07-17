/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
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

import java.util.List;

import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.model.MailboxPath;
import org.junit.jupiter.api.Test;

import com.github.steveash.guavate.Guavate;

import nl.jqno.equalsverifier.EqualsVerifier;

public interface CassandraMailboxPathDAOContract {
    String USER = "user";
    String OTHER_USER = "other";
    CassandraId INBOX_ID = CassandraId.timeBased();
    CassandraId OUTBOX_ID = CassandraId.timeBased();
    CassandraId otherMailboxId = CassandraId.timeBased();

    MailboxPath USER_INBOX_MAILBOXPATH = MailboxPath.forUser(USER, "INBOX");
    CassandraIdAndPath INBOX_ID_AND_PATH = new CassandraIdAndPath(INBOX_ID, USER_INBOX_MAILBOXPATH);
    MailboxPath USER_OUTBOX_MAILBOXPATH = MailboxPath.forUser(USER, "OUTBOX");
    MailboxPath OTHER_USER_MAILBOXPATH = MailboxPath.forUser(OTHER_USER, "INBOX");

    CassandraMailboxPathDAO testee();

    @Test
    default void cassandraIdAndPathShouldRespectBeanContract() {
        EqualsVerifier.forClass(CassandraIdAndPath.class).verify();
    }

    @Test
    default void saveShouldInsertNewEntry() {
        assertThat(testee().save(USER_INBOX_MAILBOXPATH, INBOX_ID).join()).isTrue();

        assertThat(testee().retrieveId(USER_INBOX_MAILBOXPATH).join())
            .contains(INBOX_ID_AND_PATH);
    }

    @Test
    default void saveOnSecondShouldBeFalse() {
        assertThat(testee().save(USER_INBOX_MAILBOXPATH, INBOX_ID).join()).isTrue();
        assertThat(testee().save(USER_INBOX_MAILBOXPATH, INBOX_ID).join()).isFalse();
    }

    @Test
    default void retrieveIdShouldReturnEmptyWhenEmptyData() {
        assertThat(testee().retrieveId(USER_INBOX_MAILBOXPATH).join()
            .isPresent())
            .isFalse();
    }

    @Test
    default void retrieveIdShouldReturnStoredData() {
        testee().save(USER_INBOX_MAILBOXPATH, INBOX_ID).join();

        assertThat(testee().retrieveId(USER_INBOX_MAILBOXPATH).join())
            .contains(INBOX_ID_AND_PATH);
    }

    @Test
    default void getUserMailboxesShouldReturnAllMailboxesOfUser() {
        testee().save(USER_INBOX_MAILBOXPATH, INBOX_ID).join();
        testee().save(USER_OUTBOX_MAILBOXPATH, OUTBOX_ID).join();
        testee().save(OTHER_USER_MAILBOXPATH, otherMailboxId).join();

        List<CassandraIdAndPath> cassandraIds = testee()
            .listUserMailboxes(USER_INBOX_MAILBOXPATH.getNamespace(), USER_INBOX_MAILBOXPATH.getUser())
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(cassandraIds)
            .hasSize(2)
            .containsOnly(INBOX_ID_AND_PATH, new CassandraIdAndPath(OUTBOX_ID, USER_OUTBOX_MAILBOXPATH));
    }

    @Test
    default void deleteShouldNotThrowWhenEmpty() {
        testee().delete(USER_INBOX_MAILBOXPATH).join();
    }

    @Test
    default void deleteShouldDeleteTheExistingMailboxId() {
        testee().save(USER_INBOX_MAILBOXPATH, INBOX_ID).join();

        testee().delete(USER_INBOX_MAILBOXPATH).join();

        assertThat(testee().retrieveId(USER_INBOX_MAILBOXPATH).join())
            .isEmpty();
    }
}