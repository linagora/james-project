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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.james.jmap.methods.MethodDispatcher;
import org.apache.james.jmap.model.Protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.CharStreams;

public class JMAPServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String JSON_CONTENT_TYPE_UTF8 = "application/json; charset=UTF-8";

    private ObjectMapper objectMapper = new ObjectMapper();
    private MethodDispatcher methodDispatcher;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        try {
            String input = CharStreams.toString(new InputStreamReader(req.getInputStream()));
            List<Protocol> requests = deserialize(input);
            List<Protocol> responses = requests.stream()
                .map(methodDispatcher::process)
                .collect(Collectors.toList());

            serialize(responses, resp.getOutputStream());
        } catch (IOException e) {
            resp.setStatus(SC_BAD_REQUEST);
        }
    }

    @VisibleForTesting List<Protocol> deserialize(String input) throws IOException {
        JsonNode[][] objects = objectMapper.readValue(input, JsonNode[][].class);
        return Arrays.stream(objects)
            .map(Protocol::deserialize)
            .collect(Collectors.toList());
    }

    @VisibleForTesting void serialize(List<Protocol> responses, OutputStream outputStream) throws IOException {
        List<Object[]> list = responses.stream()
            .map(p -> p.serialize())
            .collect(Collectors.toList());
        objectMapper.writeValue(outputStream, list);
    }

    public void setMethodDispatcher(MethodDispatcher methodDispatcher) {
        this.methodDispatcher = methodDispatcher;
    }
}
