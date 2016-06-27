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

import static com.jayway.restassured.RestAssured.with;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Date;

import javax.inject.Inject;
import javax.mail.Flags;

import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;
import com.jayway.restassured.specification.RequestSpecification;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.java.guice.ScenarioScoped;

@ScenarioScoped
public class DownloadStepdefs {

    private static final String NAME = "myFileName.txt";

    private final UserStepdefs userStepdefs;
    private final MainStepdefs mainStepdefs;
    private Response response;
    private ValidatableResponse validatableResponse;

    @Inject
    private DownloadStepdefs(MainStepdefs mainStepdefs, UserStepdefs userStepdefs) {
        this.mainStepdefs = mainStepdefs;
        this.userStepdefs = userStepdefs;
    }

    @Given("^a message containing an attachment$")
    public void appendMessageWithAttachment() throws Exception {
        mainStepdefs.jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, userStepdefs.username, "INBOX");
        MailboxPath mailboxPath = new MailboxPath(MailboxConstants.USER_NAMESPACE, userStepdefs.username, "INBOX");

        mainStepdefs.jmapServer.serverProbe().appendMessage(userStepdefs.username, mailboxPath,
                ClassLoader.getSystemResourceAsStream("eml/oneAttachment.eml"), new Date(), false, new Flags());
    }

    @When("^checking for the availability of the attachment endpoint$")
    public void optionDownload() {
        RequestSpecification requestSpecification = with()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON);

        if (userStepdefs.accessToken != null) {
            requestSpecification.header("Authorization", userStepdefs.accessToken.serialize());
        }

        response = requestSpecification.options("/download/myBlob");
    }

    @When("^asking for an attachment$")
    public void getDownload() {
        RequestSpecification requestSpecification = with()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON);

        if (userStepdefs.accessToken != null) {
            requestSpecification.header("Authorization", userStepdefs.accessToken.serialize());
        }

        response = requestSpecification.get("/download/myBlob");
    }

    @When("^asking for an attachment without blobId parameter$")
    public void getDownloadWithoutBlobId() {
        response = with()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", userStepdefs.accessToken.serialize())
            .get("/download/");
    }

    @When("^getting the attachment with its correct blobId$")
    public void getDownloadWithKnownBlobId() {
        response = with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", userStepdefs.accessToken.serialize())
                .get("/download/4000c5145f633410b80be368c44e1c394bff9437");
    }

    @When("^getting the attachment with an unknown blobId$")
    public void getDownloadWithUnknownBlobId() {
        response = with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", userStepdefs.accessToken.serialize())
                .get("/download/badbadbadbadbadbadbadbadbadbadbadbadbadb");
    }

    @When("^getting the attachment with its correct blobId and a desired name$")
    public void getDownloadWithKnownBlobIdAndName() {
        response = with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", userStepdefs.accessToken.serialize())
                .get("/download/4000c5145f633410b80be368c44e1c394bff9437/" + NAME);
    }

    @Then("^the user should be authorized$")
    public void httpStatusDifferentFromUnauthorized() {
        response.then()
            .statusCode(not(401));
    }

    @Then("^the user should not be authorized$")
    public void httpUnauthorizedStatus() {
        response.then()
            .statusCode(401);
    }

    @Then("^the user should receive a bad request response$")
    public void httpBadRequestStatus() {
        response.then()
            .statusCode(400);
    }

    @Then("^the user should receive that attachment$")
    public void httpOkStatusAndExpectedContent() {
        validatableResponse = response.then()
            .statusCode(200)
            .content(notNullValue());
    }

    @Then("^the user should receive a not found response$")
    public void httpNotFoundStatus() {
        response.then()
            .statusCode(404);
    }

    @Then("^the response contains a Content-Disposition header file with that desired name$")
    public void assertContentDisposition() {
        validatableResponse.header("Content-Disposition", NAME);
    }
}
