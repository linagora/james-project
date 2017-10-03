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

package org.apache.james.mailbox.acl;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxACL.Entry;
import org.apache.james.mailbox.model.MailboxACL.EntryKey;
import org.apache.james.mailbox.model.MailboxACL.Rfc4314Rights;
import org.apache.james.mailbox.model.MailboxACL.Right;
import org.junit.Test;

public class ACLChangeTest {
    private static final EntryKey ENTRY_KEY = EntryKey.createUser("user");
    private static final Rfc4314Rights RIGHTS = new Rfc4314Rights(Right.Administer);

    @Test
    public void addedEntriesShouldReturnEmptyWhenSameACL() {
        ACLChange aclChange = new ACLChange(
            MailboxACL.EMPTY,
            MailboxACL.EMPTY);

        assertThat(aclChange.addedEntries()).isEmpty();
    }

    @Test
    public void removedEntriesShouldReturnEmptyWhenSameACL() {
        ACLChange aclChange = new ACLChange(
            MailboxACL.EMPTY,
            MailboxACL.EMPTY);

        assertThat(aclChange.removedEntries()).isEmpty();
    }

    @Test
    public void changedEntriesShouldReturnEmptyWhenSameACL() {
        ACLChange aclChange = new ACLChange(
            MailboxACL.EMPTY,
            MailboxACL.EMPTY);

        assertThat(aclChange.changedEntries()).isEmpty();
    }

    @Test
    public void addedEntriesShouldReturnNewEntryWhenAddedEntry() throws Exception {
        ACLChange aclChange = new ACLChange(
            MailboxACL.EMPTY,
            MailboxACL.EMPTY.apply(
                MailboxACL.command()
                    .key(ENTRY_KEY)
                    .rights(RIGHTS)
                    .asAddition()));

        assertThat(aclChange.addedEntries())
            .containsOnly(new Entry(ENTRY_KEY, RIGHTS));
    }

    @Test
    public void changedEntriesShouldReturnEmptyWhenAddedEntry() throws Exception {
        ACLChange aclChange = new ACLChange(
            MailboxACL.EMPTY,
            MailboxACL.EMPTY.apply(
                MailboxACL.command()
                    .key(ENTRY_KEY)
                    .rights(RIGHTS)
                    .asAddition()));

        assertThat(aclChange.changedEntries())
            .isEmpty();
    }

    @Test
    public void removedEntriesShouldReturnEmptyWhenAddedEntry() throws Exception {
        ACLChange aclChange = new ACLChange(
            MailboxACL.EMPTY,
            MailboxACL.EMPTY.apply(
                MailboxACL.command()
                    .key(ENTRY_KEY)
                    .rights(RIGHTS)
                    .asAddition()));

        assertThat(aclChange.removedEntries())
            .isEmpty();
    }

    @Test
    public void addedEntriesShouldReturnEmptyWhenRemovedEntry() throws Exception {
        ACLChange aclChange = new ACLChange(
            MailboxACL.EMPTY.apply(
                MailboxACL.command()
                    .key(ENTRY_KEY)
                    .rights(RIGHTS)
                    .asAddition()),
            MailboxACL.EMPTY);

        assertThat(aclChange.addedEntries())
            .isEmpty();
    }

    @Test
    public void changedEntriesShouldReturnEmptyWhenRemovedEntry() throws Exception {
        ACLChange aclChange = new ACLChange(
            MailboxACL.EMPTY.apply(
                MailboxACL.command()
                    .key(ENTRY_KEY)
                    .rights(RIGHTS)
                    .asAddition()),
            MailboxACL.EMPTY);

        assertThat(aclChange.changedEntries())
            .isEmpty();
    }

    @Test
    public void removedEntriesShouldReturnEntryWhenRemovedEntry() throws Exception {
        ACLChange aclChange = new ACLChange(
            MailboxACL.EMPTY.apply(
                MailboxACL.command()
                    .key(ENTRY_KEY)
                    .rights(RIGHTS)
                    .asAddition()),
            MailboxACL.EMPTY);

        assertThat(aclChange.removedEntries())
            .containsOnly(new Entry(ENTRY_KEY, RIGHTS));
    }

    @Test
    public void removedEntriesShouldReturnEmptyWhenChangedEntry() throws Exception {
        ACLChange aclChange = new ACLChange(
            MailboxACL.EMPTY.apply(
                MailboxACL.command()
                    .key(ENTRY_KEY)
                    .rights(RIGHTS)
                    .asAddition()),
            MailboxACL.EMPTY.apply(
                MailboxACL.command()
                    .key(ENTRY_KEY)
                    .rights(Right.Lookup)
                    .asAddition()));

        assertThat(aclChange.removedEntries())
            .isEmpty();
    }

    @Test
    public void addedEntriesShouldReturnEmptyWhenChangedEntry() throws Exception {
        ACLChange aclChange = new ACLChange(
            MailboxACL.EMPTY.apply(
                MailboxACL.command()
                    .key(ENTRY_KEY)
                    .rights(RIGHTS)
                    .asAddition()),
            MailboxACL.EMPTY.apply(
                MailboxACL.command()
                    .key(ENTRY_KEY)
                    .rights(Right.Lookup)
                    .asAddition()));

        assertThat(aclChange.addedEntries())
            .isEmpty();
    }

    @Test
    public void changedEntriesShouldReturnEntryWhenChangedEntry() throws Exception {
        ACLChange aclChange = new ACLChange(
            MailboxACL.EMPTY.apply(
                MailboxACL.command()
                    .key(ENTRY_KEY)
                    .rights(Right.Administer)
                    .asAddition()),
            MailboxACL.EMPTY.apply(
                MailboxACL.command()
                    .key(ENTRY_KEY)
                    .rights(Right.Lookup)
                    .asAddition()));

        assertThat(aclChange.changedEntries())
            .containsOnly(new Entry(ENTRY_KEY, new Rfc4314Rights(MailboxACL.Right.Lookup)));
    }
}