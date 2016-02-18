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
package org.apache.james.mpt.smtp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.CassandraJamesServerMain;
import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.cassandra.CassandraDomainListModule;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.jmap.JMAPConfiguration;
import org.apache.james.jmap.PortConfiguration;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMessageModule;
import org.apache.james.mailbox.elasticsearch.ClientProvider;
import org.apache.james.mailbox.elasticsearch.EmbeddedElasticSearch;
import org.apache.james.modules.TestFilesystemModule;
import org.apache.james.mpt.api.SmtpHostSystem;
import org.apache.james.mpt.smtp.dns.InMemoryDNSService;
import org.apache.james.mpt.smtp.host.JamesSmtpHostSystem;
import org.apache.james.rrt.cassandra.CassandraRRTModule;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.cassandra.CassandraUsersRepositoryModule;
import org.apache.james.utils.ConfigurationsPerformer;
import org.elasticsearch.client.Client;
import org.junit.rules.TemporaryFolder;

import com.datastax.driver.core.Session;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

public class SmtpTestModule extends AbstractModule {

    private final TemporaryFolder folder = new TemporaryFolder();
    private final CassandraCluster cassandraClusterSingleton;
    private final EmbeddedElasticSearch embeddedElasticSearch;

    public SmtpTestModule() throws IOException {
        folder.create();
        CassandraModuleComposite cassandraModuleComposite = new CassandraModuleComposite(
                new CassandraMailboxModule(),
                new CassandraMessageModule(),
                new CassandraDomainListModule(),
                new CassandraUsersRepositoryModule(),
                new CassandraRRTModule());
        cassandraClusterSingleton = CassandraCluster.create(cassandraModuleComposite);

        embeddedElasticSearch = new EmbeddedElasticSearch(folder);
    }

    @Override
    protected void configure() {
        install(Modules.override(CassandraJamesServerMain.defaultModule)
            .with(Modules.combine(
                new TestFilesystemModule(folder),
                new CassandraModule(cassandraClusterSingleton.getConf()),
                new DNSModule(),
                new ElasticSearchModule(embeddedElasticSearch),
                new JMAPModule())));
    }

    private static class CassandraModule extends AbstractModule {

        private final Session session;

        public CassandraModule(Session session) {
            this.session = session;
        }

        @Override
        protected void configure() {
        }

        @Provides
        public com.datastax.driver.core.Session cassandraSession() {
            return session;
        }
    }

    private static class DNSModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(InMemoryDNSService.class).in(Scopes.SINGLETON);
            bind(DNSService.class).to(InMemoryDNSService.class);
        }
    }

    private static class ElasticSearchModule extends AbstractModule {

        private final EmbeddedElasticSearch embeddedElasticSearch;

        public ElasticSearchModule(EmbeddedElasticSearch embeddedElasticSearch) {
            this.embeddedElasticSearch = embeddedElasticSearch;
        }

        @Override
        protected void configure() {
        }

        @Provides
        @Singleton
        protected ClientProvider provideClientProvider(FileSystem fileSystem) throws ConfigurationException, FileNotFoundException {
            return new ClientProvider() {
                
                @Override
                public Client get() {
                    return embeddedElasticSearch.getNode().client();
                }
            };
        }
    }

    private static class JMAPModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(PortConfiguration.class).toInstance(new PortConfiguration() {
                
                @Override
                public Optional<Integer> getPort() {
                    return Optional.empty();
                }
            });
        }

        @Provides
        @Singleton
        JMAPConfiguration provideConfiguration(FileSystem fileSystem) throws ConfigurationException, IOException{
            return JMAPConfiguration.builder()
                    .keystore("keystore")
                    .secret("james72laBalle")
                    .build();
        }
    }

    @Provides
    @Singleton
    public SmtpHostSystem provideHostSystem(ConfigurationsPerformer configurationsPerformer, DomainList domainList, UsersRepository usersRepository) throws Exception {
        return new JamesSmtpHostSystem(configurationsPerformer, domainList, usersRepository);
    }
}
