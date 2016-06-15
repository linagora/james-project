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

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.api.UsersRepositoryManagementMBean;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.WebAdminServerImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

public class UserServletTest {

    public static final Optional<Integer> RANDOM_PORT = Optional.empty();

    private WebAdminServerImpl webAdminServerImpl;
    private UsersRepositoryManagementMBean usersRepositoryManagementMBean;

    @Before
    public void setUp() throws Exception {
        usersRepositoryManagementMBean = mock(UsersRepositoryManagementMBean.class);
        webAdminServerImpl = new WebAdminServerImpl(RANDOM_PORT, mock(DomainServlet.class), new UserServlet(usersRepositoryManagementMBean));
        webAdminServerImpl.configure(null);

        RestAssured.port = webAdminServerImpl.getPort();
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8));
    }

    @After
    public void tearDown() {
        webAdminServerImpl.stop();
    }

    @Test
    public void postShouldCreateUserIfNotExist() throws Exception {
        String user = "user";
        String password = "password";
        when(usersRepositoryManagementMBean.verifyExists(user)).thenReturn(false);

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("{\"password\":\"" + password + "\"}")
        .when()
            .post(Constants.USER + "/" + user)
        .then()
            .statusCode(200);

        verify(usersRepositoryManagementMBean).verifyExists(user);
        verify(usersRepositoryManagementMBean).addUser(user, password);
        verifyNoMoreInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void postShouldModifyPasswordIfExist() throws Exception {
        String user = "user";
        String password = "password";
        when(usersRepositoryManagementMBean.verifyExists(user)).thenReturn(true);

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("{\"password\":\"" + password + "\"}")
        .when()
            .post(Constants.USER + "/" + user)
        .then()
            .statusCode(200);

        verify(usersRepositoryManagementMBean).verifyExists(user);
        verify(usersRepositoryManagementMBean).setPassword(user, password);
        verifyNoMoreInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void postShouldNotAccessUnderlyingStorageWhenNoPath() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("{\"password\":\"password\"}")
        .when()
            .post(Constants.USER)
        .then()
            .statusCode(400);

        verifyZeroInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void postShouldNotAccessUnderlyingStorageWhenEmptyPath() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("{\"password\":\"password\"}")
        .when()
            .post(Constants.USER + "/")
        .then()
            .statusCode(400);

        verifyZeroInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void postShouldNotAccessUnderlyingStorageWhenNoJsonContent() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .when()
            .post(Constants.USER + "/user")
            .then()
            .statusCode(400);

        verifyZeroInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void postShouldNotAccessUnderlyingStorageWhenMalformedJsonContent() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("{\"password\":\"password\"")
            .when()
            .post(Constants.USER + "/user")
            .then()
            .statusCode(400);

        verifyZeroInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void postShouldNotAccessUnderlyingStorageWhenWrongJsonContent() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("{\"any\":\"password\"")
        .when()
            .post(Constants.USER + "/user")
        .then()
            .statusCode(400);

        verifyZeroInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void postShouldNotAccessUnderlyingStorageWhenAdditionalJsonContent() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("{\"any\":\"password\",\"password\":\"password\"}")
        .when()
            .post(Constants.USER + "/user")
        .then()
            .statusCode(400);

        verifyZeroInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void postShouldDisplayAnInternalErrorWhenProblemCheckingUserExists() throws Exception {
        String user = "user";

        when(usersRepositoryManagementMBean.verifyExists(user)).thenThrow(new Exception());

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("{\"password\":\"password\"}")
        .when()
            .post(Constants.USER + "/" + user)
        .then()
            .statusCode(500);

        verify(usersRepositoryManagementMBean).verifyExists(user);
        verifyNoMoreInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void postShouldDisplayAnInternalErrorWhenProblemAddingUser() throws Exception {
        String user = "user";
        String password = "password";

        when(usersRepositoryManagementMBean.verifyExists(user)).thenReturn(false);
        doThrow(new Exception("message"))
            .when(usersRepositoryManagementMBean)
            .addUser(user, password);

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("{\"password\":\"" + password + "\"}")
        .when()
            .post(Constants.USER + "/" + user)
        .then()
            .statusCode(500);

        verify(usersRepositoryManagementMBean).verifyExists(user);
        verify(usersRepositoryManagementMBean).addUser(user, password);
        verifyNoMoreInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void postShouldDisplayAnInternalErrorWhenProblemUpdatingUser() throws Exception {
        String user = "user";
        String password = "password";

        when(usersRepositoryManagementMBean.verifyExists(user)).thenReturn(true);
        doThrow(new Exception("message"))
            .when(usersRepositoryManagementMBean)
            .setPassword(user, password);

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("{\"password\":\"" + password + "\"}")
        .when()
            .post(Constants.USER + "/" + user)
        .then()
            .statusCode(500);

        verify(usersRepositoryManagementMBean).verifyExists(user);
        verify(usersRepositoryManagementMBean).setPassword(user, password);
        verifyNoMoreInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void getShouldReturnEmptyArrayByDefault() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .get(Constants.USER)
        .then()
            .statusCode(200)
            .body("usernames", hasSize(0));

        verify(usersRepositoryManagementMBean).listAllUsers();
        verifyNoMoreInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void getShouldReturnOneElementWhenOneUser() throws Exception {
        String user = "user";
        when(usersRepositoryManagementMBean.listAllUsers()).thenReturn(new String[]{user});

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .get(Constants.USER)
        .then()
            .statusCode(200)
            .body("usernames", hasSize(1))
            .body("usernames[0]", equalTo(user));

        verify(usersRepositoryManagementMBean).listAllUsers();
        verifyNoMoreInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void getUserShouldReturnTwoElementsWhenTwoUsers() throws Exception {
        String user1 = "user1";
        String user2 = "user2";
        when(usersRepositoryManagementMBean.listAllUsers()).thenReturn(new String[]{user1, user2});

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .get(Constants.USER)
        .then()
            .statusCode(200)
            .body("usernames", hasSize(2))
            .body("usernames[0]", equalTo(user1))
            .body("usernames[1]", equalTo(user2));

        verify(usersRepositoryManagementMBean).listAllUsers();
        verifyNoMoreInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void getUserShouldDisplayAnInternalErrorWhenProblemAddingUser() throws Exception {
        when(usersRepositoryManagementMBean.listAllUsers()).thenThrow(new Exception("message"));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .get(Constants.USER)
        .then()
            .statusCode(500);

        verify(usersRepositoryManagementMBean).listAllUsers();
        verifyNoMoreInteractions(usersRepositoryManagementMBean);
    }


    @Test
    public void deleteShouldAccessUnderlyingStorage() throws Exception {
        String user = "user";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .delete(Constants.USER + "/" + user)
        .then()
            .statusCode(200);

        verify(usersRepositoryManagementMBean).deleteUser(user);
        verifyNoMoreInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void deleteShouldNotAccessUnderlyingStorageWhenNoPath() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .delete(Constants.USER)
        .then()
            .statusCode(400);

        verifyZeroInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void deleteShouldNotAccessUnderlyingStorageWhenEmptyPath() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .delete(Constants.USER + "/")
        .then()
            .statusCode(400);

        verifyZeroInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void deleteShouldStillAccessUnderlyingStorageWhenAdditionalJsonContent() throws Exception {
        String user = "user";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("\"bad\":\"any\"}")
        .when()
            .delete(Constants.USER + "/" + user)
        .then()
            .statusCode(200);

        verify(usersRepositoryManagementMBean).deleteUser(user);
        verifyNoMoreInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void deleteShouldDisplayAnInternalErrorWhenUnknownProblemAddingUser() throws Exception {
        String user = "user";

        doThrow(new Exception("message"))
            .when(usersRepositoryManagementMBean)
            .deleteUser(user);

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .delete(Constants.USER + "/" + user)
        .then()
            .statusCode(500);

        verify(usersRepositoryManagementMBean).deleteUser(user);
        verifyNoMoreInteractions(usersRepositoryManagementMBean);
    }

    @Test
    public void deleteShouldDisplayAnInternalErrorWhenProblemAddingUser() throws Exception {
        String user = "user";

        doThrow(new UsersRepositoryException("message"))
            .when(usersRepositoryManagementMBean)
            .deleteUser(user);

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .delete(Constants.USER + "/" + user)
        .then()
            .statusCode(406);

        verify(usersRepositoryManagementMBean).deleteUser(user);
        verifyNoMoreInteractions(usersRepositoryManagementMBean);
    }

}
