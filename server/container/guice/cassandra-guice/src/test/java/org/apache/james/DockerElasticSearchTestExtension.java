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

import org.apache.james.mailbox.elasticsearch.IndexAttachments;
import org.apache.james.modules.mailbox.ElasticSearchConfiguration;
import org.apache.james.util.Host;
import org.apache.james.util.docker.Images;
import org.apache.james.util.docker.SwarmGenericContainer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.google.inject.Module;

public class DockerElasticSearchTestExtension implements BeforeEachCallback, AfterEachCallback, GuiceModuleTestExtension {

    private static final int ELASTIC_SEARCH_PORT = 9300;
    public static final int ELASTIC_SEARCH_HTTP_PORT = 9200;

    private SwarmGenericContainer container = new SwarmGenericContainer(Images.ELASTICSEARCH)
        .withExposedPorts(ELASTIC_SEARCH_HTTP_PORT, ELASTIC_SEARCH_PORT);

    public ElasticSearchConfiguration getElasticSearchConfigurationForDocker() {
        return ElasticSearchConfiguration.builder()
            .addHost(Host.from(getIp(), container.getMappedPort(ELASTIC_SEARCH_PORT)))
            .indexAttachment(IndexAttachments.NO)
            .build();
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        container = new SwarmGenericContainer(Images.ELASTICSEARCH).withExposedPorts(9200, 9300);
        container.start();

        registerSelfToContext(extensionContext);
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        container.stop();
    }

    @Override
    public Module getModule() {
        return (binder) ->
                binder.bind(ElasticSearchConfiguration.class)
                    .toInstance(getElasticSearchConfigurationForDocker());
    }

    public String getIp() {
        return container.getHostIp();
    }

    public SwarmGenericContainer getElasticSearchContainer() {
        return container;
    }

    public void pause() {
        container.pause();
    }

    public void unpause() {
        container.unpause();
    }
}
