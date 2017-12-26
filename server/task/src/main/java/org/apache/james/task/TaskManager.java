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

package org.apache.james.task;

import java.util.Arrays;
import java.util.List;

public interface TaskManager {
    enum Status {
        UNKNOWN("unknown"),
        WAITING("waiting"),
        IN_PROGRESS("inProgress"),
        COMPLETED("completed"),
        CANCELLED("canceled"),
        FAILED("failed");

        public static Status fromString(String value) {
            return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Unknown status value '%s'", value)));
        }

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    class StatusReport {
        private final Task.TaskId taskId;
        private final Class<? extends Task> clazz;
        private final Status status;

        public StatusReport(Task.TaskId taskId, Class<? extends Task> clazz, Status status) {
            this.taskId = taskId;
            this.clazz = clazz;
            this.status = status;
        }

        public Task.TaskId getTaskId() {
            return taskId;
        }

        public Status getStatus() {
            return status;
        }

        public Class<? extends Task> getClazz() {
            return clazz;
        }
    }

    Task.TaskId submit(Task task);

    Status getStatus(Task.TaskId id);

    List<StatusReport> list();

    List<StatusReport> list(Status status);

    void cancel(Task.TaskId id);

    Status await(Task.TaskId id);
}
