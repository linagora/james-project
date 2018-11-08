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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.james.core.User;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.metrics.logger.DefaultMetricFactory;
import org.apache.james.sieverepository.api.ScriptContent;
import org.apache.james.sieverepository.api.ScriptName;
import org.apache.james.sieverepository.api.SieveRepository;
import org.apache.james.sieverepository.api.exception.ScriptNotFoundException;
import org.apache.james.sieverepository.api.exception.StorageException;
import org.apache.james.sieverepository.file.SieveFileRepository;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.memory.MemoryUsersRepository;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

public class SieveScriptRoutesTest {

    private static final String SIEVE_ROOT = FileSystem.FILE_PROTOCOL + "sieve";

    private FileSystem fileSystem;

    private WebAdminServer webAdminServer;
    private SieveRepository sieveRepository;
    private UsersRepository usersRepository;
    private String sieveContent;

    @BeforeEach
    public void setUp() throws ConfigurationException, IOException, UsersRepositoryException {
        this.fileSystem = new FileSystem() {
            @Override
            public File getBasedir() throws FileNotFoundException {
                return new File(System.getProperty("java.io.tmpdir"));
            }

            @Override
            public InputStream getResource(String url) throws IOException {
                return new FileInputStream(getFile(url));
            }

            @Override
            public File getFile(String fileURL) throws FileNotFoundException {
                return new File(getBasedir(), fileURL.substring(FileSystem.FILE_PROTOCOL.length()));
            }
        };

        sieveRepository = new SieveFileRepository(fileSystem);
        usersRepository = MemoryUsersRepository.withoutVirtualHosting();
        usersRepository.addUser("userA", "password");

        URL sieveResource = ClassLoader.getSystemResource("sieve/my_sieve");
        sieveContent = IOUtils.toString(sieveResource, StandardCharsets.UTF_8);

        webAdminServer = WebAdminUtils.createWebAdminServer(
            new DefaultMetricFactory(),
            new SieveScriptRoutes(sieveRepository, usersRepository));
        webAdminServer.configure(NO_CONFIGURATION);
        webAdminServer.await();

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webAdminServer)
            .build();
    }

    @AfterEach
    public void tearDown() throws IOException {
        File root = fileSystem.getFile(SIEVE_ROOT);
        // Remove files from the previous test, if any
        if (root.exists()) {
            FileUtils.forceDelete(root);
        }
        webAdminServer.destroy();
    }

    @Test
    public void defineAddActiveSieveScriptReturnNotFoundWhenUserNotExisted() throws IOException {
        given()
            .pathParam("userName", "unknown")
            .pathParam("scriptName", "scriptA")
            .body(sieveContent)
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void defineAddActiveSieveScriptReturnNotFoundWhenScriptNameIsWhiteSpace() throws IOException {
        String errorBody =
            "{\"statusCode\": 400," +
            " \"type\":\"InvalidArgument\"," +
            " \"message\":\"Invalid Sieve script name\"," +
            " \"details\":null" +
            "}";
        String body = given()
            .pathParam("userName", "userA")
            .pathParam("scriptName", " ")
            .body(sieveContent)
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .extract()
            .body().asString();

        assertThatJson(body).isEqualTo(errorBody);
    }

    @Test
    public void defineAddActiveSieveScriptReturnNotFoundWhenUserNameWhiteSpace() {
        String errorBody =
            "{\"statusCode\": 400," +
            " \"type\":\"InvalidArgument\"," +
            " \"message\":\"Invalid username\"," +
            " \"details\":null" +
            "}";
        String body = given()
            .pathParam("userName", " ")
            .pathParam("scriptName", "scriptA")
            .body(sieveContent)
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .extract()
            .body().asString();

        assertThatJson(body).isEqualTo(errorBody);
    }

    @Test
    public void defineAddActiveSieveScriptReturnInternalErrorWhenScriptIsNotSet() {
        given()
            .pathParam("userName", "userA")
            .pathParam("scriptName", "scriptA")
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }

    @Test
    public void defineAddActiveSieveScriptReturnSucceededWhenScriptIsWhiteSpace() throws ScriptNotFoundException, StorageException, IOException {
        given()
            .pathParam("userName", "userA")
            .pathParam("scriptName", "scriptA")
            .body(" ")
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(getScriptContent(sieveRepository
            .getScript(User.fromUsername("userA"), new ScriptName("scriptA"))))
            .isEqualTo(new ScriptContent(" "));
    }

    @Test
    public void defineAddActiveSieveScriptAddScriptSucceededOneWhenNotAddActivateParam() throws Exception {
        given()
            .pathParam("userName", "userA")
            .pathParam("scriptName", "scriptA")
            .body(sieveContent)
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(getScriptContent(sieveRepository
            .getScript(User.fromUsername("userA"), new ScriptName("scriptA"))))
            .isEqualTo(new ScriptContent(sieveContent));
    }

    @Test
    public void defineAddActiveSieveScriptSetActiveTrueWhenAddActivateParamTrue() throws Exception {
        given()
            .pathParam("userName", "userA")
            .pathParam("scriptName", "scriptA")
            .queryParam("activate", true)
            .body(sieveContent)
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(getScriptContent(sieveRepository
            .getActive(User.fromUsername("userA"))))
            .isEqualTo(new ScriptContent(sieveContent));
    }

    @Test
    public void defineAddActiveSieveScriptInvokeSieveRepositoryOneWhenAddActivateParamFalse() throws Exception {
        given()
            .pathParam("userName", "userA")
            .pathParam("scriptName", "scriptA")
            .queryParam("activate", false)
            .body(sieveContent)
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThatThrownBy(() -> sieveRepository.getActive(User.fromUsername("userA")))
            .isInstanceOf(ScriptNotFoundException.class);
    }

    @Test
    public void defineAddActiveSieveScriptInvokeSieveRepositoryOneWhenAddActivateParamWithNotBooleanValue() throws Exception {
        given()
            .pathParam("userName", "userA")
            .pathParam("scriptName", "scriptA")
            .queryParam("activate", "activate")
            .body(sieveContent)
        .when()
            .put("sieve/{userName}/scripts/{scriptName}")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .body("message", equalTo("Invalid activate query parameter"));
    }

    protected ScriptContent getScriptContent(InputStream inputStream) throws IOException {
        return new ScriptContent(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
    }
}
