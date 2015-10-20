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
package org.apache.james.modules.mailbox;

import java.util.List;

import org.apache.james.adapter.mailbox.store.UserRepositoryAuthenticator;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPathLocker;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMailboxManager;
import org.apache.james.mailbox.cassandra.CassandraMailboxSessionMapperFactory;
import org.apache.james.mailbox.cassandra.CassandraSubscriptionManager;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraUidProvider;
import org.apache.james.mailbox.elasticsearch.events.ElasticSearchListeningMessageSearchIndex;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.quota.QuotaManager;
import org.apache.james.mailbox.quota.QuotaRootResolver;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.mailbox.store.NoMailboxPathLocker;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.quota.NoQuotaManager;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.apache.james.utils.ConfigurationPerformer;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class CassandraMailboxModule extends AbstractModule {

    public static final String MAILBOXMANAGER_NAME = "mailboxmanager";

    @Override
    protected void configure() {
        bind(MailboxManager.class).annotatedWith(Names.named(MAILBOXMANAGER_NAME)).to(CassandraMailboxManager.class);

        bind(new TypeLiteral<MessageSearchIndex<CassandraId>>(){}).to(new TypeLiteral<ElasticSearchListeningMessageSearchIndex<CassandraId>>(){});

        bind(SubscriptionManager.class).to(CassandraSubscriptionManager.class);
        bind(new TypeLiteral<MessageMapperFactory<CassandraId>>(){}).to(new TypeLiteral<CassandraMailboxSessionMapperFactory>(){});

        bind(MailboxPathLocker.class).to(NoMailboxPathLocker.class);
        bind(Authenticator.class).to(UserRepositoryAuthenticator.class);

        bind(new TypeLiteral<ModSeqProvider<CassandraId>>(){}).to(new TypeLiteral<CassandraModSeqProvider>(){});
        bind(new TypeLiteral<UidProvider<CassandraId>>(){}).to(new TypeLiteral<CassandraUidProvider>(){});
        Multibinder.newSetBinder(binder(), ConfigurationPerformer.class).addBinding().to(MailboxConfigurationPerformer.class);
        
        bind(QuotaManager.class).to(NoQuotaManager.class);
        bind(QuotaRootResolver.class).toInstance(new NoQuotaRootResolver());
    }

    private static class NoQuotaRootResolver implements QuotaRootResolver {

        @Override
        public QuotaRoot createQuotaRoot(String quotaRootString) {
            return null;
        }

        @Override
        public QuotaRoot getQuotaRoot(MailboxPath mailboxPath) throws MailboxException {
            return null;
        }

        @Override
        public List<MailboxPath> retrieveAssociatedMailboxes(QuotaRoot quotaRoot, MailboxSession mailboxSession) throws MailboxException {
            return null;
        }
    }

    @Singleton
    public static class MailboxConfigurationPerformer implements ConfigurationPerformer {

        private final CassandraMailboxManager storeMailboxManager;

        @Inject
        public MailboxConfigurationPerformer(CassandraMailboxManager storeMailboxManager) {
            this.storeMailboxManager = storeMailboxManager;
        }

        public void initModule() throws Exception {
            storeMailboxManager.init();
        }
    }
}