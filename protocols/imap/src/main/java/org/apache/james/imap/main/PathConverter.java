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

package org.apache.james.imap.main;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.mailbox.PathDelimiter;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class PathConverter {

    private static final int NAMESPACE = 0;

    public static PathConverter forSession(ImapSession session) {
        return new PathConverter(session);
    }

    private final ImapSession session;

    private PathConverter(ImapSession session) {
        this.session = session;
    }

    public MailboxPath buildFullPath(String mailboxName) {
        if (Strings.isNullOrEmpty(mailboxName)) {
            return buildDefaultPath();
        }
        if (isAbsolute(mailboxName)) {
            return buildAbsolutePath(mailboxName);
        } else {
            return buildRelativePath(mailboxName);
        }
    }

    private MailboxPath buildDefaultPath() {
        return new MailboxPath("", "", "");
    }

    private boolean isAbsolute(String mailboxName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(mailboxName));
        return mailboxName.charAt(0) == MailboxConstants.NAMESPACE_PREFIX_CHAR;
    }

    private MailboxPath buildRelativePath(String mailboxName) {
        return buildMailboxPath(MailboxConstants.USER_NAMESPACE, ImapSessionUtils.getUserName(session), mailboxName);
    }

    private MailboxPath buildAbsolutePath(String absolutePath) {
        PathDelimiter pathDelimiter = ImapSessionUtils.getMailboxSession(session).getPathDelimiter();
        List<String> mailboxPathParts = pathDelimiter.split(absolutePath);
        String namespace = mailboxPathParts.get(NAMESPACE);
        String mailboxName = pathDelimiter.join(mailboxPathParts.stream().skip(1).collect(Collectors.toList()));
        return buildMailboxPath(namespace, retrieveUserName(namespace), mailboxName);
    }

    private String retrieveUserName(String namespace) {
        if (namespace.equals(MailboxConstants.USER_NAMESPACE)) {
            return ImapSessionUtils.getUserName(session);
        }
        return null;
    }

    private MailboxPath buildMailboxPath(String namespace, String user, String mailboxName) {
        if (!namespace.equals(MailboxConstants.USER_NAMESPACE)) {
            throw new DeniedAccessOnSharedMailboxException();
        }
        return new MailboxPath(namespace, user, sanitizeMailboxName(mailboxName));
    }

    private String sanitizeMailboxName(String mailboxName) {
        // use uppercase for INBOX
        // See IMAP-349
        if (mailboxName.equalsIgnoreCase(MailboxConstants.INBOX)) {
            return MailboxConstants.INBOX;
        }
        return mailboxName;
    }

}
