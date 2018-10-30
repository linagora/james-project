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

import io.restassured.RestAssured;
import org.apache.james.core.healthcheck.ComponentName;
import org.apache.james.core.healthcheck.HealthCheck;
import org.apache.james.core.healthcheck.Result;
import org.apache.james.metrics.logger.DefaultMetricFactory;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminUtils;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static io.restassured.RestAssured.when;
import static org.apache.james.webadmin.WebAdminServer.NO_CONFIGURATION;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;

public class HealthCheckRoutesTest {

    private static final String NAME_1 = "component-1";
    private static final String NAME_2 = "component-2";
    private static final String NAME_3 = "component 3";
    private static final String NAME_3_ESCAPED = "component%203";

    private static final ComponentName COMPONENT_NAME_1 = new ComponentName(NAME_1);
    private static final ComponentName COMPONENT_NAME_2 = new ComponentName(NAME_2);
    private static final ComponentName COMPONENT_NAME_3 = new ComponentName(NAME_3);

    private static HealthCheck healthCheck(Result result) {
        return new HealthCheck() {
            @Override
            public ComponentName componentName() {
                return result.getComponentName();
            }

            @Override
            public Result check() {
                return result;
            }
        };
    }

    private WebAdminServer webAdminServer;
    private Set<HealthCheck> healthChecks;

    @Before
    public void setUp() throws Exception {
        healthChecks = new HashSet<>();
        webAdminServer = WebAdminUtils.createWebAdminServer(
            new DefaultMetricFactory(),
            new HealthCheckRoutes(healthChecks, new JsonTransformer()));

        webAdminServer.configure(NO_CONFIGURATION);
        webAdminServer.await();

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webAdminServer)
            .setBasePath(HealthCheckRoutes.HEALTHCHECK)
            .build();
    }

    @After
    public void tearDown() {
        webAdminServer.destroy();
    }

    @Test
    public void validateHealthchecksShouldReturnOkWhenNoHealthChecks() {
        when()
            .get()
        .then()
            .statusCode(HttpStatus.OK_200);
    }

    @Test
    public void validateHealthchecksShouldReturnOkWhenHealthChecksAreHealthy() {
        healthChecks.add(healthCheck(Result.healthy(COMPONENT_NAME_1)));
        healthChecks.add(healthCheck(Result.healthy(COMPONENT_NAME_2)));

        when()
            .get()
        .then()
            .statusCode(HttpStatus.OK_200);
    }

    @Test
    public void validateHealthchecksShouldReturnInternalErrorWhenOneHealthCheckIsUnhealthy() {
        healthChecks.add(healthCheck(Result.unhealthy(COMPONENT_NAME_1, "cause")));
        healthChecks.add(healthCheck(Result.healthy(COMPONENT_NAME_2)));

        when()
            .get()
        .then()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }

    @Test
    public void validateHealthchecksShouldReturnInternalErrorWhenAllHealthChecksAreUnhealthy() {
        healthChecks.add(healthCheck(Result.unhealthy(COMPONENT_NAME_1, "cause")));
        healthChecks.add(healthCheck(Result.unhealthy(COMPONENT_NAME_2)));

        when()
            .get()
        .then()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }

    @Test
    public void validateHealthchecksShouldReturnInternalErrorWhenOneHealthCheckIsDegraded() {
        healthChecks.add(healthCheck(Result.degraded(COMPONENT_NAME_1, "cause")));
        healthChecks.add(healthCheck(Result.healthy(COMPONENT_NAME_2)));

        when()
            .get()
        .then()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }

    @Test
    public void getHealthchecksShouldReturnEmptyWhenNoHealthChecks() {
        when()
                .get(HealthCheckRoutes.CHECKS)
                .then()
                .body(emptyArray())
                .statusCode(HttpStatus.OK_200);
    }

    @Test
    public void getHealthchecksShouldReturnHealthChecksWhenHealthChecksPresent() {
        healthChecks.add(healthCheck(Result.healthy(COMPONENT_NAME_3)));
        when()
                .get(HealthCheckRoutes.CHECKS)
                .then()
                .body("componentName[0]", equalTo(NAME_3))
                .body("escapedComponentName[0]", equalTo(NAME_3_ESCAPED))
                .statusCode(HttpStatus.OK_200);
    }
}