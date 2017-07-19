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

import static com.jayway.restassured.RestAssured.when;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.apache.james.webadmin.WebAdminServer.NO_CONFIGURATION;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionDAO;
import org.apache.james.mailbox.cassandra.mail.migration.Migration;
import org.apache.james.metrics.logger.DefaultMetricFactory;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.service.MigrationService;
import org.apache.james.webadmin.utils.JsonTransformer;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MigrationRoutesTest {

    private static final Integer LATEST_VERSION = 3;
    private static final Integer CURRENT_VERSION = 2;
    private static final Integer OLDER_VERSION = 1;
    private WebAdminServer webAdminServer;
    private CassandraSchemaVersionDAO schemaVersionDAO;
    private Map<Integer, Migration> allMigrationClazz;

    private void createServer() throws Exception {
        ImmutableMap.Builder<Integer, Migration> builder = ImmutableMap.builder();
        allMigrationClazz = builder.put(OLDER_VERSION, mock(Migration.class))
                .put(CURRENT_VERSION, mock(Migration.class))
                .put(LATEST_VERSION, mock(Migration.class))
                .build();
        schemaVersionDAO = mock(CassandraSchemaVersionDAO.class);

        webAdminServer = new WebAdminServer(
            new DefaultMetricFactory(),
            new MigrationRoutes(new MigrationService(schemaVersionDAO, allMigrationClazz, LATEST_VERSION), new JsonTransformer()));
        webAdminServer.configure(NO_CONFIGURATION);
        webAdminServer.await();

        RestAssured.requestSpecification = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setBasePath(MigrationRoutes.VERSION_BASE)
            .setPort(webAdminServer.getPort().toInt())
            .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8)))
            .build();
    }

    @Before
    public void setUp() throws Exception {
        createServer();
    }

    @After
    public void tearDown() {
        webAdminServer.destroy();
    }

    @Test
    public void getShouldReturnTheCurrentVersion() throws Exception {
        when(schemaVersionDAO.getCurrentSchemaVersion()).thenReturn(CompletableFuture.completedFuture(Optional.of(CURRENT_VERSION)));

        when()
            .get()
        .then()
            .statusCode(200)
            .body(is("{\"version\":2}"));
    }

    @Test
    public void getShouldReturnTheLatestVersionWhenSetUpTheLatestVersion() throws Exception {
        when()
            .get("/latest")
        .then()
            .statusCode(200)
            .body(is("{\"version\":" + LATEST_VERSION + "}"));
    }

    @Test
    public void postShouldReturnConflictWhenMigrationOnRunning() throws Exception {
        when()
            .post("/upgrade")
        .then()
            .statusCode(409);
    }

    @Test
    public void postShouldDoMigration() throws Exception {
        when()
            .post("/upgrade")
        .then()
            .statusCode(204);

        verifyNoMoreInteractions(schemaVersionDAO);
    }
}
