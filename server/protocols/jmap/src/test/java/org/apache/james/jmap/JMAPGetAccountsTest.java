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

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.jmap.methods.MethodDispatcher;
import org.apache.james.jmap.model.Protocol;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

public class JMAPGetAccountsTest {

    private static final int RANDOM_PORT = 0;

    private Server server;
    private MethodDispatcher methodDispatcher;

    @Before
    public void setup() throws Exception {
        server = new Server(RANDOM_PORT);

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        methodDispatcher = mock(MethodDispatcher.class);

        JMAPServlet jmapServlet = new JMAPServlet();
        jmapServlet.setMethodDispatcher(methodDispatcher);
        ServletHolder servletHolder = new ServletHolder(jmapServlet);
        handler.addServletWithMapping(servletHolder, "/*");

        server.start();

        int localPort = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        RestAssured.port = localPort;
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset("UTF-8"));

    }

    @Test
    public void mustReturnBadRequestOnMalformedRequest() {
        String missingAnOpeningBracket = "[\"getAccounts\", {\"state\":false}, \"#0\"]]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(missingAnOpeningBracket)
        .when()
            .post("/")
        .then()
            .statusCode(400);
    }

    @Test
    public void mustReturnInvalidArgumentOnInvalidState() {
        ObjectNode json = new ObjectNode(new JsonNodeFactory(false));
        json.put("type", "invalidArgument");

        when(methodDispatcher.process(any()))
            .thenReturn(new Protocol("error", json, "#0"));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("[[\"getAccounts\", {\"state\":false}, \"#0\"]]")
        .when()
            .post("/")
        .then()
            .statusCode(200)
            .content(equalTo("[[\"error\",{\"type\":\"invalidArgument\"},\"#0\"]]"));
    }

    @Test
    public void mustReturnAccountsOnValidRequest() {
        ObjectNode json = new ObjectNode(new JsonNodeFactory(false));
        json.put("state", "f6a7e214");
        ArrayNode arrayNode = json.putArray("list");
        ObjectNode list = new ObjectNode(new JsonNodeFactory(false));
        list.put("id", "6asf5");
        list.put("name", "roger@barcamp");
        arrayNode.add(list);

        when(methodDispatcher.process(any()))
            .thenReturn(new Protocol("accounts", json, "#0"));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("[[\"getAccounts\", {}, \"#0\"]]")
        .when()
            .post("/")
        .then()
            .statusCode(200)
            .content(equalTo("[[\"accounts\",{" + 
                    "\"state\":\"f6a7e214\"," + 
                    "\"list\":[" + 
                        "{" + 
                        "\"id\":\"6asf5\"," + 
                        "\"name\":\"roger@barcamp\"" + 
                        "}" + 
                    "]" + 
                    "},\"#0\"]]"));
    }
    
    @After
    public void teardown() throws Exception {
        server.stop();
    }

}
