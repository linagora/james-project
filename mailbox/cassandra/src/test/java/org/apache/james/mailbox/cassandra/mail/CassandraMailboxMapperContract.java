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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.exception.TooLongMailboxNameException;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.junit.jupiter.api.Test;

public interface CassandraMailboxMapperContract {
    
    int UID_VALIDITY = 52;
    String USER = "user";
    CassandraId MAILBOX_ID = CassandraId.timeBased();
    MailboxPath MAILBOX_PATH = MailboxPath.forUser(USER, "name");
    Mailbox MAILBOX = new SimpleMailbox(MAILBOX_PATH, UID_VALIDITY, MAILBOX_ID);

    CassandraId MAILBOX_ID_2 = CassandraId.timeBased();


    Mailbox MAILBOX_BIS = new SimpleMailbox(MAILBOX_PATH, UID_VALIDITY, MAILBOX_ID_2);
    String WILDCARD = "%";

    class Testee {
        private final CassandraMailboxDAO mailboxDAO;
        private final CassandraMailboxPathDAOImpl mailboxPathDAO;
        private final CassandraMailboxPathV2DAO mailboxPathV2DAO;
        private final CassandraMailboxMapper mailboxMapper;

        public Testee(CassandraMailboxDAO mailboxDAO, CassandraMailboxPathDAOImpl mailboxPathDAO, CassandraMailboxPathV2DAO mailboxPathV2DAO, CassandraMailboxMapper mailboxMapper) {
            this.mailboxDAO = mailboxDAO;
            this.mailboxPathDAO = mailboxPathDAO;
            this.mailboxPathV2DAO = mailboxPathV2DAO;
            this.mailboxMapper = mailboxMapper;
        }
    }

    Testee testee();

    @Test
    default void saveShouldNotRemoveOldMailboxPathWhenCreatingTheNewMailboxPathFails() throws Exception {
        testee().mailboxMapper.save(new SimpleMailbox(MAILBOX_PATH, UID_VALIDITY));
        Mailbox mailbox = testee().mailboxMapper.findMailboxByPath(MAILBOX_PATH);

        SimpleMailbox newMailbox = new SimpleMailbox(tooLongMailboxPath(mailbox.generateAssociatedPath()), UID_VALIDITY, mailbox.getMailboxId());
        assertThatThrownBy(() ->
            testee().mailboxMapper.save(newMailbox))
            .isInstanceOf(TooLongMailboxNameException.class);

        assertThat(testee().mailboxPathV2DAO.retrieveId(MAILBOX_PATH).join())
            .isPresent();
    }

    default MailboxPath tooLongMailboxPath(MailboxPath fromMailboxPath) {
        return new MailboxPath(fromMailboxPath, StringUtils.repeat("b", 65537));
    }

    @Test
    default void deleteShouldDeleteMailboxAndMailboxPathFromV1Table() {
        testee().mailboxDAO.save(MAILBOX)
            .join();
        testee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        testee().mailboxMapper.delete(MAILBOX);

        assertThatThrownBy(() -> testee().mailboxMapper.findMailboxByPath(MAILBOX_PATH))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    default void deleteShouldDeleteMailboxAndMailboxPathFromV2Table() {
        testee().mailboxDAO.save(MAILBOX)
            .join();
        testee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        testee().mailboxMapper.delete(MAILBOX);

        assertThatThrownBy(() -> testee().mailboxMapper.findMailboxByPath(MAILBOX_PATH))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    default void findMailboxByPathShouldReturnMailboxWhenExistsInV1Table() throws Exception {
        testee().mailboxDAO.save(MAILBOX)
            .join();
        testee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        Mailbox mailbox = testee().mailboxMapper.findMailboxByPath(MAILBOX_PATH);

        assertThat(mailbox.generateAssociatedPath()).isEqualTo(MAILBOX_PATH);
    }

    @Test
    default void findMailboxByPathShouldReturnMailboxWhenExistsInV2Table() throws Exception {
        testee().mailboxDAO.save(MAILBOX)
            .join();
        testee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        Mailbox mailbox = testee().mailboxMapper.findMailboxByPath(MAILBOX_PATH);

        assertThat(mailbox.generateAssociatedPath()).isEqualTo(MAILBOX_PATH);
    }

    @Test
    default void findMailboxByPathShouldReturnMailboxWhenExistsInBothTables() throws Exception {
        testee().mailboxDAO.save(MAILBOX)
            .join();
        testee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
        testee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        Mailbox mailbox = testee().mailboxMapper.findMailboxByPath(MAILBOX_PATH);

        assertThat(mailbox.generateAssociatedPath()).isEqualTo(MAILBOX_PATH);
    }

    @Test
    default void deleteShouldRemoveMailboxWhenInBothTables() {
        testee().mailboxDAO.save(MAILBOX)
            .join();
        testee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
        testee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        testee().mailboxMapper.delete(MAILBOX);

        assertThatThrownBy(() -> testee().mailboxMapper.findMailboxByPath(MAILBOX_PATH))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    default void deleteShouldRemoveMailboxWhenInV1Tables() {
        testee().mailboxDAO.save(MAILBOX)
            .join();
        testee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        testee().mailboxMapper.delete(MAILBOX);

        assertThatThrownBy(() -> testee().mailboxMapper.findMailboxByPath(MAILBOX_PATH))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    default void deleteShouldRemoveMailboxWhenInV2Table() {
        testee().mailboxDAO.save(MAILBOX)
            .join();
        testee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        testee().mailboxMapper.delete(MAILBOX);

        assertThatThrownBy(() -> testee().mailboxMapper.findMailboxByPath(MAILBOX_PATH))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    default void findMailboxByPathShouldThrowWhenDoesntExistInBothTables() {
        testee().mailboxDAO.save(MAILBOX)
            .join();

        assertThatThrownBy(() -> testee().mailboxMapper.findMailboxByPath(MAILBOX_PATH))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    default void findMailboxWithPathLikeShouldReturnMailboxesWhenExistsInV1Table() {
        testee().mailboxDAO.save(MAILBOX)
            .join();
        testee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
    
        List<Mailbox> mailboxes = testee().mailboxMapper.findMailboxWithPathLike(new MailboxPath(MailboxConstants.USER_NAMESPACE, USER, WILDCARD));

        assertThat(mailboxes).containsOnly(MAILBOX);
    }

    @Test
    default void findMailboxWithPathLikeShouldReturnMailboxesWhenExistsInBothTables() {
        testee().mailboxDAO.save(MAILBOX)
            .join();
        testee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
        testee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        List<Mailbox> mailboxes = testee().mailboxMapper.findMailboxWithPathLike(new MailboxPath(MailboxConstants.USER_NAMESPACE, USER, WILDCARD));

        assertThat(mailboxes).containsOnly(MAILBOX);
    }

    @Test
    default void findMailboxWithPathLikeShouldReturnMailboxesWhenExistsInV2Table() {
        testee().mailboxDAO.save(MAILBOX)
            .join();
        testee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
    
        List<Mailbox> mailboxes = testee().mailboxMapper.findMailboxWithPathLike(new MailboxPath(MailboxConstants.USER_NAMESPACE, USER, WILDCARD));

        assertThat(mailboxes).containsOnly(MAILBOX);
    }

    @Test
    default void hasChildrenShouldReturnChildWhenExistsInV1Table() {
        testee().mailboxDAO.save(MAILBOX)
            .join();
        testee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
        CassandraId childMailboxId = CassandraId.timeBased();
        MailboxPath childMailboxPath = MailboxPath.forUser(USER, "name.child");
        Mailbox childMailbox = new SimpleMailbox(childMailboxPath, UID_VALIDITY, childMailboxId);
        testee().mailboxDAO.save(childMailbox)
            .join();
        testee().mailboxPathDAO.save(childMailboxPath, childMailboxId)
            .join();
    
        boolean hasChildren = testee().mailboxMapper.hasChildren(MAILBOX, '.');

        assertThat(hasChildren).isTrue();
    }

    @Test
    default void hasChildrenShouldReturnChildWhenExistsInBothTables() {
        testee().mailboxDAO.save(MAILBOX)
            .join();
        testee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
        testee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
        CassandraId childMailboxId = CassandraId.timeBased();
        MailboxPath childMailboxPath = MailboxPath.forUser(USER, "name.child");
        Mailbox childMailbox = new SimpleMailbox(childMailboxPath, UID_VALIDITY, childMailboxId);
        testee().mailboxDAO.save(childMailbox)
            .join();
        testee().mailboxPathDAO.save(childMailboxPath, childMailboxId)
            .join();

        boolean hasChildren = testee().mailboxMapper.hasChildren(MAILBOX, '.');

        assertThat(hasChildren).isTrue();
    }

    @Test
    default void hasChildrenShouldReturnChildWhenExistsInV2Table() {
        testee().mailboxDAO.save(MAILBOX)
            .join();
        testee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
        CassandraId childMailboxId = CassandraId.timeBased();
        MailboxPath childMailboxPath = MailboxPath.forUser(USER, "name.child");
        Mailbox childMailbox = new SimpleMailbox(childMailboxPath, UID_VALIDITY, childMailboxId);
        testee().mailboxDAO.save(childMailbox)
            .join();
        testee().mailboxPathV2DAO.save(childMailboxPath, childMailboxId)
            .join();
    
        boolean hasChildren = testee().mailboxMapper.hasChildren(MAILBOX, '.');
    
        assertThat(hasChildren).isTrue();
    }

    @Test
    default void findMailboxWithPathLikeShouldRemoveDuplicatesAndKeepV2() {
        testee().mailboxDAO.save(MAILBOX).join();
        testee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID).join();

        testee().mailboxDAO.save(MAILBOX_BIS).join();
        testee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID_2).join();

        assertThat(testee().mailboxMapper.findMailboxWithPathLike(
            new MailboxPath(MailboxConstants.USER_NAMESPACE, USER, WILDCARD)))
            .containsOnly(MAILBOX);
    }
}
