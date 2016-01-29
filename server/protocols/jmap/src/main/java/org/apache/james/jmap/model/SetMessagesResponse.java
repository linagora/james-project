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

import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.apache.james.jmap.methods.Method;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

@JsonDeserialize(builder = SetMessagesResponse.Builder.class)
public class SetMessagesResponse implements Method.Response {

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private String accountId;
        private String oldState;
        private String newState;
        private ImmutableList.Builder<Message> created;
        private ImmutableList.Builder<MessageId> updated;
        private ImmutableList.Builder<MessageId> destroyed;
        private ImmutableList.Builder<SetError> notCreated;
        private ImmutableList.Builder<SetError> notUpdated;
        private ImmutableList.Builder<SetError> notDestroyed;

        private Builder() {
            created = ImmutableList.builder();
            updated = ImmutableList.builder();
            destroyed = ImmutableList.builder();
            notCreated = ImmutableList.builder();
            notUpdated = ImmutableList.builder();
            notDestroyed = ImmutableList.builder();
        }

        public Builder accountId(String accountId) {
            throw new NotImplementedException();
        }

        public Builder oldState(String oldState) {
            throw new NotImplementedException();
        }

        public Builder newState(String newState) {
            throw new NotImplementedException();
        }

        public Builder created(List<Message> created) {
            this.created.addAll(created);
            return this;
        }

        public Builder updated(List<MessageId> updated) {
            this.updated.addAll(updated);
            return this;
        }

        public Builder destroyed(List<MessageId> destroyed) {
            this.destroyed.addAll(destroyed);
            return this;
        }

        public Builder notCreated(List<SetError> notCreated) {
            this.notCreated.addAll(notCreated);
            return this;
        }

        public Builder notUpdated(List<SetError> notUpdated) {
            this.notUpdated.addAll(notUpdated);
            return this;
        }

        public Builder notDestroyed(List<SetError> notDestroyed) {
            this.notDestroyed.addAll(notDestroyed);
            return this;
        }

        public SetMessagesResponse build() {
            return new SetMessagesResponse(accountId, oldState, newState, 
                    created.build(), updated.build(), destroyed.build(), notCreated.build(), notUpdated.build(), notDestroyed.build());
        }
    }

    private final String accountId;
    private final String oldState;
    private final String newState;
    private final List<Message> created;
    private final List<MessageId> updated;
    private final List<MessageId> destroyed;
    private final List<SetError> notCreated;
    private final List<SetError> notUpdated;
    private final List<SetError> notDestroyed;

    @VisibleForTesting SetMessagesResponse(String accountId, String oldState, String newState, List<Message> created, List<MessageId> updated, List<MessageId> destroyed, 
            List<SetError> notCreated, List<SetError> notUpdated, List<SetError> notDestroyed) {
        this.accountId = accountId;
        this.oldState = oldState;
        this.newState = newState;
        this.created = created;
        this.updated = updated;
        this.destroyed = destroyed;
        this.notCreated = notCreated;
        this.notUpdated = notUpdated;
        this.notDestroyed = notDestroyed;
    }

    @JsonSerialize
    public String getAccountId() {
        return accountId;
    }

    @JsonSerialize
    public String getOldState() {
        return oldState;
    }

    @JsonSerialize
    public String getNewState() {
        return newState;
    }

    @JsonSerialize
    public List<Message> getCreated() {
        return created;
    }

    @JsonSerialize
    public List<MessageId> getUpdated() {
        return updated;
    }

    @JsonSerialize
    public List<MessageId> getDestroyed() {
        return destroyed;
    }

    @JsonSerialize
    public List<SetError> getNotCreated() {
        return notCreated;
    }

    @JsonSerialize
    public List<SetError> getNotUpdated() {
        return notUpdated;
    }

    @JsonSerialize
    public List<SetError> getNotDestroyed() {
        return notDestroyed;
    }
}
