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



package org.apache.james.transport.matchers;

import static org.apache.james.smtpserver.AddDefaultAttributesMessageHook.SMTP_AUTH_NETWORK_NAME;

import java.util.Collection;
import java.util.Objects;

import org.apache.james.core.MailAddress;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMatcher;

import com.github.steveash.guavate.Guavate;

/**
 * <p>Matches mails that are allowed to be relayed by the SMTP server. This includes authenticated users and authorized networks.</p>
 * <p>If the sender was not authenticated, and the sender not part of an authorized network (as defined in smtpserver.xml)
 * it will not match.</p>
 *
 * <pre><code>
 * &lt;mailet match=&quot;IsSmtpRelayAllowed&quot; class=&quot;&lt;any-class&gt;&quot;&gt;
 * </code></pre>
 */
public class IsSmtpRelayAllowed extends GenericMatcher {
    public Collection<MailAddress> match(Mail mail) {
        return mail.getRecipients().stream()
            .filter(any -> isAllowed(mail))
            .collect(Guavate.toImmutableList());
    }

    private boolean isAllowed(Mail mail) {
        boolean allowedUser = mail.getAttribute(Mail.SMTP_AUTH_USER_ATTRIBUTE_NAME) != null;
        boolean allowedAddress = Objects.equals(mail.getAttribute(SMTP_AUTH_NETWORK_NAME), "true");
        return allowedUser || allowedAddress;
    }
}
