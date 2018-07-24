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

    Testee mailboxMapperTestee();

    @Test
    default void saveShouldNotRemoveOldMailboxPathWhenCreatingTheNewMailboxPathFails() throws Exception {
        mailboxMapperTestee().mailboxMapper.save(new SimpleMailbox(MAILBOX_PATH, UID_VALIDITY));
        Mailbox mailbox = mailboxMapperTestee().mailboxMapper.findMailboxByPath(MAILBOX_PATH);

        SimpleMailbox newMailbox = new SimpleMailbox(tooLongMailboxPath(mailbox.generateAssociatedPath()), UID_VALIDITY, mailbox.getMailboxId());
        assertThatThrownBy(() ->
            mailboxMapperTestee().mailboxMapper.save(newMailbox))
            .isInstanceOf(TooLongMailboxNameException.class);

        assertThat(mailboxMapperTestee().mailboxPathV2DAO.retrieveId(MAILBOX_PATH).join())
            .isPresent();
    }

    default MailboxPath tooLongMailboxPath(MailboxPath fromMailboxPath) {
        return new MailboxPath(fromMailboxPath, StringUtils.repeat("b", 65537));
    }

    @Test
    default void deleteShouldDeleteMailboxAndMailboxPathFromV1Table() {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();
        mailboxMapperTestee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        mailboxMapperTestee().mailboxMapper.delete(MAILBOX);

        assertThatThrownBy(() -> mailboxMapperTestee().mailboxMapper.findMailboxByPath(MAILBOX_PATH))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    default void deleteShouldDeleteMailboxAndMailboxPathFromV2Table() {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();
        mailboxMapperTestee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        mailboxMapperTestee().mailboxMapper.delete(MAILBOX);

        assertThatThrownBy(() -> mailboxMapperTestee().mailboxMapper.findMailboxByPath(MAILBOX_PATH))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    default void findMailboxByPathShouldReturnMailboxWhenExistsInV1Table() throws Exception {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();
        mailboxMapperTestee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        Mailbox mailbox = mailboxMapperTestee().mailboxMapper.findMailboxByPath(MAILBOX_PATH);

        assertThat(mailbox.generateAssociatedPath()).isEqualTo(MAILBOX_PATH);
    }

    @Test
    default void findMailboxByPathShouldReturnMailboxWhenExistsInV2Table() throws Exception {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();
        mailboxMapperTestee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        Mailbox mailbox = mailboxMapperTestee().mailboxMapper.findMailboxByPath(MAILBOX_PATH);

        assertThat(mailbox.generateAssociatedPath()).isEqualTo(MAILBOX_PATH);
    }

    @Test
    default void findMailboxByPathShouldReturnMailboxWhenExistsInBothTables() throws Exception {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();
        mailboxMapperTestee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
        mailboxMapperTestee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        Mailbox mailbox = mailboxMapperTestee().mailboxMapper.findMailboxByPath(MAILBOX_PATH);

        assertThat(mailbox.generateAssociatedPath()).isEqualTo(MAILBOX_PATH);
    }

    @Test
    default void deleteShouldRemoveMailboxWhenInBothTables() {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();
        mailboxMapperTestee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
        mailboxMapperTestee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        mailboxMapperTestee().mailboxMapper.delete(MAILBOX);

        assertThatThrownBy(() -> mailboxMapperTestee().mailboxMapper.findMailboxByPath(MAILBOX_PATH))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    default void deleteShouldRemoveMailboxWhenInV1Tables() {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();
        mailboxMapperTestee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        mailboxMapperTestee().mailboxMapper.delete(MAILBOX);

        assertThatThrownBy(() -> mailboxMapperTestee().mailboxMapper.findMailboxByPath(MAILBOX_PATH))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    default void deleteShouldRemoveMailboxWhenInV2Table() {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();
        mailboxMapperTestee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        mailboxMapperTestee().mailboxMapper.delete(MAILBOX);

        assertThatThrownBy(() -> mailboxMapperTestee().mailboxMapper.findMailboxByPath(MAILBOX_PATH))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    default void findMailboxByPathShouldThrowWhenDoesntExistInBothTables() {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();

        assertThatThrownBy(() -> mailboxMapperTestee().mailboxMapper.findMailboxByPath(MAILBOX_PATH))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    default void findMailboxWithPathLikeShouldReturnMailboxesWhenExistsInV1Table() {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();
        mailboxMapperTestee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
    
        List<Mailbox> mailboxes = mailboxMapperTestee().mailboxMapper.findMailboxWithPathLike(new MailboxPath(MailboxConstants.USER_NAMESPACE, USER, WILDCARD));

        assertThat(mailboxes).containsOnly(MAILBOX);
    }

    @Test
    default void findMailboxWithPathLikeShouldReturnMailboxesWhenExistsInBothTables() {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();
        mailboxMapperTestee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
        mailboxMapperTestee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();

        List<Mailbox> mailboxes = mailboxMapperTestee().mailboxMapper.findMailboxWithPathLike(new MailboxPath(MailboxConstants.USER_NAMESPACE, USER, WILDCARD));

        assertThat(mailboxes).containsOnly(MAILBOX);
    }

    @Test
    default void findMailboxWithPathLikeShouldReturnMailboxesWhenExistsInV2Table() {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();
        mailboxMapperTestee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
    
        List<Mailbox> mailboxes = mailboxMapperTestee().mailboxMapper.findMailboxWithPathLike(new MailboxPath(MailboxConstants.USER_NAMESPACE, USER, WILDCARD));

        assertThat(mailboxes).containsOnly(MAILBOX);
    }

    @Test
    default void hasChildrenShouldReturnChildWhenExistsInV1Table() {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();
        mailboxMapperTestee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
        CassandraId childMailboxId = CassandraId.timeBased();
        MailboxPath childMailboxPath = MailboxPath.forUser(USER, "name.child");
        Mailbox childMailbox = new SimpleMailbox(childMailboxPath, UID_VALIDITY, childMailboxId);
        mailboxMapperTestee().mailboxDAO.save(childMailbox)
            .join();
        mailboxMapperTestee().mailboxPathDAO.save(childMailboxPath, childMailboxId)
            .join();
    
        boolean hasChildren = mailboxMapperTestee().mailboxMapper.hasChildren(MAILBOX, '.');

        assertThat(hasChildren).isTrue();
    }

    @Test
    default void hasChildrenShouldReturnChildWhenExistsInBothTables() {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();
        mailboxMapperTestee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
        mailboxMapperTestee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
        CassandraId childMailboxId = CassandraId.timeBased();
        MailboxPath childMailboxPath = MailboxPath.forUser(USER, "name.child");
        Mailbox childMailbox = new SimpleMailbox(childMailboxPath, UID_VALIDITY, childMailboxId);
        mailboxMapperTestee().mailboxDAO.save(childMailbox)
            .join();
        mailboxMapperTestee().mailboxPathDAO.save(childMailboxPath, childMailboxId)
            .join();

        boolean hasChildren = mailboxMapperTestee().mailboxMapper.hasChildren(MAILBOX, '.');

        assertThat(hasChildren).isTrue();
    }

    @Test
    default void hasChildrenShouldReturnChildWhenExistsInV2Table() {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX)
            .join();
        mailboxMapperTestee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID)
            .join();
        CassandraId childMailboxId = CassandraId.timeBased();
        MailboxPath childMailboxPath = MailboxPath.forUser(USER, "name.child");
        Mailbox childMailbox = new SimpleMailbox(childMailboxPath, UID_VALIDITY, childMailboxId);
        mailboxMapperTestee().mailboxDAO.save(childMailbox)
            .join();
        mailboxMapperTestee().mailboxPathV2DAO.save(childMailboxPath, childMailboxId)
            .join();
    
        boolean hasChildren = mailboxMapperTestee().mailboxMapper.hasChildren(MAILBOX, '.');
    
        assertThat(hasChildren).isTrue();
    }

    @Test
    default void findMailboxWithPathLikeShouldRemoveDuplicatesAndKeepV2() {
        mailboxMapperTestee().mailboxDAO.save(MAILBOX).join();
        mailboxMapperTestee().mailboxPathV2DAO.save(MAILBOX_PATH, MAILBOX_ID).join();

        mailboxMapperTestee().mailboxDAO.save(MAILBOX_BIS).join();
        mailboxMapperTestee().mailboxPathDAO.save(MAILBOX_PATH, MAILBOX_ID_2).join();

        assertThat(mailboxMapperTestee().mailboxMapper.findMailboxWithPathLike(
            new MailboxPath(MailboxConstants.USER_NAMESPACE, USER, WILDCARD)))
            .containsOnly(MAILBOX);
    }
}
