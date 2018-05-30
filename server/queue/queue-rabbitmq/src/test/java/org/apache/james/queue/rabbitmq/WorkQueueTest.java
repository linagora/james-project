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

import static org.apache.james.queue.rabbitmq.RabbitMQFixture.AUTO_DELETE;
import static org.apache.james.queue.rabbitmq.RabbitMQFixture.DURABLE;
import static org.apache.james.queue.rabbitmq.RabbitMQFixture.EXCHANGE_NAME;
import static org.apache.james.queue.rabbitmq.RabbitMQFixture.EXCLUSIVE;
import static org.apache.james.queue.rabbitmq.RabbitMQFixture.MESSAGES;
import static org.apache.james.queue.rabbitmq.RabbitMQFixture.MESSAGES_AS_BYTES;
import static org.apache.james.queue.rabbitmq.RabbitMQFixture.NO_PROPERTIES;
import static org.apache.james.queue.rabbitmq.RabbitMQFixture.ROUTING_KEY;
import static org.apache.james.queue.rabbitmq.RabbitMQFixture.WORK_QUEUE;
import static org.apache.james.queue.rabbitmq.RabbitMQFixture.awaitAtMostOneMinute;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import com.github.fge.lambdas.Throwing;
import com.google.common.collect.Iterables;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@ExtendWith(DockerRabbitMQExtension.class)
public class WorkQueueTest {

    private ConnectionFactory connectionFactory1;
    private ConnectionFactory connectionFactory2;
    private ConnectionFactory connectionFactory3;
    private ConnectionFactory connectionFactory4;

    @BeforeEach
    public void setup(DockerRabbitMQ rabbitMQ) {
        connectionFactory1 = rabbitMQ.connectionFactory();
        connectionFactory2 = rabbitMQ.connectionFactory();
        connectionFactory3 = rabbitMQ.connectionFactory();
        connectionFactory4 = rabbitMQ.connectionFactory();
    }

    @Test
    public void rabbitMQShouldSupportTheWorkQueueCase() throws Exception {
        try (Connection connection1 = connectionFactory1.newConnection();
             Channel publisherChannel = connection1.createChannel();
             Connection connection2 = connectionFactory2.newConnection();
             Channel subscriberChannel2 = connection2.createChannel();
             Connection connection3 = connectionFactory3.newConnection();
             Channel subscriberChannel3 = connection3.createChannel();
             Connection connection4 = connectionFactory4.newConnection();
             Channel subscriberChannel4 = connection4.createChannel()) {

            // Declare the exchange and a single queue attached to it.
            publisherChannel.exchangeDeclare(EXCHANGE_NAME, "direct", DURABLE);
            publisherChannel.queueDeclare(WORK_QUEUE, DURABLE, !EXCLUSIVE, AUTO_DELETE, ImmutableMap.of());
            publisherChannel.queueBind(WORK_QUEUE, EXCHANGE_NAME, ROUTING_KEY);
            // Publisher will produce 100 messages
            MESSAGES_AS_BYTES.forEach(Throwing.consumer(
                    bytes -> publisherChannel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, NO_PROPERTIES, bytes)));

            InMemoryConsumer consumer2 = new InMemoryConsumer(subscriberChannel2);
            InMemoryConsumer consumer3 = new InMemoryConsumer(subscriberChannel3);
            InMemoryConsumer consumer4 = new InMemoryConsumer(subscriberChannel4);
            subscriberChannel2.basicConsume(WORK_QUEUE, consumer2);
            subscriberChannel3.basicConsume(WORK_QUEUE, consumer3);
            subscriberChannel4.basicConsume(WORK_QUEUE, consumer4);

            awaitAtMostOneMinute.until(() -> allMessageReceived(MESSAGES, consumer2, consumer3, consumer4));

            assertThat(Iterables.concat(consumer2.getConsumedMessages(), consumer3.getConsumedMessages(), consumer4.getConsumedMessages()))
                .containsOnlyElementsOf(MESSAGES);
        }
    }

    private boolean allMessageReceived(List<Integer> expectedResult, InMemoryConsumer consumer2,
                                       InMemoryConsumer consumer3, InMemoryConsumer consumer4) {
        return Iterables.size(
            Iterables.concat(consumer2.getConsumedMessages(),
                consumer3.getConsumedMessages(),
                consumer4.getConsumedMessages()))
            == expectedResult.size();
    }
}
