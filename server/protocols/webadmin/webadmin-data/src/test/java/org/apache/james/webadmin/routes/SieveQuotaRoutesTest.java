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

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.apache.james.webadmin.Constants.SEPARATOR;
import static org.apache.james.webadmin.WebAdminServer.NO_CONFIGURATION;
import static org.apache.james.webadmin.routes.SieveQuotaRoutes.ROOT_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;
import org.apache.james.metrics.logger.DefaultMetricFactory;
import org.apache.james.sieverepository.api.SieveQuotaRepository;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminUtils;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SieveQuotaRoutesTest {

    private static final String USER_A = "userA";

    private WebAdminServer webAdminServer;
    private SieveQuotaRepository sieveRepository;

    @Before
    public void setUp() throws Exception {
        sieveRepository = new InMemorySieveQuotaRepository();
        webAdminServer = WebAdminUtils.createWebAdminServer(
                new DefaultMetricFactory(),
                new SieveQuotaRoutes(sieveRepository, new JsonTransformer()));
        webAdminServer.configure(NO_CONFIGURATION);
        webAdminServer.await();

        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8)))
                .setPort(webAdminServer.getPort().toInt())
                .build();
    }

    @After
    public void tearDown() throws Exception {
        webAdminServer.destroy();
    }

    @Test
    public void getGlobalSieveQuotaShouldReturn404WhenNoQuotaSet() throws Exception {
        given()
            .get(SieveQuotaRoutes.ROOT_PATH)
        .then()
            .statusCode(404);
    }

    @Test
    public void getGlobalSieveQuotaShouldReturnStoredValue() throws Exception {
        final long value = 1000L;
        sieveRepository.setQuota(value);

        final long actual =
            given()
                .get(SieveQuotaRoutes.ROOT_PATH)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(Long.class);

        assertThat(actual).isEqualTo(value);
    }

    @Test
    public void updateGlobalSieveQuotaShouldUpdateStoredValue() throws Exception {
        sieveRepository.setQuota(500L);
        final long requiredSize = 1024L;

        given()
            .body(requiredSize)
            .put(SieveQuotaRoutes.ROOT_PATH)
        .then()
            .statusCode(200);

        assertThat(sieveRepository.getQuota()).isEqualTo(requiredSize);
    }

    @Test
    public void updateGlobalSieveQuotaShouldReturn400WhenMalformedJSON() throws Exception {
        given()
            .body("invalid")
            .put(SieveQuotaRoutes.ROOT_PATH)
        .then()
            .statusCode(400);
    }

    @Test
    public void updateGlobalSieveQuotaShouldReturn400WhenRequestedSizeNotPositiveInteger() throws Exception {
        given()
            .body(-100L)
            .put(SieveQuotaRoutes.ROOT_PATH)
        .then()
            .statusCode(400);
    }

    @Test
    public void removeGlobalSieveQuotaShouldReturn404WhenNoQuotaSet() throws Exception {
        given()
            .delete(SieveQuotaRoutes.ROOT_PATH)
        .then()
            .statusCode(404);
    }

    @Test
    public void removeGlobalSieveQuotaShouldRemoveGlobalSieveQuota() throws Exception {
        sieveRepository.setQuota(1024L);

        given()
            .delete(SieveQuotaRoutes.ROOT_PATH)
        .then()
            .statusCode(204);
    }

    @Test
    public void getPerUserQuotaShouldReturn404WhenNoQuotaSetForUser() throws Exception {
        given()
            .get(ROOT_PATH + SEPARATOR + USER_A)
        .then()
            .statusCode(404);
    }

    @Test
    public void getPerUserSieveQuotaShouldReturnedStoredValue() throws Exception {
        final long value = 1024L;
        sieveRepository.setQuota(USER_A, value);

        final long actual =
            given()
                .get(ROOT_PATH + SEPARATOR + USER_A)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(Long.class);

        assertThat(actual).isEqualTo(value);
    }

    @Test
    public void updatePerUserSieveQuotaShouldUpdateStoredValue() throws Exception {
        sieveRepository.setQuota(USER_A, 500L);
        final long requiredSize = 1024L;

        given()
            .body(requiredSize)
            .put(ROOT_PATH + SEPARATOR + USER_A)
        .then()
            .statusCode(200);

        assertThat(sieveRepository.getQuota(USER_A)).isEqualTo(requiredSize);
    }

    @Test
    public void updatePerUserSieveQuotaShouldReturn400WhenMalformedJSON() throws Exception {
        given()
            .body("invalid")
            .put(ROOT_PATH + SEPARATOR + USER_A)
        .then()
            .statusCode(400);
    }

    @Test
    public void updatePerUserSieveQuotaShouldReturn400WhenRequestedSizeNotPositiveInteger() throws Exception {
        given()
            .body(-100L)
            .put(ROOT_PATH + SEPARATOR + USER_A)
        .then()
            .statusCode(400);
    }

    @Test
    public void removePerUserSieveQuotaShouldReturn404WhenNoQuotaSetForUser() throws Exception {
        given()
            .delete(ROOT_PATH + SEPARATOR + USER_A)
        .then()
            .statusCode(404);
    }

    @Test
    public void removePerUserSieveQuotaShouldRemoveQuotaForUser() throws Exception {
        sieveRepository.setQuota(USER_A, 1024L);

        given()
            .delete(ROOT_PATH + SEPARATOR + USER_A)
        .then()
            .statusCode(204);
    }
}
