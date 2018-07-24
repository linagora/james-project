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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.cassandra.mail.CassandraIdAndPath;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathDAOImpl;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathV2DAO;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public interface CassandraMailboxMapperFallbackContract {

    MailboxPath MAILBOX_PATH_1 = MailboxPath.forUser("bob", "Important");
    int UID_VALIDITY_1 = 452;
    SimpleMailbox MAILBOX_1 = new SimpleMailbox(MAILBOX_PATH_1, UID_VALIDITY_1);
    CassandraId MAILBOX_ID_1 = CassandraId.timeBased();

    @BeforeAll
    static void setUpClass() {
        MAILBOX_1.setMailboxId(MAILBOX_ID_1);
    }

    class Testee {
        private final CassandraMailboxPathDAOImpl daoV1;
        private final CassandraMailboxPathV2DAO daoV2;
        private final CassandraMailboxMapper mailboxMapper;
        private final CassandraMailboxDAO mailboxDAO;

        public Testee(CassandraMailboxPathDAOImpl daoV1, CassandraMailboxPathV2DAO daoV2, CassandraMailboxMapper mailboxMapper, CassandraMailboxDAO mailboxDAO) {
            this.daoV1 = daoV1;
            this.daoV2 = daoV2;
            this.mailboxMapper = mailboxMapper;
            this.mailboxDAO = mailboxDAO;
        }
    }

    Testee mailboxMapperFallbackTestee();

    @Test
    default void newValuesShouldBeSavedInMostRecentDAO() throws Exception {
        mailboxMapperFallbackTestee().mailboxMapper.save(MAILBOX_1);

        assertThat(mailboxMapperFallbackTestee().daoV2.retrieveId(MAILBOX_PATH_1).join())
            .contains(new CassandraIdAndPath(MAILBOX_ID_1, MAILBOX_PATH_1));
    }

    @Test
    default void newValuesShouldNotBeSavedInOldDAO() throws Exception {
        mailboxMapperFallbackTestee().mailboxMapper.save(MAILBOX_1);

        assertThat(mailboxMapperFallbackTestee().daoV1.retrieveId(MAILBOX_PATH_1).join())
            .isEmpty();
    }

    @Test
    default void readingOldValuesShouldMigrateThem() throws Exception {
        mailboxMapperFallbackTestee().daoV1.save(MAILBOX_PATH_1, MAILBOX_ID_1).join();
        mailboxMapperFallbackTestee().mailboxDAO.save(MAILBOX_1).join();

        mailboxMapperFallbackTestee().mailboxMapper.findMailboxByPath(MAILBOX_PATH_1);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(mailboxMapperFallbackTestee().daoV1.retrieveId(MAILBOX_PATH_1).join()).isEmpty();
        softly.assertThat(mailboxMapperFallbackTestee().daoV2.retrieveId(MAILBOX_PATH_1).join())
            .contains(new CassandraIdAndPath(MAILBOX_ID_1, MAILBOX_PATH_1));
        softly.assertAll();
    }

    @Test
    default void migrationTaskShouldMoveDataToMostRecentDao() {
        mailboxMapperFallbackTestee().daoV1.save(MAILBOX_PATH_1, MAILBOX_ID_1).join();

        new MailboxPathV2Migration(mailboxMapperFallbackTestee().daoV1, mailboxMapperFallbackTestee().daoV2).run();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(mailboxMapperFallbackTestee().daoV1.retrieveId(MAILBOX_PATH_1).join()).isEmpty();
        softly.assertThat(mailboxMapperFallbackTestee().daoV2.retrieveId(MAILBOX_PATH_1).join())
            .contains(new CassandraIdAndPath(MAILBOX_ID_1, MAILBOX_PATH_1));
        softly.assertAll();
    }
}