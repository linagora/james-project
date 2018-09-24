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

package org.apache.james.modules.data;

import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.mailrepository.api.MailRepositoryUrlStore;
import org.apache.james.mailrepository.cassandra.CassandraMailRepositoryUrlStore;
import org.apache.james.queue.rabbitmq.view.cassandra.BrowseStartDAO;
import org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueBrowser;
import org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueMailDelete;
import org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueMailStore;
import org.apache.james.queue.rabbitmq.view.cassandra.DeletedMailsDAO;
import org.apache.james.queue.rabbitmq.view.cassandra.EnqueuedMailsDAO;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;

public class CassandraMailQueueViewModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(EnqueuedMailsDAO.class).in(Scopes.SINGLETON);
        bind(DeletedMailsDAO.class).in(Scopes.SINGLETON);
        bind(BrowseStartDAO.class).in(Scopes.SINGLETON);

        bind(CassandraMailQueueBrowser.class).in(Scopes.SINGLETON);
        bind(CassandraMailQueueMailDelete.class).in(Scopes.SINGLETON);
        bind(CassandraMailQueueMailStore.class).in(Scopes.SINGLETON);

        bind(MailRepositoryUrlStore.class).to(CassandraMailRepositoryUrlStore.class);

        Multibinder<CassandraModule> cassandraModuleBinder = Multibinder.newSetBinder(binder(), CassandraModule.class);
        cassandraModuleBinder.addBinding().toInstance(org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.MODULE);
    }
}
