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

package org.apache.james.mailbox.quota.cassandra;

import javax.inject.Inject;

import org.apache.james.core.User;
import org.apache.james.mailbox.quota.QuotaThresholdHistoryStore;
import org.apache.james.mailbox.quota.model.QuotaThresholdChange;
import org.apache.james.mailbox.quota.model.QuotaThresholdHistory;

public class CassandraQuotaThresholdHistoryStore implements QuotaThresholdHistoryStore {
    private final CountChangesDao countChangesDao;
    private final SizeChangesDao sizeChangesDao;

    @Inject
    public CassandraQuotaThresholdHistoryStore(CountChangesDao countChangesDao, SizeChangesDao sizeChangesDao) {
        this.countChangesDao = countChangesDao;
        this.sizeChangesDao = sizeChangesDao;
    }

    @Override
    public QuotaThresholdHistory retrieveQuotaSizeThresholdChanges(User user) {
        return sizeChangesDao.retrieve(user);
    }

    @Override
    public void persistQuotaSizeThresholdChange(User user, QuotaThresholdChange change) {
        sizeChangesDao.add(user, change);
    }

    @Override
    public QuotaThresholdHistory retrieveQuotaCountThresholdChanges(User user) {
        return countChangesDao.retrieve(user);
    }

    @Override
    public void persistQuotaCountThresholdChange(User user, QuotaThresholdChange change) {
        countChangesDao.add(user, change);
    }
}
