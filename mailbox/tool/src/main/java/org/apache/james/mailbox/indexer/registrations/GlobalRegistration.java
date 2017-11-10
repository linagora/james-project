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

package org.apache.james.mailbox.indexer.registrations;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;

public class GlobalRegistration implements MailboxListener {

    private final ConcurrentHashMap<MailboxId, Boolean> isPathDeleted;
    private final ConcurrentHashMap<MailboxId, MailboxPath> nameCorrespondence;

    public GlobalRegistration() {
        this.isPathDeleted = new ConcurrentHashMap<>();
        this.nameCorrespondence = new ConcurrentHashMap<>();
    }

    public Optional<MailboxPath> getPathToIndex(MailboxId mailboxId) {
        if (isPathDeleted.get(mailboxId) != null) {
            return Optional.empty();
        }
        return Optional.ofNullable(nameCorrespondence.get(mailboxId));
    }

    @Override
    public ListenerType getType() {
        return ListenerType.EACH_NODE;
    }

    @Override
    public ExecutionMode getExecutionMode() {
        return ExecutionMode.SYNCHRONOUS;
    }

    @Override
    public void event(Event event) {
        if (event instanceof MailboxDeletion) {
            isPathDeleted.put(event.getMailboxId(), true);
        } else if (event instanceof MailboxRenamed) {
            nameCorrespondence.put(event.getMailboxId(), ((MailboxRenamed) event).getNewPath());
        }
    }
}
