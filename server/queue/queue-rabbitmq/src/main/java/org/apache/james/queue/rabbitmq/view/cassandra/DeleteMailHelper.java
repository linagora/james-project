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
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.james.queue.rabbitmq.view.cassandra.model.EnqueuedMail;
import org.apache.james.queue.rabbitmq.view.cassandra.model.MailKey;
import org.apache.mailet.Mail;

class DeleteMailHelper {

    private final DeletedMailsDAO deletedMailsDao;
    private final BrowseStartDAO browseStartDao;
    private final BrowseHelper browseHelper;
    private final CassandraMailQueueViewConfiguration configuration;
    private final Random random;

    @Inject
    DeleteMailHelper(DeletedMailsDAO deletedMailsDao,
                     BrowseStartDAO browseStartDao,
                     BrowseHelper browseHelper, CassandraMailQueueViewConfiguration configuration) {
        this.deletedMailsDao = deletedMailsDao;
        this.browseStartDao = browseStartDao;
        this.browseHelper = browseHelper;

        this.configuration = configuration;
        this.random = new Random();
    }

    CompletableFuture<Void> updateDeleteTable(Mail mail, MailQueueName mailQueueName) {
        return deletedMailsDao
            .markAsDeleted(mailQueueName, MailKey.fromMail(mail))
            .thenRunAsync(() -> updateBrowseStart(mailQueueName));
    }

    private void updateBrowseStart(MailQueueName mailQueueName) {
        if (shouldBrowseStart()) {
            findNewBrowseStart(mailQueueName)
                .thenCompose(newBrowseStart -> setNewBrowseStart(mailQueueName, newBrowseStart))
                .join();
        }
    }

    private CompletableFuture<Optional<Instant>> findNewBrowseStart(MailQueueName mailQueueName) {
        return browseHelper.browseReferences(mailQueueName)
            .map(EnqueuedMail::getTimeRangeStart)
            .completableFuture()
            .thenApply(Stream::findFirst);
    }

    private CompletableFuture<Void> setNewBrowseStart(MailQueueName mailQueueName, Optional<Instant> newBrowseStart) {
        return newBrowseStart.map(value ->
            browseStartDao.updateBrowseStart(mailQueueName, value))
            .orElse(CompletableFuture.completedFuture(null));
    }

    private boolean shouldBrowseStart() {
        int threshold = configuration.getUpdateFirstEnqueuedPace();
        return Math.abs(random.nextInt()) % threshold == 0;
    }
}
