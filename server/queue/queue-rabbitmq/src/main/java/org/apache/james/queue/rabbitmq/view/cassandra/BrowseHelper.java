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

import static org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices.BucketId;
import static org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices.Slice;
import static org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices.Slice.allSlicesTill;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.james.queue.rabbitmq.view.cassandra.model.EnqueuedMail;
import org.apache.james.util.CompletableFutureUtil;
import org.apache.james.util.FluentFutureStream;
import org.apache.james.util.StreamUtils;

class BrowseHelper {

    private final BrowseStartDAO browseStartDao;
    private final DeletedMailsDAO deletedMailsDao;
    private final EnqueuedMailsDAO enqueuedMailsDao;
    private final CassandraMailQueueViewConfiguration configuration;
    private final Clock clock;

    @Inject
    BrowseHelper(BrowseStartDAO browseStartDao, DeletedMailsDAO deletedMailsDao,
                 EnqueuedMailsDAO enqueuedMailsDao, CassandraMailQueueViewConfiguration configuration) {
        this.browseStartDao = browseStartDao;
        this.deletedMailsDao = deletedMailsDao;
        this.enqueuedMailsDao = enqueuedMailsDao;
        this.configuration = configuration;
        clock = Clock.systemUTC();
    }

    CompletableFuture<Stream<ManageableMailQueue.MailQueueItemView>> browse(MailQueueName queueName) {

        return browseReferences(queueName)
            .map(EnqueuedMail::getMail)
            .map(ManageableMailQueue.MailQueueItemView::new)
            .completableFuture();
    }

    FluentFutureStream<EnqueuedMail> browseReferences(MailQueueName queueName) {
        return FluentFutureStream.of(browseStartDao.findBrowseStart(queueName)
            .thenApply(maybeStart -> maybeStart.map(this::allSlicesFrom).orElse(Stream.empty())))
            .thenFlatCompose(currentSlice -> browseOnlyEnqueuedInOrder(queueName, currentSlice));
    }

    private CompletableFuture<Stream<EnqueuedMail>> browseOnlyEnqueuedInOrder(MailQueueName queueName, Slice currentSlice) {

         return CompletableFutureUtil.allOf(allBucketIds()
            .map(bucketId -> browseOnlyEnqueuedInOrder(queueName, currentSlice, bucketId)))
            .thenApply(StreamUtils::flatten)
            .thenApply(enqueuedMailStream -> enqueuedMailStream.sorted(EnqueuedMail.getEnqueuedTimeComparator()));
    }

    private CompletableFuture<Stream<EnqueuedMail>> browseOnlyEnqueuedInOrder(
        MailQueueName queueName, Slice currentSlice, BucketId bucketId) {

        return FluentFutureStream.of(
            enqueuedMailsDao
                .selectEnqueuedMails(queueName, currentSlice, bucketId))
                .thenFlatComposeOnOptional(mailReference -> filterDeleted(queueName, mailReference))
                .completableFuture();
    }

    private Stream<Slice> allSlicesFrom(Instant startingSliceInstant) {
        Slice firstSlice = Slice.of(startingSliceInstant, configuration.getSliceWindow().getSeconds());
        return allSlicesTill(firstSlice, clock.instant());
    }

    private Stream<BucketId> allBucketIds() {
        return IntStream
            .range(0, configuration.getBucketCount())
            .mapToObj(BucketId::of);
    }

    private CompletableFuture<Optional<EnqueuedMail>> filterDeleted(MailQueueName mailQueueName, EnqueuedMail mailReference) {
        return deletedMailsDao.checkDeleted(mailQueueName, mailReference.getMailKey())
            .thenApply(wasDeleted ->
                Optional.of(mailReference)
                    .filter(any -> !wasDeleted));
    }
}
