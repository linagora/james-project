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

import java.io.IOException;

import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.apache.james.mailbox.extractor.TextExtractor;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.apache.james.mailbox.store.search.PDFTextExtractor;
import org.apache.james.mailbox.store.search.SimpleMessageSearchIndex;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.server.core.configuration.Configuration;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

public class MemoryJmapTestExtension implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, ParameterResolver {

    public static class Builder {
        private boolean ignoreEach;
        private ImmutableList<Module> addtionalModules;

        public Builder() {
            this.ignoreEach = false;
            this.addtionalModules = EMPTY_MODULES;
        }

        public Builder ignoreEach() {
            this.ignoreEach = true;
            return this;
        }

        public Builder modules(Module... addtionalModules) {
            this.addtionalModules = ImmutableList.copyOf(addtionalModules);
            return this;
        }

        public MemoryJmapTestExtension build() {
            return new MemoryJmapTestExtension(addtionalModules, ignoreEach);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private static final ImmutableList<Module> EMPTY_MODULES = ImmutableList.of();
    private static final int LIMIT_TO_10_MESSAGES = 10;
    
    private final TemporaryFolder temporaryFolder;
    private final ImmutableList<Module> addtionalModules;
    private final boolean ignoreEach;

    private GuiceJamesServer jamesServer;

    private MemoryJmapTestExtension(ImmutableList<Module> addtionalModules, boolean ignoreEach) {
        this.ignoreEach = ignoreEach;
        this.temporaryFolder = new TemporaryFolder();
        this.addtionalModules = addtionalModules;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        temporaryFolder.create();
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        if (!ignoreEach) {
            jamesServer = jmapServer();
            jamesServer.start();
        }
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        if (!ignoreEach) {
            jamesServer.stop();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == GuiceJamesServer.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return jamesServer;
    }

    public GuiceJamesServer getJamesServer() {
        return jamesServer;
    }

    public GuiceJamesServer jmapServer(Module... customModules) throws IOException {
        Configuration configuration = Configuration.builder()
            .workingDirectory(temporaryFolder.newFolder())
            .configurationFromClasspath()
            .build();
        return GuiceJamesServer.forConfiguration(configuration)
            .combineWith(MemoryJamesServerMain.IN_MEMORY_SERVER_AGGREGATE_MODULE)
            .overrideWith(new TestJMAPServerModule(LIMIT_TO_10_MESSAGES))
            .overrideWith(binder -> binder.bind(PersistenceAdapter.class).to(MemoryPersistenceAdapter.class))
            .overrideWith(binder -> binder.bind(TextExtractor.class).to(PDFTextExtractor.class))
            .overrideWith(binder -> binder.bind(MessageSearchIndex.class).to(SimpleMessageSearchIndex.class))
            .overrideWith(addtionalModules)
            .overrideWith(customModules);
    }
}
