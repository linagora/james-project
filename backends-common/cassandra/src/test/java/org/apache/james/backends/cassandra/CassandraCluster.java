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
package org.apache.james.backends.cassandra;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.CassandraTableManager;
import org.apache.james.backends.cassandra.init.CassandraTypesProvider;
import org.apache.james.backends.cassandra.init.ClusterFactory;
import org.apache.james.backends.cassandra.init.ClusterWithKeyspaceCreatedFactory;
import org.apache.james.backends.cassandra.init.SessionWithInitializedTablesFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.google.common.base.Throwables;
import com.nurkiewicz.asyncretry.AsyncRetryExecutor;

public final class CassandraCluster {
    private static final String CLUSTER_IP = "localhost";
    private static final int CLUSTER_PORT_TEST = 9142;
    private static final String KEYSPACE_NAME = "apache_james";
    private static final int REPLICATION_FACTOR = 1;

    private static final long SLEEP_BEFORE_RETRY = 200;
    private static final int MAX_RETRY = 2000;

    private final CassandraModule module;
    private Session session;
    private CassandraTypesProvider typesProvider;

    public static CassandraCluster create(CassandraModule module) throws RuntimeException {
        return new CassandraCluster(module, EmbeddedCassandra.createStartServer(), Executors.newSingleThreadScheduledExecutor());
    }

    @Inject
    private CassandraCluster(CassandraModule module, EmbeddedCassandra embeddedCassandra, ScheduledExecutorService scheduler) throws RuntimeException {
        this.module = module;
        try {
            AsyncRetryExecutor executor = new AsyncRetryExecutor(scheduler);
            session = executor.retryOn(NoHostAvailableException.class)
                    .withMaxRetries(MAX_RETRY)
                    .withMinDelay(SLEEP_BEFORE_RETRY)
                    .getWithRetry(CassandraCluster.this::initializeSession)
                    .get();
            typesProvider = new CassandraTypesProvider(module, this.session);
        } catch (Exception exception) {
            Throwables.propagate(exception);
        }
    }

    public Session getConf() {
        return session;
    }

    public void ensureAllTables() {
        new CassandraTableManager(module, session).ensureAllTables();
    }

    @PreDestroy
    public void clearAllTables() {
        new CassandraTableManager(module, session).clearAllTables();
    }

    private Session initializeSession() {
        Cluster clusterWithInitializedKeyspace = ClusterWithKeyspaceCreatedFactory
            .clusterWithInitializedKeyspace(getCluster(), KEYSPACE_NAME, REPLICATION_FACTOR);
        return new SessionWithInitializedTablesFactory(module).createSession(clusterWithInitializedKeyspace, KEYSPACE_NAME);
    }

    public Cluster getCluster() {
        return ClusterFactory.createTestingCluster(CLUSTER_IP, CLUSTER_PORT_TEST);
    }

    public CassandraTypesProvider getTypesProvider() {
        return typesProvider;
    }
}
