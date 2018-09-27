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

import org.apache.james.server.core.configuration.Configuration;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.rules.TemporaryFolder;

import com.google.inject.Module;

public class JPAJamesServerTestExtension implements BeforeEachCallback, BeforeAllCallback, AfterEachCallback, ParameterResolver {

    private final TemporaryFolder temporaryFolder;
    private final Module[] aditionalModules;
    private GuiceJamesServer jamesServer;

    JPAJamesServerTestExtension(Module... aditionalModules) {
        this.aditionalModules = aditionalModules;
        this.temporaryFolder = new TemporaryFolder();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        temporaryFolder.create();
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        jamesServer = createJamesServer();
        jamesServer.start();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        jamesServer.stop();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == GuiceJamesServer.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return jamesServer;
    }

    private GuiceJamesServer createJamesServer() throws IOException {
        Configuration configuration = Configuration.builder()
            .workingDirectory(temporaryFolder.newFolder())
            .configurationFromClasspath()
            .build();

        return GuiceJamesServer.forConfiguration(configuration)
            .combineWith(JPAJamesServerMain.JPA_SERVER_MODULE, JPAJamesServerMain.PROTOCOLS)
            .overrideWith(aditionalModules);
    }
}
