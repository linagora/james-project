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
package org.apache.james.jmap.cassandra;

import static org.apache.james.CassandraJmapTestExtension.EMBEDDED_ES;

import java.io.IOException;

import org.apache.james.CassandraJmapTestExtension;
import org.apache.james.jmap.methods.integration.JamesWithSpamAssassin;
import org.apache.james.jmap.methods.integration.SpamAssassinModule;
import org.apache.james.mailbox.extractor.TextExtractor;
import org.apache.james.mailbox.store.search.PDFTextExtractor;
import org.apache.james.modules.TestESMetricReporterModule;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.spamassassin.SpamAssassinExtension;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class SpamAssassinCassandraJmapExtension implements BeforeAllCallback, AfterAllCallback,
    BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final int LIMIT_TO_20_MESSAGES = 20;

    private final CassandraJmapTestExtension jamesExtension;
    private final SpamAssassinExtension spamAssassinExtension;
    private JamesWithSpamAssassin james;

    public SpamAssassinCassandraJmapExtension() {
        this.spamAssassinExtension = new SpamAssassinExtension();
        this.jamesExtension = CassandraJmapTestExtension.builder()
            .extensions(EMBEDDED_ES)
            .overrideModules(
                binder -> binder.bind(TextExtractor.class).to(PDFTextExtractor.class),
                new TestJMAPServerModule(LIMIT_TO_20_MESSAGES),
                new TestESMetricReporterModule(),
                new SpamAssassinModule(spamAssassinExtension))
            .build();
    }

    private JamesWithSpamAssassin james() throws IOException {
        return new JamesWithSpamAssassin(
            jamesExtension.getJamesServer(),
            spamAssassinExtension);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        jamesExtension.beforeAll(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        jamesExtension.afterAll(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        jamesExtension.beforeEach(context);
        james = james();
        spamAssassinExtension.beforeEach(context);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        spamAssassinExtension.afterEach(context);
        jamesExtension.afterEach(context);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == JamesWithSpamAssassin.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return james;
    }
}
