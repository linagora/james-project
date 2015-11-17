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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.james.mailbox.MailboxSession;

public class JmapAuthenticatedRequest implements HttpServletRequest {
    
    private HttpServletRequest httpServletRequest;
    private MailboxSession mailboxSession;

    public JmapAuthenticatedRequest(HttpServletRequest httpServletRequest, MailboxSession mailboxSession) {
        this.httpServletRequest = httpServletRequest;
        this.mailboxSession = mailboxSession;
    }

    public Object getAttribute(String name) {
        return httpServletRequest.getAttribute(name);
    }

    public String getAuthType() {
        return httpServletRequest.getAuthType();
    }

    public Cookie[] getCookies() {
        return httpServletRequest.getCookies();
    }

    public Enumeration<String> getAttributeNames() {
        return httpServletRequest.getAttributeNames();
    }

    public long getDateHeader(String name) {
        return httpServletRequest.getDateHeader(name);
    }

    public String getCharacterEncoding() {
        return httpServletRequest.getCharacterEncoding();
    }

    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        httpServletRequest.setCharacterEncoding(env);
    }

    public String getHeader(String name) {
        return httpServletRequest.getHeader(name);
    }

    public int getContentLength() {
        return httpServletRequest.getContentLength();
    }

    public String getContentType() {
        return httpServletRequest.getContentType();
    }

    public Enumeration<String> getHeaders(String name) {
        return httpServletRequest.getHeaders(name);
    }

    public ServletInputStream getInputStream() throws IOException {
        return httpServletRequest.getInputStream();
    }

    public String getParameter(String name) {
        return httpServletRequest.getParameter(name);
    }

    public Enumeration<String> getHeaderNames() {
        return httpServletRequest.getHeaderNames();
    }

    public int getIntHeader(String name) {
        return httpServletRequest.getIntHeader(name);
    }

    public Enumeration<String> getParameterNames() {
        return httpServletRequest.getParameterNames();
    }

    public String[] getParameterValues(String name) {
        return httpServletRequest.getParameterValues(name);
    }

    public String getMethod() {
        return httpServletRequest.getMethod();
    }

    public String getPathInfo() {
        return httpServletRequest.getPathInfo();
    }

    public Map<String, String[]> getParameterMap() {
        return httpServletRequest.getParameterMap();
    }

    public String getProtocol() {
        return httpServletRequest.getProtocol();
    }

    public String getPathTranslated() {
        return httpServletRequest.getPathTranslated();
    }

    public String getScheme() {
        return httpServletRequest.getScheme();
    }

    public String getServerName() {
        return httpServletRequest.getServerName();
    }

    public String getContextPath() {
        return httpServletRequest.getContextPath();
    }

    public int getServerPort() {
        return httpServletRequest.getServerPort();
    }

    public BufferedReader getReader() throws IOException {
        return httpServletRequest.getReader();
    }

    public String getQueryString() {
        return httpServletRequest.getQueryString();
    }

    public String getRemoteAddr() {
        return httpServletRequest.getRemoteAddr();
    }

    public String getRemoteUser() {
        return httpServletRequest.getRemoteUser();
    }

    public String getRemoteHost() {
        return httpServletRequest.getRemoteHost();
    }

    public boolean isUserInRole(String role) {
        return httpServletRequest.isUserInRole(role);
    }

    public void setAttribute(String name, Object o) {
        httpServletRequest.setAttribute(name, o);
    }

    public Principal getUserPrincipal() {
        return httpServletRequest.getUserPrincipal();
    }

    public String getRequestedSessionId() {
        return httpServletRequest.getRequestedSessionId();
    }

    public void removeAttribute(String name) {
        httpServletRequest.removeAttribute(name);
    }

    public String getRequestURI() {
        return httpServletRequest.getRequestURI();
    }

    public Locale getLocale() {
        return httpServletRequest.getLocale();
    }

    public Enumeration<Locale> getLocales() {
        return httpServletRequest.getLocales();
    }

    public StringBuffer getRequestURL() {
        return httpServletRequest.getRequestURL();
    }

    public boolean isSecure() {
        return httpServletRequest.isSecure();
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return httpServletRequest.getRequestDispatcher(path);
    }

    public String getServletPath() {
        return httpServletRequest.getServletPath();
    }

    public HttpSession getSession(boolean create) {
        return httpServletRequest.getSession(create);
    }

    public String getRealPath(String path) {
        return httpServletRequest.getRealPath(path);
    }

    public int getRemotePort() {
        return httpServletRequest.getRemotePort();
    }

    public String getLocalName() {
        return httpServletRequest.getLocalName();
    }

    public String getLocalAddr() {
        return httpServletRequest.getLocalAddr();
    }

    public HttpSession getSession() {
        return httpServletRequest.getSession();
    }

    public int getLocalPort() {
        return httpServletRequest.getLocalPort();
    }

    public ServletContext getServletContext() {
        return httpServletRequest.getServletContext();
    }

    public boolean isRequestedSessionIdValid() {
        return httpServletRequest.isRequestedSessionIdValid();
    }

    public AsyncContext startAsync() throws IllegalStateException {
        return httpServletRequest.startAsync();
    }

    public boolean isRequestedSessionIdFromCookie() {
        return httpServletRequest.isRequestedSessionIdFromCookie();
    }

    public boolean isRequestedSessionIdFromURL() {
        return httpServletRequest.isRequestedSessionIdFromURL();
    }

    public boolean isRequestedSessionIdFromUrl() {
        return httpServletRequest.isRequestedSessionIdFromUrl();
    }

    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return httpServletRequest.authenticate(response);
    }

    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return httpServletRequest.startAsync(servletRequest, servletResponse);
    }

    public void login(String username, String password) throws ServletException {
        httpServletRequest.login(username, password);
    }

    public void logout() throws ServletException {
        httpServletRequest.logout();
    }

    public Collection<Part> getParts() throws IOException, ServletException {
        return httpServletRequest.getParts();
    }

    public boolean isAsyncStarted() {
        return httpServletRequest.isAsyncStarted();
    }

    public Part getPart(String name) throws IOException, ServletException {
        return httpServletRequest.getPart(name);
    }

    public boolean isAsyncSupported() {
        return httpServletRequest.isAsyncSupported();
    }

    public AsyncContext getAsyncContext() {
        return httpServletRequest.getAsyncContext();
    }

    public DispatcherType getDispatcherType() {
        return httpServletRequest.getDispatcherType();
    }
    
    public MailboxSession getMailboxSession() {
        return mailboxSession;
    }
}
