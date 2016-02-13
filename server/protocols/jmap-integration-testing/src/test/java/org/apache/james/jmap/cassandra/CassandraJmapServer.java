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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.inject.Singleton;

import org.apache.james.CassandraJamesServer;
import org.apache.james.CassandraJamesServerMain;
import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.EmbeddedCassandra;
import org.apache.james.jmap.FixedDateZonedDateTimeProvider;
import org.apache.james.jmap.JmapServer;
import org.apache.james.jmap.utils.ZonedDateTimeProvider;
import org.apache.james.mailbox.elasticsearch.EmbeddedElasticSearch;
import org.apache.james.modules.TestElasticSearchModule;
import org.apache.james.modules.TestFilesystemModule;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.utils.ExtendedServerProbe;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.util.Modules;

public class CassandraJmapServer implements JmapServer {

    private static final int LIMIT_TO_3_MESSAGES = 3;
    private static final ZonedDateTime REFERENCE_DATE = ZonedDateTime.parse("2011-12-03T10:15:30+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    private CassandraJamesServer server;
    private EmbeddedElasticSearch embeddedElasticSearch;
    private EmbeddedCassandra cassandra;
    private FixedDateZonedDateTimeProvider zonedDateTimeProvider;

    private final Module module;

    public CassandraJmapServer(TemporaryFolder temporaryFolder) {
        embeddedElasticSearch = new EmbeddedElasticSearch(temporaryFolder);
        cassandra = EmbeddedCassandra.createStartServer();
        zonedDateTimeProvider = new FixedDateZonedDateTimeProvider();
        this.module = Modules.override(CassandraJamesServerMain.defaultModule)
                .with(defaultOverrideModule(temporaryFolder));
    }

    private Module defaultOverrideModule(TemporaryFolder temporaryFolder) {
        return Modules.combine(new TestElasticSearchModule(embeddedElasticSearch),
                new TestFilesystemModule(temporaryFolder),
                new TestJMAPServerModule(LIMIT_TO_3_MESSAGES),
                new AbstractModule() {
        
                    @Override
                    protected void configure() {
                        bind(EmbeddedCassandra.class).toInstance(cassandra);
                        bind(ZonedDateTimeProvider.class).toInstance(zonedDateTimeProvider);
                    }
        
                    @Provides
                    @Singleton
                    com.datastax.driver.core.Session provideSession(CassandraCluster initializedCassandra) {
                        return initializedCassandra.getConf();
                    }
                });
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            
            @Override
            public void evaluate() throws Throwable {
                try {
                    before();
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }

    private void before() throws Throwable {
        embeddedElasticSearch.before();
        server = new CassandraJamesServer(module);
        server.start();
        zonedDateTimeProvider.setFixedDateTime(REFERENCE_DATE);
    }

    private void after() {
        server.stop();
        embeddedElasticSearch.after();
    }

    @Override
    public int getPort() {
        return server.getJmapPort();
    }

    @Override
    public ExtendedServerProbe serverProbe() {
        return server.serverProbe();
    }

    @Override
    public void awaitForIndexation() {
        embeddedElasticSearch.awaitForElasticSearch();
    }

    @Override
    public void setFixedDateTime(ZonedDateTime date) {
        zonedDateTimeProvider.setFixedDateTime(date);
    }
}
