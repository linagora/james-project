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

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.ATTRIBUTES;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.BUCKET_ID;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.ENQUEUED_TIME;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.ERROR_MESSAGE;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.HEADER_NAME;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.HEADER_TYPE;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.HEADER_VALUE;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.LAST_UPDATED;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.MAIL_KEY;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.PER_RECIPIENT_SPECIFIC_HEADERS;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.QUEUE_NAME;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.RECIPIENTS;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.REMOTE_ADDR;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.REMOTE_HOST;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.SENDER;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.STATE;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.TABLE_NAME;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueuedMailsTable.TIME_RANGE_START;
import static org.apache.james.queue.rabbitmq.view.cassandra.EnqueuedMailsDAO.EnqueuedMailsDaoUtil.asStringList;
import static org.apache.james.queue.rabbitmq.view.cassandra.EnqueuedMailsDAO.EnqueuedMailsDaoUtil.toHeaderMap;
import static org.apache.james.queue.rabbitmq.view.cassandra.EnqueuedMailsDAO.EnqueuedMailsDaoUtil.toRawAttributeMap;
import static org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices.BucketId;
import static org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices.Slice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.mail.internet.AddressException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.backends.cassandra.init.CassandraTypesProvider;
import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.core.MailAddress;
import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.james.queue.rabbitmq.view.cassandra.model.EnqueuedMail;
import org.apache.james.queue.rabbitmq.view.cassandra.model.MailKey;
import org.apache.james.server.core.MailImpl;
import org.apache.james.util.streams.Iterators;
import org.apache.mailet.Mail;
import org.apache.mailet.PerRecipientHeaders;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;
import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

class EnqueuedMailsDAO {

    static class EnqueuedMailsDaoUtil {

        static EnqueuedMail toEnqueuedMail(Row row) {
            MailQueueName queueName = MailQueueName.fromString(row.getString(QUEUE_NAME));
            Instant timeRangeStart = row.getTimestamp(TIME_RANGE_START).toInstant();
            BucketId bucketId = BucketId.of(row.getInt(BUCKET_ID));
            Instant enqueuedTime = row.getTimestamp(ENQUEUED_TIME).toInstant();

            MailAddress sender = Optional.ofNullable(row.getString(SENDER))
                .map(Throwing.function(MailAddress::new))
                .orElse(null);
            List<MailAddress> recipients = row.getList(RECIPIENTS, String.class)
                .stream()
                .map(Throwing.function(MailAddress::new))
                .collect(Guavate.toImmutableList());
            String state = row.getString(STATE);
            String remoteAddr = row.getString(REMOTE_ADDR);
            String remoteHost = row.getString(REMOTE_HOST);
            String errorMessage = row.getString(ERROR_MESSAGE);
            String name = row.getString(MAIL_KEY);
            Date lastUpdated = row.getTimestamp(LAST_UPDATED);
            Map<String, ByteBuffer> rawAttributes = row.getMap(ATTRIBUTES, String.class, ByteBuffer.class);
            PerRecipientHeaders perRecipientHeaders = fromHeaderMap(row.getMap(PER_RECIPIENT_SPECIFIC_HEADERS, String.class, UDTValue.class));

            MailImpl mail = MailImpl.builder()
                .name(name)
                .sender(sender)
                .recipients(recipients)
                .lastUpdated(lastUpdated)
                .errorMessage(errorMessage)
                .remoteHost(remoteHost)
                .remoteAddr(remoteAddr)
                .state(state)
                .addAllHeadersForRecipients(perRecipientHeaders)
                .attributes(toAttributes(rawAttributes))
                .build();

            return EnqueuedMail.builder()
                .mail(mail)
                .bucketId(bucketId)
                .timeRangeStart(timeRangeStart)
                .enqueuedTime(enqueuedTime)
                .mailKey(MailKey.of(name))
                .mailQueueName(queueName)
                .build();
        }

        private static Map<String, Serializable> toAttributes(Map<String, ByteBuffer> rowAttributes) {
            return rowAttributes.entrySet()
                .stream()
                .map(entry -> Pair.of(entry.getKey(), fromByteBuffer(entry.getValue())))
                .collect(Guavate.toImmutableMap(Pair::getLeft, Pair::getRight));
        }

        private static Serializable fromByteBuffer(ByteBuffer byteBuffer) {
            try {
                byte[] data = new byte[byteBuffer.remaining()];
                byteBuffer.get(data);
                ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data));
                return (Serializable) objectInputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private static PerRecipientHeaders fromHeaderMap(Map<String, UDTValue> rawMap) {
            PerRecipientHeaders result = new PerRecipientHeaders();

            rawMap.forEach((key, value) -> result.addHeaderForRecipient(PerRecipientHeaders.Header.builder()
                    .name(value.getString(HEADER_NAME))
                    .value(value.getString(HEADER_VALUE))
                    .build(),
                toMailAddress(key)));
            return result;
        }

        private static MailAddress toMailAddress(String rawValue) {
            try {
                return new MailAddress(rawValue);
            } catch (AddressException e) {
                throw new RuntimeException(e);
            }
        }

        static ImmutableList<String> asStringList(Collection<MailAddress> mailAddresses) {
            return mailAddresses.stream().map(MailAddress::asString).collect(Guavate.toImmutableList());
        }

        static ImmutableMap<String, ByteBuffer> toRawAttributeMap(Mail mail) {
            return Iterators.toStream(mail.getAttributeNames())
                .map(name -> Pair.of(name, mail.getAttribute(name)))
                .map(pair -> Pair.of(pair.getLeft(), toByteBuffer(pair.getRight())))
                .collect(Guavate.toImmutableMap(Pair::getLeft, Pair::getRight));
        }

        private static ByteBuffer toByteBuffer(Serializable serializable) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                new ObjectOutputStream(outputStream).writeObject(serializable);
                return ByteBuffer.wrap(outputStream.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        static ImmutableMap<String, UDTValue> toHeaderMap(CassandraTypesProvider cassandraTypesProvider,
                                                           PerRecipientHeaders perRecipientHeaders) {
            return perRecipientHeaders.getHeadersByRecipient()
                .asMap()
                .entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().stream().map(value -> Pair.of(entry.getKey(), value)))
                .map(entry -> Pair.of(entry.getKey().asString(),
                    cassandraTypesProvider.getDefinedUserType(HEADER_TYPE)
                        .newValue()
                        .setString(HEADER_NAME, entry.getRight().getName())
                        .setString(HEADER_VALUE, entry.getRight().getValue())))
                .collect(Guavate.toImmutableMap(Pair::getLeft, Pair::getRight));
        }
    }

    private final CassandraAsyncExecutor executor;
    private final PreparedStatement selectFrom;
    private final PreparedStatement insert;
    private final CassandraUtils cassandraUtils;
    private final CassandraTypesProvider cassandraTypesProvider;

    @Inject
    EnqueuedMailsDAO(Session session, CassandraUtils cassandraUtils, CassandraTypesProvider cassandraTypesProvider) {
        this.executor = new CassandraAsyncExecutor(session);
        this.cassandraUtils = cassandraUtils;
        this.cassandraTypesProvider = cassandraTypesProvider;

        this.selectFrom = prepareSelectFrom(session);
        this.insert = prepareInsert(session);
    }

    private PreparedStatement prepareSelectFrom(Session session) {
        return session.prepare(select()
                .from(TABLE_NAME)
                .where(eq(QUEUE_NAME, bindMarker(QUEUE_NAME)))
                .and(eq(TIME_RANGE_START, bindMarker(TIME_RANGE_START)))
                .and(eq(BUCKET_ID, bindMarker(BUCKET_ID))));
    }

    private PreparedStatement prepareInsert(Session session) {
        return session.prepare(insertInto(TABLE_NAME)
            .value(QUEUE_NAME, bindMarker(QUEUE_NAME))
            .value(TIME_RANGE_START, bindMarker(TIME_RANGE_START))
            .value(BUCKET_ID, bindMarker(BUCKET_ID))
            .value(MAIL_KEY, bindMarker(MAIL_KEY))
            .value(ENQUEUED_TIME, bindMarker(ENQUEUED_TIME))
            .value(STATE, bindMarker(STATE))
            .value(SENDER, bindMarker(SENDER))
            .value(RECIPIENTS, bindMarker(RECIPIENTS))
            .value(ATTRIBUTES, bindMarker(ATTRIBUTES))
            .value(ERROR_MESSAGE, bindMarker(ERROR_MESSAGE))
            .value(REMOTE_ADDR, bindMarker(REMOTE_ADDR))
            .value(REMOTE_HOST, bindMarker(REMOTE_HOST))
            .value(LAST_UPDATED, bindMarker(LAST_UPDATED))
            .value(PER_RECIPIENT_SPECIFIC_HEADERS, bindMarker(PER_RECIPIENT_SPECIFIC_HEADERS)));
    }

    CompletableFuture<Void> insert(EnqueuedMail enqueuedMail) {
        Mail mail = enqueuedMail.getMail();

        return executor.executeVoid(insert.bind()
            .setString(QUEUE_NAME, enqueuedMail.getMailQueueName().asString())
            .setTimestamp(TIME_RANGE_START, Date.from(enqueuedMail.getTimeRangeStart()))
            .setInt(BUCKET_ID, enqueuedMail.getBucketId().getValue())
            .setTimestamp(ENQUEUED_TIME, Date.from(enqueuedMail.getEnqueuedTime()))
            .setString(MAIL_KEY, mail.getName())
            .setString(STATE, mail.getState())
            .setString(SENDER, Optional.ofNullable(mail.getSender())
                .map(MailAddress::asString)
                .orElse(null))
            .setList(RECIPIENTS, asStringList(mail.getRecipients()))
            .setString(ERROR_MESSAGE, mail.getErrorMessage())
            .setString(REMOTE_ADDR, mail.getRemoteAddr())
            .setString(REMOTE_HOST, mail.getRemoteHost())
            .setTimestamp(LAST_UPDATED, mail.getLastUpdated())
            .setMap(ATTRIBUTES, toRawAttributeMap(mail))
            .setMap(PER_RECIPIENT_SPECIFIC_HEADERS, toHeaderMap(cassandraTypesProvider, mail.getPerRecipientSpecificHeaders()))
        );
    }

    CompletableFuture<Stream<EnqueuedMail>> selectEnqueuedMails(
        MailQueueName queueName, Slice slice, BucketId bucketId) {

        return executor.execute(
            selectFrom.bind()
                .setString(QUEUE_NAME, queueName.asString())
                .setTimestamp(TIME_RANGE_START, Date.from(slice.getStartSliceInstant()))
                .setInt(BUCKET_ID, bucketId.getValue()))
            .thenApply(resultSet -> cassandraUtils.convertToStream(resultSet)
                .map(EnqueuedMailsDaoUtil::toEnqueuedMail));
    }

}
