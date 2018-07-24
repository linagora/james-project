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

import org.apache.james.mailbox.acl.ACLDiff;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxACL.EntryKey;
import org.apache.james.mailbox.model.MailboxACL.NameType;
import org.apache.james.mailbox.model.MailboxACL.Rfc4314Rights;
import org.apache.james.mailbox.model.MailboxACL.Right;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

public interface MailboxMapperACLContract {
    long UID_VALIDITY = 42;
    boolean POSITIVE = true;
    boolean NEGATIVE = !POSITIVE;

    MailboxPath benwaInboxPath = MailboxPath.forUser("benwa", "INBOX");

    Mailbox mailbox();

    MailboxMapper mailboxMapper();

    @Test
    default void storedAclShouldBeEmptyByDefault() throws MailboxException {
        mailboxMapper().save(mailbox());

        assertThat(
            mailboxMapper().findMailboxById(mailbox().getMailboxId())
                .getACL()
                .getEntries())
            .isEmpty();
    }

    @Test
    default void updateAclShouldSaveAclWhenReplace() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = new EntryKey("user", NameType.user, NEGATIVE);
        Rfc4314Rights rights = new Rfc4314Rights(Right.Administer, Right.PerformExpunge, Right.Write, Right.WriteSeenFlag);
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key).rights(rights).asReplacement());

        assertThat(
            mailboxMapper().findMailboxById(mailbox().getMailboxId())
                .getACL()
                .getEntries())
            .hasSize(1)
            .containsEntry(key, rights);
    }

    @Test
    default void updateAclShouldOverwriteStoredAclWhenReplace() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = new EntryKey("user", NameType.user, NEGATIVE);
        Rfc4314Rights rights = new Rfc4314Rights(Right.Administer, Right.PerformExpunge, Right.Write, Right.WriteSeenFlag);
        Rfc4314Rights newRights = new Rfc4314Rights(Right.WriteSeenFlag, Right.CreateMailbox, Right.Administer, Right.PerformExpunge, Right.DeleteMessages);

        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key).rights(rights).asReplacement());
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key).rights(newRights).asReplacement());

        assertThat(
            mailboxMapper().findMailboxById(mailbox().getMailboxId())
                .getACL()
                .getEntries())
            .hasSize(1)
            .containsEntry(key, newRights);
    }

    @Test
    default void updateAclShouldTreatNegativeAndPositiveRightSeparately() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key1 = new EntryKey("user", NameType.user, NEGATIVE);
        EntryKey key2 = new EntryKey("user", NameType.user, POSITIVE);
        Rfc4314Rights rights = new Rfc4314Rights(Right.Administer, Right.PerformExpunge, Right.Write, Right.WriteSeenFlag);
        Rfc4314Rights newRights = new Rfc4314Rights(Right.WriteSeenFlag, Right.CreateMailbox, Right.Administer, Right.PerformExpunge, Right.DeleteMessages);
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key1).rights(rights).asReplacement());
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key2).rights(newRights).asReplacement());

        assertThat(
            mailboxMapper().findMailboxById(mailbox().getMailboxId())
                .getACL()
                .getEntries())
            .hasSize(2)
            .containsEntry(key1, rights)
            .containsEntry(key2, newRights);
    }

    @Test
    default void updateAclShouldTreatNameTypesRightSeparately() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key1 = new EntryKey("user", NameType.user, NEGATIVE);
        EntryKey key2 = new EntryKey("user", NameType.group, NEGATIVE);
        Rfc4314Rights rights = new Rfc4314Rights(Right.Administer, Right.PerformExpunge, Right.Write, Right.WriteSeenFlag);
        Rfc4314Rights newRights = new Rfc4314Rights(Right.WriteSeenFlag, Right.CreateMailbox, Right.Administer, Right.PerformExpunge, Right.DeleteMessages);
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key1).rights(rights).asReplacement());
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key2).rights(newRights).asReplacement());

        assertThat(
            mailboxMapper().findMailboxById(mailbox().getMailboxId())
                .getACL()
                .getEntries())
            .hasSize(2)
            .containsEntry(key1, rights)
            .containsEntry(key2, newRights);
    }

    @Test
    default void updateAclShouldCleanAclEntryWhenEmptyReplace() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = new EntryKey("user", NameType.user, NEGATIVE);
        Rfc4314Rights rights = new Rfc4314Rights(Right.Administer, Right.PerformExpunge, Right.Write, Right.WriteSeenFlag);
        Rfc4314Rights newRights = new Rfc4314Rights();
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key).rights(rights).asReplacement());
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key).rights(newRights).asReplacement());

        assertThat(
            mailboxMapper().findMailboxById(mailbox().getMailboxId())
                .getACL()
                .getEntries())
            .isEmpty();
    }

    @Test
    default void updateAclShouldCombineStoredAclWhenAdd() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = new EntryKey("user", NameType.user, NEGATIVE);
        Rfc4314Rights rights = new Rfc4314Rights(Right.Administer, Right.PerformExpunge, Right.Write, Right.WriteSeenFlag);
        Rfc4314Rights newRights = new Rfc4314Rights(Right.WriteSeenFlag, Right.CreateMailbox, Right.Administer, Right.PerformExpunge, Right.DeleteMessages);
        Rfc4314Rights bothRights = new Rfc4314Rights(Right.Administer, Right.WriteSeenFlag, Right.PerformExpunge, Right.Write, Right.CreateMailbox, Right.DeleteMessages);
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key).rights(rights).asReplacement());
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key).rights(newRights).asAddition());

        assertThat(
            mailboxMapper().findMailboxById(mailbox().getMailboxId())
                .getACL()
                .getEntries())
            .hasSize(1)
            .containsEntry(key, bothRights);
    }

    @Test
    default void removeAclShouldRemoveSomeStoredAclWhenAdd() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = new EntryKey("user", NameType.user, NEGATIVE);
        Rfc4314Rights rights = new Rfc4314Rights(Right.Administer, Right.PerformExpunge, Right.Write, Right.WriteSeenFlag);
        Rfc4314Rights removedRights = new Rfc4314Rights(Right.WriteSeenFlag, Right.PerformExpunge);
        Rfc4314Rights finalRights = new Rfc4314Rights(Right.Administer, Right.Write);
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key).rights(rights).asReplacement());
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key).rights(removedRights).asRemoval());

        assertThat(
            mailboxMapper().findMailboxById(mailbox().getMailboxId())
                .getACL()
                .getEntries())
            .hasSize(1)
            .containsEntry(key, finalRights);
    }

    @Test
    default void removeAclShouldNotFailWhenRemovingNonExistingRight() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = new EntryKey("user", NameType.user, NEGATIVE);
        Rfc4314Rights rights = new Rfc4314Rights(Right.Administer, Right.PerformExpunge, Right.Write, Right.WriteSeenFlag);
        Rfc4314Rights removedRights = new Rfc4314Rights(Right.WriteSeenFlag, Right.PerformExpunge, Right.Lookup);
        Rfc4314Rights finalRights = new Rfc4314Rights(Right.Administer, Right.Write);
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key).rights(rights).asReplacement());
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key).rights(removedRights).asRemoval());

        assertThat(
            mailboxMapper().findMailboxById(mailbox().getMailboxId())
                .getACL()
                .getEntries())
            .hasSize(1)
            .containsEntry(key, finalRights);
    }

    @Test
    default void resetAclShouldReplaceStoredAcl() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = new EntryKey("user", NameType.user, NEGATIVE);
        Rfc4314Rights rights = new Rfc4314Rights(Right.Administer, Right.PerformExpunge, Right.Write, Right.WriteSeenFlag);
        Rfc4314Rights newRights = new Rfc4314Rights(Right.WriteSeenFlag, Right.CreateMailbox, Right.Administer, Right.PerformExpunge, Right.DeleteMessages);
        mailboxMapper().updateACL(mailbox(), MailboxACL.command().key(key).rights(rights).asReplacement());
        mailboxMapper().setACL(mailbox(), new MailboxACL(ImmutableMap.of(key, newRights)));

        assertThat(
            mailboxMapper().findMailboxById(mailbox().getMailboxId())
                .getACL()
                .getEntries())
            .hasSize(1)
            .containsEntry(key, newRights);
    }
    
    @Test
    default void resetAclShouldInitializeStoredAcl() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = new EntryKey("user", NameType.user, NEGATIVE);
        Rfc4314Rights rights = new Rfc4314Rights(Right.WriteSeenFlag, Right.CreateMailbox, Right.Administer, Right.PerformExpunge, Right.DeleteMessages);
        mailboxMapper().setACL(mailbox(),
            new MailboxACL(ImmutableMap.of(key, rights)));

        assertThat(
            mailboxMapper().findMailboxById(mailbox().getMailboxId())
                .getACL()
                .getEntries())
            .hasSize(1)
            .containsEntry(key, rights);
    }

    @Test
    default void findMailboxesShouldReturnEmptyWhenNone() throws MailboxException {
        mailboxMapper().save(mailbox());

        assertThat(mailboxMapper().findNonPersonalMailboxes("user", Right.Administer)).isEmpty();
    }

    @Test
    default void findMailboxesShouldReturnEmptyWhenRightDoesntMatch() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = EntryKey.createUserEntryKey("user");
        Rfc4314Rights rights = new Rfc4314Rights(Right.Administer);
        mailboxMapper().updateACL(mailbox(),
            MailboxACL.command()
                .key(key)
                .rights(rights)
                .asReplacement());

        assertThat(mailboxMapper().findNonPersonalMailboxes("user", Right.Read)).isEmpty();
    }

    @Test
    default void updateACLShouldInsertUsersRights() throws MailboxException {
        mailboxMapper().save(mailbox());

        Rfc4314Rights rights = new Rfc4314Rights(Right.Administer, Right.PerformExpunge);
        mailboxMapper().updateACL(mailbox(),
            MailboxACL.command()
                .key(EntryKey.createUserEntryKey("user"))
                .rights(rights)
                .asAddition());

        assertThat(mailboxMapper().findNonPersonalMailboxes("user", Right.Administer))
            .containsOnly(mailbox());
    }

    @Test
    default void updateACLShouldOverwriteUsersRights() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = EntryKey.createUserEntryKey("user");
        Rfc4314Rights initialRights = new Rfc4314Rights(Right.Administer);
        mailboxMapper().updateACL(mailbox(),
            MailboxACL.command()
                .key(key)
                .rights(initialRights)
                .asReplacement());
        Rfc4314Rights newRights = new Rfc4314Rights(Right.Read);
        mailboxMapper().updateACL(mailbox(),
            MailboxACL.command()
                .key(key)
                .rights(newRights)
                .asReplacement());

        assertThat(mailboxMapper().findNonPersonalMailboxes("user", Right.Read))
            .containsOnly(mailbox());

        assertThat(mailboxMapper().findNonPersonalMailboxes("user", Right.Administer))
            .isEmpty();
    }

    @Test
    default void findMailboxesShouldNotReportDeletedACLViaReplace() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = EntryKey.createUserEntryKey("user");
        Rfc4314Rights initialRights = new Rfc4314Rights(Right.Administer);
        mailboxMapper().updateACL(mailbox(),
            MailboxACL.command()
                .key(key)
                .mode(MailboxACL.EditMode.REPLACE)
                .rights(initialRights)
                .build());
        mailboxMapper().updateACL(mailbox(),
            MailboxACL.command()
                .key(key)
                .mode(MailboxACL.EditMode.REPLACE)
                .rights(new Rfc4314Rights())
                .build());

        assertThat(mailboxMapper().findNonPersonalMailboxes("user", Right.Administer))
            .isEmpty();
    }

    @Test
    default void findMailboxesShouldNotReportDeletedACLViaRemove() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = EntryKey.createUserEntryKey("user");
        Rfc4314Rights initialRights = new Rfc4314Rights(Right.Administer);
        mailboxMapper().updateACL(mailbox(),
            MailboxACL.command()
                .key(key)
                .rights(initialRights)
                .asReplacement());
        mailboxMapper().updateACL(mailbox(),
            MailboxACL.command()
                .key(key)
                .rights(initialRights)
                .asRemoval());

        assertThat(mailboxMapper().findNonPersonalMailboxes("user", Right.Administer))
            .isEmpty();
    }

    @Test
    default void findMailboxesShouldNotReportDeletedMailboxes() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = EntryKey.createUserEntryKey("user");
        Rfc4314Rights initialRights = new Rfc4314Rights(Right.Administer);
        mailboxMapper().updateACL(mailbox(),
            MailboxACL.command()
                .key(key)
                .rights(initialRights)
                .asReplacement());
        mailboxMapper().delete(mailbox());

        assertThat(mailboxMapper().findNonPersonalMailboxes("user", Right.Administer))
            .isEmpty();
    }

    @Test
    default void setACLShouldStoreMultipleUsersRights() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey user1 = EntryKey.createUserEntryKey("user1");
        EntryKey user2 = EntryKey.createUserEntryKey("user2");

        mailboxMapper().setACL(mailbox(), new MailboxACL(
            new MailboxACL.Entry(user1, new Rfc4314Rights(Right.Administer)),
            new MailboxACL.Entry(user2, new Rfc4314Rights(Right.Read))));

        assertThat(mailboxMapper().findNonPersonalMailboxes("user1", Right.Administer))
            .containsOnly(mailbox());
        assertThat(mailboxMapper().findNonPersonalMailboxes("user2", Right.Read))
            .containsOnly(mailbox());
    }

    @Test
    default void findMailboxesShouldNotReportRightsRemovedViaSetAcl() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey user1 = EntryKey.createUserEntryKey("user1");
        EntryKey user2 = EntryKey.createUserEntryKey("user2");

        mailboxMapper().setACL(mailbox(), new MailboxACL(
            new MailboxACL.Entry(user1, new Rfc4314Rights(Right.Administer)),
            new MailboxACL.Entry(user2, new Rfc4314Rights(Right.Read))));

        mailboxMapper().setACL(mailbox(), new MailboxACL(
            new MailboxACL.Entry(user2, new Rfc4314Rights(Right.Read))));

        assertThat(mailboxMapper().findNonPersonalMailboxes("user1", Right.Administer))
            .isEmpty();
    }

    @Test
    default void findMailboxesShouldReportRightsUpdatedViaSetAcl() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey user1 = EntryKey.createUserEntryKey("user1");
        EntryKey user2 = EntryKey.createUserEntryKey("user2");

        mailboxMapper().setACL(mailbox(), new MailboxACL(
            new MailboxACL.Entry(user1, new Rfc4314Rights(Right.Administer)),
            new MailboxACL.Entry(user2, new Rfc4314Rights(Right.Read))));

        mailboxMapper().setACL(mailbox(), new MailboxACL(
            new MailboxACL.Entry(user2, new Rfc4314Rights(Right.Write))));

        assertThat(mailboxMapper().findNonPersonalMailboxes("user2", Right.Write))
            .containsOnly(mailbox());
    }

    @Test
    default void findMailboxByPathShouldReturnMailboxWithACL() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = EntryKey.createUserEntryKey("user");
        Rfc4314Rights rights = new Rfc4314Rights(Right.WriteSeenFlag, Right.CreateMailbox, Right.Administer, Right.PerformExpunge, Right.DeleteMessages);
        mailboxMapper().setACL(mailbox(),
            new MailboxACL(ImmutableMap.of(key, rights)));

        assertThat(
            mailboxMapper().findMailboxByPath(mailbox().generateAssociatedPath())
                .getACL()
                .getEntries())
            .hasSize(1)
            .containsEntry(key, rights);
    }

    @Test
    default void setACLShouldReturnACLDiff() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = EntryKey.createUserEntryKey("user");
        Rfc4314Rights rights = new Rfc4314Rights(Right.WriteSeenFlag, Right.CreateMailbox, Right.Administer, Right.PerformExpunge, Right.DeleteMessages);

        ACLDiff expectAclDiff = ACLDiff.computeDiff(MailboxACL.EMPTY, MailboxACL.EMPTY.apply(
            MailboxACL.command()
                .key(key)
                .rights(rights)
                .asAddition()));

        assertThat(mailboxMapper().setACL(mailbox(),
            new MailboxACL(ImmutableMap.of(key, rights)))).isEqualTo(expectAclDiff);
    }

    @Test
    default void updateACLShouldReturnACLDiff() throws MailboxException {
        mailboxMapper().save(mailbox());

        EntryKey key = EntryKey.createUserEntryKey("user");
        Rfc4314Rights rights = new Rfc4314Rights(Right.WriteSeenFlag, Right.CreateMailbox, Right.Administer, Right.PerformExpunge, Right.DeleteMessages);

        MailboxACL.ACLCommand aclCommand = MailboxACL.command()
            .key(key)
            .rights(rights)
            .asAddition();

        ACLDiff expectAclDiff = ACLDiff.computeDiff(MailboxACL.EMPTY, MailboxACL.EMPTY.apply(aclCommand));

        assertThat(mailboxMapper().updateACL(mailbox(), aclCommand)).isEqualTo(expectAclDiff);
    }
}
