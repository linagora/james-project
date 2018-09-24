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

import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;
import javax.mail.internet.MimeMessage;

import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.Store;
import org.apache.james.blob.mail.MimeMessagePartsId;
import org.apache.james.blob.mail.MimeMessageStore;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.queue.rabbitmq.view.api.MailQueueView;
import org.apache.mailet.Mail;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;

public class RabbitMQMailQueueFactory implements MailQueueFactory<RabbitMQMailQueue> {
    private final RabbitClient rabbitClient;
    private final RabbitMQManagementApi mqManagementApi;

    private final MetricFactory metricFactory;
    private final MailReferenceSerializer mailReferenceSerializer;
    private final Store<MimeMessage, MimeMessagePartsId> mimeMessageStore;
    private final Function<MailReferenceDTO, Mail> mailLoader;
    private final MailQueueView.Factory mailQueueViewFactory;
    private final Clock clock;

    @Inject
    RabbitMQMailQueueFactory(RabbitClient rabbitClient,
                             RabbitMQManagementApi mqManagementApi,
                             MetricFactory metricFactory,
                             MimeMessageStore.Factory mimeMessageStoreFactory,
                             BlobId.Factory blobIdFactory,
                             MailQueueView.Factory mailQueueViewFactory) {

        this(rabbitClient, mqManagementApi, metricFactory, mimeMessageStoreFactory, blobIdFactory, mailQueueViewFactory,
            Clock.systemUTC());
    }

    @VisibleForTesting
    RabbitMQMailQueueFactory(RabbitClient rabbitClient,
                             RabbitMQManagementApi mqManagementApi,
                             MetricFactory metricFactory,
                             MimeMessageStore.Factory mimeMessageStoreFactory,
                             BlobId.Factory blobIdFactory,
                             MailQueueView.Factory mailQueueViewFactory,
                             Clock clock) {
        this.rabbitClient = rabbitClient;
        this.mqManagementApi = mqManagementApi;
        this.metricFactory = metricFactory;
        this.mailReferenceSerializer = new MailReferenceSerializer();

        this.mailQueueViewFactory = mailQueueViewFactory;
        this.clock = clock;

        this.mimeMessageStore = mimeMessageStoreFactory.mimeMessageStore();
        this.mailLoader = Throwing.function(new MailLoader(mimeMessageStore, blobIdFactory)::load).sneakyThrow();
    }

    @Override
    public Optional<RabbitMQMailQueue> getQueue(String name) {
        return getQueue(MailQueueName.fromString(name));
    }

    @Override
    public RabbitMQMailQueue createQueue(String name) {
        MailQueueName mailQueueName = MailQueueName.fromString(name);
        return getQueue(mailQueueName)
            .orElseGet(() -> attemptQueueCreation(mailQueueName));
    }

    @Override
    public Set<RabbitMQMailQueue> listCreatedMailQueues() {
        return mqManagementApi.listCreatedMailQueueNames()
            .map(this::createMailQueueInstance)
            .collect(Guavate.toImmutableSet());
    }

    private RabbitMQMailQueue attemptQueueCreation(MailQueueName mailQueueName) {
        rabbitClient.attemptQueueCreation(mailQueueName);
        return this.createMailQueueInstance(mailQueueName);
    }

    private Optional<RabbitMQMailQueue> getQueue(MailQueueName name) {
        return mqManagementApi.listCreatedMailQueueNames()
            .filter(name::equals)
            .map(this::createMailQueueInstance)
            .findFirst();
    }

    private RabbitMQMailQueue createMailQueueInstance(MailQueueName mailQueueName) {
        MailQueueView mailQueueView = mailQueueViewFactory.create(mailQueueName);

        return new RabbitMQMailQueue(
            metricFactory,
            mailQueueName,
            new Enqueuer(mailQueueName, rabbitClient, mimeMessageStore, mailReferenceSerializer,
                metricFactory, mailQueueView, clock),
            new Dequeuer(mailQueueName, rabbitClient, mailLoader, mailReferenceSerializer,
                metricFactory, mailQueueView),
            mailQueueView);
    }
}
