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

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices.BucketId;
import org.apache.james.queue.rabbitmq.view.cassandra.model.EnqueuedMail;
import org.apache.james.queue.rabbitmq.view.cassandra.model.MailKey;
import org.apache.mailet.Mail;

class StoreMailHelper {

    private final EnqueuedMailsDAO enqueuedMailsDao;
    private final BrowseStartDAO browseStartDao;
    private final CassandraMailQueueViewConfiguration configuration;

    @Inject
    StoreMailHelper(EnqueuedMailsDAO enqueuedMailsDao,
                    BrowseStartDAO browseStartDao,
                    CassandraMailQueueViewConfiguration configuration) {
        this.enqueuedMailsDao = enqueuedMailsDao;
        this.browseStartDao = browseStartDao;
        this.configuration = configuration;
    }

    CompletableFuture<Void> storeMailInEnqueueTable(Mail mail, MailQueueName mailQueueName) {
        EnqueuedMail enqueuedMail = convertToEnqueuedMail(mail, mailQueueName);

        return enqueuedMailsDao.insert(enqueuedMail)
            .thenCompose(any -> browseStartDao.insertInitialBrowseStart(mailQueueName, enqueuedMail.getTimeRangeStart()));
    }

    private EnqueuedMail convertToEnqueuedMail(Mail mail, MailQueueName mailQueueName) {
        return EnqueuedMail.builder()
            .mail(mail)
            .bucketId(computedBucketId(mail))
            .timeRangeStart(currentSliceStartInstant())
            .enqueuedTime(Instant.now())
            .mailKey(MailKey.fromMail(mail))
            .mailQueueName(mailQueueName)
            .build();
    }

    private Instant currentSliceStartInstant() {
        long sliceSide = configuration.getSliceWindowInSecond();
        long sliceId = Instant.now().getEpochSecond() / sliceSide;
        return Instant.ofEpochSecond(sliceId * sliceSide);
    }

    private BucketId computedBucketId(Mail mail) {
        int mailKeyHasCode = mail.getName().hashCode();
        int bucketIdValue = mailKeyHasCode % configuration.getBucketCount();
        return BucketId.of(bucketIdValue);
    }
}
