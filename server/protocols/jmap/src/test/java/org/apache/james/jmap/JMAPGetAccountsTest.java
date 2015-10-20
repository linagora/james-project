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
import static com.jayway.restassured.RestAssured.with;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.apache.james.jmap.api.AccessTokenManager;
import org.apache.james.jmap.api.ContinuationTokenManager;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.jmap.crypto.AccessTokenManagerImpl;
import org.apache.james.jmap.crypto.JamesSignatureHandlerProvider;
import org.apache.james.jmap.crypto.SignedContinuationTokenManager;
import org.apache.james.jmap.memory.access.MemoryAccessTokenRepository;
import org.apache.james.jmap.utils.ZonedDateTimeProvider;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

public class JMAPGetAccountsTest {

    private static final int RANDOM_PORT = 0;
    private static final ZonedDateTime oldDate = ZonedDateTime.parse("2011-12-03T10:15:30+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    private static final ZonedDateTime newDate = ZonedDateTime.parse("2011-12-03T10:16:30+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    private static final ZonedDateTime afterExpirationDate = ZonedDateTime.parse("2011-12-03T10:30:31+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);


    private Server server;

    private UsersRepository mockedUsersRepository;
    private ContinuationTokenManager continuationTokenManager;
    private ZonedDateTimeProvider mockedZonedDateTimeProvider;
    private AccessTokenManager accessTokenManager;

    @Before
    public void setup() throws Exception {
        server = new Server(RANDOM_PORT);

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        mockedUsersRepository = mock(UsersRepository.class);
        mockedZonedDateTimeProvider = mock(ZonedDateTimeProvider.class);
        continuationTokenManager = new SignedContinuationTokenManager(new JamesSignatureHandlerProvider().provide(), mockedZonedDateTimeProvider);
        accessTokenManager = new AccessTokenManagerImpl(new MemoryAccessTokenRepository(100));

        Servlet jmapServlet = new JMAPServlet();
        ServletHolder servletHolder = new ServletHolder(jmapServlet);
        handler.addServletWithMapping(servletHolder, "/*");

//        AuthenticationFilter authenticationFilter = new AuthenticationFilter(accessTokenManager);
//        Filter getAuthenticationFilter = new BypassOnPostFilter(authenticationFilter);
//        FilterHolder authenticationFilterHolder = new FilterHolder(getAuthenticationFilter);
//        handler.addFilterWithMapping(authenticationFilterHolder, "/*", null);
        
        server.start();

        int localPort = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        RestAssured.port = localPort;
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset("UTF-8"));

    }

    @Test
    public void mustReturnInvalidArgumentOnInvalidState() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("[\"getAccounts\", {\"state\":false}, \"#0\"]")
        .when()
            .post("/")
        .then()
            .statusCode(200)
            .content(containsString("[\"error\", {type: \"invalidArgument\"}, \"client-id\"]"));
    }

    @Test
    public void mustReturnAccountsOnValidRequest() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("[\"getAccounts\", {}, \"#0\"]")
        .when()
            .post("/")
        .then()
            .statusCode(200)
            .content(containsString("[ \"accounts\", {" + 
                    "  \"state\": \"f6a7e214\"," + 
                    "  \"list\": [" + 
                    "    {" + 
                    "      \"id\": \"6asf5\"," + 
                    "      \"name\": \"roger@barcamp\"" + 
                    "    }" + 
                    "  ]" + 
                    "}, \"#0\" ]"));
    }
    
    @After
    public void teardown() throws Exception {
        server.stop();
    }

}
