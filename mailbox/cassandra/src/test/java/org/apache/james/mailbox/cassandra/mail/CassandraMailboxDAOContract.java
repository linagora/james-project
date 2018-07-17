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

import java.util.List;
import java.util.Optional;

import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.junit.jupiter.api.Test;

import com.github.steveash.guavate.Guavate;

public interface CassandraMailboxDAOContract {

    int UID_VALIDITY_1 = 145;
    int UID_VALIDITY_2 = 147;
    MailboxPath NEW_MAILBOX_PATH = MailboxPath.forUser("user", "xyz");
    CassandraId CASSANDRA_ID_1 = CassandraId.timeBased();
    CassandraId CASSANDRA_ID_2 = CassandraId.timeBased();
    SimpleMailbox mailbox1 = new SimpleMailbox(MailboxPath.forUser("user", "abcd"),
        UID_VALIDITY_1,
        CASSANDRA_ID_1);
    SimpleMailbox mailbox2 = new SimpleMailbox(MailboxPath.forUser("user", "defg"),
        UID_VALIDITY_2,
        CASSANDRA_ID_2);

    CassandraMailboxDAO testee();

    @Test
    default void retrieveMailboxShouldReturnEmptyWhenNone() {
        assertThat(testee().retrieveMailbox(CASSANDRA_ID_1).join())
            .isEmpty();
    }

    @Test
    default void saveShouldAddAMailbox() {
        testee().save(mailbox1).join();

        Optional<SimpleMailbox> readMailbox = testee().retrieveMailbox(CASSANDRA_ID_1)
            .join();
        assertThat(readMailbox.isPresent()).isTrue();
        assertThat(readMailbox.get()).isEqualToComparingFieldByField(mailbox1);
    }

    @Test
    default void saveShouldOverride() {
        testee().save(mailbox1).join();

        mailbox2.setMailboxId(CASSANDRA_ID_1);
        testee().save(mailbox2).join();


        Optional<SimpleMailbox> readMailbox = testee().retrieveMailbox(CASSANDRA_ID_1)
            .join();
        assertThat(readMailbox.isPresent()).isTrue();
        assertThat(readMailbox.get()).isEqualToComparingFieldByField(mailbox2);
    }

    @Test
    default void retrieveAllMailboxesShouldBeEmptyByDefault() {
        List<SimpleMailbox> mailboxes = testee().retrieveAllMailboxes()
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(mailboxes).isEmpty();
    }

    @Test
    default void retrieveAllMailboxesShouldReturnSingleMailbox() {
        testee().save(mailbox1).join();

        List<SimpleMailbox> mailboxes = testee().retrieveAllMailboxes()
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(mailboxes).containsOnly(mailbox1);
    }

    @Test
    default void retrieveAllMailboxesShouldReturnMultiMailboxes() {
        testee().save(mailbox1).join();
        testee().save(mailbox2).join();

        List<SimpleMailbox> mailboxes = testee().retrieveAllMailboxes()
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(mailboxes).containsOnly(mailbox1, mailbox2);
    }

    @Test
    default void deleteShouldNotFailWhenMailboxIsAbsent() {
        testee().delete(CASSANDRA_ID_1).join();
    }

    @Test
    default void deleteShouldRemoveExistingMailbox() {
        testee().save(mailbox1).join();

        testee().delete(CASSANDRA_ID_1).join();

        assertThat(testee().retrieveMailbox(CASSANDRA_ID_1).join())
            .isEmpty();
    }

    @Test
    default void updateShouldNotFailWhenMailboxIsAbsent() {
        testee().updatePath(CASSANDRA_ID_1, NEW_MAILBOX_PATH).join();
    }

    @Test
    default void updateShouldChangeMailboxPath() {
        testee().save(mailbox1).join();

        testee().updatePath(CASSANDRA_ID_1, NEW_MAILBOX_PATH).join();

        mailbox1.setNamespace(NEW_MAILBOX_PATH.getNamespace());
        mailbox1.setUser(NEW_MAILBOX_PATH.getUser());
        mailbox1.setName(NEW_MAILBOX_PATH.getName());
        Optional<SimpleMailbox> readMailbox = testee().retrieveMailbox(CASSANDRA_ID_1).join();
        assertThat(readMailbox.isPresent()).isTrue();
        assertThat(readMailbox.get()).isEqualToComparingFieldByField(mailbox1);
    }
}
