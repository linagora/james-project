/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;

import java.io.IOException;

import org.apache.commons.net.imap.IMAPClient;
import org.apache.james.core.Domain;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.modules.protocols.ImapGuiceProbe;
import org.apache.james.modules.protocols.SmtpGuiceProbe;
import org.apache.james.utils.IMAPMessageReader;
import org.apache.james.utils.SMTPMessageSender;
import org.apache.james.utils.SpoolerProbe;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CassandraLdapJamesServerTest implements JmapJamesServerContract {
    private static final int LIMIT_TO_3_MESSAGES = 3;
    private static final String JAMES_USER = "james-user";
    private static final String PASSWORD = "secret";
    private static final Duration slowPacedPollInterval = ONE_HUNDRED_MILLISECONDS;
    private static final ConditionFactory calmlyAwait = Awaitility.with()
        .pollInterval(slowPacedPollInterval)
        .and()
        .with()
        .pollDelay(slowPacedPollInterval)
        .await();

    private static final LdapExtension ldapExtension = new LdapExtension();
    private static final EmbeddedElasticSearchExtension embbededESExtension = new EmbeddedElasticSearchExtension();
    @RegisterExtension
    static CassandraJmapTestExtension testExtension = CassandraJmapTestExtension.builder()
        .coreModule(CassandraLdapJamesServerMain.cassandraLdapServerModule)
        .defaultOverrideModules(
            new TestJMAPServerModule(LIMIT_TO_3_MESSAGES),
            DOMAIN_LIST_CONFIGURATION_MODULE)
        .extensions(embbededESExtension, ldapExtension)
        .build();

    private IMAPClient imapClient;
    private IMAPMessageReader imapMessageReader;
    private SMTPMessageSender messageSender;

    @Override
    public GuiceJamesServer jamesServer() {
        return testExtension.getJamesServer();
    }

    @BeforeEach
    public void setUpLocally() {
        this.imapClient = new IMAPClient();
        this.imapMessageReader = new IMAPMessageReader();
        this.messageSender = new SMTPMessageSender(Domain.LOCALHOST.asString());
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.messageSender.close();
        this.imapMessageReader.close();
        this.imapClient.disconnect();
    }

    @Test
    void userFromLdapShouldLoginViaImapProtocol() throws Exception {
        imapClient.connect(JAMES_SERVER_HOST, jamesServer().getProbe(ImapGuiceProbe.class).getImapPort());

        assertThat(imapClient.login(JAMES_USER, PASSWORD)).isTrue();
    }

    @Test
    void mailsShouldBeWellReceivedBeforeFirstUserConnectionWithLdap() throws Exception {
        messageSender.connect(JAMES_SERVER_HOST, jamesServer().getProbe(SmtpGuiceProbe.class).getSmtpPort())
            .sendMessage("bob@any.com", JAMES_USER + "@localhost");

        calmlyAwait.until(() -> jamesServer().getProbe(SpoolerProbe.class).processingFinished());

        imapMessageReader.connect(JAMES_SERVER_HOST, jamesServer().getProbe(ImapGuiceProbe.class).getImapPort())
            .login(JAMES_USER, PASSWORD)
            .select("INBOX")
            .awaitMessage(calmlyAwait);
    }
}
