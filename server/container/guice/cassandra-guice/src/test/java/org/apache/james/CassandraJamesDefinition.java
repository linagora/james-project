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

import static org.apache.james.CassandraJamesServerMain.ALL_BUT_JMX_CASSANDRA_MODULE;

import java.util.List;

import org.apache.james.mailbox.extractor.TextExtractor;
import org.apache.james.mailbox.store.search.PDFTextExtractor;
import org.apache.james.modules.TestESMetricReporterModule;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.server.core.configuration.Configuration;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import com.google.inject.util.Modules;

public class CassandraJamesDefinition implements JamesServerExtension.JamesDefinition {

    public static class Builder {
        private final ImmutableList.Builder<GuiceModuleTestExtension> extensions;
        private final ImmutableList.Builder<Module> modules;

        private Builder() {
            extensions = ImmutableList.builder();
            modules = ImmutableList.builder();
        }

        public Builder defaultExtensions() {
            this.extensions.add(
                new EmbeddedElasticSearchExtension(),
                new CassandraRegistrableExtension());
            return this;
        }

        public Builder addExtensions(GuiceModuleTestExtension... extensions) {
            this.extensions.add(extensions);
            return this;
        }

        public Builder addModules(Module... modules) {
            this.modules.add(modules);
            return this;
        }

        public CassandraJamesDefinition build() {
            return new CassandraJamesDefinition(modules.build(), extensions.build());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    static class CassandraRegistrableExtension implements GuiceModuleTestExtension {

        private final DockerCassandraRule cassandra;

        CassandraRegistrableExtension() {
            this.cassandra = new DockerCassandraRule();
        }

        @Override
        public void beforeAll(ExtensionContext extensionContext) {
            cassandra.start();
        }

        @Override
        public void afterAll(ExtensionContext extensionContext) {
            cassandra.stop();
        }

        @Override
        public Module getModule() {
            return cassandra.getModule();
        }
    }

    private static final int LIMIT_TO_10_MESSAGES = 10;

    private final TemporaryFolderRegistrableExtension folderRegistrableExtension;
    private final List<Module> additionalModules;
    private final List<GuiceModuleTestExtension> extensions;

    private CassandraJamesDefinition(List<Module> additionalModules, List<GuiceModuleTestExtension> extensions) {
        this.additionalModules = additionalModules;
        this.extensions = extensions;

        this.folderRegistrableExtension = new TemporaryFolderRegistrableExtension();
    }

    @Override
    public GuiceJamesServer getServer() throws Exception {
        Configuration configuration = Configuration.builder()
            .workingDirectory(folderRegistrableExtension.getTemporaryFolder().newFolder())
            .configurationFromClasspath()
            .build();

        return GuiceJamesServer.forConfiguration(configuration)
            .combineWith(ALL_BUT_JMX_CASSANDRA_MODULE)
            .overrideWith(Modules.combine(
                binder -> binder.bind(TextExtractor.class).to(PDFTextExtractor.class),
                new TestJMAPServerModule(LIMIT_TO_10_MESSAGES),
                new TestESMetricReporterModule()))
            .overrideWith(extensions.stream()
                .map(GuiceModuleTestExtension::getModule)
                .collect(ImmutableList.toImmutableList()))
            .overrideWith(additionalModules);
    }

    @Override
    public List<RegistrableExtension> registrableExtensions() {
        return ImmutableList.<RegistrableExtension>builder()
            .add(folderRegistrableExtension)
            .addAll(extensions)
            .build();
    }
}
