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

package org.apache.james.util.docker;

import java.net.Socket;
import java.time.Duration;

import javax.net.SocketFactory;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class TestContainerRule implements TestRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestContainerRule.class);
    private static final String NO_DOCKER_ENVIRONMENT = "Could not find a valid Docker environment.";
    private static final String SKIPPING_TEST_CAUTION = "Skipping all docker tests as no Docker environment was found";

    private GenericContainer<?> container;

    public TestContainerRule(String dockerImageName) {
        try {
            this.container = new GenericContainer<>(dockerImageName);
        } catch (IllegalStateException e) {
            logAndCheckSkipTest(e);
        }
    }

    public TestContainerRule(ImageFromDockerfile imageFromDockerfile) {
        try {
            this.container = new GenericContainer<>(imageFromDockerfile);
        } catch (IllegalStateException e) {
            logAndCheckSkipTest(e);
        }
    }

    public TestContainerRule(GenericContainer<?> container) {
        this.container = container;
    }
    
    private void logAndCheckSkipTest(IllegalStateException e) {
        LOGGER.error("Cannot initial a docker container", e);
        if (e.getMessage().startsWith(NO_DOCKER_ENVIRONMENT)) {
            Assume.assumeTrue(SKIPPING_TEST_CAUTION, false);
        }
    }

    public TestContainerRule withEnv(String key, String value) {
        container.addEnv(key, value);
        return this;
    }

    public TestContainerRule withExposedPorts(Integer... ports) {
        container.withExposedPorts(ports);
        return this;
    }

    public TestContainerRule waitingFor(WaitStrategy waitStrategy) {
        container.waitingFor(waitStrategy);
        return this;
    }

    public TestContainerRule withStartupTimeout(Duration startupTimeout) {
        container.withStartupTimeout(startupTimeout);
        return this;
    }

    public TestContainerRule withCommands(String... commands) {
        container.withCommand(commands);
        return this;
    }

    public void start() {
        container.start();
    }

    public void stop() {
        container.stop();
    }

    public void pause() {
        DockerClientFactory.instance().client().pauseContainerCmd(container.getContainerInfo().getId());
    }

    public void unpause() {
        DockerClientFactory.instance().client().unpauseContainerCmd(container.getContainerInfo().getId());
    }

    public Integer getMappedPort(int originalPort) {
        return container.getMappedPort(originalPort);
    }

    @SuppressWarnings("deprecation")
    public String getContainerIp() {
        return container.getContainerInfo().getNetworkSettings().getIpAddress();
    }
    
    public String getHostIp() {
        return container.getContainerIpAddress();
    }

    public boolean tryConnect(int port) {
        try {
            Socket socket = SocketFactory.getDefault().createSocket(getContainerIp(), port);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    container.start();
                    base.evaluate();
                } finally {
                    container.stop();
                }
            }
        };
    }

}
