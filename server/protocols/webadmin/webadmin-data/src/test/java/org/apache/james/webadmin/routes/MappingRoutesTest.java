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

package org.apache.james.webadmin.routes;

import static io.restassured.RestAssured.when;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;

import javax.mail.internet.AddressException;

import net.javacrumbs.jsonunit.core.Option;
import org.apache.james.core.Domain;
import org.apache.james.core.MailAddress;
import org.apache.james.core.User;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.domainlist.memory.MemoryDomainList;
import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.lib.MappingSource;
import org.apache.james.rrt.memory.MemoryRecipientRewriteTable;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminUtils;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;

class MappingRoutesTest {

    private WebAdminServer webAdminServer;
    private MemoryRecipientRewriteTable recipientRewriteTable;

    @BeforeEach
    void beforeEach() throws DomainListException {
        JsonTransformer jsonTransformer = new JsonTransformer();
        recipientRewriteTable = new MemoryRecipientRewriteTable();
        DNSService dnsService = mock(DNSService.class);
        DomainList domainList = new MemoryDomainList(dnsService);
        domainList.addDomain(Domain.of("domain.tld"));
        domainList.addDomain(Domain.of("aliasdomain.tld"));
        domainList.addDomain(Domain.of("gmail.com"));

        recipientRewriteTable.setDomainList(domainList);

        webAdminServer = WebAdminUtils.createWebAdminServer(new MappingRoutes(jsonTransformer, recipientRewriteTable))
            .start();

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webAdminServer)
            .setBasePath(MappingRoutes.BASE_PATH)
            .log(LogDetail.METHOD)
            .build();
    }

    @AfterEach
    void stop() {
        webAdminServer.destroy();
    }

    @Test
    void getMappingsShouldReturnEmptyWhenNoMappings() {
        when()
            .get()
        .then()
            .contentType(ContentType.JSON)
            .statusCode(HttpStatus.OK_200)
            .body(is("{}"));
    }

    @Test
    void getMappingsShouldReturnAliasMappings() throws RecipientRewriteTableException {
        recipientRewriteTable.addAliasMapping(
            MappingSource.fromUser(User.fromUsername("alias@domain.tld")),
            "user@domain.tld");

        recipientRewriteTable.addAliasMapping(
            MappingSource.fromUser(User.fromUsername("alias@domain.tld")),
            "user1@domain.tld");

        String jsonBody = when()
            .get()
        .then()
            .contentType(ContentType.JSON)
            .statusCode(HttpStatus.OK_200)
        .extract()
            .body()
            .asString();

        assertThatJson(jsonBody)
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo("{" +
                    "  \"alias@domain.tld\": [" +
                    "    {" +
                    "      \"type\": \"Alias\"," +
                    "      \"mapping\": \"user@domain.tld\"" +
                    "    }," +
                    "    {" +
                    "      \"type\": \"Alias\"," +
                    "      \"mapping\": \"user1@domain.tld\"" +
                    "    }" +
                    "  ]" +
                    "}");
    }

    @Test
    void getMappingsShouldReturnAliasDomainMappings() throws RecipientRewriteTableException {
        recipientRewriteTable.addAliasDomainMapping(
            MappingSource.fromDomain(Domain.of("aliasdomain.tld")),
            Domain.of("realdomain.tld"));

        recipientRewriteTable.addAliasDomainMapping(
            MappingSource.fromDomain(Domain.of("aliasdomain.tld")),
            Domain.of("realdomain1.tld"));

        String jsonBody = when()
            .get()
        .then()
            .contentType(ContentType.JSON)
            .statusCode(HttpStatus.OK_200)
        .extract()
            .body()
            .asString();

        assertThatJson(jsonBody)
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo("{" +
                    "  \"aliasdomain.tld\": [" +
                    "    {" +
                    "      \"type\": \"Domain\"," +
                    "      \"mapping\": \"realdomain.tld\"" +
                    "    }," +
                    "    {" +
                    "      \"type\": \"Domain\"," +
                    "      \"mapping\": \"realdomain1.tld\"" +
                    "    }" +
                    "  ]" +
                    "}");
    }

    @Test
    void getMappingsShouldReturnAddressMappings() throws AddressException, RecipientRewriteTableException {
        MailAddress mailAddress = new MailAddress("group@domain.tld");
        recipientRewriteTable.addAddressMapping(
            MappingSource.fromMailAddress(mailAddress), "user@domain.tld" );

        recipientRewriteTable.addAddressMapping(
                MappingSource.fromMailAddress(mailAddress), "user1@domain.tld" );

        String jsonBody = when()
            .get()
        .then()
            .contentType(ContentType.JSON)
            .statusCode(HttpStatus.OK_200)
        .extract()
            .body()
            .asString();

        assertThatJson(jsonBody)
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo("{" +
                    "  \"group@domain.tld\": [" +
                    "    {" +
                    "      \"type\": \"Address\"," +
                    "      \"mapping\": \"user@domain.tld\"" +
                    "    }," +
                    "    {" +
                    "      \"type\": \"Address\"," +
                    "      \"mapping\": \"user1@domain.tld\"" +
                    "    }" +
                    "  ]" +
                    "}");
    }

    @Test
    void getMappingsShouldReturnAllMappings() throws RecipientRewriteTableException, AddressException {

         MailAddress mailAddress = new MailAddress("group@domain.tld");

        recipientRewriteTable.addAliasMapping(
             MappingSource.fromUser(User.fromUsername("alias@domain.tld")),
            "user@domain.tld");

        recipientRewriteTable.addAliasDomainMapping(
            MappingSource.fromDomain(Domain.of("aliasdomain.tld")),
            Domain.of("realdomain.tld"));

        recipientRewriteTable.addAddressMapping(
            MappingSource.fromMailAddress(mailAddress), "user@domain.tld" );

        String jsonBody = when()
            .get()
        .then()
            .contentType(ContentType.JSON)
            .statusCode(HttpStatus.OK_200)
        .extract()
            .body()
            .asString();

        assertThatJson(jsonBody)
            .isEqualTo("{" +
                "  \"alias@domain.tld\" : [" +
                "    {" +
                "      \"type\": \"Alias\"," +
                "      \"mapping\": \"user@domain.tld\"" +
                "    }" +
                "  ]," +
                "  " +
                "  \"aliasdomain.tld\" : [" +
                "  {" +
                "    \"type\": \"Domain\"," +
                "    \"mapping\": \"realdomain.tld\"" +
                "  }" +
                "  ]," +
                "  " +
                "  \"group@domain.tld\": [" +
                "    {" +
                "      \"type\": \"Address\"," +
                "      \"mapping\": \"user@domain.tld\"" +
                "    }" +
                "    ]" +
                "  " +
                "}"
                );
    }
}