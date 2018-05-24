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

package org.apache.james.mailbox;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.mail.Flags;

import org.apache.james.mailbox.MessageManager.FlagsUpdateMode;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.mailbox.model.MessageResult.FetchGroup;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public interface MessageIdManager {

    Set<MessageId> accessibleMessages(Collection<MessageId> messageIds, final MailboxSession mailboxSession) throws MailboxException;

    void setFlags(Flags newState, FlagsUpdateMode replace, MessageId messageId, List<MailboxId> mailboxIds, MailboxSession mailboxSession) throws MailboxException;

    List<MessageResult> getMessages(List<MessageId> messageId, FetchGroup minimal, MailboxSession mailboxSession) throws MailboxException;

    DeleteResult delete(MessageId messageId, List<MailboxId> mailboxIds, MailboxSession mailboxSession) throws MailboxException;

    DeleteResult delete(List<MessageId> messageId, MailboxSession mailboxSession) throws MailboxException;

    void setInMailboxes(MessageId messageId, Collection<MailboxId> mailboxIds, MailboxSession mailboxSession) throws MailboxException;

    default DeleteResult delete(MessageId messageId, MailboxSession mailboxSession) throws MailboxException {
        return delete(ImmutableList.of(messageId), mailboxSession);
    }

    class DeleteResult {
        public static class Builder {
            private final ImmutableSet.Builder<MessageId> destroyed;
            private final ImmutableSet.Builder<MessageId> notFound;

            public Builder() {
                destroyed = ImmutableSet.builder();
                notFound = ImmutableSet.builder();
            }

            public Builder addDestroyed(Collection<MessageId> messageIds) {
                destroyed.addAll(messageIds);
                return this;
            }

            public Builder addNotFound(Collection<MessageId> messageIds) {
                notFound.addAll(messageIds);
                return this;
            }

            public Builder addDestroyed(MessageId messageId) {
                destroyed.add(messageId);
                return this;
            }

            public Builder addNotFound(MessageId messageId) {
                notFound.add(messageId);
                return this;
            }

            public DeleteResult build() {
                return new DeleteResult(
                    destroyed.build(),
                    notFound.build());
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        private final Set<MessageId> destroyed;
        private final Set<MessageId> notFound;

        public DeleteResult(Set<MessageId> destroyed, Set<MessageId> notFound) {
            this.destroyed = destroyed;
            this.notFound = notFound;
        }

        public Set<MessageId> getDestroyed() {
            return destroyed;
        }

        public Set<MessageId> getNotFound() {
            return notFound;
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof DeleteResult) {
                DeleteResult result = (DeleteResult) o;

                return Objects.equals(this.destroyed, result.destroyed)
                    && Objects.equals(this.notFound, result.notFound);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(destroyed, notFound);
        }
    }
}
