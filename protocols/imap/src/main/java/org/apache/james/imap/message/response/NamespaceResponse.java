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
package org.apache.james.imap.message.response;

import java.util.List;

import org.apache.james.imap.api.message.response.ImapResponseMessage;
import org.apache.james.mailbox.PathDelimiter;

import com.google.common.base.Objects;

/**
 * Describes a NAMESPACE response.
 */
public class NamespaceResponse implements ImapResponseMessage {

    private final List<Namespace> personal;

    private final List<Namespace> users;

    private final List<Namespace> shared;

    public NamespaceResponse(List<Namespace> personal, List<Namespace> users, List<Namespace> shared) {
        super();
        this.personal = personal;
        this.users = users;
        this.shared = shared;
    }

    /**
     * Gets the personal namespace.
     * 
     * @return possibly null
     */
    public List<Namespace> getPersonal() {
        return personal;
    }

    /**
     * Gets shared namespaces.
     * 
     * @return possibly null
     */
    public List<Namespace> getShared() {
        return shared;
    }

    /**
     * Gets the namespaces for other users.
     * 
     * @return possibly null
     */
    public List<Namespace> getUsers() {
        return users;
    }

    /**
     * Describes a namespace.
     */
    public static final class Namespace {
        
        private final String prefix;
        private final PathDelimiter pathDelimiter;

        public Namespace(String prefix, PathDelimiter pathDelimiter) {
            super();
            this.prefix = prefix;
            this.pathDelimiter = pathDelimiter;
        }

        /**
         * Gets the pathDelimiter used to separate mailboxes.
         * 
         * @return not null
         */
        public PathDelimiter getDelimiter() {
            return pathDelimiter;
        }

        /**
         * Gets the leading prefix used by this namespace.
         * 
         * @return not null
         */
        public String getPrefix() {
            return prefix;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(prefix, pathDelimiter);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final Namespace other = (Namespace) obj;
            if (pathDelimiter != other.pathDelimiter)
                return false;
            if (prefix == null) {
                if (other.prefix != null)
                    return false;
            } else if (!prefix.equals(other.prefix))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Namespace [prefix=" + prefix + ", delim=" + pathDelimiter + "]";
        }
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((personal == null) ? 0 : personal.hashCode());
        result = PRIME * result + ((shared == null) ? 0 : shared.hashCode());
        result = PRIME * result + ((users == null) ? 0 : users.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final NamespaceResponse other = (NamespaceResponse) obj;
        if (personal == null) {
            if (other.personal != null)
                return false;
        } else if (!personal.equals(other.personal))
            return false;
        if (shared == null) {
            if (other.shared != null)
                return false;
        } else if (!shared.equals(other.shared))
            return false;
        if (users == null) {
            if (other.users != null)
                return false;
        } else if (!users.equals(other.users))
            return false;
        return true;
    }

    /**
     * Renders object suitably for logging.
     * 
     * @return a <code>String</code> representation of this object.
     */
    public String toString() {
        return "NamespaceResponse [" + "personal = " + this.personal + " " + "users = " + this.users + " " + "shared = " + this.shared + " " + " ]";
    }
}
