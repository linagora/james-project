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

import org.apache.james.core.MailAddress;
import org.apache.james.queue.api.MailQueue.MailQueueException;
import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.queue.api.ManageableMailQueue.Type;
import org.apache.james.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

public class MailQueueSenderDeletionTask implements Task {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailQueueSenderDeletionTask.class);

    private final ManageableMailQueue queue;
    private final MailAddress sender;

    public MailQueueSenderDeletionTask(ManageableMailQueue queue, MailAddress sender) {
        this.queue = queue;
        this.sender = sender;
    }

    @Override
    public Result run() {
        try {
            queue.remove(Type.Sender, sender.asString());
            return Result.COMPLETED;
        } catch (MailQueueException e) {
            LOGGER.error("Fails while deleting mails from the queue", e);
            throw Throwables.propagate(e);
        }
    }

}
