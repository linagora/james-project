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

package org.apache.james.jmap.methods;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.jmap.exceptions.MailboxParentNotFoundException;
import org.apache.james.jmap.model.MailboxCreationId;
import org.apache.james.jmap.model.SetError;
import org.apache.james.jmap.model.SetMailboxesRequest;
import org.apache.james.jmap.model.SetMailboxesResponse;
import org.apache.james.jmap.model.mailbox.Mailbox;
import org.apache.james.jmap.model.mailbox.MailboxRequest;
import org.apache.james.jmap.utils.CollectionHierarchySorter;
import org.apache.james.jmap.utils.MailboxUtils;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.google.common.annotations.VisibleForTesting;

public class SetMailboxesCreationProcessor<Id extends MailboxId> implements SetMailboxesProcessor<Id> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetMailboxesCreationProcessor.class);

    private final MailboxManager mailboxManager;
    private final CollectionHierarchySorter<Map.Entry<MailboxCreationId, MailboxRequest>, String> collectionHierarchySorter;
    private final MailboxUtils<Id> mailboxUtils;

    @Inject
    @VisibleForTesting
    SetMailboxesCreationProcessor(MailboxManager mailboxManager, MailboxUtils<Id> mailboxUtils) {
        this.mailboxManager = mailboxManager;
        this.collectionHierarchySorter =
            new CollectionHierarchySorter<>(
                x -> x.getKey().getCreationId(),
                x -> x.getValue().getParentId());
        this.mailboxUtils = mailboxUtils;
    }

    public SetMailboxesResponse process(SetMailboxesRequest request, MailboxSession mailboxSession) {
        SetMailboxesResponse.Builder builder = SetMailboxesResponse.builder();
        Map<MailboxCreationId, String> creationIdsToCreatedMailboxId = new HashMap<>();
        collectionHierarchySorter.sortFromRootToLeaf(request.getCreate().entrySet())
            .forEach(entry -> 
                createMailbox(entry.getKey(), entry.getValue(), mailboxSession, creationIdsToCreatedMailboxId, builder));
        return builder.build();
    }

    private void createMailbox(MailboxCreationId mailboxCreationId, MailboxRequest mailboxRequest, MailboxSession mailboxSession,
            Map<MailboxCreationId, String> creationIdsToCreatedMailboxId, SetMailboxesResponse.Builder builder) {
        try {
            MailboxPath mailboxPath = getMailboxPath(mailboxRequest, creationIdsToCreatedMailboxId, mailboxSession);
            mailboxManager.createMailbox(mailboxPath, mailboxSession);
            Optional<Mailbox> mailbox = mailboxUtils.mailboxFromMailboxPath(mailboxPath, mailboxSession);
            if (mailbox.isPresent()) {
                builder.creation(mailboxCreationId, mailbox.get());
                creationIdsToCreatedMailboxId.put(mailboxCreationId, mailbox.get().getId());
            } else {
                builder.notCreated(mailboxCreationId, SetError.builder()
                        .type("anErrorOccurred")
                        .description("An error occurred when creating the mailbox")
                        .build());
            }
        } catch (MailboxParentNotFoundException e) {
            builder.notCreated(mailboxCreationId, SetError.builder()
                    .type("invalidArguments")
                    .description(e.getMessage())
                    .build());
        } catch (MailboxException e) {
            String message = String.format("An error occurred when creating the mailbox '%s'", mailboxCreationId.getCreationId());
            LOGGER.error(message, e);
            builder.notCreated(mailboxCreationId, SetError.builder()
                    .type("anErrorOccurred")
                    .description(message)
                    .build());
        }
    }

    private MailboxPath getMailboxPath(MailboxRequest mailboxRequest, Map<MailboxCreationId, String> creationIdsToCreatedMailboxId, MailboxSession mailboxSession) throws MailboxException {
        if (mailboxRequest.getParentId().isPresent()) {
            String parentId = mailboxRequest.getParentId().get();
            String parentName = mailboxUtils.getMailboxNameFromId(parentId, mailboxSession)
                    .orElseGet(Throwing.supplier(() ->
                        mailboxUtils.getMailboxNameFromId(creationIdsToCreatedMailboxId.get(MailboxCreationId.of(parentId)), mailboxSession)
                            .orElseThrow(() -> new MailboxParentNotFoundException(parentId))
                    ));

            return new MailboxPath(mailboxSession.getPersonalSpace(), mailboxSession.getUser().getUserName(), 
                    parentName + mailboxSession.getPathDelimiter() + mailboxRequest.getName());
        }
        return new MailboxPath(mailboxSession.getPersonalSpace(), mailboxSession.getUser().getUserName(), mailboxRequest.getName());
    }
}
