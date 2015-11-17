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

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.james.jmap.api.AccessTokenManager;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.BadCredentialsException;
import org.apache.james.mailbox.exception.MailboxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AuthenticationFilter implements Filter {
    
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationFilter.class);

    private AccessTokenManager accessTokenManager;
    private MailboxManager mailboxManager;

    @Inject
    public AuthenticationFilter(AccessTokenManager accessTokenManager, MailboxManager mailboxManager) {
        this.accessTokenManager = accessTokenManager;
        this.mailboxManager = mailboxManager;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        Optional<String> authHeader = Optional.ofNullable(httpRequest.getHeader("Authorization"));
        if (!checkAuthorizationHeader(authHeader)) {
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            MailboxSession mailboxSession = fromHeader(authHeader);
            JmapAuthenticatedRequest jmapAuthenticatedRequest = new JmapAuthenticatedRequest(httpRequest, mailboxSession);
            chain.doFilter(jmapAuthenticatedRequest, response);
        } catch (MailboxException e) {
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private MailboxSession fromHeader(Optional<String> authHeader) throws BadCredentialsException, MailboxException {
        String username = authHeader
            .map(header -> accessTokenManager.getUsernameFromToken(AccessToken.fromString(header)))
            .orElseThrow(() -> new BadCredentialsException());
        return mailboxManager.createSystemSession(username, LOG);
    }

    private boolean checkAuthorizationHeader(Optional<String> authHeader) throws IOException {
        return authHeader
                .map(x -> accessTokenManager.isValid(AccessToken.fromString(x)))
                .orElse(false);
    }

    @Override
    public void destroy() {
    }

}
