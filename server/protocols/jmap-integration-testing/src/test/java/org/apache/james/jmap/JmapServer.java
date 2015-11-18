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

package org.apache.james.jmap;

import java.io.InputStream;
import java.util.Date;

import javax.mail.Flags;

import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.mailbox.exception.BadCredentialsException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.junit.rules.TestRule;

public interface JmapServer extends TestRule {

    void clean();

    int getPort();

    void createJamesUser(String username, String password);

    AccessToken authenticateJamesUser(String username, String password);

    void createMailbox(String username, MailboxPath mailboxPath) throws BadCredentialsException, MailboxException;

    void appendMessage(String username, MailboxPath mailboxPath, InputStream message, Date internalDate, boolean isRecent, Flags flags) throws BadCredentialsException, MailboxException;
}
