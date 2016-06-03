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

package org.apache.james.jmap.utils;

import static com.jayway.restassured.RestAssured.with;

import java.util.List;
import java.util.Map;

import org.apache.james.jmap.api.access.AccessToken;

import com.jayway.restassured.http.ContentType;

public class MailboxRetriever {

    private static final String ARGUMENTS = "[0][1]";

    public static String getOutboxId(AccessToken accessToken) {
        return getMailboxIdByRole(accessToken, "outbox");
    }

    public static String getInboxId(AccessToken accessToken) {
        return getMailboxIdByRole(accessToken, "inbox");
    }

    public static String getSentId(AccessToken accessToken) {
        return getMailboxIdByRole(accessToken, "sent");
    }

    public static String getMailboxIdByRole(AccessToken accessToken, String role) {
        return getAllMailboxesIds(accessToken).stream()
            .filter(x -> x.get("role").equals(role))
            .map(x -> x.get("id"))
            .findFirst()
            .get();
    }

    public static List<Map<String, String>> getAllMailboxesIds(AccessToken accessToken) {
        return with()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMailboxes\", {\"properties\": [\"role\", \"id\"]}, \"#0\"]]")
            .post("/jmap")
        .andReturn()
            .body()
            .jsonPath()
            .getList(ARGUMENTS + ".list");
    }
}
