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

package org.apache.james.queue.api;

import org.apache.james.queue.api.MailQueue.MailQueueException;
import org.apache.james.queue.api.MailQueue.MailQueueItem;
import org.apache.james.queue.api.MailQueueActionProvider.MailQueueAction;
import org.apache.mailet.Mail;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class ComposedMailQueueItem implements MailQueueItem, MailQueueAction {
    private final MailQueueItem mailQueueItem;
    private final MailQueueAction mailQueueAction;
    
    public static Builder builder(MailQueueItem mailQueueItem) {
        return new Builder(mailQueueItem);
    }
    
    public static class Builder {
        private MailQueueItem mailQueueItem;
        private Optional<MailQueueAction> mailQueueAction = Optional.absent();

        public Builder(MailQueueItem mailQueueItem) {
            Preconditions.checkNotNull(mailQueueItem);
            this.mailQueueItem = mailQueueItem;
        }
        
        public Builder composeWithNextAction(MailQueueAction mailQueueAction) {
            if (this.mailQueueAction.isPresent()) {
                throw new IllegalStateException("Only once mailQueueAction can be composed");
            }
            this.mailQueueAction = Optional.of(mailQueueAction);
            return this;
        }
        
        public ComposedMailQueueItem build() {
            return new ComposedMailQueueItem(mailQueueItem, mailQueueAction.or(MailQueueActionProvider.NOOP));
        }
    }

    @VisibleForTesting ComposedMailQueueItem(MailQueueItem mailQueueItem, MailQueueAction mailQueueAction) {
        this.mailQueueItem = mailQueueItem;
        this.mailQueueAction = mailQueueAction;
    }

    @Override
    public Mail getMail() {
        return mailQueueItem.getMail();
    }

    @Override
    public void done(boolean success) throws MailQueueException {
        mailQueueItem.done(success);
        mailQueueAction.done(success);
    }

}