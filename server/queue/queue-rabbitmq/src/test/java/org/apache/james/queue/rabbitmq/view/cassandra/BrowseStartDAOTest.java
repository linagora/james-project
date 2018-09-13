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

package org.apache.james.queue.rabbitmq.view.cassandra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.DockerCassandraRule;
import org.apache.james.queue.rabbitmq.MailQueueName;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class BrowseStartDAOTest {

    private static final MailQueueName OUT_GOING_1 = MailQueueName.fromString("OUT_GOING_1");
    private static final MailQueueName OUT_GOING_2 = MailQueueName.fromString("OUT_GOING_2");
    private static Instant NOW = Instant.now();
    private static Instant NOW_PLUS_TEN_SECONDS = NOW.plusSeconds(10);

    @ClassRule
    public static DockerCassandraRule cassandraServer = new DockerCassandraRule();
    private static CassandraCluster cassandra;

    private BrowseStartDAO testee;

    @BeforeClass
    public static void setUpClass() {
        cassandra = CassandraCluster.create(CassandraMailQueueViewModule.MODULE, cassandraServer.getHost());
    }

    @Before
    public void setUp() {
        testee = new BrowseStartDAO(cassandra.getConf());
    }

    @After
    public void tearDown() {
        cassandra.clearTables();
    }

    @AfterClass
    public static void tearDownClass() {
        cassandra.closeCluster();
    }

    @Test
    public void findBrowseStartShouldReturnEmptyWhenTableDoesntContainQueueName() {
        testee.updateBrowseStart(OUT_GOING_1, NOW).join();

        Optional<Instant> firstEnqueuedItemFromQueue2 = testee.findBrowseStart(OUT_GOING_2).join();
        assertThat(firstEnqueuedItemFromQueue2)
            .isEmpty();
    }

    @Test
    public void findBrowseStartShouldReturnInstantWhenTableContainsQueueName() {
        testee.updateBrowseStart(OUT_GOING_1, NOW).join();
        testee.updateBrowseStart(OUT_GOING_2, NOW).join();

        Optional<Instant> firstEnqueuedItemFromQueue2 = testee.findBrowseStart(OUT_GOING_2).join();
        assertThat(firstEnqueuedItemFromQueue2)
            .isNotEmpty();
    }

    @Test
    public void updateFirstEnqueuedTimeShouldWork() {
        testee.updateBrowseStart(OUT_GOING_1, NOW).join();

        assertThat(testee.selectOne(OUT_GOING_1).join())
            .isNotEmpty();
    }

    @Test
    public void insertInitialBrowseStartShouldInsertFirstInstant() {
        testee.insertInitialBrowseStart(OUT_GOING_1, NOW).join();
        testee.insertInitialBrowseStart(OUT_GOING_1, NOW_PLUS_TEN_SECONDS).join();

        assertThat(testee.findBrowseStart(OUT_GOING_1).join())
            .contains(NOW);
    }
}