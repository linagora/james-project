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

package org.apache.james.mailbox.store;

import org.apache.james.mailbox.PathDelimiter;
import org.apache.james.mailbox.StandardMailboxMetaDataComparator;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxPath;

public class SimpleMailboxMetaData implements MailboxMetaData, Comparable<MailboxMetaData> {

    public static MailboxMetaData createNoSelect(MailboxPath path, MailboxId mailboxId, PathDelimiter pathDelimiter) {
        return new SimpleMailboxMetaData(path, mailboxId, pathDelimiter, Children.CHILDREN_ALLOWED_BUT_UNKNOWN, Selectability.NOSELECT);
    }

    private final MailboxPath path;

    private final PathDelimiter pathDelimiter;

    private final Children inferiors;

    private final Selectability selectability;

    private final MailboxId mailboxId;

    public SimpleMailboxMetaData(MailboxPath path, MailboxId mailboxId, PathDelimiter pathDelimiter) {
        this(path, mailboxId, pathDelimiter, Children.CHILDREN_ALLOWED_BUT_UNKNOWN, Selectability.NONE);
    }

    public SimpleMailboxMetaData(MailboxPath path, MailboxId mailboxId, PathDelimiter pathDelimiter, Children inferiors, Selectability selectability) {
        super();
        this.path = path;
        this.mailboxId = mailboxId;
        this.pathDelimiter = pathDelimiter;
        this.inferiors = inferiors;
        this.selectability = selectability;
    }

    /**
     * Is this mailbox <code>\Noinferiors</code> as per RFC3501.
     * 
     * @return true if marked, false otherwise
     */
    public final Children inferiors() {
        return inferiors;
    }

    /**
     * Gets the RFC3501 Selectability flag.
     */
    public final Selectability getSelectability() {
        return selectability;
    }

    /**
     * @see org.apache.james.mailbox.model.MailboxMetaData#getHierarchyDelimiter()
     */
    public PathDelimiter getHierarchyDelimiter() {
        return pathDelimiter;
    }

    /**
     * @see org.apache.james.mailbox.model.MailboxMetaData#getPath()
     */
    public MailboxPath getPath() {
        return path;
    }

    @Override
    public MailboxId getId() {
        return mailboxId;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "ListResult: " + path;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SimpleMailboxMetaData other = (SimpleMailboxMetaData) obj;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(MailboxMetaData o) {
        return StandardMailboxMetaDataComparator.order(this, o);
    }

}
