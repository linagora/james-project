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

package org.apache.james.imap.processor.base;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;

import org.apache.james.mailbox.MessageUid;

public class UidMsnMapper {

    public final static int FIRST_MSN = 1;

    private final HashBiMap<Integer, MessageUid> msnToUid;

    public UidMsnMapper() {
        msnToUid = HashBiMap.create();
    }

    public Optional<Integer> getMsn(MessageUid uid) {
        return Optional.fromNullable(msnToUid.inverse().get(uid));
    }

    public Optional<MessageUid> getUid(int msn) {
        return Optional.fromNullable(msnToUid.get(msn));
    }

    public Optional<MessageUid> getLastUid() {
        return getUid(getLastMsn());
    }

    public Optional<MessageUid> getFirstUid() {
        return getUid(FIRST_MSN);
    }

    public int getNumMessage() {
        return msnToUid.size();
    }

    public synchronized void remove(MessageUid uid) {
        int msn = getMsn(uid).get();
        msnToUid.remove(msn);

        for (int aMsn = msn + 1; aMsn <= getNumMessage() + 1; aMsn++) {
            MessageUid aUid = msnToUid.remove(aMsn);
            addMapping(aMsn - 1, aUid);
        }
    }

    public boolean isEmpty() {
        return msnToUid.isEmpty();
    }

    public synchronized void clear() {
        msnToUid.clear();
    }

    public void addUid(MessageUid uid) {
        this.addMapping(nextMsn(), uid);
    }

    @VisibleForTesting
    ImmutableBiMap<Integer, MessageUid> getInternals() {
        return ImmutableBiMap.copyOf(msnToUid);
    }

    private synchronized void addMapping(Integer msn, MessageUid uid) {
        if (msnToUid.inverse().get(uid) == null) {
            msnToUid.forcePut(msn, uid);
        }
    }

    private int nextMsn() {
        return getNumMessage() + FIRST_MSN;
    }

    private int getLastMsn() {
        return getNumMessage();
    }
}
