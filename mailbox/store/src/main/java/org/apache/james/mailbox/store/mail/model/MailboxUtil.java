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

import java.util.List;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.PathDelimiter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class MailboxUtil {

    public static boolean isMailboxChildOf(Mailbox mailbox, Mailbox potentialParent, MailboxSession mailboxSession) {
        return mailbox.getNamespace().equals(potentialParent.getNamespace())
            && mailbox.getUser().equals(potentialParent.getUser())
            && isChildren(potentialParent.getName(), mailbox.getName(), mailboxSession.getPathDelimiter());
    }

    public static boolean isChildren(String potentialParent, String potentialChild, PathDelimiter pathDelimiter) {
        List<String> parentsParts = pathDelimiter.split(potentialParent);
        List<String> childParts = pathDelimiter.split(potentialChild);

        if (parentsParts.size() >= childParts.size()) {
            return false;
        }
        return ImmutableList.copyOf(parentsParts).equals(
            ImmutableList.copyOf(
                Iterables.limit(childParts, parentsParts.size())));
    }
}
