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

import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMailboxSessionMapperFactory;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraUidProvider;
import org.apache.james.mailbox.elasticsearch.events.ElasticSearchListeningMessageSearchIndex;
import org.apache.james.mailbox.store.mail.MailboxMapperFactory;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.search.MessageSearchIndex;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class CassandraTypesModule extends AbstractModule {

	@Override
	protected void configure() {
        bind(new TypeLiteral<MessageSearchIndex<CassandraId>>(){}).to(new TypeLiteral<ElasticSearchListeningMessageSearchIndex<CassandraId>>(){});
        bind(new TypeLiteral<MessageMapperFactory<CassandraId>>(){}).to(CassandraMailboxSessionMapperFactory.class);
        bind(new TypeLiteral<MailboxMapperFactory<CassandraId>>(){}).to(CassandraMailboxSessionMapperFactory.class);
        bind(new TypeLiteral<ModSeqProvider<CassandraId>>(){}).to(new TypeLiteral<CassandraModSeqProvider>(){});
        bind(new TypeLiteral<UidProvider<CassandraId>>(){}).to(new TypeLiteral<CassandraUidProvider>(){});
	}

}
