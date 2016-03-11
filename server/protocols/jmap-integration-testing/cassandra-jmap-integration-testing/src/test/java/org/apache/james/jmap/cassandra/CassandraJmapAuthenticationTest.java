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
package org.apache.james.jmap.cassandra;

import org.apache.james.backends.cassandra.EmbeddedCassandra;
import org.apache.james.jmap.JMAPAuthenticationTest;
import org.apache.james.jmap.JmapServer;
import org.apache.james.jmap.servers.CassandraJmapServer;
import org.apache.james.jmap.utils.ZonedDateTimeProvider;
import org.apache.james.mailbox.elasticsearch.EmbeddedElasticSearch;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import com.google.inject.util.Modules;

public class CassandraJmapAuthenticationTest extends JMAPAuthenticationTest {

    private TemporaryFolder temporaryFolder = new TemporaryFolder();
    private EmbeddedElasticSearch embeddedElasticSearch = new EmbeddedElasticSearch();
    private EmbeddedCassandra cassandra = EmbeddedCassandra.createStartServer();
    private JmapServer jmapServer = new CassandraJmapServer(
        Modules.combine(
            CassandraJmapServer.defaultOverrideModule(temporaryFolder, embeddedElasticSearch, cassandra),
            (binder) -> binder.bind(ZonedDateTimeProvider.class).toInstance(zonedDateTimeProvider)));

    @Rule
    public RuleChain chain = RuleChain
        .outerRule(temporaryFolder)
        .around(embeddedElasticSearch)
        .around(jmapServer);

    @Override
    protected JmapServer getJmapServer() {
        return jmapServer;
    }
    
}
