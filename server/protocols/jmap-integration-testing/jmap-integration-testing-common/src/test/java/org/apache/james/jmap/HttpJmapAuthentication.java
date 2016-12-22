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

package org.apache.james.jmap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.james.jmap.api.access.AccessToken;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;
import com.jayway.jsonpath.JsonPath;

public class HttpJmapAuthentication {

    private static final ConditionFactory CALMLY_AWAIT = Awaitility.with()
            .pollInterval(Duration.FIVE_HUNDRED_MILLISECONDS)
            .and().with()
            .pollDelay(Duration.FIVE_HUNDRED_MILLISECONDS)
            .await();

    public static AccessToken authenticateJamesUser(URIBuilder uriBuilder, String username, String password) throws ClientProtocolException, IOException, URISyntaxException {
        String continuationToken = getContinuationToken(uriBuilder, username);

        Optional<Response> response = CALMLY_AWAIT.atMost(30, TimeUnit.SECONDS)
        .until(() -> postAuthenticate(uriBuilder, password, continuationToken), new OptionalMatcher());

        return AccessToken.fromString(
                    JsonPath.parse(response.get().returnContent().asString())
                    .read("accessToken"));
    }

    private static Optional<Response> postAuthenticate(URIBuilder uriBuilder, String password, String continuationToken) {
        try {
            return Optional.of(Request.Post(uriBuilder.setPath("/authentication").build())
                    .bodyString("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"" + password + "\"}", 
                            ContentType.APPLICATION_JSON)
                    .setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType())
                    .execute());
        } catch (IOException | URISyntaxException e) {
            return Optional.empty();
        }
    }

    private static String getContinuationToken(URIBuilder uriBuilder, String username) throws ClientProtocolException, IOException, URISyntaxException {
        Response response = Request.Post(uriBuilder.setPath("/authentication").build())
            .bodyString("{\"username\": \"" + username + "\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}", 
                    ContentType.APPLICATION_JSON)
            .setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType())
            .execute();
        return JsonPath.parse(response.returnContent().asString())
            .read("continuationToken");
    }

    private static class OptionalMatcher extends TypeSafeMatcher<Optional<?>> {
        
        public void describeTo(Description description) {
            description.appendText("is <Present>");
        }

        @Override
        protected boolean matchesSafely(Optional<?> item) {
            return item.isPresent();
        }
        
        @Override
        protected void describeMismatchSafely(Optional<?> item, Description mismatchDescription) {
            mismatchDescription.appendText("was <Empty>");
        }
    }

}
