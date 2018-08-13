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

package org.apache.james;

import org.apache.commons.text.RandomStringGenerator;
import org.apache.james.backends.cassandra.DockerCassandraRule;
import org.apache.james.backends.cassandra.init.configuration.ClusterConfiguration;
import org.apache.james.util.Host;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;

import com.google.inject.Module;

public class DockerCassandraTestExtension implements BeforeEachCallback, AfterEachCallback, GuiceModuleTestExtension {

    private DockerCassandraRule cassandraContainer;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        cassandraContainer = new DockerCassandraRule();
        cassandraContainer.start();

        registerSelfToContext(context);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        cassandraContainer.stop();
    }

    @Override
    public Module getModule() {
        String keyspace = new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(12);
        return (binder) -> binder.bind(ClusterConfiguration.class)
            .toInstance(ClusterConfiguration.builder()
                .host(cassandraContainer.getHost())
                .keyspace(keyspace)
                .replicationFactor(1)
                .maxRetry(20)
                .minDelay(5000)
                .build());
    }

    public String getIp() {
        return cassandraContainer.getIp();
    }

    public Host getHost() {
        return cassandraContainer.getHost();
    }

    public Integer getMappedPort(int originalPort) {
        return cassandraContainer.getBindingPort();
    }

    public void start() {
        cassandraContainer.start();
    }

    public void stop() {
        cassandraContainer.stop();
    }

    public GenericContainer<?> getRawContainer() {
        return cassandraContainer.getRawContainer();
    }

    public void pause() {
        cassandraContainer.pause();
    }

    public void unpause() {
        cassandraContainer.unpause();
    }
}
