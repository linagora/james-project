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

package org.apache.james.webadmin.routes;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.apache.james.webadmin.WebAdminServer.NO_CONFIGURATION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.core.User;
import org.apache.james.metrics.logger.DefaultMetricFactory;
import org.apache.james.sieverepository.api.ScriptContent;
import org.apache.james.sieverepository.api.ScriptName;
import org.apache.james.sieverepository.api.SieveRepository;
import org.apache.james.sieverepository.api.exception.QuotaExceededException;
import org.apache.james.sieverepository.api.exception.ScriptNotFoundException;
import org.apache.james.sieverepository.api.exception.StorageException;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.restassured.RestAssured;

public class SieveScriptRoutesTest {

    private WebAdminServer webAdminServer;

    @Mock
    SieveRepository sieveRepository;

    @Mock
    UsersRepository usersRepository;

    @BeforeEach
    public void setUp() throws ConfigurationException {
        MockitoAnnotations.initMocks(this);

        webAdminServer = WebAdminUtils.createWebAdminServer(
            new DefaultMetricFactory(),
            new SieveScriptRoutes(sieveRepository, usersRepository));
        webAdminServer.configure(NO_CONFIGURATION);
        webAdminServer.await();

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webAdminServer)
            .build();
    }

    @Test
    public void defineAddActiveSieveScriptReturnNotFoundWhenUserNotExisted() throws UsersRepositoryException {
        when(usersRepository.contains(any(String.class))).thenReturn(false);

        given()
            .pathParam("userName", "unknown")
            .pathParam("scriptName", "scriptA")
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void defineAddActiveSieveScriptReturnNotFoundWhenScriptNameIsWhiteSpace() throws UsersRepositoryException {
        when(usersRepository.contains("userA")).thenReturn(true);
        String errorBody =
            "{\"statusCode\": 404," +
            " \"type\":\"InvalidArgument\"," +
            " \"message\":\"Invalid Sieve script name\"," +
            " \"details\":null" +
            "}";
        String body = given()
            .pathParam("userName", "userA")
            .pathParam("scriptName", " ")
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.NOT_FOUND_404)
            .extract()
            .body().asString();

        assertThatJson(body).isEqualTo(errorBody);
    }

    @Test
    public void defineAddActiveSieveScriptReturnNotFoundWhenUserNameWhiteSpace() throws UsersRepositoryException {
        when(usersRepository.contains(" ")).thenReturn(false);
        String errorBody =
            "{\"statusCode\": 404," +
            " \"type\":\"InvalidArgument\"," +
            " \"message\":\"Invalid or non existent user\"," +
            " \"details\":null" +
            "}";
        String body = given()
            .pathParam("userName", " ")
            .pathParam("scriptName", "scriptA")
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.NOT_FOUND_404)
            .extract()
            .body().asString();

        assertThatJson(body).isEqualTo(errorBody);
    }

    @Test
    public void defineAddActiveSieveScriptReturnNotFoundWhenScriptIsNotSet() throws UsersRepositoryException {
        when(usersRepository.contains("userA")).thenReturn(true);
        String errorBody =
            "{\"statusCode\": 404," +
            " \"type\":\"InvalidArgument\"," +
            " \"message\":\"Empty script is not accepted\"," +
            " \"details\":null" +
            "}";
        String body = given()
            .pathParam("userName", "userA")
            .pathParam("scriptName", "scriptA")
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.NOT_FOUND_404)
            .extract()
            .body().asString();

        assertThatJson(body).isEqualTo(errorBody);
    }

    @Test
    public void defineAddActiveSieveScriptInvokeSieveRepositoryOneWhenNotAddActivateParam() throws UsersRepositoryException, QuotaExceededException, StorageException {
        when(usersRepository.contains("userA")).thenReturn(true);

        given()
            .pathParam("userName", "userA")
            .pathParam("scriptName", "scriptA")
            .body("sieve script")
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        verify(sieveRepository, only()).putScript(any(User.class), any(ScriptName.class), any(ScriptContent.class));
        verifyNoMoreInteractions(sieveRepository);
    }

    @Test
    public void defineAddActiveSieveScriptInvokeSieveRepositoryOneWhenAddActivateParamTrue() throws UsersRepositoryException, QuotaExceededException, StorageException, ScriptNotFoundException {
        when(usersRepository.contains("userA")).thenReturn(true);

        given()
            .pathParam("userName", "userA")
            .pathParam("scriptName", "scriptA")
            .queryParam("activate", true)
            .body("sieve script")
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        verify(sieveRepository, times(1)).putScript(any(User.class), any(ScriptName.class), any(ScriptContent.class));
        verify(sieveRepository, times(1)).setActive(any(User.class), any(ScriptName.class));
        verifyNoMoreInteractions(sieveRepository);
    }

    @Test
    public void defineAddActiveSieveScriptInvokeSieveRepositoryOneWhenAddActivateParamFalse() throws UsersRepositoryException, QuotaExceededException, StorageException, ScriptNotFoundException {
        when(usersRepository.contains("userA")).thenReturn(true);

        given()
            .pathParam("userName", "userA")
            .pathParam("scriptName", "scriptA")
            .queryParam("activate", false)
            .body("sieve script")
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        verify(sieveRepository, only()).putScript(any(User.class), any(ScriptName.class), any(ScriptContent.class));
        verifyNoMoreInteractions(sieveRepository);
    }
}
