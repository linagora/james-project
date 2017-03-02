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

package org.apache.james.jmap.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MailboxPathBuilderTest {

    private MailboxSession session;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        session = mock(MailboxSession.class);
        when(session.getPathDelimiter()).thenReturn('.');
    }

    @Test
    public void buildShouldThrowOnMissingUser() {
        expectedException.expect(IllegalStateException.class);

        MailboxPathBuilder.builder()
            .name("toto")
            .build(session);
    }

    @Test
    public void buildShouldThrowOnMissingName() {
        expectedException.expect(IllegalStateException.class);

        MailboxPathBuilder.builder()
            .forUser("toto")
            .build(session);
    }

    @Test
    public void buildShouldBuildMailboxWithoutParent() {
        String user = "toto";
        String name = "tata";
        assertThat(
            MailboxPathBuilder.builder()
                .forUser(user)
                .name(name)
                .build(session))
            .isEqualTo(new MailboxPath(MailboxConstants.USER_NAMESPACE, user, name));
    }

    @Test
    public void buildShouldEscapeName() {
        String user = "toto";
        String name = "tata.tutu";
        assertThat(
            MailboxPathBuilder.builder()
                .forUser(user)
                .name(name)
                .build(session))
            .isEqualTo(new MailboxPath(MailboxConstants.USER_NAMESPACE, user, "tata\\/tutu"));
    }

    @Test
    public void buildShouldBuildMailboxWithParent() {
        String user = "toto";
        String name = "tata";
        assertThat(
            MailboxPathBuilder.builder()
                .forUser(user)
                .name(name)
                .withParent(Optional.of(new MailboxPath(MailboxConstants.USER_NAMESPACE, user, "titi")))
                .build(session))
            .isEqualTo(new MailboxPath(MailboxConstants.USER_NAMESPACE, user, "titi.tata"));
    }

    @Test
    public void buildShouldEscapeNameWithParent() {
        String user = "toto";
        String name = "tata.tutu";
        assertThat(
            MailboxPathBuilder.builder()
                .forUser(user)
                .name(name)
                .withParent(Optional.of(new MailboxPath(MailboxConstants.USER_NAMESPACE, user, "titi")))
                .build(session))
            .isEqualTo(new MailboxPath(MailboxConstants.USER_NAMESPACE, user, "titi.tata\\/tutu"));
    }

}
