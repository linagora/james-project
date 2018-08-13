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

package org.apache.james.jmap.methods.integration;

import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.apache.james.jmap.JmapURIBuilder.baseUri;
import static org.apache.james.jmap.TestingConstants.BOB;
import static org.apache.james.jmap.TestingConstants.BOB_PASSWORD;
import static org.apache.james.jmap.TestingConstants.DOMAIN;
import static org.apache.james.jmap.methods.integration.SetMessagesMethodWithBigMessageTest.Fixture.PASSWORD;
import static org.apache.james.jmap.methods.integration.SetMessagesMethodWithBigMessageTest.Fixture.USERNAME;

import java.nio.charset.StandardCharsets;

import org.apache.james.GuiceJamesServer;
import org.apache.james.jmap.HttpJmapAuthentication;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.mailbox.DefaultMailboxes;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.store.probe.MailboxProbe;
import org.apache.james.modules.MailboxProbeImpl;
import org.apache.james.probe.DataProbe;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.JmapGuiceProbe;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.parsing.Parser;

public class SetMessagesMethodWithBigMessageTestSystem {

    private AccessToken accessToken;
    private GuiceJamesServer jamesServer;
    private MailboxProbe mailboxProbe;
    private DataProbe dataProbe;

    public SetMessagesMethodWithBigMessageTestSystem(GuiceJamesServer jamesServer) throws Exception {
        this.jamesServer = jamesServer;

        init();
    }

    private void init() throws Exception {

        jamesServer.start();

        mailboxProbe = jamesServer.getProbe(MailboxProbeImpl.class);
        dataProbe = jamesServer.getProbe(DataProbeImpl.class);

        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(StandardCharsets.UTF_8)))
                .setPort(jamesServer.getProbe(JmapGuiceProbe.class).getJmapPort())
                .build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.defaultParser = Parser.JSON;
    }

    public void before() throws Exception {
        dataProbe.addDomain(DOMAIN);
        dataProbe.addUser(USERNAME, PASSWORD);
        dataProbe.addUser(BOB, BOB_PASSWORD);
        mailboxProbe.createMailbox("#private", USERNAME, DefaultMailboxes.INBOX);
        accessToken = HttpJmapAuthentication.authenticateJamesUser(baseUri(jamesServer), USERNAME, PASSWORD);

        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USERNAME, DefaultMailboxes.OUTBOX);
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USERNAME, DefaultMailboxes.TRASH);
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USERNAME, DefaultMailboxes.DRAFTS);
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USERNAME, DefaultMailboxes.SENT);
    }

    public void after() {
        jamesServer.stop();
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }
}
