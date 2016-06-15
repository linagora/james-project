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

package org.apache.james.webadmin.servlet;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.domainlist.api.DomainListManagementMBean;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.WebAdminServerImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

public class DomainServletTest {

    public static final Optional<Integer> RANDOM_PORT = Optional.empty();

    private WebAdminServerImpl webAdminServerImpl;
    private DomainListManagementMBean domainListManagementMBean;

    @Before
    public void setUp() throws Exception {
        domainListManagementMBean = mock(DomainListManagementMBean.class);
        webAdminServerImpl = new WebAdminServerImpl(RANDOM_PORT, new DomainServlet(domainListManagementMBean), mock(UserServlet.class));
        webAdminServerImpl.configure(null);

        RestAssured.port = webAdminServerImpl.getPort();
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8));
    }

    @After
    public void tearDown() {
        webAdminServerImpl.stop();
    }

    @Test
    public void postShouldAccessUnderlyingStorage() throws Exception {
        String domain = "domain";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .post(Constants.DOMAIN + "/" + domain)
        .then()
            .statusCode(200);

        verify(domainListManagementMBean).addDomain(domain);
        verifyNoMoreInteractions(domainListManagementMBean);
    }

    @Test
    public void postShouldNotAccessUnderlyingStorageWhenNoPath() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .post(Constants.DOMAIN)
        .then()
            .statusCode(400);

        verifyZeroInteractions(domainListManagementMBean);
    }

    @Test
    public void postShouldNotAccessUnderlyingStorageWhenEmptyPath() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .post(Constants.DOMAIN + "/")
        .then()
            .statusCode(400);

        verifyZeroInteractions(domainListManagementMBean);
    }

    @Test
    public void postShouldStillAccessUnderlyingStorageWhenAdditionalJsonContent() throws Exception {
        String domain = "domain";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("\"bad\":\"any\"}")
        .when()
            .post(Constants.DOMAIN + "/" + domain)
        .then()
            .statusCode(200);

        verify(domainListManagementMBean).addDomain(domain);
        verifyNoMoreInteractions(domainListManagementMBean);
    }

    @Test
    public void postShouldDisplayAnInternalErrorUnknownWhenProblemAddingDomain() throws Exception {
        String domain = "domain";

        doThrow(new Exception("message"))
            .when(domainListManagementMBean)
            .addDomain(domain);

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .post(Constants.DOMAIN + "/" + domain)
        .then()
            .statusCode(500);

        verify(domainListManagementMBean).addDomain(domain);
        verifyNoMoreInteractions(domainListManagementMBean);
    }

    @Test
    public void postShouldDisplayAnInternalErrorWhenProblemAddingDomain() throws Exception {
        String domain = "domain";

        doThrow(new DomainListException("message"))
            .when(domainListManagementMBean)
            .addDomain(domain);

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .when()
            .post(Constants.DOMAIN + "/" + domain)
            .then()
            .statusCode(406);

        verify(domainListManagementMBean).addDomain(domain);
        verifyNoMoreInteractions(domainListManagementMBean);
    }

    @Test
    public void getDomainShouldReturnEmptyArrayByDefault() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .get(Constants.DOMAIN)
        .then()
            .statusCode(200)
            .body("domains", hasSize(0));

        verify(domainListManagementMBean).getDomains();
        verifyNoMoreInteractions(domainListManagementMBean);
    }

    @Test
    public void getDomainShouldReturnOneElementWhenOneDomain() throws Exception {
        String domain = "domain";
        when(domainListManagementMBean.getDomains()).thenReturn(Lists.newArrayList(domain));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .get(Constants.DOMAIN)
        .then()
            .statusCode(200)
            .body("domains", hasSize(1))
            .body("domains[0]", equalTo(domain));

        verify(domainListManagementMBean).getDomains();
        verifyNoMoreInteractions(domainListManagementMBean);
    }

    @Test
    public void getDomainShouldReturnTwoElementsWhenTwoDomains() throws Exception {
        String domain1 = "domain1";
        String domain2 = "domain2";
        when(domainListManagementMBean.getDomains()).thenReturn(Lists.newArrayList(domain1, domain2));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .get(Constants.DOMAIN)
        .then()
            .statusCode(200)
            .body("domains", hasSize(2))
            .body("domains[0]", equalTo(domain1))
            .body("domains[1]", equalTo(domain2));

        verify(domainListManagementMBean).getDomains();
        verifyNoMoreInteractions(domainListManagementMBean);
    }

    @Test
    public void getDomainShouldDisplayAnInternalErrorWhenProblemAddingDomain() throws Exception {
        when(domainListManagementMBean.getDomains()).thenThrow(new DomainListException("message"));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .get(Constants.DOMAIN)
        .then()
            .statusCode(500);

        verify(domainListManagementMBean).getDomains();
        verifyNoMoreInteractions(domainListManagementMBean);
    }


    @Test
    public void deleteShouldAccessUnderlyingStorage() throws Exception {
        String domain = "domain";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .delete(Constants.DOMAIN + "/" + domain)
        .then()
            .statusCode(200);

        verify(domainListManagementMBean).removeDomain(domain);
        verifyNoMoreInteractions(domainListManagementMBean);
    }

    @Test
    public void deleteShouldNotAccessUnderlyingStorageWhenNoPath() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .delete(Constants.DOMAIN)
        .then()
            .statusCode(400);

        verifyZeroInteractions(domainListManagementMBean);
    }

    @Test
    public void deleteShouldNotAccessUnderlyingStorageWhenEmptyPath() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .delete(Constants.DOMAIN + "/")
        .then()
            .statusCode(400);

        verifyZeroInteractions(domainListManagementMBean);
    }

    @Test
    public void deleteShouldStillAccessUnderlyingStorageWhenAdditionalJsonContent() throws Exception {
        String domain = "domain";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("\"bad\":\"any\"}")
        .when()
            .delete(Constants.DOMAIN + "/" + domain)
        .then()
            .statusCode(200);

        verify(domainListManagementMBean).removeDomain(domain);
        verifyNoMoreInteractions(domainListManagementMBean);
    }

    @Test
    public void deleteShouldDisplayAnInternalErrorWhenUnknownProblemAddingDomain() throws Exception {
        String domain = "domain";

        doThrow(new Exception("message"))
            .when(domainListManagementMBean)
            .removeDomain(domain);

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .delete(Constants.DOMAIN + "/" + domain)
        .then()
            .statusCode(500);

        verify(domainListManagementMBean).removeDomain(domain);
        verifyNoMoreInteractions(domainListManagementMBean);
    }

    @Test
    public void deleteShouldDisplayAnInternalErrorWhenProblemAddingDomain() throws Exception {
        String domain = "domain";

        doThrow(new DomainListException("message"))
            .when(domainListManagementMBean)
            .removeDomain(domain);

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .delete(Constants.DOMAIN + "/" + domain)
        .then()
            .statusCode(406);

        verify(domainListManagementMBean).removeDomain(domain);
        verifyNoMoreInteractions(domainListManagementMBean);
    }

}
