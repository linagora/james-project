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
package org.apache.james.mailbox.store;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.junit.Before;
import org.junit.Test;

public class GroupFolderResolverTest {

    private static final long UID_VALIDITY = 9999;
    private GroupFolderResolver testee;

    @Before
    public void setUp() {
        testee = new GroupFolderResolver();
    }

    @Test
    public void isGroupFolderShouldReturnFalseWhenMailboxNamespaceIsNull() {
        SimpleMailbox mailbox = new SimpleMailbox(new MailboxPath(null, "user", "name"), UID_VALIDITY);
        assertThat(testee.isGroupFolder(mailbox)).isFalse();
    }
    
    @Test
    public void isGroupFolderShouldReturnFalseWhenMailboxNamespaceEqualsToUserNamespace() {
        SimpleMailbox mailbox = new SimpleMailbox(MailboxPath.forUser("user", "name"), UID_VALIDITY);
        assertThat(testee.isGroupFolder(mailbox)).isFalse();
    }
    
    @Test
    public void isGroupFolderShouldReturnTrueWhenMailboxNamespaceDoesntEqualToOtherUsersNamespace() {
        SimpleMailbox mailbox = new SimpleMailbox(new MailboxPath("namespace", "user", "name"), UID_VALIDITY);
        assertThat(testee.isGroupFolder(mailbox)).isTrue();
    }
}
