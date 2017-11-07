/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.james.mailbox.acl;

import org.apache.james.mailbox.exception.UnsupportedRightException;
import org.apache.james.mailbox.model.MailboxShares;
import org.apache.james.mailbox.model.MailboxShares.Entry;
import org.apache.james.mailbox.model.MailboxShares.EntryKey;
import org.apache.james.mailbox.model.MailboxShares.NameType;
import org.apache.james.mailbox.model.MailboxShares.Rfc4314Rights;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Peter Palaga
 */
public class UnionMailboxSharesResolverTest {

    private static final String GROUP_1 = "group1";
    private static final String GROUP_2 = "group2";

    private static final String USER_1 = "user1";
    private static final String USER_2 = "user2";

    private MailboxShares anybodyRead;
    private MailboxShares anybodyReadNegative;
    private UnionMailboxSharesResolver anyoneReadListGlobal;
    private MailboxShares authenticatedRead;
    private UnionMailboxSharesResolver authenticatedReadListWriteGlobal;
    private MailboxShares authenticatedReadNegative;
    private MailboxShares group1Read;
    private MailboxShares group1ReadNegative;
    private SimpleGroupMembershipResolver groupMembershipResolver;
    private UnionMailboxSharesResolver negativeGroup2FullGlobal;
    private UnionMailboxSharesResolver noGlobals;
    private UnionMailboxSharesResolver ownerFullGlobal;
    private MailboxShares ownerRead;
    private MailboxShares ownerReadNegative;
    private MailboxShares user1Read;
    private MailboxShares user1ReadNegative;
    private EntryKey user1Key;
    private EntryKey user2Key;
    private EntryKey group1Key;
    private EntryKey group2Key;

    @Before
    public void setUp() throws Exception {
        user1Key = EntryKey.createUserEntryKey(USER_1);
        user2Key = EntryKey.createUserEntryKey(USER_2);
        group1Key = EntryKey.createGroupEntryKey(GROUP_1);
        group2Key = EntryKey.createGroupEntryKey(GROUP_2);

        MailboxShares mailboxShares = new MailboxShares(new Entry(MailboxShares.AUTHENTICATED_KEY, MailboxShares.FULL_RIGHTS));
        authenticatedReadListWriteGlobal = new UnionMailboxSharesResolver(mailboxShares, mailboxShares);
        mailboxShares = new MailboxShares(new Entry(MailboxShares.ANYBODY_KEY, Rfc4314Rights.fromSerializedRfc4314Rights("rl")));
        anyoneReadListGlobal = new UnionMailboxSharesResolver(mailboxShares, mailboxShares);
        mailboxShares = new MailboxShares(new Entry(MailboxShares.OWNER_KEY, MailboxShares.FULL_RIGHTS));
        ownerFullGlobal = new UnionMailboxSharesResolver(mailboxShares, mailboxShares);
        noGlobals = new UnionMailboxSharesResolver(MailboxShares.EMPTY, MailboxShares.EMPTY);
        mailboxShares = new MailboxShares(new Entry(new EntryKey(GROUP_2, NameType.group, true), MailboxShares.FULL_RIGHTS));
        negativeGroup2FullGlobal = new UnionMailboxSharesResolver(mailboxShares, new MailboxShares(new Entry(new EntryKey(GROUP_2, NameType.group, true), MailboxShares.FULL_RIGHTS)));

        groupMembershipResolver = new SimpleGroupMembershipResolver();
        groupMembershipResolver.addMembership(GROUP_1, USER_1);
        groupMembershipResolver.addMembership(GROUP_2, USER_2);

        user1Read = new MailboxShares(new Entry(user1Key, Rfc4314Rights.fromSerializedRfc4314Rights("r")));
        user1ReadNegative = new MailboxShares(new Entry(EntryKey.createUserEntryKey(USER_1, true), Rfc4314Rights.fromSerializedRfc4314Rights("r")));

        group1Read = new MailboxShares(new Entry(group1Key, Rfc4314Rights.fromSerializedRfc4314Rights("r")));
        group1ReadNegative = new MailboxShares(new Entry(EntryKey.createGroupEntryKey(GROUP_1, true), Rfc4314Rights.fromSerializedRfc4314Rights("r")));

        anybodyRead = new MailboxShares(new Entry(MailboxShares.ANYBODY_KEY, Rfc4314Rights.fromSerializedRfc4314Rights("r")));
        anybodyReadNegative = new MailboxShares(new Entry(MailboxShares.ANYBODY_NEGATIVE_KEY, Rfc4314Rights.fromSerializedRfc4314Rights("r")));

        authenticatedRead = new MailboxShares(new Entry(MailboxShares.AUTHENTICATED_KEY, Rfc4314Rights.fromSerializedRfc4314Rights("r")));
        authenticatedReadNegative = new MailboxShares(new Entry(MailboxShares.AUTHENTICATED_NEGATIVE_KEY, Rfc4314Rights.fromSerializedRfc4314Rights("r")));

        ownerRead = new MailboxShares(new Entry(MailboxShares.OWNER_KEY, Rfc4314Rights.fromSerializedRfc4314Rights("r")));
        ownerReadNegative = new MailboxShares(new Entry(MailboxShares.OWNER_NEGATIVE_KEY, Rfc4314Rights.fromSerializedRfc4314Rights("r")));

    }

    @Test
    public void testAppliesNullUser() throws UnsupportedRightException {

        Assert.assertFalse(UnionMailboxSharesResolver.applies(user1Key, null, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(user2Key, null, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(group1Key, null, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(group2Key, null, groupMembershipResolver, USER_1, false));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(MailboxShares.ANYBODY_KEY, null, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(MailboxShares.AUTHENTICATED_KEY, null, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(MailboxShares.OWNER_KEY, null, groupMembershipResolver, USER_1, false));
    }

    @Test
    public void testAppliesUser() throws UnsupportedRightException {
        /* requester is the resource owner */
        Assert.assertTrue(UnionMailboxSharesResolver.applies(user1Key, user1Key, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(user2Key, user1Key, groupMembershipResolver, USER_1, false));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(group1Key, user1Key, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(group2Key, user1Key, groupMembershipResolver, USER_1, false));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(MailboxShares.ANYBODY_KEY, user1Key, groupMembershipResolver, USER_1, false));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(MailboxShares.AUTHENTICATED_KEY, user1Key, groupMembershipResolver, USER_1, false));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(MailboxShares.OWNER_KEY, user1Key, groupMembershipResolver, USER_1, false));

        /* requester is not the resource user */
        Assert.assertTrue(UnionMailboxSharesResolver.applies(user1Key, user1Key, groupMembershipResolver, USER_2, false));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(user2Key, user1Key, groupMembershipResolver, USER_2, false));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(group1Key, user1Key, groupMembershipResolver, USER_2, false));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(group2Key, user1Key, groupMembershipResolver, USER_2, false));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(MailboxShares.ANYBODY_KEY, user1Key, groupMembershipResolver, USER_2, false));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(MailboxShares.AUTHENTICATED_KEY, user1Key, groupMembershipResolver, USER_2, false));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(MailboxShares.OWNER_KEY, user1Key, groupMembershipResolver, USER_2, false));

        /* requester member of owner group */
        Assert.assertTrue(UnionMailboxSharesResolver.applies(user1Key, user1Key, groupMembershipResolver, GROUP_1, true));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(user2Key, user1Key, groupMembershipResolver, GROUP_1, true));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(group1Key, user1Key, groupMembershipResolver, GROUP_1, true));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(group2Key, user1Key, groupMembershipResolver, GROUP_1, true));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(MailboxShares.ANYBODY_KEY, user1Key, groupMembershipResolver, GROUP_1, true));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(MailboxShares.AUTHENTICATED_KEY, user1Key, groupMembershipResolver, GROUP_1, true));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(MailboxShares.OWNER_KEY, user1Key, groupMembershipResolver, GROUP_1, true));

        /* requester not member of owner group */
        Assert.assertTrue(UnionMailboxSharesResolver.applies(user1Key, user1Key, groupMembershipResolver, GROUP_2, true));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(user2Key, user1Key, groupMembershipResolver, GROUP_2, true));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(group1Key, user1Key, groupMembershipResolver, GROUP_2, true));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(group2Key, user1Key, groupMembershipResolver, GROUP_2, true));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(MailboxShares.ANYBODY_KEY, user1Key, groupMembershipResolver, GROUP_2, true));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(MailboxShares.AUTHENTICATED_KEY, user1Key, groupMembershipResolver, GROUP_2, true));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(MailboxShares.OWNER_KEY, user1Key, groupMembershipResolver, GROUP_2, true));

        /* owner query */
        Assert.assertFalse(UnionMailboxSharesResolver.applies(user1Key, MailboxShares.OWNER_KEY, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(user2Key, MailboxShares.OWNER_KEY, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(group1Key, MailboxShares.OWNER_KEY, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(UnionMailboxSharesResolver.applies(group2Key, MailboxShares.OWNER_KEY, groupMembershipResolver, USER_1, false));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(MailboxShares.ANYBODY_KEY, MailboxShares.OWNER_KEY, groupMembershipResolver, USER_1, false));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(MailboxShares.AUTHENTICATED_KEY, MailboxShares.OWNER_KEY, groupMembershipResolver, USER_1, false));
        Assert.assertTrue(UnionMailboxSharesResolver.applies(MailboxShares.OWNER_KEY, MailboxShares.OWNER_KEY, groupMembershipResolver, USER_1, false));

    }

    @Test
    public void testResolveRightsNullUser() throws UnsupportedRightException {

        assertThat(
            anyoneReadListGlobal.resolveRights(null, groupMembershipResolver, user1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();

        assertThat(
            anyoneReadListGlobal.resolveRights(null, groupMembershipResolver, user1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(null, groupMembershipResolver, user1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(null, groupMembershipResolver, user1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(null, groupMembershipResolver, user1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(null, groupMembershipResolver, user1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            ownerFullGlobal.resolveRights(null, groupMembershipResolver, user1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(null, groupMembershipResolver, user1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            noGlobals.resolveRights(null, groupMembershipResolver, user1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(null, groupMembershipResolver, user1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(null, groupMembershipResolver, user1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(null, groupMembershipResolver, group1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(null, groupMembershipResolver, group1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(null, groupMembershipResolver, group1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(null, groupMembershipResolver, group1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(null, groupMembershipResolver, group1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            ownerFullGlobal.resolveRights(null, groupMembershipResolver, group1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(null, groupMembershipResolver, group1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            noGlobals.resolveRights(null, groupMembershipResolver, group1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(null, groupMembershipResolver, group1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(null, groupMembershipResolver, group1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(null, groupMembershipResolver, anybodyRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(null, groupMembershipResolver, anybodyReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(null, groupMembershipResolver, anybodyRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(null, groupMembershipResolver, anybodyReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(null, groupMembershipResolver, anybodyRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(null, groupMembershipResolver, anybodyReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(null, groupMembershipResolver, anybodyRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(null, groupMembershipResolver, anybodyReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(null, groupMembershipResolver, anybodyRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(null, groupMembershipResolver, anybodyReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(null, groupMembershipResolver, authenticatedRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(null, groupMembershipResolver, authenticatedReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(null, groupMembershipResolver, authenticatedRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(null, groupMembershipResolver, authenticatedReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(null, groupMembershipResolver, authenticatedRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            ownerFullGlobal.resolveRights(null, groupMembershipResolver, authenticatedReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(null, groupMembershipResolver, authenticatedRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            noGlobals.resolveRights(null, groupMembershipResolver, authenticatedReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(null, groupMembershipResolver, authenticatedRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(null, groupMembershipResolver, authenticatedReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(null, groupMembershipResolver, ownerRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(null, groupMembershipResolver, ownerReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(null, groupMembershipResolver, ownerRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(null, groupMembershipResolver, ownerReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(null, groupMembershipResolver, ownerRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            ownerFullGlobal.resolveRights(null, groupMembershipResolver, ownerReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(null, groupMembershipResolver, ownerRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            noGlobals.resolveRights(null, groupMembershipResolver, ownerReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(null, groupMembershipResolver, ownerRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(null, groupMembershipResolver, ownerReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

    }

    @Test
    public void testResolveRightsNullUserGlobals() throws UnsupportedRightException {
        assertThat(
            anyoneReadListGlobal.resolveRights(null, groupMembershipResolver, user1Read, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(null, groupMembershipResolver, MailboxShares.EMPTY, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            ownerFullGlobal.resolveRights(null, groupMembershipResolver, MailboxShares.EMPTY, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            noGlobals.resolveRights(null, groupMembershipResolver, MailboxShares.EMPTY, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(null, groupMembershipResolver, MailboxShares.EMPTY, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
    }


    @Test
    public void testResolveRightsUserSelfOwner() throws UnsupportedRightException {

        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, user1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, group1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, anybodyRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, ownerRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, USER_1, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

    }


    @Test
    public void testResolveRightsUserNotOwner() throws UnsupportedRightException {

        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, user1Read, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, group1Read, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, anybodyRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isTrue();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, ownerRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, USER_2, false)
                .contains(MailboxShares.Right.Read))
            .isFalse();

    }
    @Test
    public void testResolveRightsUserMemberOfOwnerGroup() throws UnsupportedRightException {

        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, user1Read, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, group1Read, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, anybodyRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, ownerRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, GROUP_1, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

    }


    @Test
    public void testResolveRightsUserNotMemberOfOwnerGroup() throws UnsupportedRightException {

        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, user1Read, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, user1Read, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, user1ReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, group1Read, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, group1Read, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, group1ReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, anybodyRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, anybodyReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, authenticatedReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();


        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            anyoneReadListGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();

        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();
        assertThat(
            authenticatedReadListWriteGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isTrue();

        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            ownerFullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, ownerRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            noGlobals.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerRead, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();
        assertThat(
            negativeGroup2FullGlobal.resolveRights(USER_1, groupMembershipResolver, ownerReadNegative, GROUP_2, true)
                .contains(MailboxShares.Right.Read))
            .isFalse();

    }
}
