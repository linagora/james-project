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
package org.apache.james.queue.activemq;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.apache.james.metrics.api.NoopMetricFactory;
import org.apache.james.queue.api.DelayedMailQueueContract;
import org.apache.james.queue.api.DelayedManageableMailQueueContract;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.api.MailQueueContract;
import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.queue.api.ManageableMailQueueContract;
import org.apache.james.queue.api.PriorityMailQueueContract;
import org.apache.james.queue.api.PriorityManageableMailQueueContract;
import org.apache.james.queue.api.RawMailQueueItemDecoratorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import com.google.common.collect.ImmutableList;

public class ActiveMQMailQueueTest implements MailQueueContract, ManageableMailQueueContract, DelayedMailQueueContract,
    DelayedManageableMailQueueContract, PriorityMailQueueContract, PriorityManageableMailQueueContract {

    private final static String QUEUE_NAME = "test";
    public static final boolean USE_BLOB = true;

    private ActiveMQMailQueue mailQueue;

    private BrokerService broker;

    @Override
    public MailQueue getMailQueue() {
        return mailQueue;
    }

    @Override
    public ManageableMailQueue getManageableMailQueue() {
        return mailQueue;
    }

    @AfterEach
    public void tearDown() throws Exception {
        broker.stop();
    }

    @BeforeEach
    public void setUp() throws Exception {
        broker = createBroker();
        broker.start();
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?create=false");
        RawMailQueueItemDecoratorFactory mailQueueItemDecoratorFactory = new RawMailQueueItemDecoratorFactory();
        NoopMetricFactory metricFactory = new NoopMetricFactory();
        mailQueue = new ActiveMQMailQueue(connectionFactory, mailQueueItemDecoratorFactory, QUEUE_NAME, !USE_BLOB, metricFactory);

    }

    protected static BrokerService createBroker() throws Exception {
        BrokerService broker = new BrokerService();
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.addConnector("tcp://127.0.0.1:61616");

        // Enable priority support
        PolicyMap pMap = new PolicyMap();
        PolicyEntry entry = new PolicyEntry();
        entry.setPrioritizedMessages(true);
        entry.setQueue(QUEUE_NAME);
        pMap.setPolicyEntries(ImmutableList.of(entry));
        broker.setDestinationPolicy(pMap);
        // Enable statistics
        broker.setPlugins(new BrokerPlugin[]{new StatisticsBrokerPlugin()});
        broker.setEnableStatistics(true);

        return broker;
    }

    @Override
    @Disabled("JAMES-2295 Disabled as test was dead-locking")
    public void dequeueCouldBeInterleaving() {

    }

    @Override
    @Disabled("JAMES-2296 Not handled by JMS mailqueue. Only single recipient per-recipient removal works")
    public void removeByRecipientShouldRemoveSpecificEmailWhenMultipleRecipients() {

    }

    @Override
    @Disabled("JAMES-2301 Per recipients headers are not attached to the message.")
    public void queueShouldPreservePerRecipientHeaders() {

    }
}
