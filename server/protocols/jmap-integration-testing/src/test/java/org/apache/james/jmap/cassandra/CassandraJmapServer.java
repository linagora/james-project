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

import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.jayway.restassured.RestAssured.with;
import static org.apache.james.user.cassandra.tables.CassandraUserTable.ALGORITHM;
import static org.apache.james.user.cassandra.tables.CassandraUserTable.NAME;
import static org.apache.james.user.cassandra.tables.CassandraUserTable.PASSWORD;
import static org.apache.james.user.cassandra.tables.CassandraUserTable.REALNAME;
import static org.apache.james.user.cassandra.tables.CassandraUserTable.TABLE_NAME;

import java.io.InputStream;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.Flags;

import org.apache.james.CassandraJamesServer;
import org.apache.james.CassandraJamesServerMain;
import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.jmap.JmapServer;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.elasticsearch.EmbeddedElasticSearch;
import org.apache.james.mailbox.exception.BadCredentialsException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.modules.TestElasticSearchModule;
import org.apache.james.modules.TestFilesystemModule;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.user.lib.model.DefaultUser;
import org.apache.james.utils.ConfigurationPerformer;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;
import com.jayway.restassured.http.ContentType;

public class CassandraJmapServer implements JmapServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraJmapServer.class);

    private final TemporaryFolder temporaryFolder;
    private final EmbeddedElasticSearch embeddedElasticSearch;

    private CassandraJamesServer server;
    private CassandraCluster cassandra;
    private static MailboxManager mailboxManager;

    public CassandraJmapServer(TemporaryFolder temporaryFolder, EmbeddedElasticSearch embeddedElasticSearch) {
        this.temporaryFolder = temporaryFolder;
        this.embeddedElasticSearch = embeddedElasticSearch;
    }


    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            
            @Override
            public void evaluate() throws Throwable {
                try {
                    before();
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }

    private void before() throws Throwable {
        server = new CassandraJamesServer(Modules.override(CassandraJamesServerMain.defaultModule)
                .with(new TestElasticSearchModule(embeddedElasticSearch),
                        new TestFilesystemModule(temporaryFolder.newFolder()),
                        new TestJMAPServerModule(),
                        new AbstractModule() {

                    @Override
                    protected void configure() {
                        Multibinder.newSetBinder(binder(), ConfigurationPerformer.class).addBinding().to(InjectedClassProxy.class);
                    }

                    @Provides
                    @Singleton
                    com.datastax.driver.core.Session provideSession(CassandraModule cassandraModule) {
                        cassandra = CassandraCluster.create(cassandraModule);
                        return cassandra.getConf();
                    }
                }));

        server.start();
    }

    private static class InjectedClassProxy implements ConfigurationPerformer {

        private MailboxManager injectedMailboxManager;

        @Inject
        public InjectedClassProxy(MailboxManager mailboxManager) {
            this.injectedMailboxManager = mailboxManager;
        }

        @Override
        public void initModule() throws Exception {
            mailboxManager = injectedMailboxManager;
        }
    }

    private void after() {
        try {
            server.stop();
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public void clean() {
        cassandra.clearAllTables();
    }

    @Override
    public int getPort() {
        return 1080;
    }

    @Override
    public void createJamesUser(String username, String password) {
        DefaultUser user = new DefaultUser("username@domain.tld", "SHA1");
        user.setPassword("password");
        cassandra.getConf().execute(
                insertInto(TABLE_NAME)
                    .value(NAME, user.getUserName().toLowerCase())
                    .value(REALNAME, user.getUserName())
                    .value(PASSWORD, user.getHashedPassword())
                    .value(ALGORITHM, user.getHashAlgorithm())
                    .ifNotExists());
    }

    @Override
    public AccessToken authenticateJamesUser(String username, String password) {
        String continuationToken = getContinuationToken(username);

        return AccessToken.fromString(
            with()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"password\"}")
            .post("/authentication")
                .body()
                .jsonPath()
                .getString("accessToken")
        );
    }

    private String getContinuationToken(String username) {
        return with()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"username\": \"" + username + "\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}")
        .post("/authentication")
            .body()
            .path("continuationToken")
            .toString();
    }

    @Override
    public void createMailbox(String username, MailboxPath mailboxPath) throws BadCredentialsException, MailboxException {
        MailboxSession mailboxSession = mailboxManager.createSystemSession(username, LOGGER);
        mailboxManager.startProcessingRequest(mailboxSession);
        mailboxManager.createMailbox(mailboxPath, mailboxSession);
        mailboxManager.logout(mailboxSession, true);
        mailboxManager.endProcessingRequest(mailboxSession);
    }

    @Override
    public void appendMessage(String username, MailboxPath mailboxPath,
            InputStream message, Date internalDate, boolean isRecent, Flags flags) 
                    throws BadCredentialsException, MailboxException {

        MailboxSession mailboxSession = mailboxManager.createSystemSession(username, LOGGER);
        MessageManager messageManager = mailboxManager.getMailbox(mailboxPath, mailboxSession);
        messageManager.appendMessage(message, internalDate, mailboxSession, isRecent, flags);
    }
}
