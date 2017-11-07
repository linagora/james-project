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

package org.apache.james.mpt.imapmailbox.suite;

import java.util.Locale;

import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxACL.Rfc4314Rights;
import org.apache.james.mailbox.model.MailboxACL.Right;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mpt.api.ImapHostSystem;
import org.apache.james.mpt.imapmailbox.ImapTestConstants;
import org.apache.james.mpt.imapmailbox.suite.base.BasicImapCommands;
import org.apache.james.mpt.script.ImapScriptedTestProtocol;
import org.junit.Before;
import org.junit.Test;

public abstract class ListingWithSharingTest implements ImapTestConstants {

    public static final String IMAPUSER_DOMAIN_COM = "imapuser@domain.com";
    public static final String OTHER_DOMAIN_COM = "Boby@domain.com";

    private static final String OTHER_USER_NAME = "Boby";
    private static final String OTHER_USER_PASSWORD = "password";
    private static final MailboxPath OTHER_USER_SHARED_MAILBOX =
        MailboxPath.forUser(OTHER_USER_NAME, "sharedMailbox");
    private static final MailboxPath OTHER_USER_SHARED_MAILBOX_CHILD =
        MailboxPath.forUser(OTHER_USER_NAME, "sharedMailbox.child");
    private static final MailboxPath OTHER_USER_INSUFFICIENT_RIGHT_SHARED_MAILBOX =
        MailboxPath.forUser(OTHER_USER_NAME, "notEnoughSharedMailbox");
    private static final MailboxPath MAILBOX_1_VIRTUAL_HOSTED =
        MailboxPath.forUser(OTHER_USER_NAME, "sharedMailbox");
    private static final MailboxPath MAILBOX_2_VIRTUAL_HOSTED =
        MailboxPath.forUser(OTHER_USER_NAME, "sharedMailbox.child");
    private static final MailboxPath MAILBOX_3_VIRTUAL_HOSTED =
        MailboxPath.forUser(OTHER_USER_NAME, "notEnoughSharedMailbox");

    protected abstract ImapHostSystem createImapHostSystem();

    protected abstract ImapHostSystem createImapHostSystemWithVirtualHosting();

    private ImapHostSystem system;
    private ImapHostSystem virtualHostedSystem;
    private ImapScriptedTestProtocol scriptedTestProtocol;
    private ImapScriptedTestProtocol scriptedVirtualHostedTestProtocol;

    @Before
    public void setUp() throws Exception {
        system = createImapHostSystem();
        scriptedTestProtocol = new ImapScriptedTestProtocol("/org/apache/james/imap/scripts/", system)
            .withUser(USER, PASSWORD)
            .withUser(OTHER_USER_NAME, OTHER_USER_PASSWORD);
        BasicImapCommands.welcome(scriptedTestProtocol);
        BasicImapCommands.authenticate(scriptedTestProtocol);

        virtualHostedSystem = createImapHostSystemWithVirtualHosting();
        virtualHostedSystem.beforeTest();
        virtualHostedSystem.addUser(IMAPUSER_DOMAIN_COM, PASSWORD);
        scriptedVirtualHostedTestProtocol = new ImapScriptedTestProtocol("/org/apache/james/imap/scripts/", virtualHostedSystem)
            .withUser(IMAPUSER_DOMAIN_COM, PASSWORD)
            .withUser(OTHER_DOMAIN_COM, "other pass");
        BasicImapCommands.welcome(scriptedVirtualHostedTestProtocol);
        BasicImapCommands.addLogin(scriptedVirtualHostedTestProtocol, IMAPUSER_DOMAIN_COM, PASSWORD);
    }

    @Test
    public void testListWithSharedMailboxUS() throws Exception {
        scriptedTestProtocol
            .withMailbox(OTHER_USER_SHARED_MAILBOX)
            .withMailbox(OTHER_USER_SHARED_MAILBOX_CHILD)
            .withMailbox(OTHER_USER_INSUFFICIENT_RIGHT_SHARED_MAILBOX)
            .withRights(OTHER_USER_SHARED_MAILBOX, USER,  new Rfc4314Rights(Right.Lookup, Right.Read))
            .withRights(OTHER_USER_SHARED_MAILBOX_CHILD, USER, new Rfc4314Rights(Right.Lookup, Right.Read))
            .withRights(OTHER_USER_INSUFFICIENT_RIGHT_SHARED_MAILBOX, USER, new Rfc4314Rights(Right.Read))
            .withLocale(Locale.US)
            .run("ListWithSharedMailbox");
    }

    @Test
    public void listShouldNotDisplaySelectForNonReadableMailboxes() throws Exception {
        scriptedTestProtocol
            .withMailbox(OTHER_USER_SHARED_MAILBOX)
            .withMailbox(OTHER_USER_SHARED_MAILBOX_CHILD)
            .withMailbox(OTHER_USER_INSUFFICIENT_RIGHT_SHARED_MAILBOX)
            .withRights(OTHER_USER_SHARED_MAILBOX, USER, new Rfc4314Rights(Right.Lookup))
            .withRights(OTHER_USER_SHARED_MAILBOX_CHILD, USER, new Rfc4314Rights(Right.Lookup))
            .withLocale(Locale.US)
            .run("ListWithSharedMailboxAndNoLookup");
    }

    @Test
    public void listShouldOmmitAddressPartsWhenDelegationAndVirtualHosing() throws Exception {
        scriptedVirtualHostedTestProtocol
            .withMailbox(MAILBOX_1_VIRTUAL_HOSTED)
            .withMailbox(MAILBOX_2_VIRTUAL_HOSTED)
            .withMailbox(MAILBOX_3_VIRTUAL_HOSTED)
            .withRights(MAILBOX_1_VIRTUAL_HOSTED, IMAPUSER_DOMAIN_COM,  new MailboxACL.Rfc4314Rights(MailboxACL.Right.Lookup, MailboxACL.Right.Read))
            .withRights(MAILBOX_2_VIRTUAL_HOSTED, IMAPUSER_DOMAIN_COM, new MailboxACL.Rfc4314Rights(MailboxACL.Right.Lookup, MailboxACL.Right.Read))
            .withRights(MAILBOX_3_VIRTUAL_HOSTED, IMAPUSER_DOMAIN_COM, new MailboxACL.Rfc4314Rights(MailboxACL.Right.Read))
            .withLocale(Locale.US)
            .run("ListWithSharedMailbox");
    }
}
