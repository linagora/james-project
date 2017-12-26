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

import java.util.UUID;

import javax.inject.Inject;

import org.apache.james.task.Task;
import org.apache.james.task.TaskManager;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.dto.StatusReportDto;
import org.apache.james.webadmin.dto.StatusResponseDto;
import org.apache.james.webadmin.dto.TaskFilter;
import org.apache.james.webadmin.utils.ErrorResponder;
import org.apache.james.webadmin.utils.JsonExtractException;
import org.apache.james.webadmin.utils.JsonExtractor;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.eclipse.jetty.http.HttpStatus;

import spark.Request;
import spark.Response;
import spark.Service;

public class TaskRoutes implements Routes {

    public static final String BASE = "/task";
    private final TaskManager taskManager;
    private final JsonTransformer jsonTransformer;
    private final JsonExtractor<TaskFilter> jsonExtractor;

    @Inject
    public TaskRoutes(TaskManager taskManager, JsonTransformer jsonTransformer) {
        this.taskManager = taskManager;
        this.jsonTransformer = jsonTransformer;
        this.jsonExtractor = new JsonExtractor<>(TaskFilter.class);
    }

    @Override
    public void define(Service service) {
        service.get(BASE + "/:id", this::getStatus, jsonTransformer);

        service.get(BASE + "/:id/await", this::await, jsonTransformer);

        service.delete(BASE + "/:id", this::cancel, jsonTransformer);

        service.post(BASE, this::list, jsonTransformer);
    }

    private Object list(Request req, Response response) throws org.apache.james.webadmin.utils.JsonExtractException {
        try {
            if (req.body().isEmpty()) {
                return StatusReportDto.from(taskManager.list());
            }
            TaskFilter taskFilter = jsonExtractor.parse(req.body());
            return StatusReportDto.from(
                taskFilter.getStatus()
                    .map(taskManager::list)
                    .orElse(taskManager.list()));
        } catch (JsonExtractException e) {
            throw ErrorResponder.builder()
                .statusCode(HttpStatus.BAD_REQUEST_400)
                .type(ErrorResponder.ErrorType.INVALID_ARGUMENT)
                .cause(e)
                .message("Invalid JSON data")
                .haltError();
        }
    }

    private Object getStatus(Request req, Response response) {
        Task.TaskId taskId = getTaskId(req);
        TaskManager.Status status = taskManager.getStatus(taskId);
        return respondStatus(taskId, status);
    }

    private Object await(Request req, Response response) {
        Task.TaskId taskId = getTaskId(req);
        TaskManager.Status status = taskManager.await(taskId);
        return respondStatus(taskId, status);
    }

    private Object respondStatus(Task.TaskId taskId, TaskManager.Status status) {
        if (status == TaskManager.Status.UNKNOWN) {
            throw ErrorResponder.builder()
                .message(String.format("%s can not be found", taskId.getValue()))
                .statusCode(HttpStatus.NOT_FOUND_404)
                .type(ErrorResponder.ErrorType.NOT_FOUND)
                .haltError();
        }
        return StatusResponseDto.from(status);
    }

    private Object cancel(Request req, Response response) {
        Task.TaskId taskId = getTaskId(req);
        taskManager.cancel(taskId);
        return Constants.EMPTY_BODY;
    }

    private Task.TaskId getTaskId(Request req) {
        try {
            String id = req.params("id");
            return new Task.TaskId(UUID.fromString(id));
        } catch (Exception e) {
            throw ErrorResponder.builder()
                .statusCode(HttpStatus.BAD_REQUEST_400)
                .cause(e)
                .type(ErrorResponder.ErrorType.INVALID_ARGUMENT)
                .message("Invalid task id")
                .haltError();
        }
    }
}
