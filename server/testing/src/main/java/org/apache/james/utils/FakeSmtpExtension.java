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

package org.apache.james.utils;

import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.ONE_MILLISECOND;
import static org.awaitility.Duration.ONE_MINUTE;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.james.util.docker.Images;
import org.apache.james.util.docker.SwarmGenericContainer;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

public class FakeSmtpExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback {

    public static FakeSmtpExtension withSmtpPort(Integer smtpPort) {
        SwarmGenericContainer container = fakeSmtpContainer()
            .withCommands("node", "cli", "--listen", "80", "--smtp", smtpPort.toString());

        return new FakeSmtpExtension(container, smtpPort, AWAIT);
    }

    private static SwarmGenericContainer fakeSmtpContainer() {
        return new SwarmGenericContainer(Images.FAKE_SMTP)
            .withAffinityToContainer()
            .waitingFor(new HostPortWaitStrategy());
    }

    private static final ConditionFactory AWAIT = Awaitility.with()
            .pollInterval(ONE_HUNDRED_MILLISECONDS)
            .and()
            .with()
            .pollDelay(ONE_MILLISECOND)
            .atMost(ONE_MINUTE)
            .await();
    private static final int SMTP_PORT = 25;
    private static final ResponseSpecification RESPONSE_SPECIFICATION = new ResponseSpecBuilder().build();
    private final SwarmGenericContainer container;
    private final Integer smtpPort;
    private final ConditionFactory calmyAwait;

    public FakeSmtpExtension() {
        this(fakeSmtpContainer().withExposedPorts(SMTP_PORT), SMTP_PORT, AWAIT);
    }

    private FakeSmtpExtension(SwarmGenericContainer container, Integer smtpPort, ConditionFactory calmyAwait) {
        this.smtpPort = smtpPort;
        this.container = container;
        this.calmyAwait = calmyAwait;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        container.start();
        awaitStarted();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        clean();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        container.stop();
    }

    public void awaitStarted() {
            calmyAwait.until(() -> container.tryConnect(smtpPort));
        }

    public boolean isReceived(Function<ValidatableResponse, ValidatableResponse> expectations) {
        try {
            expectations.apply(
                given(requestSpecification(), RESPONSE_SPECIFICATION)
                    .get("/api/email")
                .then()
                    .statusCode(200));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private RequestSpecification requestSpecification() {
        return new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(StandardCharsets.UTF_8)))
            .setPort(80)
            .setBaseUri("http://" + container.getContainerIp())
            .build();
    }

    public SwarmGenericContainer getContainer() {
        return container;
    }

    public void clean() {
        given(requestSpecification(), RESPONSE_SPECIFICATION)
            .get("/api/email")
            .jsonPath()
            .getList("id", String.class)
            .stream()
            .mapToInt(Integer::valueOf)
            .max()
            .ifPresent(id -> given(requestSpecification(), RESPONSE_SPECIFICATION)
                .get("/api/email/purge/" + id));
    }

}
