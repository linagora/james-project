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

package org.apache.james.mailbox.cassandra.mail.task;

import java.io.Closeable;
import java.io.IOException;

import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.task.Task;
import org.apache.james.util.MDCBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

public class MailboxMergingTask implements Task {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailboxMergingTask.class);
    public static final String MAILBOX_MERGING = "mailboxMerging";
    public static final String OLD_MAILBOX_ID = "oldMailboxId";
    public static final String NEW_MAILBOX_ID = "newMailboxId";

    private final MailboxMergingTaskRunner taskRunner;
    private final CassandraId oldMailboxId;
    private final CassandraId newMailboxId;

    public MailboxMergingTask(MailboxMergingTaskRunner taskRunner, CassandraId oldMailboxId, CassandraId newMailboxId) {
        this.taskRunner = taskRunner;
        this.oldMailboxId = oldMailboxId;
        this.newMailboxId = newMailboxId;
    }

    @Override
    public Result run() {
        TaskId taskId = Task.generateTaskId();
        try (Closeable mdc = MDCBuilder.create()
                 .addContext(TASK_ID, taskId)
                 .addContext(TASK_TYPE, MAILBOX_MERGING)
                 .addContext(OLD_MAILBOX_ID, oldMailboxId)
                 .addContext(NEW_MAILBOX_ID, newMailboxId)
                .build()) {
            return doRun();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private Result doRun() {
        try {
            return taskRunner.run(oldMailboxId, newMailboxId)
                .onComplete(() -> LOGGER.info("Mailbox merging succeeded"));
        } catch (MailboxException e) {
            LOGGER.warn("Mailbox merging failed", e);
            return Result.PARTIAL;
        }
    }
}
