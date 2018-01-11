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

import static org.apache.james.queue.api.MailQueueFixture.createMimeMessage;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import javax.mail.MessagingException;

import org.apache.mailet.Mail;
import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.FakeMail;
import org.junit.jupiter.api.Test;

public interface PriorityManageableMailQueueContract {

    ManageableMailQueue getManageableMailQueue();

    @Test
    default void browseShouldBeOrderedByPriority() throws Exception {
        getManageableMailQueue().enQueue(mailBuilder()
            .name("priority3")
            .attribute(MailPrioritySupport.MAIL_PRIORITY, 3)
            .build());

        getManageableMailQueue().enQueue(mailBuilder()
            .name("priority9")
            .attribute(MailPrioritySupport.MAIL_PRIORITY, 9)
            .build());

        getManageableMailQueue().enQueue(mailBuilder()
            .name("priority1")
            .attribute(MailPrioritySupport.MAIL_PRIORITY, 1)
            .build());

        getManageableMailQueue().enQueue(mailBuilder()
            .name("priority8")
            .attribute(MailPrioritySupport.MAIL_PRIORITY, 8)
            .build());

        getManageableMailQueue().enQueue(mailBuilder()
            .name("priority6")
            .attribute(MailPrioritySupport.MAIL_PRIORITY, 6)
            .build());

        getManageableMailQueue().enQueue(mailBuilder()
            .name("priority0")
            .attribute(MailPrioritySupport.MAIL_PRIORITY, 0)
            .build());

        getManageableMailQueue().enQueue(mailBuilder()
            .name("priority7")
            .attribute(MailPrioritySupport.MAIL_PRIORITY, 7)
            .build());

        getManageableMailQueue().enQueue(mailBuilder()
            .name("priority4")
            .attribute(MailPrioritySupport.MAIL_PRIORITY, 4)
            .build());

        getManageableMailQueue().enQueue(mailBuilder()
            .name("priority2")
            .attribute(MailPrioritySupport.MAIL_PRIORITY, 2)
            .build());

        getManageableMailQueue().enQueue(mailBuilder()
            .name("priority5")
            .attribute(MailPrioritySupport.MAIL_PRIORITY, 5)
            .build());

        assertThat(getManageableMailQueue().browse())
            .extracting(ManageableMailQueue.MailQueueItemView::getMail)
            .extracting(Mail::getName)
            .containsExactly("priority9", "priority8", "priority7", "priority6", "priority5", "priority4", "priority3", "priority2", "priority1", "priority0");
    }

    static FakeMail.Builder mailBuilder() throws MessagingException {
        return FakeMail.builder()
            .mimeMessage(createMimeMessage())
            .sender(MailAddressFixture.OTHER_AT_LOCAL)
            .recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.OTHER_AT_JAMES)
            .lastUpdated(new Date());
    }
}
