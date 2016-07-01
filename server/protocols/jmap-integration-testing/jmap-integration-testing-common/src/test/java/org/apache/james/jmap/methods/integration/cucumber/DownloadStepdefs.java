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
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.mail.Flags;

import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
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

    private static final String KNOWN_BLOB_ID = "4000c5145f633410b80be368c44e1c394bff9437";
    private static final String EXPIRED_ATTACHMENT_TOKEN = "usera@domain.tld_"
            + "2016-06-29T13:41:22.124Z_"
            + "DiZa0O14MjLWrAA8P6MG35Gt5CBp7mt5U1EH/M++rIoZK7nlGJ4dPW0dvZD7h4m3o5b/Yd8DXU5x2x4+s0HOOKzD7X0RMlsU7JHJMNLvTvRGWF/C+MUyC8Zce7DtnRVPEQX2uAZhL2PBABV07Vpa8kH+NxoS9CL955Bc1Obr4G+KN2JorADlocFQA6ElXryF5YS/HPZSvq1MTC6aJIP0ku8WRpRnbwgwJnn26YpcHXcJjbkCBtd9/BhlMV6xNd2hTBkfZmYdoNo+UKBaXWzLxAlbLuxjpxwvDNJfOEyWFPgHDoRvzP+G7KzhVWjanHAHrhF0GilEa/MKpOI1qHBSwA==";
    private static final String INVALID_ATTACHMENT_TOKEN = "usera@domain.tld_"
            + "2015-06-29T13:41:22.124Z_"
            + "DiZa0O14MjLWrAA8P6MG35Gt5CBp7mt5U1EH/M++rIoZK7nlGJ4dPW0dvZD7h4m3o5b/Yd8DXU5x2x4+s0HOOKzD7X0RMlsU7JHJMNLvTvRGWF/C+MUyC8Zce7DtnRVPEQX2uAZhL2PBABV07Vpa8kH+NxoS9CL955Bc1Obr4G+KN2JorADlocFQA6ElXryF5YS/HPZSvq1MTC6aJIP0ku8WRpRnbwgwJnn26YpcHXcJjbkCBtd9/BhlMV6xNd2hTBkfZmYdoNo+UKBaXWzLxAlbLuxjpxwvDNJfOEyWFPgHDoRvzP+G7KzhVWjanHAHrhF0GilEa/MKpOI1qHBSwA==";

    private final UserStepdefs userStepdefs;
    private final MainStepdefs mainStepdefs;
    private Response response;
    private Multimap<String, String> attachmentsByMessageId;
    private Map<String, String> blobIdByAttachmentId;
    private ValidatableResponse validatableResponse;
    private String attachmentAccessToken;

    @Inject
    private DownloadStepdefs(MainStepdefs mainStepdefs, UserStepdefs userStepdefs) {
        this.mainStepdefs = mainStepdefs;
        this.userStepdefs = userStepdefs;
        this.attachmentsByMessageId = ArrayListMultimap.create();
        this.blobIdByAttachmentId = new HashMap<>();
    }

    @Given("^\"([^\"]*)\" mailbox \"([^\"]*)\" contains a message \"([^\"]*)\" with an attachment \"([^\"]*)\"$")
    public void appendMessageWithAttachmentToMailbox(String user, String mailbox, String messageId, String attachmentId) throws Throwable {
        MailboxPath mailboxPath = new MailboxPath(MailboxConstants.USER_NAMESPACE, user, mailbox);

        // blodId = "4000c5145f633410b80be368c44e1c394bff9437"
        mainStepdefs.jmapServer.serverProbe().appendMessage(user, mailboxPath,
                ClassLoader.getSystemResourceAsStream("eml/oneAttachment.eml"), new Date(), false, new Flags());
        
        attachmentsByMessageId.put(messageId, attachmentId);
        blobIdByAttachmentId.put(attachmentId, "4000c5145f633410b80be368c44e1c394bff9437");
    }

    @When("^\"([^\"]*)\" checks for the availability of the attachment endpoint$")
    public void optionDownload(String username) throws Throwable {
        AccessToken accessToken = userStepdefs.tokenByUser.get(username);
        RequestSpecification with = with();
        if (accessToken != null) {
            with.header("Authorization", accessToken.serialize());
        }

        response = with
                .options("/download/" + KNOWN_BLOB_ID);
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\"$")
    public void downloads(String username, String attachmentId) throws Throwable {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        RequestSpecification with = with();
        addAuthentication(with, blobId, username);
        response = with
                .get("/download/" + blobId);
    }

    private void addAuthentication(RequestSpecification with, String blobId, String username) {
        AccessToken accessToken = userStepdefs.tokenByUser.get(username);
        if (!Strings.isNullOrEmpty(attachmentAccessToken)) {
            with.param("access_token", attachmentAccessToken(blobId, accessToken));
            return;
        }
        if (accessToken != null) {
            with.header("Authorization", accessToken.serialize());
        }
    }

    @When("^\"([^\"]*)\" is trusted for attachment \"([^\"]*)\"$")
    public void attachmentAccessTokenFor(String username, String blobId) throws Throwable {
        userStepdefs.connectUser(username);
        attachmentAccessToken = attachmentAccessToken(blobId, userStepdefs.tokenByUser.get(username));
    }

    private String attachmentAccessToken(String blobId, AccessToken accessToken) {
        return
            with()
                .header("Authorization", accessToken.serialize())
            .post("/download/" + blobId)
            .then()
                .extract()
                .asString();
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" with a valid authentication token$")
    public void downloadsWithValidToken(String username, String unknownBlobId) throws Throwable {
        String blobId = blobIdByAttachmentId.getOrDefault("2", null);
        AccessToken accessToken = userStepdefs.tokenByUser.get(username);
        response = with()
                .param("access_token", attachmentAccessToken(blobId, accessToken))
                .get("/download/" + unknownBlobId);
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" without any authentication token$")
    public void getDownloadWithoutToken(String username, String attachmentId) {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        response = with().get("/download/" + blobId);
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" with an empty authentication token$")
    public void getDownloadWithEmptyToken(String username, String attachmentId) {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        response = with()
                .param("access_token", "")
                .get("/download/" + blobId);
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" with a bad authentication token$")
    public void getDownloadWithBadToken(String username, String attachmentId) {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        response = with()
                .param("access_token", "bad")
                .get("/download/" + blobId);
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" with an invalid authentication token$")
    public void getDownloadWithUnknownToken(String username, String attachmentId) {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        response = with()
                .param("access_token", INVALID_ATTACHMENT_TOKEN)
                .get("/download/" + blobId);
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" without blobId parameter$")
    public void getDownloadWithoutBlobId(String username, String attachmentId) throws Throwable {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        AccessToken accessToken = userStepdefs.tokenByUser.get(username);
        response = with()
            .param("access_token", attachmentAccessToken(blobId, accessToken))
            .get("/download/");
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" with wrong blobId$")
    public void getDownloadWithWrongBlobId(String username, String attachmentId) throws Throwable {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        AccessToken accessToken = userStepdefs.tokenByUser.get(username);
        response = with()
                .param("access_token", attachmentAccessToken(blobId, accessToken))
                .get("/download/badbadbadbadbadbadbadbadbadbadbadbadbadb");
    }

    @When("^\"([^\"]*)\" asks for a token for attachment \"([^\"]*)\"$")
    public void postDownload(String username, String attachmentId) throws Throwable {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        AccessToken accessToken = userStepdefs.tokenByUser.get(username);
        RequestSpecification with = with();
        if (accessToken != null) {
            with = with.header("Authorization", accessToken.serialize());
        }
        response = with
                .post("/download/" + blobId);
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" with \"([^\"]*)\" name$")
    public void downloadsWithName(String username, String attachmentId, String name) {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        AccessToken accessToken = userStepdefs.tokenByUser.get(username);
        response = with()
                .param("access_token", attachmentAccessToken(blobId, accessToken))
                .get("/download/" + blobId + "/" + name);
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" with an expired token$")
    public void getDownloadWithExpiredToken(String username, String attachmentId) {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        response = with()
                .param("access_token", EXPIRED_ATTACHMENT_TOKEN)
                .get("/download/" + blobId);
    }

    @Then("^the user should be authorized$")
    public void httpStatusDifferentFromUnauthorized() {
        response.then()
            .statusCode(isIn(ImmutableList.of(200, 404)));
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

    @Then("^the user should receive an attachment access token$")
    public void accessTokenResponse() throws Throwable {
        response.then()
            .statusCode(200)
            .contentType(ContentType.TEXT)
            .content(notNullValue());
    }

    @Then("^the attachment is named \"([^\"]*)\"$")
    public void assertContentDisposition(String name) {
        validatableResponse.header("Content-Disposition", name);
    }
}
