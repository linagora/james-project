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

package org.apache.james.queue.rabbitmq;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import javax.mail.internet.MimeMessage;

import org.apache.http.client.utils.URIBuilder;
import org.apache.james.backend.rabbitmq.DockerRabbitMQ;
import org.apache.james.backend.rabbitmq.ReusableDockerRabbitMQExtension;
import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.DockerCassandraExtension;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.configuration.CassandraConfiguration;
import org.apache.james.blob.api.HashBlobId;
import org.apache.james.blob.api.Store;
import org.apache.james.blob.cassandra.CassandraBlobModule;
import org.apache.james.blob.cassandra.CassandraBlobsDAO;
import org.apache.james.blob.mail.MimeMessagePartsId;
import org.apache.james.blob.mail.MimeMessageStore;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.queue.api.ManageableMailQueueContract;
import org.apache.james.queue.rabbitmq.helper.api.MailQueueView;
import org.apache.james.queue.rabbitmq.helper.cassandra.CassandraMailQueueViewConfiguration;
import org.apache.james.queue.rabbitmq.helper.cassandra.CassandraMailQueueViewModule;
import org.apache.james.queue.rabbitmq.helper.cassandra.CassandraMailQueueViewTestFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({ReusableDockerRabbitMQExtension.class, DockerCassandraExtension.class})
public class RabbitMQMailQueueTest implements ManageableMailQueueContract {
    private static final HashBlobId.Factory BLOB_ID_FACTORY = new HashBlobId.Factory();
    public static final int BUCKET_COUNT = 3;
    public static final int UPDATE_FIRST_ENQUEUED_PACE = 100;
    public static final Duration SLICE_WINDOW = Duration.ofHours(1);
    public static final String SPOOL = "spool";

    private static CassandraCluster cassandra;

    private RabbitMQMailQueueFactory mailQueueFactory;

    @BeforeAll
    static void setUpClass(DockerCassandraExtension.DockerCassandra dockerCassandra) {
        cassandra = CassandraCluster.create(
            CassandraModule.aggregateModules(
                CassandraBlobModule.MODULE,
                CassandraMailQueueViewModule.MODULE),
            dockerCassandra.getHost());
    }

    @BeforeEach
    void setup(DockerRabbitMQ rabbitMQ) throws IOException, TimeoutException, URISyntaxException {
        CassandraBlobsDAO blobsDAO = new CassandraBlobsDAO(cassandra.getConf(), CassandraConfiguration.DEFAULT_CONFIGURATION, BLOB_ID_FACTORY);
        Store<MimeMessage, MimeMessagePartsId> mimeMessageStore = MimeMessageStore.factory(blobsDAO).mimeMessageStore();

        MailQueueView mailQueueView = CassandraMailQueueViewTestFactory.factory(cassandra.getConf(), cassandra.getTypesProvider(),
            new CassandraMailQueueViewConfiguration(BUCKET_COUNT, UPDATE_FIRST_ENQUEUED_PACE, SLICE_WINDOW))
            .create(MailQueueName.fromString(SPOOL));

        URI rabbitManagementUri = new URIBuilder()
            .setScheme("http")
            .setHost(rabbitMQ.getHostIp())
            .setPort(rabbitMQ.getAdminPort())
            .build();

        RabbitClient rabbitClient = new RabbitClient(rabbitMQ.connectionFactory().newConnection().createChannel());
        RabbitMQMailQueue.Factory factory = new RabbitMQMailQueue.Factory(rabbitClient, mimeMessageStore, BLOB_ID_FACTORY, mailQueueView);
        RabbitMQManagementApi mqManagementApi = new RabbitMQManagementApi(rabbitManagementUri, new RabbitMQManagementCredentials("guest", "guest".toCharArray()));
        mailQueueFactory = new RabbitMQMailQueueFactory(rabbitClient, mqManagementApi, factory);
    }

    @AfterEach
    void tearDown() {
        cassandra.clearTables();
    }

    @AfterAll
    static void tearDownClass() {
        cassandra.closeCluster();
    }

    @Override
    public MailQueue getMailQueue() {
        return mailQueueFactory.createQueue(SPOOL);
    }

    @Override
    public ManageableMailQueue getManageableMailQueue() {
        return mailQueueFactory.createQueue(SPOOL);
    }

    @Disabled
    @Override
    public void clearShouldNotFailWhenBrowsingIterating() {

    }

    @Disabled
    @Override
    public void browseShouldNotFailWhenConcurrentClearWhenIterating() {

    }

    @Disabled
    @Override
    public void removeShouldNotFailWhenBrowsingIterating() {

    }

    @Disabled
    @Override
    public void browseShouldNotFailWhenConcurrentRemoveWhenIterating() {

    }

    @Disabled
    @Override
    public void removeByNameShouldRemoveSpecificEmail() {

    }

    @Disabled
    @Override
    public void removeBySenderShouldRemoveSpecificEmail() {

    }

    @Disabled
    @Override
    public void removeByRecipientShouldRemoveSpecificEmail() {

    }

    @Disabled
    @Override
    public void removeByRecipientShouldRemoveSpecificEmailWhenMultipleRecipients() {

    }

    @Disabled
    @Override
    public void removeByNameShouldNotFailWhenQueueIsEmpty() {

    }

    @Disabled
    @Override
    public void removeBySenderShouldNotFailWhenQueueIsEmpty() {

    }

    @Disabled
    @Override
    public void removeByRecipientShouldNotFailWhenQueueIsEmpty() {

    }

    @Disabled
    @Override
    public void clearShouldNotFailWhenQueueIsEmpty() {

    }

    @Disabled
    @Override
    public void clearShouldRemoveAllElements() {

    }
}
