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

import org.apache.james.jmap.utils.MailboxNameEscaper;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;

import com.google.common.base.Preconditions;

public class MailboxPathBuilder {

    public static MailboxPathBuilder builder() {
        return new MailboxPathBuilder();
    }

    private String namespace;
    private String user;
    private Optional<String> parentName = Optional.empty();
    private String name;

    public MailboxPathBuilder forUser(String user) {
        this.namespace = MailboxConstants.USER_NAMESPACE;
        this.user = user;
        return this;
    }

    public MailboxPathBuilder withParent(Optional<MailboxPath> parent) {
        this.parentName = parent.map(MailboxPath::getName);
        return this;
    }

    public MailboxPathBuilder name(String name) {
        this.name = MailboxNameEscaper.escape(name);
        return this;
    }

    public MailboxPath build(MailboxSession mailboxSession) {
        Preconditions.checkState(namespace != null, "Namespace is compulsory");
        Preconditions.checkState(user != null, "User is compulsory");
        Preconditions.checkState(name != null, "Name is compulsory");
        return new MailboxPath(
            namespace,
            user,
            parentName.map(parentName -> parentName + mailboxSession.getPathDelimiter() + name)
                .orElse(name));
    }


}
