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

import static io.restassured.RestAssured.given;
import static org.apache.james.webadmin.Constants.JSON_CONTENT_TYPE;
import static org.apache.james.webadmin.WebAdminServer.NO_CONFIGURATION;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.apache.james.metrics.logger.DefaultMetricFactory;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminUtils;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

class DLPExpressionRoutesTest {

    private static WebAdminServer webadmin;

    @BeforeAll
    static void setup() throws Exception {
        webadmin = WebAdminUtils.createWebAdminServer(
            new DefaultMetricFactory(),
            new DLPExpressionRoutes(new JsonTransformer()));
        webadmin.configure(NO_CONFIGURATION);
        webadmin.await();

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webadmin)
            .setBasePath("/dlp/expression")
            .build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterAll
    static void tearDown() {
        webadmin.destroy();
    }

    @Nested
    class ValidationExpression {
        @Nested
        class RequestValidation {
            @Test
            void validateExpressionShouldThrowWhenNoExpression() {
                given()
                    .body("{}")
                .when()
                    .post("/validate")
                .then()
                    .statusCode(400)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("statusCode", is(HttpStatus.BAD_REQUEST_400))
                    .body("type", is("InvalidArgument"))
                    .body("message", is("JSON payload of the request is not valid"))
                    .body("details", containsString("The regex expression can not be null"));
            }

            @Test
            void validateExpressionShouldThrowWhenPatternDoesntTakeCareEscapeWhenJacksonParsing() {
                given()
                    .body("{\"expression\": \"a valid white space pattern but doesnt care about jacskon parsing should fail \\s\"}")
                .when()
                    .post("/validate")
                .then()
                    .statusCode(400)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("statusCode", is(HttpStatus.BAD_REQUEST_400))
                    .body("type", is("InvalidArgument"))
                    .body("message", is("JSON payload of the request is not valid"));
            }
        }

        @Nested
        class RequestExecution {
            @Test
            void validateExpressionShouldReturnValidWhenExpressionIsEmpty() {
                given()
                    .body("{\"expression\": \"\"}")
                    .when()
                    .post("/validate")
                    .then()
                    .statusCode(200)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("isValid", is(true));
            }

            @Test
            void validateExpressionShouldTakeCareJacksonParsingEscape() {
                given()
                    .body("{\"expression\": \"because jackson cares about escape when parsing, that means if you want to " +
                        "pass any escape character to webadmin, put four instead of two slashes. This should be valid pattern with a " +
                        "white space by putting four slashes \\\\s\"}")
                    .when()
                    .post("/validate")
                    .then()
                    .statusCode(200)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("isValid", is(true));
            }

            @Test
            void validateExpressionShouldReturnInValidWhenPatternContainsInvalidEscapeCharacters() {
                given()
                    .body("{\"expression\": \"care about jackson parsing but slash + i is not a valid java regex escape \\\\i\"}")
                    .when()
                    .post("/validate")
                    .then()
                    .statusCode(200)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("isValid", is(false));
            }

            @Test
            void validateExpressionShouldReturnValidWhenExpressionIsOnlyLetterPattern() {
                given()
                    .body("{\"expression\": \"[a-zA-Z]+\"}")
                .when()
                    .post("/validate")
                .then()
                    .statusCode(200)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("isValid", is(true));
            }

            @Test
            void validateExpressionShouldReturnValidWhenExpressionIsOnlyNumberPattern() {
                given()
                    .body("{\"expression\": \"[0-9]+\"}")
                .when()
                    .post("/validate")
                .then()
                    .statusCode(200)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("isValid", is(true));
            }

            @Test
            void validateExpressionShouldReturnValidWhenExpressionIsStartWithPlus84ThenSpaceOrNotThenDigits() {
                given()
                    .body("{\"expression\": \"\\\\+84[ ]?\\\\d+\"}")
                .when()
                    .post("/validate")
                .then()
                    .statusCode(200)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("isValid", is(true));
            }
        }
    }

    @Nested
    class SampleMatch {
        @Nested
        class RequestValidation {
            @Test
            void sampleMatchShouldThrowWhenNoExpression() {
                given()
                    .body("{\"sampleValue\": \"james.org\"}")
                .when()
                    .post("/sampleMatch")
                 .then()
                    .statusCode(400)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("statusCode", is(HttpStatus.BAD_REQUEST_400))
                    .body("type", is("InvalidArgument"))
                    .body("message", is("JSON payload of the request is not valid"))
                    .body("details", containsString("The regex expression can not be null"));
            }

            @Test
            void sampleMatchShouldThrowWhenNoSampleValue() {
                given()
                    .body("{\"expression\": \"[a-zA-Z]+@james.org\"}")
                .when()
                    .post("/sampleMatch")
                .then()
                    .statusCode(400)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("statusCode", is(HttpStatus.BAD_REQUEST_400))
                    .body("type", is("InvalidArgument"))
                    .body("message", is("JSON payload of the request is not valid"))
                    .body("details", containsString("The sample value can not be null"));
            }

            @Test
            void sampleMatchShouldThrowWhenExpressionAndSampleValueAreNull() {
                given()
                    .body("{}")
                .when()
                    .post("/sampleMatch")
                .then()
                    .statusCode(400)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("statusCode", is(HttpStatus.BAD_REQUEST_400))
                    .body("type", is("InvalidArgument"))
                    .body("message", is("JSON payload of the request is not valid"))
                    .body("details", containsString("The regex expression can not be null"));
            }

            @Test
            void sampleMatchShouldThrowWhenExpressionDoesntCareAboutEscape() {
                given()
                    .body("{\"expression\": \"\\s\\\", \"sampleValue\": \"contains white space then a normal slash: \\\\\"}")
                .when()
                    .post("/sampleMatch")
                .then()
                    .statusCode(400)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("statusCode", is(HttpStatus.BAD_REQUEST_400))
                    .body("type", is("InvalidArgument"))
                    .body("message", is("JSON payload of the request is not valid"));
            }

            @Test
            void sampleMatchShouldThrowWhenSampleValueDoesntCareAboutEscape() {
                given()
                    .body("{\"expression\": \"\\\\s\\\\\\\\\", \"sampleValue\": \"contains white space then a normal slash: \\\"}")
                .when()
                    .post("/sampleMatch")
                .then()
                    .statusCode(400)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("statusCode", is(HttpStatus.BAD_REQUEST_400))
                    .body("type", is("InvalidArgument"))
                    .body("message", is("JSON payload of the request is not valid"));
            }
        }

        @Nested
        class RequestExecution {
            @Test
            void sampleMatchShouldMatchWhenSampleValueMatchesLetterOnlyExpression() {
                given()
                    .body("{\"expression\": \"^[a-zA-Z]+$\", \"sampleValue\": \"containsLetterOnly\"}")
                .when()
                    .post("/sampleMatch")
                .then()
                    .statusCode(200)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("isValid", is(true))
                    .body("isMatched", is(true));
            }

            @Test
            void sampleMatchShouldReturnInvalidWhenSuppliedExpressionIsNotValid() {
                given()
                    .body("{\"expression\": \"(invalid\", \"sampleValue\": \"some data\"}")
                .when()
                    .post("/sampleMatch")
                .then()
                    .statusCode(200)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("isValid", is(false))
                    .body("isMatched", nullValue());
            }

            @Test
            void sampleMatchShouldNotMatchWhenSampleValueDoestMatchLetterOnlyExpression() {
                given()
                    .body("{\"expression\": \"^[a-zA-Z]+$\", \"sampleValue\": \"containsLetterOnlyButASpaceAtLast \"}")
                .when()
                    .post("/sampleMatch")
                .then()
                    .statusCode(200)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("isValid", is(true))
                    .body("isMatched", is(false));
            }

            @Test
            void sampleMatchShouldMatchWhenExpressionIsEmpty() {
                given()
                    .body("{\"expression\": \"\", \"sampleValue\": \"any value\"}")
                .when()
                    .post("/sampleMatch")
                .then()
                    .statusCode(200)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("isValid", is(true))
                    .body("isMatched", is(true));
            }

            @Test
            void sampleMatchShouldMatchWhenBothAreEmpty() {
                given()
                    .body("{\"expression\": \"\", \"sampleValue\": \"\"}")
                .when()
                    .post("/sampleMatch")
                .then()
                    .statusCode(200)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("isValid", is(true))
                    .body("isMatched", is(true));
            }

            @Test
            void sampleMatchShouldCareAboutEscape() {
                given()
                    .body("{\"expression\": \"\\\\s\\\\\\\\\", \"sampleValue\": \"contains white space then a normal slash: \\\\\"}")
                .when()
                    .post("/sampleMatch")
                .then()
                    .statusCode(200)
                    .contentType(JSON_CONTENT_TYPE)
                    .body("isValid", is(true))
                    .body("isMatched", is(true));
            }
        }
    }
}