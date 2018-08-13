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

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static org.apache.james.jmap.JmapCommonRequests.getMailboxId;
import static org.apache.james.jmap.JmapCommonRequests.getOutboxId;
import static org.apache.james.jmap.TestingConstants.ARGUMENTS;
import static org.apache.james.jmap.TestingConstants.DOMAIN;
import static org.apache.james.jmap.TestingConstants.NAME;
import static org.apache.james.jmap.TestingConstants.calmlyAwait;
import static org.apache.james.jmap.methods.integration.SetMessagesMethodWithBigMessageTest.Fixture.BIG_MESSAGE_SIZE;
import static org.apache.james.jmap.methods.integration.SetMessagesMethodWithBigMessageTest.Fixture.USERNAME;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;

import java.util.concurrent.TimeUnit;

import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.mailbox.Role;
import org.junit.jupiter.api.Test;

import com.google.common.base.Strings;
import com.jayway.awaitility.Duration;

public interface SetMessagesMethodWithBigMessageTest {

    interface Fixture {
        String USERNAME = "username@" + DOMAIN;
        String PASSWORD = "password";
        int BIG_MESSAGE_SIZE = 20 * 1024 * 1024;
    }

    @Test
    default void setMessagesWithABigBodyShouldReturnCreatedMessageWhenSendingMessage(SetMessagesMethodWithBigMessageTestSystem testSystem) {
        AccessToken accessToken = testSystem.getAccessToken();

        String messageCreationId = "creationId1337";
        String fromAddress = USERNAME;
        String body = Strings.repeat("d", BIG_MESSAGE_SIZE);
        {
            String requestBody = "[" +
                "  [" +
                "    \"setMessages\"," +
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"name\": \"Me\", \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"Me\", \"email\": \"" + fromAddress + "\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"textBody\": \"" + body + "\"," +
                "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

            given()
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".notCreated", aMapWithSize(0))
                .body(ARGUMENTS + ".created", aMapWithSize(1))
                .body(ARGUMENTS + ".created", hasEntry(equalTo(messageCreationId), hasEntry(equalTo("textBody"), equalTo(body))));
        }

        calmlyAwait
            .pollDelay(Duration.FIVE_HUNDRED_MILLISECONDS)
            .atMost(30, TimeUnit.SECONDS).until(() -> hasANewMailWithBody(accessToken, body));
    }

    default boolean hasANewMailWithBody(AccessToken accessToken, String body) {
        try {
            String inboxId = getMailboxId(accessToken, Role.INBOX);
            String receivedMessageId =
                with()
                    .header("Authorization", accessToken.serialize())
                    .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + inboxId + "\"]}}, \"#0\"]]")
                    .post("/jmap")
                .then()
                    .extract()
                    .path(ARGUMENTS + ".messageIds[0]");

            given()
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + receivedMessageId + "\"]}, \"#0\"]]")
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .body(NAME, equalTo("messages"))
                .body(ARGUMENTS + ".list", hasSize(1))
                .body(ARGUMENTS + ".list[0].textBody", equalTo(body));
            return true;

        } catch (AssertionError e) {
            return false;
        }
    }
}
