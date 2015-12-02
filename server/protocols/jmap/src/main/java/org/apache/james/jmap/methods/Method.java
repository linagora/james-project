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

import static com.google.common.base.Objects.toStringHelper;

import java.util.Objects;

import org.apache.james.mailbox.MailboxSession;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;

public interface Method {

    interface Response {};

    public static RequestName name(String name) {
        return new RequestName(name);
    }

    public static ResponseName responseFrom(RequestName requestName) {
        String name = requestName.getName();
        Preconditions.checkNotNull(name);
        Preconditions.checkArgument(!name.isEmpty());

        String substring = name.substring(3);
        return new ResponseName(Character.toLowerCase(substring.charAt(0)) + substring.substring(1));
    }

    public static ResponseName error() {
        return new ResponseError();
    }

    public interface Name {

        String getName();
    }

    public class RequestName implements Name {

        private final String name;
        
        private RequestName(String name) {
            Preconditions.checkNotNull(name);
            Preconditions.checkArgument(!name.isEmpty());
            this.name = name;
        }

        @JsonValue
        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RequestName) {
                RequestName other = (RequestName) obj;
                return Objects.equals(name, other.name);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return toStringHelper(this).add("name", name).toString();
        }
    }

    public class ResponseName implements Name {

        private final String name;

        protected ResponseName(String name) {
            Preconditions.checkNotNull(name);
            Preconditions.checkArgument(!name.isEmpty());
            this.name = name;
        }

        @JsonValue
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ResponseName) {
                ResponseName other = (ResponseName) obj;
                return Objects.equals(name, other.name);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
        
        @Override
        public String toString() {
            return toStringHelper(this).add("name", name).toString();
        }
    }

    public class ResponseError extends ResponseName {

        protected ResponseError() {
            super("error");
        }
    }

    RequestName methodName();

    Class<? extends JmapRequest> requestType();
    
    Response process(JmapRequest request, MailboxSession mailboxSession);

}
