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

package org.apache.james.webadmin.servlet;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.domainlist.api.DomainListManagementMBean;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.model.GetDomainResponse;
import org.apache.james.webadmin.utils.ResourceAccessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.lambdas.ThrownByLambdaException;
import com.github.fge.lambdas.consumers.ThrowingConsumer;

public class DomainServlet extends HttpServlet {

    private final ObjectMapper objectMapper;
    private final DomainListManagementMBean domainListManagementMBean;

    @Inject
    public DomainServlet(DomainListManagementMBean domainListManagementMBean) {
        this.domainListManagementMBean = domainListManagementMBean;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        accessResourceErrorProtected(req, resp, domainListManagementMBean::addDomain);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            GetDomainResponse response = new GetDomainResponse(domainListManagementMBean.getDomains());

            resp.setContentType(Constants.JSON_CONTENT_TYPE);
            resp.setStatus(SC_OK);
            resp.getOutputStream().write(objectMapper.writeValueAsBytes(response));
        } catch (Exception e) {
            manageInternalError(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        accessResourceErrorProtected(req, resp, domainListManagementMBean::removeDomain);
    }

    private void accessResourceErrorProtected(HttpServletRequest req, HttpServletResponse resp, ThrowingConsumer<String> operation) throws IOException {
        try {
            ResourceAccessor.applyOnResource(req, resp, operation);
        } catch (ThrownByLambdaException lambdaException) {
            if (lambdaException.getCause().getClass().equals(DomainListException.class)) {
                resp.setStatus(SC_NOT_ACCEPTABLE);
                resp.getOutputStream().println(lambdaException.getCause().getMessage());
            } else {
                manageInternalError(resp, lambdaException.getCause());
            }
        } catch (Exception e) {
            manageInternalError(resp, e);
        }
    }

    private void manageInternalError(HttpServletResponse resp, Throwable t) throws IOException {
        resp.setStatus(SC_INTERNAL_SERVER_ERROR);
        resp.getOutputStream().println(t.getClass() + " " + t.getMessage());
    }
}
