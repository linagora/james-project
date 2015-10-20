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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.james.jmap.api.AccessTokenManager;
import org.apache.james.jmap.api.ContinuationTokenManager;
import org.apache.james.jmap.exceptions.BadRequestException;
import org.apache.james.jmap.exceptions.InternalErrorException;
import org.apache.james.jmap.json.MultipleObjectMapperBuilder;
import org.apache.james.jmap.model.AccessTokenRequest;
import org.apache.james.jmap.model.AccessTokenResponse;
import org.apache.james.jmap.model.ContinuationTokenRequest;
import org.apache.james.jmap.model.ContinuationTokenResponse;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

public class JMAPServlet extends HttpServlet {

    public static class RawRequest {
        public Object element;

        public RawRequest(Object element) {
            super();
            this.element = element;
        }

        public RawRequest(String element) {
            super();
            this.element = element;
        }

        public RawRequest(GetAccountsRequest element) {
            super();
            this.element = element;
        }
        
        
    }
    
    public static class Request {
        public String action = "getAccounts";
        public GetAccountsRequest arguments;
        public String clientId;
    }
    
    public static class GetAccountsRequest {
        public String state;
    }
    
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String JSON_CONTENT_TYPE_UTF8 = "application/json; charset=UTF-8";

    private static final Logger LOG = LoggerFactory.getLogger(JMAPServlet.class);

    private ObjectMapper objectMapper = new ObjectMapper();
    
    private UsersRepository usersRepository;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = "roger@barcamp";
        String action = "getAccounts";
        String input = CharStreams.toString(new InputStreamReader(req.getInputStream()));
        
        try (OutputStreamWriter output = new OutputStreamWriter(resp.getOutputStream())) {
            output.append("[\"error\", {type: \"invalidArgument\"}, \"client-id\"]");
        }
    }
    
    @VisibleForTesting RawRequest[] unserialize(String input) throws JsonParseException, JsonMappingException, IOException {
        return objectMapper.readValue(input, RawRequest[].class);
    }

}
