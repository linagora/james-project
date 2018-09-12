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

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.DockerCassandraRule;
import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.james.queue.rabbitmq.view.cassandra.model.MailKey;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class DeletedMailsDAOTest {

    private static final MailQueueName OUT_GOING_1 = MailQueueName.fromString("OUT_GOING_1");
    private static final MailQueueName OUT_GOING_2 = MailQueueName.fromString("OUT_GOING_2");
    private static final MailKey MAIL_KEY_1 = MailKey.of("mailkey1");
    private static final MailKey MAIL_KEY_2 = MailKey.of("mailkey2");

    @ClassRule
    public static DockerCassandraRule cassandraServer = new DockerCassandraRule();
    private static CassandraCluster cassandra;

    private DeletedMailsDAO testee;

    @BeforeClass
    public static void setUpClass() {
        cassandra = CassandraCluster.create(CassandraMailQueueViewModule.MODULE, cassandraServer.getHost());
    }

    @Before
    public void setUp() {
        testee = new DeletedMailsDAO(cassandra.getConf());
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
    public void markAsDeletedShouldWork() {
        Boolean isDeletedBeforeMark = testee
                .checkDeleted(OUT_GOING_1, MAIL_KEY_1)
                .join();
        assertThat(isDeletedBeforeMark).isFalse();

        testee.markAsDeleted(OUT_GOING_1, MAIL_KEY_1).join();

        Boolean isDeletedAfterMark = testee
            .checkDeleted(OUT_GOING_1, MAIL_KEY_1)
            .join();

        assertThat(isDeletedAfterMark).isTrue();
    }

    @Test
    public void checkDeletedShouldReturnFalseWhenTableDoesntContainBothMailQueueAndMailKey() {
        testee.markAsDeleted(OUT_GOING_2, MAIL_KEY_2).join();

        Boolean isDeleted = testee
            .checkDeleted(OUT_GOING_1, MAIL_KEY_1)
            .join();

        assertThat(isDeleted).isFalse();
    }

    @Test
    public void checkDeletedShouldReturnFalseWhenTableContainsMailQueueButNotMailKey() {
        testee.markAsDeleted(OUT_GOING_1, MAIL_KEY_2).join();

        Boolean isDeleted = testee
            .checkDeleted(OUT_GOING_1, MAIL_KEY_1)
            .join();

        assertThat(isDeleted).isFalse();
    }

    @Test
    public void checkDeletedShouldReturnFalseWhenTableContainsMailKeyButNotMailQueue() {
        testee.markAsDeleted(OUT_GOING_2, MAIL_KEY_1).join();

        Boolean isDeleted = testee
            .checkDeleted(OUT_GOING_1, MAIL_KEY_1)
            .join();

        assertThat(isDeleted).isFalse();
    }

    @Test
    public void checkDeletedShouldReturnTrueWhenTableContainsMailItem() {
        testee.markAsDeleted(OUT_GOING_1, MAIL_KEY_1).join();

        Boolean isDeleted = testee
            .checkDeleted(OUT_GOING_1, MAIL_KEY_1)
            .join();

        assertThat(isDeleted).isTrue();
    }
}