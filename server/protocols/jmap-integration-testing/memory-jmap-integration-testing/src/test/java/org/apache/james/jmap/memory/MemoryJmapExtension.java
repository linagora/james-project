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
import java.util.Iterator;

import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.james.GuiceJamesServer;
import org.apache.james.MemoryJamesServerMain;
import org.apache.james.jmap.methods.integration.JamesWithSpamAssassin;
import org.apache.james.mailbox.extractor.TextExtractor;
import org.apache.james.mailbox.spamassassin.SpamAssassinListener;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.apache.james.mailbox.store.search.PDFTextExtractor;
import org.apache.james.mailbox.store.search.SimpleMessageSearchIndex;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.modules.mailbox.MailboxListenersLoaderImpl;
import org.apache.james.server.core.configuration.Configuration;
import org.apache.james.transport.mailets.SpamAssassin;
import org.apache.james.util.scanner.SpamAssassinExtension;
import org.apache.james.utils.GuiceMailetLoader;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MailetContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.rules.TemporaryFolder;

import com.google.inject.multibindings.Multibinder;

public class MemoryJmapExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final int LIMIT_TO_20_MESSAGES = 20;

    private final TemporaryFolder temporaryFolder;
    private final SpamAssassinExtension spamAssassinExtension;
    private final JamesWithSpamAssassin james;

    public MemoryJmapExtension() throws IOException {
        this.temporaryFolder = new TemporaryFolder();
        this.spamAssassinExtension = new SpamAssassinExtension();
        this.james = james();
    }

    private JamesWithSpamAssassin james() throws IOException {
        temporaryFolder.create();
        Configuration configuration = Configuration.builder()
            .workingDirectory(temporaryFolder.newFolder())
            .configurationFromClasspath()
            .build();

        return new JamesWithSpamAssassin(
            new GuiceJamesServer(configuration)
                .combineWith(MemoryJamesServerMain.IN_MEMORY_SERVER_AGGREGATE_MODULE)
                .overrideWith(new TestJMAPServerModule(LIMIT_TO_20_MESSAGES))
                .overrideWith(binder -> binder.bind(PersistenceAdapter.class).to(MemoryPersistenceAdapter.class))
                .overrideWith(binder -> binder.bind(TextExtractor.class).to(PDFTextExtractor.class))
                .overrideWith(binder -> binder.bind(MessageSearchIndex.class).to(SimpleMessageSearchIndex.class))
                .overrideWith(
                    binder -> Multibinder.newSetBinder(binder, MailboxListenersLoaderImpl.DynamicConfiguration.class)
                        .addBinding()
                        .toInstance(generateListenerConfiguration(spamAssassinExtension)))
                .overrideWith(
                    binder -> Multibinder.newSetBinder(binder, GuiceMailetLoader.DynamicConfiguration.class)
                        .addBinding()
                        .toInstance(generateMailetConfiguration(spamAssassinExtension))),
            spamAssassinExtension);
    }

    public MailboxListenersLoaderImpl.DynamicConfiguration generateListenerConfiguration(SpamAssassinExtension extension) {
        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
        builder.addProperty("host", extension.getSpamAssassin().getIp());
        builder.addProperty("port", extension.getSpamAssassin().getBindingPort());

        return new MailboxListenersLoaderImpl.DynamicConfiguration(SpamAssassinListener.class, builder);
    }

    public GuiceMailetLoader.DynamicConfiguration generateMailetConfiguration(SpamAssassinExtension extension) {
        GuiceMailetLoader.MailetConfigWrapper wrapper = config -> new MailetConfig() {
            @Override
            public String getInitParameter(String name) {
                if (name.equals(SpamAssassin.SPAMD_HOST)) {
                    return extension.getSpamAssassin().getIp();
                }
                if (name.equals(SpamAssassin.SPAMD_PORT)) {
                    return String.valueOf(extension.getSpamAssassin().getBindingPort());
                }
                return config.getInitParameter(name);
            }

            @Override
            public Iterator<String> getInitParameterNames() {
                return config.getInitParameterNames();
            }

            @Override
            public MailetContext getMailetContext() {
                return config.getMailetContext();
            }

            @Override
            public String getMailetName() {
                return config.getMailetName();
            }
        };

        return new GuiceMailetLoader.DynamicConfiguration(SpamAssassin.class, wrapper);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        spamAssassinExtension.beforeEach(context);
        temporaryFolder.create();
        james.getJmapServer().start();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        james.getJmapServer().stop();
        spamAssassinExtension.afterEach(context);
        temporaryFolder.delete();
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
