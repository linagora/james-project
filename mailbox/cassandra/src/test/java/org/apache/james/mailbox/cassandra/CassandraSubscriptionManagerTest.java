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

package org.apache.james.mailbox.cassandra;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.mailbox.AbstractSubscriptionManagerTest;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraUidProvider;
import org.apache.james.mailbox.cassandra.modules.CassandraSubscriptionModule;

import com.google.common.collect.ImmutableList;
import com.nurkiewicz.asyncretry.AsyncRetryExecutor;

/**
 * Test Cassandra subscription against some general purpose written code.
 */
public class CassandraSubscriptionManagerTest extends AbstractSubscriptionManagerTest {

    private static final CassandraCluster cassandra = CassandraCluster.create(new CassandraSubscriptionModule());
    private ImmutableList.Builder<ScheduledExecutorService> schedulers = ImmutableList.builder();

    @Override
    public SubscriptionManager createSubscriptionManager() {
        return new CassandraSubscriptionManager(
            new CassandraMailboxSessionMapperFactory(
                new CassandraUidProvider(cassandra.getConf(), createRetryer()),
                new CassandraModSeqProvider(cassandra.getConf(), createRetryer()),
                cassandra.getConf(),
                cassandra.getTypesProvider(),
                    createSingleThreadedScheduler())
        );
    }

    @Override
    protected void initialize() {
        schedulers = ImmutableList.builder();
    }

    @Override
    protected void shutDown() {
        schedulers.build().asList().stream()
                .filter(this::isNeitherNullNorShutdown)
                .forEach(ExecutorService::shutdown);
    }

    private boolean isNeitherNullNorShutdown(ScheduledExecutorService scheduler) {
        return scheduler != null && (!scheduler.isShutdown() || !scheduler.isTerminated());
    }

    private AsyncRetryExecutor createRetryer() {
        return new AsyncRetryExecutor(createSingleThreadedScheduler());
    }

    private ScheduledExecutorService createSingleThreadedScheduler() {
        ScheduledExecutorService newScheduler = Executors.newSingleThreadScheduledExecutor();
        schedulers.add(newScheduler);
        return newScheduler;
    }
}
