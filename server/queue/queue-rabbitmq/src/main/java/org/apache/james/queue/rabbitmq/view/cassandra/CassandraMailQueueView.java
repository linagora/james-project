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

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.james.queue.rabbitmq.view.api.DeleteCondition;
import org.apache.james.queue.rabbitmq.view.api.MailQueueView;
import org.apache.mailet.Mail;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

public class CassandraMailQueueView implements MailQueueView {

    class CassandraMailQueueIterator implements ManageableMailQueue.MailQueueIterator {

        private final Iterator<ManageableMailQueue.MailQueueItemView> iterator;

        public CassandraMailQueueIterator(Iterator<ManageableMailQueue.MailQueueItemView> iterator) {
            Preconditions.checkNotNull(iterator);

            this.iterator = iterator;
        }

        @Override
        public void close() {}

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public ManageableMailQueue.MailQueueItemView next() {
            return iterator.next();
        }
    }

    public static class Factory {
        private final StoreMailHelper storeHelper;
        private final BrowseHelper browseHelper;
        private final DeleteMailHelper deleteMailHelper;

        @Inject
        public Factory(StoreMailHelper storeHelper, BrowseHelper browseHelper, DeleteMailHelper deleteMailHelper) {
            this.storeHelper = storeHelper;
            this.browseHelper = browseHelper;
            this.deleteMailHelper = deleteMailHelper;
        }

        public MailQueueView create(MailQueueName mailQueueName) {
            return new CassandraMailQueueView(storeHelper, mailQueueName, browseHelper, deleteMailHelper);
        }
    }

    private final StoreMailHelper storeHelper;
    private final BrowseHelper browseHelper;
    private final DeleteMailHelper deleteMailHelper;

    private final MailQueueName mailQueueName;

    private CassandraMailQueueView(StoreMailHelper storeHelper,
                                   MailQueueName mailQueueName,
                                   BrowseHelper browseHelper,
                                   DeleteMailHelper deleteMailHelper) {
        this.mailQueueName = mailQueueName;
        this.storeHelper = storeHelper;
        this.browseHelper = browseHelper;
        this.deleteMailHelper = deleteMailHelper;
    }

    @Override
    public CompletableFuture<Void> storeMail(Mail mail) {
        return storeHelper.storeMailInEnqueueTable(mail, mailQueueName);
    }

    @Override
    public CompletableFuture<Void> deleteMail(Mail mail) {
        return deleteMailHelper.markAsDeletedWithPossibleBrowseStartUpdate(mail, mailQueueName);
    }

    @Override
    public ManageableMailQueue.MailQueueIterator browse() {
        return new CassandraMailQueueIterator(
            browseHelper.browse(mailQueueName)
                .join()
                .iterator());
    }

    @Override
    public long getSize() {
        return Iterators.size(browse());
    }

    @Override
    public CompletableFuture<Long> delete(DeleteCondition deleteCondition) {
        CompletableFuture<Long> result = browseHelper.browseReferences(mailQueueName)
            .filter(mailReference -> deleteCondition.shouldBeDeleted(mailReference.getMail()))
            .thenComposeOnAll(mailReference -> deleteMailHelper.markAsDeleted(mailReference.getMail(), mailQueueName))
            .completableFuture()
            .thenApply(Stream::count);

        result.thenRunAsync(() -> deleteMailHelper.updateBrowseStart(mailQueueName));

        return result;
    }

    @Override
    public CompletableFuture<Boolean> isDeleted(Mail mail) {
        return deleteMailHelper.isDeleted(mail, mailQueueName);
    }
}
