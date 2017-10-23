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

package org.apache.james.jmap.methods.integration.cucumber;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.modules.MailboxProbeImpl;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.java.guice.ScenarioScoped;

@ScenarioScoped
public class SetMessagesMethodStepdefs {

    private final MainStepdefs mainStepdefs;
    private final UserStepdefs userStepdefs;
    private final GetMessagesMethodStepdefs getMessagesMethodStepdefs;

    private HttpResponse response;
    private DocumentContext jsonPath;

    @Inject
    private SetMessagesMethodStepdefs(MainStepdefs mainStepdefs, UserStepdefs userStepdefs, GetMessagesMethodStepdefs getMessagesMethodStepdefs) {
        this.mainStepdefs = mainStepdefs;
        this.userStepdefs = userStepdefs;
        this.getMessagesMethodStepdefs = getMessagesMethodStepdefs;
    }

    @When("^\"([^\"]*)\" move \"([^\"]*)\" to user mailbox \"([^\"]*)\"")
    public void moveMessageToMailboxWithUser(String username, String message, String mailbox) throws Throwable {
        userStepdefs.execWithUser(username, () -> moveMessageToMailbox(message, mailbox));
    }

    @When("^the user move \"([^\"]*)\" to user mailbox \"([^\"]*)\"")
    public void moveMessageToMailbox(String message, String mailbox) throws Throwable {
        MessageId messageId = getMessagesMethodStepdefs.getMessageId(message);
        MailboxId mailboxId = mainStepdefs.jmapServer
            .getProbe(MailboxProbeImpl.class)
            .getMailbox(MailboxConstants.USER_NAMESPACE, userStepdefs.getConnectedUser(), mailbox)
            .getMailboxId();

        post("[" +
            "  [" +
            "    \"setMessages\","+
            "    {" +
            "      \"update\": { \"" + messageId.serialize() + "\" : {" +
            "        \"mailboxIds\": [\"" + mailboxId.serialize() + "\"]" +
            "      }}" +
            "    }," +
            "    \"#0\"" +
            "  ]" +
            "]");
        mainStepdefs.awaitMethod.run();
    }

    @When("^\"([^\"]*)\" copy \"([^\"]*)\" from mailbox \"([^\"]*)\" to mailbox \"([^\"]*)\"")
    public void copyMessageToMailbox(String username, String message, String sourceMailbox, String destinationMailbox) throws Throwable {
        userStepdefs.execWithUser(username, () -> copyMessageToMailbox(message, sourceMailbox, destinationMailbox));
    }

    @When("^the user copy \"([^\"]*)\" from mailbox \"([^\"]*)\" to mailbox \"([^\"]*)\"")
    public void copyMessageToMailbox(String message, String sourceMailbox, String destinationMailbox) throws Throwable {
        MessageId messageId = getMessagesMethodStepdefs.getMessageId(message);
        MailboxId sourceMailboxId = mainStepdefs.jmapServer
            .getProbe(MailboxProbeImpl.class)
            .getMailbox(MailboxConstants.USER_NAMESPACE, userStepdefs.getConnectedUser(), sourceMailbox)
            .getMailboxId();
        MailboxId destinationMailboxId = mainStepdefs.jmapServer
            .getProbe(MailboxProbeImpl.class)
            .getMailbox(MailboxConstants.USER_NAMESPACE, userStepdefs.getConnectedUser(), destinationMailbox)
            .getMailboxId();

        post("[" +
            "  [" +
            "    \"setMessages\","+
            "    {" +
            "      \"update\": { \"" + messageId.serialize() + "\" : {" +
            "        \"mailboxIds\": [\"" + destinationMailboxId.serialize() + "\",\"" + sourceMailboxId.serialize() + "\"]" +
            "      }}" +
            "    }," +
            "    \"#0\"" +
            "  ]" +
            "]");
        mainStepdefs.awaitMethod.run();
    }

    @When("^\"([^\"]*)\" copy \"([^\"]*)\" from mailbox \"([^\"]*)\" of user \"([^\"]*)\" to mailbox \"([^\"]*)\" of user \"([^\"]*)\"")
    public void copyMessageToMailbox(String username, String message, String sourceMailbox, String sourceUser, String destinationMailbox, String destinationUser) throws Throwable {
        userStepdefs.execWithUser(username, () -> copyMessageToMailbox(message, sourceMailbox, sourceUser, destinationMailbox, destinationUser));
    }

    @When("^the user copy \"([^\"]*)\" from mailbox \"([^\"]*)\" of user \"([^\"]*)\" to mailbox \"([^\"]*)\" of user \"([^\"]*)\"")
    public void copyMessageToMailbox(String message, String sourceMailbox, String sourceUser, String destinationMailbox, String destinationUser) throws Throwable {
        MessageId messageId = getMessagesMethodStepdefs.getMessageId(message);
        MailboxId sourceMailboxId = mainStepdefs.jmapServer
            .getProbe(MailboxProbeImpl.class)
            .getMailbox(MailboxConstants.USER_NAMESPACE, sourceUser, sourceMailbox)
            .getMailboxId();
        MailboxId destinationMailboxId = mainStepdefs.jmapServer
            .getProbe(MailboxProbeImpl.class)
            .getMailbox(MailboxConstants.USER_NAMESPACE, destinationUser, destinationMailbox)
            .getMailboxId();

        post("[" +
            "  [" +
            "    \"setMessages\","+
            "    {" +
            "      \"update\": { \"" + messageId.serialize() + "\" : {" +
            "        \"mailboxIds\": [\"" + destinationMailboxId.serialize() + "\",\"" + sourceMailboxId.serialize() + "\"]" +
            "      }}" +
            "    }," +
            "    \"#0\"" +
            "  ]" +
            "]");
        mainStepdefs.awaitMethod.run();
    }

    @Given("^\"([^\"]*)\" move \"([^\"]*)\" to mailbox \"([^\"]*)\" of user \"([^\"]*)\"")
    public void moveMessageToMailbox(String username, String message, String destinationMailbox, String destinationUser) throws Throwable {
        userStepdefs.execWithUser(username, () -> moveMessageToMailbox(message, destinationMailbox, destinationUser));
    }

    @Given("^the user move \"([^\"]*)\" to mailbox \"([^\"]*)\" of user \"([^\"]*)\"")
    public void moveMessageToMailbox(String message, String destinationMailbox, String destinationUser) throws Throwable {
        MessageId messageId = getMessagesMethodStepdefs.getMessageId(message);
        MailboxId destinationMailboxId = mainStepdefs.jmapServer
            .getProbe(MailboxProbeImpl.class)
            .getMailbox(MailboxConstants.USER_NAMESPACE, destinationUser, destinationMailbox)
            .getMailboxId();

        post("[" +
            "  [" +
            "    \"setMessages\","+
            "    {" +
            "      \"update\": { \"" + messageId.serialize() + "\" : {" +
            "        \"mailboxIds\": [\"" + destinationMailboxId.serialize() + "\"]" +
            "      }}" +
            "    }," +
            "    \"#0\"" +
            "  ]" +
            "]");
        mainStepdefs.awaitMethod.run();
    }

    @When("^\"([^\"]*)\" set flags on \"([^\"]*)\" to \"([^\"]*)\"")
    public void setFlags(String username, String message, List<String> keywords) throws Throwable {
        userStepdefs.execWithUser(username, () -> setFlags(message, keywords));
    }

    @When("^the user set flags on \"([^\"]*)\" to \"([^\"]*)\"")
    public void setFlags(String message, List<String> keywords) throws Throwable {
        MessageId messageId = getMessagesMethodStepdefs.getMessageId(message);
        String keywordString = keywords
            .stream()
            .map(value -> "\"" + value + "\" : true")
            .collect(Collectors.joining(","));

        post("[" +
            "  [" +
            "    \"setMessages\","+
            "    {" +
            "      \"update\": { \"" + messageId.serialize() + "\" : {" +
            "        \"keywords\": {" + keywordString + "}" +
            "      }}" +
            "    }," +
            "    \"#0\"" +
            "  ]" +
            "]");
        mainStepdefs.awaitMethod.run();
    }

    @Then("^the message \"([^\"]*)\" is not updated$")
    public void assertIdOfTheFirstMessage(String messageName) throws Exception {
        MessageId id = getMessagesMethodStepdefs.getMessageId(messageName);
        assertThat(jsonPath.<Map<String, String>>read("[0][1].notUpdated"))
            .containsOnlyKeys(id.serialize());
    }

    private void post(String requestBody) throws Exception {
        response = Request.Post(mainStepdefs.baseUri().setPath("/jmap").build())
            .addHeader("Authorization", userStepdefs.getTokenForUser(userStepdefs.getConnectedUser()).serialize())
            .addHeader("Accept", org.apache.http.entity.ContentType.APPLICATION_JSON.getMimeType())
            .bodyString(requestBody, org.apache.http.entity.ContentType.APPLICATION_JSON)
            .execute()
            .returnResponse();
        jsonPath = JsonPath.using(Configuration.defaultConfiguration()
            .addOptions(Option.SUPPRESS_EXCEPTIONS))
            .parse(response.getEntity().getContent());
    }
}
