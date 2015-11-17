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
import org.apache.james.jmap.methods.JmapResponse;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = GetMailboxesResponse.Builder.class)
public class GetMailboxesResponse implements JmapResponse {

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private String accountId;
        private String state;
        private List<Mailbox> list;
        private String[] notFound;

        private Builder() {
        }

        public Builder accountId(String accountId) {
            if (accountId != null) {
                throw new NotImplementedException();
            }
            return this;
        }

        public Builder state(String state) {
            if (state != null) {
                throw new NotImplementedException();
            }
            return this;
        }

        public Builder list(List<Mailbox> list) {
            this.list = list;
            return this;
        }
        
        public Builder notFound(String[] notFound) {
            if (notFound != null) {
                throw new NotImplementedException();
            }
            return this;
        }

        public GetMailboxesResponse build() {
            return new GetMailboxesResponse(accountId, state, list, notFound);
        }
    }

    private final String accountId;
    private final String state;
    private final List<Mailbox> list;
    private final String[] notFound;

    private GetMailboxesResponse(String accountId, String state, List<Mailbox> list, String[] notFound) {
        this.accountId = accountId;
        this.state = state;
        this.list = list;
        this.notFound = notFound;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getState() {
        return state;
    }

    public List<Mailbox> getList() {
        return list;
    }

    public String[] getNotFound() {
        return notFound;
    }
}
