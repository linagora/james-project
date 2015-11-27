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

import java.util.Optional;

import com.google.common.base.Preconditions;

public class Message {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        
        private MessageId messageId;
        private Optional<String> blobId;
        private Optional<String> threadId;
        private Optional<String> subject;
        
        private Builder() {
            blobId = Optional.empty();
            threadId = Optional.empty();
            subject = Optional.empty();
        }
        
        public Builder messageId(MessageId id) {
            this.messageId = id;
            return this;
        }
        
        public Builder blobId(Optional<String> blobId) {
            this.blobId = blobId;
            return this;
        }
        
        public Builder threadId(Optional<String> threadId) {
            this.threadId = threadId;
            return this;
        }
        
        public Builder subject(Optional<String> subject) {
            this.subject = subject;
            return this;
        }
        
        public Message build() {
            Preconditions.checkState(messageId != null);
            return new Message(messageId, blobId, threadId, subject);
        }
        
    }
    
    private final MessageId messageId;
    private final Optional<String> blobId;
    private final Optional<String> threadId;
    private final Optional<String> subject;

    public Message(MessageId messageId, Optional<String> blobId,
            Optional<String> threadId, Optional<String> subject) {
        this.messageId = messageId;
        this.blobId = blobId;
        this.threadId = threadId;
        this.subject = subject;
    }

    public MessageId getId() {
        return messageId;
    }

    public Optional<String> getBlobId() {
        return blobId;
    }

    public Optional<String> getThreadId() {
        return threadId;
    }

    public Optional<String> getSubject() {
        return subject;
    }

}
