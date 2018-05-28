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

import static org.apache.james.queue.rabbitmq.RabbitMQFixture.DIRECT;
import static org.apache.james.queue.rabbitmq.RabbitMQFixture.DURABLE;
import static org.apache.james.queue.rabbitmq.RabbitMQFixture.EXCHANGE_NAME;
import static org.apache.james.queue.rabbitmq.RabbitMQFixture.NO_PROPERTIES;
import static org.apache.james.queue.rabbitmq.RabbitMQFixture.awaitAtMostOneMinute;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.collect.Iterables;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@ExtendWith(DockerRabbitMQExtension.class)
public class RoutingTest {
    private static final String CONVERSATION_1 = "c1";
    private static final String CONVERSATION_2 = "c2";
    private static final String CONVERSATION_3 = "c3";
    private static final String CONVERSATION_4 = "c4";

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
    public void rabbitMQShouldSupportRouting() throws Exception {
        try (Connection connection1 = connectionFactory1.newConnection();
             Channel channel1 = connection1.createChannel();
            Connection connection2 = connectionFactory2.newConnection();
             Channel channel2 = connection2.createChannel();
            Connection connection3 = connectionFactory3.newConnection();
             Channel channel3 = connection3.createChannel();
            Connection connection4 = connectionFactory4.newConnection();
             Channel channel4 = connection4.createChannel()) {

            // Declare the exchange and a single queue attached to it.
            channel1.exchangeDeclare(EXCHANGE_NAME, DIRECT, DURABLE);

            String queue1 = channel1.queueDeclare().getQueue();
            // 1 will follow discussion 1 and 2
            channel1.queueBind(queue1, EXCHANGE_NAME, CONVERSATION_1);
            channel1.queueBind(queue1, EXCHANGE_NAME, CONVERSATION_2);

            String queue2 = channel2.queueDeclare().getQueue();
            // 2 will follow discussion 3 and 2
            channel2.queueBind(queue2, EXCHANGE_NAME, CONVERSATION_3);
            channel2.queueBind(queue2, EXCHANGE_NAME, CONVERSATION_2);

            String queue3 = channel3.queueDeclare().getQueue();
            // 3 will follow discussion 3 and 4
            channel3.queueBind(queue3, EXCHANGE_NAME, CONVERSATION_3);
            channel3.queueBind(queue3, EXCHANGE_NAME, CONVERSATION_4);

            String queue4 = channel4.queueDeclare().getQueue();
            // 4 will follow discussion 1 and 4
            channel4.queueBind(queue4, EXCHANGE_NAME, CONVERSATION_1);
            channel4.queueBind(queue4, EXCHANGE_NAME, CONVERSATION_4);

            // Publish one message on each discussion
            channel1.basicPublish(EXCHANGE_NAME, CONVERSATION_1, NO_PROPERTIES,
                "1".getBytes(StandardCharsets.UTF_8));
            channel2.basicPublish(EXCHANGE_NAME, CONVERSATION_2, NO_PROPERTIES,
                "2".getBytes(StandardCharsets.UTF_8));
            channel3.basicPublish(EXCHANGE_NAME, CONVERSATION_3, NO_PROPERTIES,
                "3".getBytes(StandardCharsets.UTF_8));
            channel4.basicPublish(EXCHANGE_NAME, CONVERSATION_4, NO_PROPERTIES,
                "4".getBytes(StandardCharsets.UTF_8));

            InMemoryConsumer consumer1 = new InMemoryConsumer(channel1);
            InMemoryConsumer consumer2 = new InMemoryConsumer(channel2);
            InMemoryConsumer consumer3 = new InMemoryConsumer(channel3);
            InMemoryConsumer consumer4 = new InMemoryConsumer(channel4);
            channel1.basicConsume(queue1, consumer1);
            channel2.basicConsume(queue2, consumer2);
            channel3.basicConsume(queue3, consumer3);
            channel4.basicConsume(queue4, consumer4);

            awaitAtMostOneMinute.until(() -> allMessageReceived(consumer1, consumer2, consumer3, consumer4));

            assertThat(consumer1.getConsumedMessages()).containsOnly(1, 2);
            assertThat(consumer2.getConsumedMessages()).containsOnly(2, 3);
            assertThat(consumer3.getConsumedMessages()).containsOnly(3, 4);
            assertThat(consumer4.getConsumedMessages()).containsOnly(1, 4);
        }
    }

    private boolean allMessageReceived(InMemoryConsumer consumer1, InMemoryConsumer consumer2,
                                       InMemoryConsumer consumer3, InMemoryConsumer consumer4) {
        return Iterables.size(
            Iterables.concat(
                consumer1.getConsumedMessages(),
                consumer2.getConsumedMessages(),
                consumer3.getConsumedMessages(),
                consumer4.getConsumedMessages()))
            == 8;
    }
}
