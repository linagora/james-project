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

import java.util.List;

import org.apache.james.util.Runnables;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import com.github.fge.lambdas.Throwing;
import com.google.common.collect.Lists;

public class JamesServerExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback, ParameterResolver {
    public interface JamesDefinition {
        GuiceJamesServer getServer() throws Exception;

        List<RegistrableExtension> registrableExtensions();
    }

    private final JamesDefinition jamesDefinition;
    private GuiceJamesServer jamesServer;

    public JamesServerExtension(JamesDefinition jamesDefinition) {
        this.jamesDefinition = jamesDefinition;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        Runnables.runParrallelStream(jamesDefinition.registrableExtensions()
            .stream()
            .map(ext -> Throwing.runnable(() -> ext.beforeAll(extensionContext))));
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        Runnables.runParrallelStream(jamesDefinition.registrableExtensions()
            .stream()
            .map(ext -> Throwing.runnable(() -> ext.beforeEach(extensionContext))));
        jamesServer = jamesDefinition.getServer();
        jamesServer.start();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        jamesServer.stop();
        Runnables.runParrallelStream(Lists.reverse(jamesDefinition.registrableExtensions())
            .stream()
            .map(ext -> Throwing.runnable(() -> ext.afterEach(extensionContext))));
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        Runnables.runParrallelStream(jamesDefinition.registrableExtensions()
            .stream()
            .map(ext -> Throwing.runnable(() -> ext.afterAll(extensionContext))));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return (parameterContext.getParameter().getType() == GuiceJamesServer.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return jamesServer;
    }
}
