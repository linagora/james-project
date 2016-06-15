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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.api.UsersRepositoryManagementMBean;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.model.GetUserResponse;
import org.apache.james.webadmin.model.UserRequest;
import org.apache.james.webadmin.utils.ResourceAccessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.lambdas.ThrownByLambdaException;

public class UserServlet extends HttpServlet {

    private final ObjectMapper objectMapper;
    private final UsersRepositoryManagementMBean usersRepositoryManagementMBean;

    @Inject
    public UserServlet(UsersRepositoryManagementMBean usersRepositoryManagementMBean) {
        this.usersRepositoryManagementMBean = usersRepositoryManagementMBean;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try{
            UserRequest userRequest = objectMapper.readValue(req.getInputStream(), UserRequest.class);
            String password = userRequest.getPassword();

            ResourceAccessor.applyOnResource(req, resp, username -> postOperation(password, username));
        } catch (IOException e) {
            manageError(SC_BAD_REQUEST, resp, e);
        } catch (Exception e) {
            manageError(SC_INTERNAL_SERVER_ERROR, resp, e);
        }
    }

    private void postOperation(String password, String username) throws Exception {
        if (usersRepositoryManagementMBean.verifyExists(username)) {
            usersRepositoryManagementMBean.setPassword(username, password);
        } else {
            usersRepositoryManagementMBean.addUser(username, password);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            ResourceAccessor.applyOnResource(req, resp, usersRepositoryManagementMBean::deleteUser);
        } catch (ThrownByLambdaException lambdaException) {
            if (lambdaException.getCause().getClass().equals(UsersRepositoryException.class)) {
                resp.setStatus(SC_NOT_ACCEPTABLE);
                resp.getOutputStream().println(lambdaException.getCause().getMessage());
            } else {
                manageError(SC_INTERNAL_SERVER_ERROR, resp, lambdaException.getCause());
            }
        } catch (Exception e) {
            manageError(SC_INTERNAL_SERVER_ERROR, resp, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            GetUserResponse response = new GetUserResponse(
                Arrays.asList(
                    Optional.ofNullable(usersRepositoryManagementMBean.listAllUsers())
                        .orElse(new String[] {})));

            resp.setContentType(Constants.JSON_CONTENT_TYPE);
            resp.setStatus(SC_OK);
            resp.getOutputStream().write(objectMapper.writeValueAsBytes(response));
        } catch (Exception e) {
            e.printStackTrace();
            manageError(SC_INTERNAL_SERVER_ERROR, resp, e);
        }
    }

    private void manageError(int code, HttpServletResponse resp, Throwable t) throws IOException {
        resp.setStatus(code);
        resp.getOutputStream().println(t.getClass() + " " + t.getMessage());
    }
}
