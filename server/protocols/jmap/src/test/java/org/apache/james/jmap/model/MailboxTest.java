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

import org.junit.Test;

public class MailboxTest {

    @Test(expected=NullPointerException.class)
    public void idShouldThrowWhenIdIsNull() {
        Mailbox.builder()
            .id(null);
    }

    @Test(expected=NullPointerException.class)
    public void nameShouldThrowWhenNameIsNull() {
        Mailbox.builder()
            .name(null);
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenIdIsNull() {
        Mailbox.builder().build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenIdIsEmpty() {
        Mailbox.builder()
            .id("")
            .build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenNameIsNull() {
        Mailbox.builder()
            .id("id")
            .build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenNameIsEmpty() {
        Mailbox.builder()
            .id("id")
            .name("")
            .build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenSortOrderIsNegative() {
        Mailbox.builder()
            .id("id")
            .name("name")
            .sortOrder(-1)
            .build();
    }

    @Test(expected=IllegalStateException.class)
    public void buildShouldThrowWhenSortOrderIsGreaterThanMax() {
        Mailbox.builder()
            .id("id")
            .name("name")
            .sortOrder(Double.valueOf(Math.pow(2, 31)).intValue())
            .build();
    }

    @Test
    public void buildShouldWork() {
        String expectedId = "id";
        String expectedName = "name";
        String expectedParentId = "parentId";
        Role expectedRole = Role.DRAFTS;
        int expectedSortOrder = 123;
        boolean expectedMustBeOnlyMailbox = true;
        boolean expectedMayReadItems = true;
        boolean expectedMayAddItems = true;
        boolean expectedMayRemoveItems = true;
        boolean expectedMayCreateChild = true;
        boolean expectedMayRename = true;
        boolean expectedMayDelete = true;
        int expectedTotalMessages = 456;
        int expectedUnreadMessages = 789;
        int expectedTotalThreads = 741;
        int expectedUnreadThreads = 852;

        Mailbox mailbox = Mailbox.builder()
            .id(expectedId)
            .name(expectedName)
            .parentId(expectedParentId)
            .role(expectedRole)
            .sortOrder(expectedSortOrder)
            .mustBeOnlyMailbox(expectedMustBeOnlyMailbox)
            .mayReadItems(expectedMayReadItems)
            .mayAddItems(expectedMayAddItems)
            .mayRemoveItems(expectedMayRemoveItems)
            .mayCreateChild(expectedMayCreateChild)
            .mayRename(expectedMayRename)
            .mayDelete(expectedMayDelete)
            .totalMessages(expectedTotalMessages)
            .unreadMessages(expectedUnreadMessages)
            .totalThreads(expectedTotalThreads)
            .unreadThreads(expectedUnreadThreads)
            .build();

        assertThat(mailbox.getId()).isEqualTo(expectedId);
        assertThat(mailbox.getName()).isEqualTo(expectedName);
        assertThat(mailbox.getParentId()).isEqualTo(expectedParentId);
        assertThat(mailbox.getRole()).isEqualTo(expectedRole);
        assertThat(mailbox.getSortOrder()).isEqualTo(expectedSortOrder);
        assertThat(mailbox.isMustBeOnlyMailbox()).isEqualTo(expectedMustBeOnlyMailbox);
        assertThat(mailbox.isMayReadItems()).isEqualTo(expectedMayReadItems);
        assertThat(mailbox.isMayAddItems()).isEqualTo(expectedMayAddItems);
        assertThat(mailbox.isMayRemoveItems()).isEqualTo(expectedMayRemoveItems);
        assertThat(mailbox.isMayCreateChild()).isEqualTo(expectedMayCreateChild);
        assertThat(mailbox.isMayRename()).isEqualTo(expectedMayRename);
        assertThat(mailbox.isMayDelete()).isEqualTo(expectedMayDelete);
        assertThat(mailbox.getTotalMessages()).isEqualTo(expectedTotalMessages);
        assertThat(mailbox.getUnreadMessages()).isEqualTo(expectedUnreadMessages);
        assertThat(mailbox.getTotalThreads()).isEqualTo(expectedTotalThreads);
        assertThat(mailbox.getUnreadThreads()).isEqualTo(expectedUnreadThreads);
    }
}
