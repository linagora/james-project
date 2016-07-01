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

import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.james.jmap.api.SimpleTokenManager;
import org.apache.james.jmap.exceptions.MailboxSessionCreationException;
import org.apache.james.jmap.exceptions.NoValidAuthHeaderException;
import org.apache.james.jmap.exceptions.UnauthorizedException;
import org.apache.james.jmap.model.AttachmentAccessToken;
import org.apache.james.jmap.utils.DownloadPath;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public class QueryParameterAccessTokenAuthenticationStrategy implements AuthenticationStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(QueryParameterAccessTokenAuthenticationStrategy.class);
    private static final String ACCESS_TOKEN = "access_token";

    private final SimpleTokenManager tokenManager;
    private final MailboxManager mailboxManager;

    @Inject
    @VisibleForTesting
    QueryParameterAccessTokenAuthenticationStrategy(SimpleTokenManager tokenManager, MailboxManager mailboxManager) {
        this.tokenManager = tokenManager;
        this.mailboxManager = mailboxManager;
    }

    @Override
    public MailboxSession createMailboxSession(HttpServletRequest httpRequest) throws MailboxSessionCreationException, NoValidAuthHeaderException {

        Optional<String> username = getAccessToken(httpRequest)
            .map(AttachmentAccessToken::getUsername)
            .findFirst();

        if (username.isPresent()) {
            try {
                return mailboxManager.createSystemSession(username.get(), LOG);
            } catch (MailboxException e) {
                throw new MailboxSessionCreationException(e);
            }
        }
        throw new UnauthorizedException();
    }

    @Override
    public boolean checkAuthorizationHeader(HttpServletRequest httpRequest) {
        return getAccessToken(httpRequest)
                .anyMatch(tokenManager::isValid);
    }

    private Stream<AttachmentAccessToken> getAccessToken(HttpServletRequest httpRequest) {
        try {
            return Stream.of(AttachmentAccessToken.from(httpRequest.getParameter(ACCESS_TOKEN), getBlobId(httpRequest)));
        } catch (IllegalArgumentException e) {
            return Stream.of();
        }
    }

    private String getBlobId(HttpServletRequest httpRequest) {
        String pathInfo = httpRequest.getPathInfo();
        return DownloadPath.from(pathInfo).getBlobId();
    }
}
