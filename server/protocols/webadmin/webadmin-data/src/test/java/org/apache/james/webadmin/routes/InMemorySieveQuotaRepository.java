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

import org.apache.james.sieverepository.api.SieveQuotaRepository;
import org.apache.james.sieverepository.api.exception.QuotaNotFoundException;
import org.apache.james.sieverepository.api.exception.StorageException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySieveQuotaRepository implements SieveQuotaRepository {

    private boolean isGlobalQuotaSet = false;
    private long globalQuota = 0L;

    private Map<String, Long> userQuota = new ConcurrentHashMap<>();

    @Override
    public boolean hasQuota() throws StorageException {
        return isGlobalQuotaSet;
    }

    @Override
    public long getQuota() throws QuotaNotFoundException, StorageException {
        if (!isGlobalQuotaSet) {
            throw new QuotaNotFoundException();
        }
        return globalQuota;
    }

    @Override
    public void setQuota(final long quota) throws StorageException {
        this.globalQuota = quota;
        this.isGlobalQuotaSet = true;
    }

    @Override
    public void removeQuota() throws QuotaNotFoundException, StorageException {
        if (!isGlobalQuotaSet) {
            throw new QuotaNotFoundException();
        }
        globalQuota = 0L;
        isGlobalQuotaSet = false;
    }

    @Override
    public boolean hasQuota(final String user) throws StorageException {
        return userQuota.containsKey(user);
    }

    @Override
    public long getQuota(final String user) throws QuotaNotFoundException, StorageException {
        final Long quotaValue = userQuota.get(user);
        if (quotaValue == null) {
            throw new QuotaNotFoundException();
        }
        return quotaValue;
    }

    @Override
    public void setQuota(final String user, final long quota) throws StorageException {
        userQuota.put(user, quota);
    }

    @Override
    public void removeQuota(final String user) throws QuotaNotFoundException, StorageException {
        final Long quotaValue = userQuota.get(user);
        if (quotaValue == null) {
            throw new QuotaNotFoundException();
        }
        userQuota.remove(user);
    }
}
