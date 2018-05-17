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

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.james.CassandraJamesServerMain;
import org.apache.james.DockerCassandraRule;
import org.apache.james.GuiceJamesServer;
import org.apache.james.backends.es.EmbeddedElasticSearch;
import org.apache.james.jmap.methods.integration.JamesWithSpamAssassin;
import org.apache.james.mailbox.elasticsearch.MailboxElasticSearchConstants;
import org.apache.james.mailbox.extractor.TextExtractor;
import org.apache.james.mailbox.spamassassin.SpamAssassinListener;
import org.apache.james.mailbox.store.search.PDFTextExtractor;
import org.apache.james.modules.TestESMetricReporterModule;
import org.apache.james.modules.TestElasticSearchModule;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.modules.mailbox.MailboxListenersLoaderImpl;
import org.apache.james.server.core.configuration.Configuration;
import org.apache.james.transport.mailets.SpamAssassin;
import org.apache.james.util.scanner.SpamAssassinExtension;
import org.apache.james.utils.GuiceMailetLoader;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MailetContext;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.rules.TemporaryFolder;

import com.google.inject.multibindings.Multibinder;

public class CassandraJmapExtension implements BeforeAllCallback, AfterAllCallback,
    BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final int LIMIT_TO_20_MESSAGES = 20;

    private final TemporaryFolder temporaryFolder;
    private final DockerCassandraRule cassandra;
    private final EmbeddedElasticSearch elasticSearch;
    private final SpamAssassinExtension spamAssassinExtension;
    private JamesWithSpamAssassin james;

    public CassandraJmapExtension() {
        this.temporaryFolder = new TemporaryFolder();
        this.cassandra = new DockerCassandraRule();
        this.elasticSearch = new EmbeddedElasticSearch(temporaryFolder, MailboxElasticSearchConstants.DEFAULT_MAILBOX_INDEX);
        this.spamAssassinExtension = new SpamAssassinExtension();
    }

    private JamesWithSpamAssassin james() throws IOException {
        Configuration configuration = Configuration.builder()
            .workingDirectory(temporaryFolder.newFolder())
            .configurationFromClasspath()
            .build();
        return new JamesWithSpamAssassin(
                new GuiceJamesServer(configuration)
                    .combineWith(CassandraJamesServerMain.CASSANDRA_SERVER_MODULE, CassandraJamesServerMain.PROTOCOLS)
                    .overrideWith(binder -> binder.bind(TextExtractor.class).to(PDFTextExtractor.class))
                    .overrideWith(new TestJMAPServerModule(LIMIT_TO_20_MESSAGES))
                    .overrideWith(new TestESMetricReporterModule())
                    .overrideWith(cassandra.getModule())
                    .overrideWith(new TestElasticSearchModule(elasticSearch))
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
    public void beforeAll(ExtensionContext context) throws Exception {
        temporaryFolder.create();
        cassandra.start();
        elasticSearch.before();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        elasticSearch.after();
        cassandra.stop();
        temporaryFolder.delete();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        james = james();
        spamAssassinExtension.beforeEach(context);
        james.getJmapServer().start();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        james.getJmapServer().stop();
        spamAssassinExtension.afterEach(context);
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
