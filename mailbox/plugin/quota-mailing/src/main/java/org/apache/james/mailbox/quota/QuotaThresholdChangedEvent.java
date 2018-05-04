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

package org.apache.james.mailbox.quota;

import org.apache.james.mailbox.Event;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.model.Quota;

public class QuotaThresholdChangedEvent implements Event {
    private final HistoryEvolution sizeHistoryEvolution;
    private final HistoryEvolution countHistoryEvolution;
    private final Quota<QuotaSize> sizeQuota;
    private final Quota<QuotaCount> countQuota;
    private final MailboxSession session;

    public QuotaThresholdChangedEvent(HistoryEvolution sizeHistoryEvolution, HistoryEvolution countHistoryEvolution, Quota<QuotaSize> sizeQuota, Quota<QuotaCount> countQuota, MailboxSession session) {
        this.sizeHistoryEvolution = sizeHistoryEvolution;
        this.countHistoryEvolution = countHistoryEvolution;
        this.sizeQuota = sizeQuota;
        this.countQuota = countQuota;
        this.session = session;
    }

    @Override
    public MailboxSession getSession() {
        return session;
    }

    public HistoryEvolution getSizeHistoryEvolution() {
        return sizeHistoryEvolution;
    }

    public HistoryEvolution getCountHistoryEvolution() {
        return countHistoryEvolution;
    }

    public Quota<QuotaSize> getSizeQuota() {
        return sizeQuota;
    }

    public Quota<QuotaCount> getCountQuota() {
        return countQuota;
    }
}
