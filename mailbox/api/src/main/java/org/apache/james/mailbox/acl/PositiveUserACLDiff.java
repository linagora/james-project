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

import java.util.Map;
import java.util.stream.Stream;

import org.apache.james.mailbox.model.MailboxShares;

public class PositiveUserACLDiff {

    public static PositiveUserACLDiff computeDiff(MailboxShares oldACL, MailboxShares newACL) {
        return new PositiveUserACLDiff(oldACL, newACL);
    }

    private final MailboxShares oldACL;
    private final MailboxShares newACL;

    private PositiveUserACLDiff(MailboxShares oldACL, MailboxShares newACL) {
        this.oldACL = oldACL;
        this.newACL = newACL;
    }

    public Stream<MailboxShares.Entry> addedEntries() {
        Map<MailboxShares.EntryKey, MailboxShares.Rfc4314Rights> oldEntries = oldACL.ofPositiveNameType(MailboxShares.NameType.user);

        return newACL.ofPositiveNameType(MailboxShares.NameType.user)
            .entrySet()
            .stream()
            .filter(entry -> !oldEntries.containsKey(entry.getKey()))
            .map(entry -> new MailboxShares.Entry(entry.getKey(), entry.getValue()));
    }

    public Stream<MailboxShares.Entry> removedEntries() {
        Map<MailboxShares.EntryKey, MailboxShares.Rfc4314Rights> newEntries = newACL.ofPositiveNameType(MailboxShares.NameType.user);

        return oldACL.ofPositiveNameType(MailboxShares.NameType.user)
            .entrySet()
            .stream()
            .filter(entry -> !newEntries.containsKey(entry.getKey()))
            .map(entry -> new MailboxShares.Entry(entry.getKey(), entry.getValue()));
    }

    public Stream<MailboxShares.Entry> changedEntries() {
        Map<MailboxShares.EntryKey, MailboxShares.Rfc4314Rights> oldEntries = oldACL.ofPositiveNameType(MailboxShares.NameType.user);

        return newACL.ofPositiveNameType(MailboxShares.NameType.user)
            .entrySet()
            .stream()
            .filter(entry -> oldEntries.containsKey(entry.getKey())
                && !oldEntries.get(entry.getKey()).equals(entry.getValue()))
            .map(entry -> new MailboxShares.Entry(entry.getKey(), entry.getValue()));
    }
}
