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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

public class MemoryTaskManager implements TaskManager {

    public static final boolean INTERRUPT_IF_RUNNING = true;
    private final ConcurrentHashMap<Task.TaskId, Class<? extends Task>> idToClass;
    private final ConcurrentHashMap<Task.TaskId, Status> idToLastKnownStatus;
    private final ConcurrentHashMap<Task.TaskId, Future> idToFuture;
    private final ExecutorService executor;

    public MemoryTaskManager() {
        idToClass = new ConcurrentHashMap<>();
        idToLastKnownStatus = new ConcurrentHashMap<>();
        idToFuture = new ConcurrentHashMap<>();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public Task.TaskId submit(Task task) {
        return submit(task, id -> {});
    }

    @VisibleForTesting
    Task.TaskId submit(Task task, Consumer<Task.TaskId> callback) {
        Task.TaskId taskId = Task.generateTaskId();

        idToClass.put(taskId, task.getClass());
        idToLastKnownStatus.put(taskId, Status.WAITING);
        idToFuture.put(taskId,
            executor.submit(() -> run(taskId, task, callback)));
        return taskId;
    }

    private void run(Task.TaskId taskId, Task task, Consumer<Task.TaskId> callback) {
        idToLastKnownStatus.put(taskId, Status.IN_PROGRESS);
        task.run()
            .onComplete(() -> idToLastKnownStatus.put(taskId, Status.COMPLETED))
            .onFailure(() -> idToLastKnownStatus.put(taskId, Status.FAILED));
        idToFuture.remove(taskId);
        callback.accept(taskId);
    }

    @Override
    public Status getStatus(Task.TaskId id) {
        return Optional.ofNullable(idToLastKnownStatus.get(id))
            .orElse(Status.UNKNOWN);
    }

    @Override
    public List<StatusReport> list() {
        Stream<Map.Entry<Task.TaskId, Status>> stream = idToLastKnownStatus.entrySet()
            .stream();
        return toReport(stream);
    }

    @Override
    public List<StatusReport> list(Status status) {
        return toReport(idToLastKnownStatus.entrySet()
            .stream()
            .filter(entry -> entry.getValue() == status));
    }

    @Override
    public void cancel(Task.TaskId id) {
        Optional.ofNullable(idToFuture.get(id))
            .ifPresent(future -> {
                future.cancel(INTERRUPT_IF_RUNNING);
                idToLastKnownStatus.put(id, Status.CANCELLED);
                idToFuture.remove(id);
            });
    }

    @Override
    public Status await(Task.TaskId id) {
        Optional.ofNullable(idToFuture.get(id))
            .ifPresent(Throwing.consumer(Future::get));
        return getStatus(id);
    }

    private ImmutableList<StatusReport> toReport(Stream<Map.Entry<Task.TaskId, Status>> stream) {
        return stream
            .map(e -> new StatusReport(e.getKey(),
                idToClass.get(e.getKey()),
                e.getValue()))
            .collect(Guavate.toImmutableList());
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
    }
}
