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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;

import org.apache.james.queue.api.MailQueue.MailQueueItem;
import org.apache.james.queue.api.MailQueueActionProvider.MailQueueAction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

public class ComposedMailQueueItemTest {
    private MailQueueItem mailQueueItem;
    private MailQueueAction mailQueueAction;

    @Before
    public void init() {
        mailQueueItem = mock(MailQueueItem.class);
        mailQueueAction = mock(MailQueueAction.class);
    }

    @Test(expected=NullPointerException.class)
    public void builderShouldThrowWhenNullMailQueueItem() {
        ComposedMailQueueItem.builder(null);
    }

    @Test(expected=NullPointerException.class)
    public void composeWithNextActionShouldThrowWhenNullMailQueueAction() {
        ComposedMailQueueItem.builder(mailQueueItem)
            .composeWithNextAction(null);
    }

    @Test(expected=IllegalStateException.class)
    public void composeWithNextActionShouldThrowWhenCalledTwice() {
        ComposedMailQueueItem.builder(mailQueueItem)
            .composeWithNextAction(mailQueueAction)
            .composeWithNextAction(mailQueueAction);
    }

    @Test
    public void buildShouldWorkWithoutNextActionAndProvideNOOP() {
        ComposedMailQueueItem expected = new ComposedMailQueueItem(mailQueueItem, MailQueueActionProvider.NOOP);
        
        ComposedMailQueueItem actual = ComposedMailQueueItem.builder(mailQueueItem)
            .build();
        
        assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void buildShouldWork() {
        ComposedMailQueueItem expected = new ComposedMailQueueItem(mailQueueItem, mailQueueAction);
        
        ComposedMailQueueItem actual = ComposedMailQueueItem.builder(mailQueueItem)
            .composeWithNextAction(mailQueueAction)
            .build();
        
        assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void doneShouldCallItemDoneBeforeActionDone() throws Exception {
        ComposedMailQueueItem testee = ComposedMailQueueItem.builder(mailQueueItem)
                .composeWithNextAction(mailQueueAction)
                .build();
        InOrder inOrder = inOrder(mailQueueItem, mailQueueAction);
        
        testee.done(true);
        
        inOrder.verify(mailQueueItem).done(true);
        inOrder.verify(mailQueueAction).done(true);
    }
}
