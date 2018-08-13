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

package org.apache.james.jmap.memory;

import java.io.IOException;

import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.apache.james.GuiceJamesServer;
import org.apache.james.MemoryJamesServerMain;
import org.apache.james.jmap.methods.integration.SetMessagesMethodWithBigMessageTestSystem;
import org.apache.james.mailbox.extractor.TextExtractor;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.apache.james.mailbox.store.search.PDFTextExtractor;
import org.apache.james.mailbox.store.search.SimpleMessageSearchIndex;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.server.core.configuration.Configuration;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.rules.TemporaryFolder;

public class MemorySetMessagesMethodWithBigMessageTestExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final int LIMIT_TO_10_MESSAGES = 10;

    private SetMessagesMethodWithBigMessageTestSystem testSystem;
    private TemporaryFolder temporaryFolder;

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        temporaryFolder = new TemporaryFolder();
        temporaryFolder.create();

        GuiceJamesServer jamesServer = createJamesServer();
        testSystem = new SetMessagesMethodWithBigMessageTestSystem(jamesServer);

        testSystem.before();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        testSystem.after();
        temporaryFolder.delete();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == SetMessagesMethodWithBigMessageTestSystem.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return testSystem;
    }

    private GuiceJamesServer createJamesServer() throws IOException {

        Configuration configuration = Configuration.builder()
            .workingDirectory(temporaryFolder.newFolder())
            .configurationFromClasspath()
            .build();

        return new GuiceJamesServer(configuration)
            .combineWith(MemoryJamesServerMain.IN_MEMORY_SERVER_AGGREGATE_MODULE)
            .overrideWith(new TestJMAPServerModule(LIMIT_TO_10_MESSAGES))
            .overrideWith(binder -> binder.bind(PersistenceAdapter.class).to(MemoryPersistenceAdapter.class))
            .overrideWith(binder -> binder.bind(TextExtractor.class).to(PDFTextExtractor.class))
            .overrideWith(binder -> binder.bind(MessageSearchIndex.class).to(SimpleMessageSearchIndex.class));
    }
}
