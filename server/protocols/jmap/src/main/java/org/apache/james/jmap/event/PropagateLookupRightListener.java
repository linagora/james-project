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

package org.apache.james.jmap.event;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.acl.ACLDiff;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxACL.ACLCommand;
import org.apache.james.mailbox.model.MailboxACL.Entry;
import org.apache.james.mailbox.model.MailboxACL.Right;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.StoreRightManager;
import org.apache.james.mailbox.store.event.EventFactory.MailboxRenamedEventImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropagateLookupRightListener implements MailboxListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropagateLookupRightListener.class);

    private final StoreRightManager storeRightManager;
    private final MailboxSessionMapperFactory mapperFactory;

    @Inject
    public PropagateLookupRightListener(StoreRightManager storeRightManager, MailboxSessionMapperFactory mapperFactory) {
        this.storeRightManager = storeRightManager;
        this.mapperFactory = mapperFactory;
    }

    @Override
    public ListenerType getType() {
        return ListenerType.ONCE;
    }

    @Override
    public ExecutionMode getExecutionMode() {
        return ExecutionMode.ASYNCHRONOUS;
    }

    @Override
    public void event(Event event) {
        MailboxSession mailboxSession = event.getSession();

        if (event instanceof MailboxACLUpdated) {
            MailboxACLUpdated aclUpdateEvent = (MailboxACLUpdated) event;

            updateLookupRightOnParent(mailboxSession, event.getMailboxId(), aclUpdateEvent.getAclDiff());
        } else if (event instanceof MailboxRenamed) {
            MailboxRenamedEventImpl renamedEvent = (MailboxRenamedEventImpl) event;

            updateLookupRightOnParent(mailboxSession, renamedEvent.getMailboxId(), renamedEvent.getMailbox().getACL());
        }
    }

    private void updateLookupRightOnParent(MailboxSession mailboxSession, MailboxId mailboxId, MailboxACL mailboxACL) {
        getParent(mailboxId, mailboxSession)
            .forEach(parentMailboxPath -> updateLookupRight(parentMailboxPath, mailboxSession,
                mailboxACL.getEntries()
                    .entrySet()
                    .stream()
                    .map(entry -> new MailboxACL.Entry(entry.getKey(), entry.getValue()))));
    }

    private void updateLookupRightOnParent(MailboxSession mailboxSession, MailboxId mailboxId, ACLDiff aclDiff) {
        getParent(mailboxId, mailboxSession)
            .forEach(parentMailboxPath -> {
                updateLookupRight(parentMailboxPath, mailboxSession, aclDiff.addedEntries());
                updateLookupRight(parentMailboxPath, mailboxSession, aclDiff.changedEntries());
            });
    }

    private void updateLookupRight(MailboxPath mailboxPath, MailboxSession session, Stream<MailboxACL.Entry> entries) {
        entries
            .filter(entry -> !entry.getKey().isNegative())
            .filter(entry -> entry.getValue().contains(Right.Lookup))
            .forEach(entry -> updateRight(mailboxPath, session, entry));
    }

    private Stream<MailboxPath> getParent(MailboxId mailboxId, MailboxSession mailboxSession) {
        try {
            MailboxPath mailboxPath = mapperFactory.getMailboxMapper(mailboxSession)
                .findMailboxById(mailboxId)
                .generateAssociatedPath();

            return mailboxPath.getHierarchyLevels(mailboxSession.getPathDelimiter())
                .stream()
                .filter(hierarchyMailboxPath -> !hierarchyMailboxPath.equals(mailboxPath));
        } catch (MailboxException e) {
            LOGGER.error(String.format("Mailbox '%s' does not exist, user '%s' cannot share mailbox",
                mailboxId,
                mailboxSession.getUser().getUserName()), e);
        }
        return Stream.of();
    }

    private void updateRight(MailboxPath mailboxPath, MailboxSession session, Entry entry) {
        try {
            boolean hasLookupRight = mapperFactory.getMailboxMapper(session)
                .findMailboxByPath(mailboxPath)
                .getACL()
                .getEntries()
                .getOrDefault(entry, MailboxACL.NO_RIGHTS)
                .contains(Right.Lookup);

            if (!hasLookupRight) {
                storeRightManager.applyRightsCommand(mailboxPath,
                    MailboxACL.command()
                        .rights(Right.Lookup)
                        .key(entry.getKey())
                        .asAddition(),
                    session);
            }
        } catch (MailboxException e) {
            LOGGER.error(String.format("Mailbox '%s' does not exist, user '%s' cannot share mailbox",
                mailboxPath,
                session.getUser().getUserName()), e);
        }
    }
}
