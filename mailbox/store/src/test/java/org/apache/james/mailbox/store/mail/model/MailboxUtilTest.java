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

package org.apache.james.mailbox.store.mail.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.junit.Test;

public class MailboxUtilTest {

    public static final int UID_VALIDITY = 36;
    public static final MockMailboxSession MAILBOX_SESSION = new MockMailboxSession("user");

    @Test
    public void isMailboxChildOfShouldReturnTrueOnChildren() {
        Mailbox mailbox1 = new SimpleMailbox(
            MailboxPath.forUser("user", "inbox"), UID_VALIDITY);
        Mailbox mailbox2 = new SimpleMailbox(
            MailboxPath.forUser("user", "inbox.hello"), UID_VALIDITY);
        assertThat(MailboxUtil.isMailboxChildOf(mailbox2, mailbox1, MAILBOX_SESSION))
            .isTrue();
    }

    @Test
    public void isMailboxChildOfShouldReturnReturnFalseWhenBelongsToOverUser() {
        Mailbox mailbox1 = new SimpleMailbox(
            MailboxPath.forUser("user", "inbox"), UID_VALIDITY);
        Mailbox mailbox2 = new SimpleMailbox(
            MailboxPath.forUser("bob", "inbox.hello"), UID_VALIDITY);
        assertThat(MailboxUtil.isMailboxChildOf(mailbox2, mailbox1, MAILBOX_SESSION))
            .isFalse();
    }

    @Test
    public void isMailboxChildOfShouldReturnFalseWhenPotentialParentIsChild() {
        Mailbox mailbox1 = new SimpleMailbox(
            MailboxPath.forUser("user", "inbox"), UID_VALIDITY);
        Mailbox mailbox2 = new SimpleMailbox(
            MailboxPath.forUser("user", "inbox.hello"), UID_VALIDITY);
        assertThat(MailboxUtil.isMailboxChildOf(mailbox1, mailbox2, MAILBOX_SESSION))
            .isFalse();
    }

    @Test
    public void isMailboxChildOfShouldReturnFalseOnPrefixMisMatch() {
        Mailbox mailbox1 = new SimpleMailbox(
            MailboxPath.forUser("user", "inbax"), UID_VALIDITY);
        Mailbox mailbox2 = new SimpleMailbox(
            MailboxPath.forUser("user", "inbox.hello"), UID_VALIDITY);
        assertThat(MailboxUtil.isMailboxChildOf(mailbox2, mailbox1, MAILBOX_SESSION))
            .isFalse();
    }


    @Test
    public void isMailboxChildOfShouldReturnFalseWhenSameMailbox() {
        Mailbox mailbox1 = new SimpleMailbox(
            MailboxPath.forUser("user", "inbox"), UID_VALIDITY);
        assertThat(MailboxUtil.isMailboxChildOf(mailbox1, mailbox1, MAILBOX_SESSION))
            .isFalse();
    }

}