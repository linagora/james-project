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

package org.apache.james.imapserver.mailbox;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.user.api.UsersRepositoryException;
import org.junit.Before;
import org.junit.Test;

public class ImapDelegationUsernameValidatorTest {

    private ImapDelegationUsernameValidator testee;

    @Before
    public void setUp() {
        MailboxManager mailboxManager = mock(MailboxManager.class);
        when(mailboxManager.getDelimiter()).thenReturn(MailboxConstants.DEFAULT_DELIMITER);

        testee = new ImapDelegationUsernameValidator(mailboxManager);
    }

    @Test
    public void validateShouldNotThrowWhenLocalPartOnlyUsername() throws UsersRepositoryException {
        testee.validate("username");
    }

    @Test
    public void validateShouldNotThrowWhenUsernameWithDomain() throws UsersRepositoryException {
        testee.validate("username@domain");
    }

    @Test
    public void validateShouldNotThrowWhenDomainContainsForbiddenChar() throws UsersRepositoryException {
        testee.validate("username@domain.com");
    }

    @Test
    public void validateShouldThrowWhenLocalPartContainsForbiddenChar() throws UsersRepositoryException {
        assertThatThrownBy(() ->
            testee.validate("user.name"))
            .isInstanceOf(UsersRepositoryException.class);
    }

    @Test
    public void validateShouldThrowWhenUsernameContainsForbiddenChar() throws UsersRepositoryException {
        assertThatThrownBy(() ->
            testee.validate("user.name@domain"))
            .isInstanceOf(UsersRepositoryException.class);
    }

}