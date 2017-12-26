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

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static com.jayway.restassured.RestAssured.with;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.apache.james.webadmin.WebAdminServer.NO_CONFIGURATION;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.apache.james.metrics.logger.DefaultMetricFactory;
import org.apache.james.task.MemoryTaskManager;
import org.apache.james.task.Task;
import org.apache.james.task.TaskManager;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminUtils;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;

public class TaskRoutesTest {

    private MemoryTaskManager taskManager;
    private WebAdminServer webAdminServer;

    @Before
    public void setUp() throws Exception {
        taskManager = new MemoryTaskManager();

        webAdminServer = WebAdminUtils.createWebAdminServer(
            new DefaultMetricFactory(),
            new TaskRoutes(taskManager, new JsonTransformer()));

        webAdminServer.configure(NO_CONFIGURATION);
        webAdminServer.await();

        RestAssured.requestSpecification = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setBasePath(TaskRoutes.BASE)
            .setPort(webAdminServer.getPort().toInt())
            .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8)))
            .build();
    }

    @After
    public void tearDown() {
        taskManager.stop();
        webAdminServer.destroy();
    }

    @Test
    public void postShouldReturnEmptyWhenNoTask() {
        when()
            .post()
        .then()
            .body("", hasSize(0));
    }

    @Test
    public void postShouldReturnTaskDetailsWhenTaskInProgress() {
        Task.TaskId taskId = taskManager.submit(() -> {
            await(new CountDownLatch(1));
            return Task.Result.COMPLETED;
        });

        when()
            .post()
        .then()
            .statusCode(HttpStatus.OK_200)
            .body("", hasSize(1))
            .body("[0].status", is(TaskManager.Status.IN_PROGRESS.getValue()))
            .body("[0].taskId", is(taskId.getValue().toString()))
            .body("[0].class", is(not(empty())));
    }

    @Test
    public void postShouldListAllTaskWhenEmptyFilter() {
        Task.TaskId taskId = taskManager.submit(() -> {
            await(new CountDownLatch(1));
            return Task.Result.COMPLETED;
        });

        given()
            .body("{}")
        .when()
            .post()
        .then()
            .statusCode(HttpStatus.OK_200)
            .body("", hasSize(1))
            .body("[0].status", is(TaskManager.Status.IN_PROGRESS.getValue()))
            .body("[0].taskId", is(taskId.getValue().toString()))
            .body("[0].class", is(not(empty())));
    }

    @Test
    public void postShouldListTaskWhenStatusFilter() {
        Task.TaskId taskId = taskManager.submit(() -> {
            await(new CountDownLatch(1));
            return Task.Result.COMPLETED;
        });

        given()
            .body("{\"status\":\"inProgress\"}")
        .when()
            .post()
        .then()
            .statusCode(HttpStatus.OK_200)
            .body("", hasSize(1))
            .body("[0].status", is(TaskManager.Status.IN_PROGRESS.getValue()))
            .body("[0].taskId", is(taskId.getValue().toString()))
            .body("[0].class", is(not(empty())));
    }

    @Test
    public void postShouldReturnEmptyWhenNonMatchingStatusFilter() {
        taskManager.submit(() -> {
            await(new CountDownLatch(1));
            return Task.Result.COMPLETED;
        });

        given()
            .body("{\"status\":\"waiting\"}")
        .when()
            .post()
        .then()
            .statusCode(HttpStatus.OK_200)
            .body("", hasSize(0));
    }

    @Test
    public void getShouldReturnTaskDetails() {
        Task.TaskId taskId = taskManager.submit(() -> {
            await(new CountDownLatch(1));
            return Task.Result.COMPLETED;
        });

        when()
            .get("/" + taskId.getValue().toString())
        .then()
            .statusCode(HttpStatus.OK_200)
            .body("status", is("inProgress"));
    }

    @Test
    public void getAwaitShouldAwaitTaskCompletion() {
        Task.TaskId taskId = taskManager.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
            return Task.Result.COMPLETED;
        });

        when()
            .get("/" + taskId.getValue().toString() + "/await")
        .then()
            .statusCode(HttpStatus.OK_200)
            .body("status", is("completed"));
    }

    @Test
    public void deleteShouldReturnOk() {
        Task.TaskId taskId = taskManager.submit(() -> {
            await(new CountDownLatch(1));
            return Task.Result.COMPLETED;
        });

        when()
            .delete("/" + taskId.getValue().toString())
        .then()
            .statusCode(HttpStatus.OK_200);
    }
    @Test
    public void deleteShouldCancelMatchingTask() {
        Task.TaskId taskId = taskManager.submit(() -> {
            await(new CountDownLatch(1));
            return Task.Result.COMPLETED;
        });

        with()
            .delete("/" + taskId.getValue().toString());

        when()
            .get("/" + taskId.getValue().toString())
        .then()
            .statusCode(HttpStatus.OK_200)
            .body("status", is("canceled"));
    }

    @Test
    public void getShouldReturnNotFoundWhenIdDontExist() {
        String taskId = UUID.randomUUID().toString();

        when()
            .get("/" + taskId)
        .then()
            .statusCode(HttpStatus.NOT_FOUND_404)
            .body("statusCode", is(HttpStatus.NOT_FOUND_404))
            .body("type", is("notFound"))
            .body("message", is(String.format("%s can not be found", taskId)));
    }

    @Test
    public void getShouldReturnErrorWhenInvalidId() {
        String taskId = "invalid";

        given()
            .when()
            .get("/" + taskId)
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .body("statusCode", is(HttpStatus.BAD_REQUEST_400))
            .body("type", is("InvalidArgument"))
            .body("message", is("Invalid task id"));
    }

    @Test
    public void deleteShouldReturnOkWhenNonExistingId() {
        String taskId = UUID.randomUUID().toString();

        given()
            .when()
            .delete("/" + taskId)
        .then()
            .statusCode(HttpStatus.OK_200);
    }

    @Test
    public void deleteShouldReturnAnErrorOnInvalidId() {
        String taskId = "invalid";

        when()
            .delete("/" + taskId)
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .body("statusCode", is(HttpStatus.BAD_REQUEST_400))
            .body("type", is("InvalidArgument"))
            .body("message", is("Invalid task id"));
    }

    @Test
    public void postShouldHandleJsonFormattingErrors() {
        given()
            .body("{ bad json string")
            .post()
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .body("statusCode", is(HttpStatus.BAD_REQUEST_400))
            .body("type", is("InvalidArgument"))
            .body("message", is("Invalid JSON data"));
    }

    @Test
    public void postShouldReturnErrorWhenNonExistingStatus() {
        given()
            .body("{\"status\":\"invalid\"}")
        .when()
            .post()
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .body("statusCode", is(HttpStatus.BAD_REQUEST_400))
            .body("type", is("InvalidArgument"))
            .body("message", is("Invalid JSON data"))
            .body("cause", containsString("Unknown status value 'invalid'"));
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

}