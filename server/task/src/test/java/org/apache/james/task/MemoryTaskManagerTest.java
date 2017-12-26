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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.consumers.ConsumerChainer;
import com.google.common.base.Throwables;

public class MemoryTaskManagerTest {

    private MemoryTaskManager memoryTaskManager;

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Before
    public void setUp() {
        memoryTaskManager = new MemoryTaskManager();
    }

    @After
    public void tearDown() {
        memoryTaskManager.stop();
    }

    @Test
    public void getStatusShouldReturnUnknownWhenUnknownId() {
        Task.TaskId unknownId = Task.generateTaskId();
        assertThat(memoryTaskManager.getStatus(unknownId))
            .isEqualTo(TaskManager.Status.UNKNOWN);
    }

    @Test
    public void getStatusShouldReturnWaitingWhenNotYetProcessed() {
        CountDownLatch task1Latch = new CountDownLatch(1);

        memoryTaskManager.submit(() -> {
            await(task1Latch);
            return Task.Result.COMPLETED;
        });

        Task.TaskId taskId = memoryTaskManager.submit(() -> Task.Result.COMPLETED);

        assertThat(memoryTaskManager.getStatus(taskId))
            .isEqualTo(TaskManager.Status.WAITING);
    }

    @Test
    public void taskCodeAfterCancelIsNotRun() {
        CountDownLatch task1Latch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger(0);

        Task.TaskId id = memoryTaskManager.submit(() -> {
            await(task1Latch);
            count.incrementAndGet();
            return Task.Result.COMPLETED;
        });

        memoryTaskManager.cancel(id);

        assertThat(count.get()).isEqualTo(0);
    }

    @Test
    public void getStatusShouldReturnCancelledWhenCancelled() {
        CountDownLatch task1Latch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger(0);

        Task.TaskId id = memoryTaskManager.submit(() -> {
            await(task1Latch);
            count.incrementAndGet();
            return Task.Result.COMPLETED;
        });

        memoryTaskManager.cancel(id);

        assertThat(memoryTaskManager.getStatus(id))
            .isEqualTo(TaskManager.Status.CANCELLED);
    }

    @Test
    public void getStatusShouldReturnInProgressWhenProcessingIsInProgress() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        Task.TaskId taskId = memoryTaskManager.submit(() -> {
            await(latch1);
            latch2.countDown();
            return Task.Result.COMPLETED;
        });
        latch1.countDown();
        latch2.await();

        assertThat(memoryTaskManager.getStatus(taskId))
            .isEqualTo(TaskManager.Status.IN_PROGRESS);
    }

    @Test
    public void getStatusShouldReturnCompletedWhenRunSuccessfully() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Task.TaskId taskId = memoryTaskManager.submit(
            () -> Task.Result.COMPLETED,
            countDownCallback(latch));

        latch.await();

        assertThat(memoryTaskManager.getStatus(taskId))
            .isEqualTo(TaskManager.Status.COMPLETED);
    }

    @Test
    public void getStatusShouldReturnFailedWhenRunPartially() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Task.TaskId taskId = memoryTaskManager.submit(
            () -> Task.Result.PARTIAL,
            countDownCallback(latch));

        latch.await();

        assertThat(memoryTaskManager.getStatus(taskId))
            .isEqualTo(TaskManager.Status.FAILED);
    }

    private ConsumerChainer<Task.TaskId> countDownCallback(CountDownLatch latch) {
        return Throwing.consumer(id -> latch.countDown());
    }

    @Test
    public void listShouldReturnTaskSatus() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch latch3 = new CountDownLatch(1);

        Task.TaskId failedId = memoryTaskManager.submit(
            () -> Task.Result.PARTIAL);
        Task.TaskId successfulId = memoryTaskManager.submit(
            () -> Task.Result.COMPLETED);
        Task.TaskId inProgressId = memoryTaskManager.submit(
            () -> {
                await(latch1);
                latch2.countDown();
                await(latch3);
                return Task.Result.COMPLETED;
            });
        Task.TaskId waitingId = memoryTaskManager.submit(
            () -> {
                await(latch3);
                latch2.countDown();
                return Task.Result.COMPLETED;
            });

        latch1.countDown();
        latch2.await();

        List<TaskManager.StatusReport> list = memoryTaskManager.list();
        softly.assertThat(list).hasSize(4);
        softly.assertThat(entryWithId(list, failedId))
            .isEqualTo(TaskManager.Status.FAILED);
        softly.assertThat(entryWithId(list, waitingId))
            .isEqualTo(TaskManager.Status.WAITING);
        softly.assertThat(entryWithId(list, successfulId))
            .isEqualTo(TaskManager.Status.COMPLETED);
        softly.assertThat(entryWithId(list, inProgressId))
            .isEqualTo(TaskManager.Status.IN_PROGRESS);
    }

    private TaskManager.Status entryWithId(List<TaskManager.StatusReport> list, Task.TaskId inProgressId) {
        return list.stream()
            .filter(e -> e.getTaskId().equals(inProgressId))
            .findFirst().get()
            .getStatus();
    }

    @Test
    public void listShouldAllowToSeeWaitingTasks() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch latch3 = new CountDownLatch(1);

        memoryTaskManager.submit(
            () -> Task.Result.PARTIAL);
        memoryTaskManager.submit(
            () -> Task.Result.COMPLETED);
        memoryTaskManager.submit(
            () -> {
                await(latch1);
                latch2.countDown();
                await(latch3);
                return Task.Result.COMPLETED;
            });
        Task.TaskId waitingId = memoryTaskManager.submit(
            () -> {
                await(latch3);
                latch2.countDown();
                return Task.Result.COMPLETED;
            });

        latch1.countDown();
        latch2.await();

        assertThat(memoryTaskManager.list(TaskManager.Status.WAITING))
            .extracting(TaskManager.StatusReport::getTaskId)
            .containsOnly(waitingId);
    }

    @Test
    public void listShouldAllowToSeeInProgressTasks() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch latch3 = new CountDownLatch(1);

        memoryTaskManager.submit(
            () -> Task.Result.PARTIAL);
        Task.TaskId successfulId = memoryTaskManager.submit(
            () -> Task.Result.COMPLETED);
        memoryTaskManager.submit(
            () -> {
                await(latch1);
                latch2.countDown();
                await(latch3);
                return Task.Result.COMPLETED;
            });
        memoryTaskManager.submit(
            () -> {
                await(latch3);
                latch2.countDown();
                return Task.Result.COMPLETED;
            });

        latch1.countDown();
        latch2.await();

        assertThat(memoryTaskManager.list(TaskManager.Status.COMPLETED))
            .extracting(TaskManager.StatusReport::getTaskId)
            .containsOnly(successfulId);
    }

    @Test
    public void listShouldAllowToSeeFailedTasks() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch latch3 = new CountDownLatch(1);

        Task.TaskId failedId = memoryTaskManager.submit(
            () -> Task.Result.PARTIAL);
        memoryTaskManager.submit(
            () -> Task.Result.COMPLETED);
        memoryTaskManager.submit(
            () -> {
                await(latch1);
                latch2.countDown();
                await(latch3);
                return Task.Result.COMPLETED;
            });
        memoryTaskManager.submit(
            () -> {
                await(latch3);
                latch2.countDown();
                return Task.Result.COMPLETED;
            });

        latch1.countDown();
        latch2.await();

        assertThat(memoryTaskManager.list(TaskManager.Status.FAILED))
            .extracting(TaskManager.StatusReport::getTaskId)
            .containsOnly(failedId);
    }

    @Test
    public void listShouldAllowToSeeSuccessfulTasks() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch latch3 = new CountDownLatch(1);

        memoryTaskManager.submit(
            () -> Task.Result.PARTIAL);
        memoryTaskManager.submit(
            () -> Task.Result.COMPLETED);
        Task.TaskId inProgressId = memoryTaskManager.submit(
            () -> {
                await(latch1);
                latch2.countDown();
                await(latch3);
                return Task.Result.COMPLETED;
            });
        memoryTaskManager.submit(
            () -> {
                await(latch3);
                latch2.countDown();
                return Task.Result.COMPLETED;
            });

        latch1.countDown();
        latch2.await();

        assertThat(memoryTaskManager.list(TaskManager.Status.IN_PROGRESS))
            .extracting(TaskManager.StatusReport::getTaskId)
            .containsOnly(inProgressId);
    }

    @Test
    public void listShouldBeEmptyWhenNoTasks() throws Exception {
        assertThat(memoryTaskManager.list()).isEmpty();
    }

    @Test
    public void listCancelledShouldBeEmptyWhenNoTasks() throws Exception {
        assertThat(memoryTaskManager.list(TaskManager.Status.CANCELLED)).isEmpty();
    }

    @Test
    public void awaitShouldNotThrowWhenCompletedTask() throws Exception {
        Task.TaskId taskId = memoryTaskManager.submit(
            () -> Task.Result.COMPLETED);
        memoryTaskManager.await(taskId);
        memoryTaskManager.await(taskId);
    }

    @Test
    public void submittedTaskShouldExecuteSequentially() {
        ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();

        Task.TaskId id1 = memoryTaskManager.submit(() -> {
            queue.add(1);
            sleep(500);
            queue.add(2);
            return Task.Result.COMPLETED;
        });
        Task.TaskId id2 = memoryTaskManager.submit(() -> {
            queue.add(3);
            sleep(500);
            queue.add(4);
            return Task.Result.COMPLETED;
        });
        memoryTaskManager.await(id1);
        memoryTaskManager.await(id2);

        assertThat(queue)
            .containsExactly(1, 2, 3, 4);
    }

    private void sleep(int sleepDurationInMs) {
        try {
            Thread.sleep(sleepDurationInMs);
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }
}