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
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.mailbox.AbstractMailboxManagerTest;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraUidProvider;
import org.apache.james.mailbox.cassandra.modules.CassandraAclModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxCounterModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMessageModule;
import org.apache.james.mailbox.cassandra.modules.CassandraSubscriptionModule;
import org.apache.james.mailbox.cassandra.modules.CassandraUidAndModSeqModule;
import org.apache.james.mailbox.exception.BadCredentialsException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.JVMMailboxPathLocker;
import org.junit.After;
import org.junit.Before;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.nurkiewicz.asyncretry.AsyncRetryExecutor;

/**
 * CassandraMailboxManagerTest that extends the StoreMailboxManagerTest.
 * 
 */
public class CassandraMailboxManagerTest extends AbstractMailboxManagerTest {

    private static final CassandraCluster CASSANDRA = CassandraCluster.create(new CassandraModuleComposite(
        new CassandraAclModule(),
        new CassandraMailboxModule(),
        new CassandraMessageModule(),
        new CassandraMailboxCounterModule(),
        new CassandraUidAndModSeqModule(),
        new CassandraSubscriptionModule()));

    private ImmutableList.Builder<ScheduledExecutorService> schedulers = ImmutableList.builder();

    /**
     * Setup the mailboxManager.
     * 
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        CASSANDRA.ensureAllTables();
        CASSANDRA.clearAllTables();
        schedulers = ImmutableList.<ScheduledExecutorService>builder();
        createMailboxManager();
    }



    /**
     * Close the system session and entityManagerFactory
     * 
     * @throws MailboxException
     * @throws BadCredentialsException
     */
    @After
    public void tearDown() throws Exception {
        deleteAllMailboxes();
        MailboxSession session = getMailboxManager().createSystemSession("test", LoggerFactory.getLogger("Test"));
        session.close();
        shutDownSchedulers();
    }

    private void shutDownSchedulers() {
        schedulers.build().asList().stream()
                .filter(this::isNeitherNullNorShutdown)
                .forEach(ExecutorService::shutdown);
    }

    private boolean isNeitherNullNorShutdown(ScheduledExecutorService scheduler) {
        return scheduler != null && (!scheduler.isShutdown() || !scheduler.isTerminated());
    }

    /*
     * (non-Javadoc)i deve
     * 
     * @see org.apache.james.mailbox.MailboxManagerTest#createMailboxManager()
     */
    @Override
    protected void createMailboxManager() throws MailboxException {
        final CassandraUidProvider uidProvider = new CassandraUidProvider(CASSANDRA.getConf(), buildRetryer());
        final CassandraModSeqProvider modSeqProvider = new CassandraModSeqProvider(CASSANDRA.getConf(), buildRetryer());
        final CassandraMailboxSessionMapperFactory mapperFactory = new CassandraMailboxSessionMapperFactory(uidProvider,
            modSeqProvider,
            CASSANDRA.getConf(),
            CASSANDRA.getTypesProvider(), createSingleThreadedScheduler());

        final CassandraMailboxManager manager = new CassandraMailboxManager(mapperFactory, null, new JVMMailboxPathLocker());
        manager.init();

        setMailboxManager(manager);

        deleteAllMailboxes();
    }

    private AsyncRetryExecutor buildRetryer() {
        return new AsyncRetryExecutor(createSingleThreadedScheduler());
    }

    private ScheduledExecutorService createSingleThreadedScheduler() {
        ScheduledExecutorService newScheduler = Executors.newSingleThreadScheduledExecutor();
        schedulers.add(newScheduler);
        return newScheduler;
    }

    private void deleteAllMailboxes() throws BadCredentialsException, MailboxException {
        MailboxSession session = getMailboxManager().createSystemSession("test", LoggerFactory.getLogger("Test"));
        CASSANDRA.clearAllTables();
        session.close();
    }
}
