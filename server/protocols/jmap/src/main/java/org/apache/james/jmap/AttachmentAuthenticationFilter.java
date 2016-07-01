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

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.james.jmap.api.SimpleTokenManager;
import org.apache.james.jmap.exceptions.UnauthorizedException;
import org.apache.james.jmap.model.AttachmentAccessToken;
import org.apache.james.jmap.utils.DownloadPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public class AttachmentAuthenticationFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentAuthenticationFilter.class);
    private static final String ACCESS_TOKEN = "access_token";

    public static final String MAILBOX_SESSION = "mailboxSession";

    private final SimpleTokenManager tokenManager;


    @Inject
    @VisibleForTesting
    AttachmentAuthenticationFilter(SimpleTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            AttachmentAccessToken accessToken = getAccessToken(httpRequest);
            validateAccessToken(accessToken);

            chain.doFilter(request, response);

        } catch (UnauthorizedException e) {
            LOGGER.error("Exception occurred during authentication process", e);
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Exception occurred during authentication process", e);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

    }

    private AttachmentAccessToken getAccessToken(HttpServletRequest httpRequest) {
        String blobId = getBlobId(httpRequest);
        try {
            return AttachmentAccessToken.from(httpRequest.getParameter(ACCESS_TOKEN), blobId);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException();
        }
    }

    private String getBlobId(HttpServletRequest httpRequest) {
        String pathInfo = httpRequest.getPathInfo();
        return DownloadPath.from(pathInfo).getBlobId();
    }

    private void validateAccessToken(AttachmentAccessToken accessToken) {
        if (!tokenManager.isValid(accessToken)) {
            throw new UnauthorizedException();
        }
    }

    @Override
    public void destroy() {
    }

}
