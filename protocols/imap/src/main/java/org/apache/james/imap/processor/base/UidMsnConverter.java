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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.apache.james.mailbox.MessageUid;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

public class UidMsnConverter {

    public final static int FIRST_MSN = 1;

    private final ArrayList<MessageUid> uids;

    public UidMsnConverter(Iterator<MessageUid> iterator) {
        uids = Lists.newArrayList(iterator);
        Collections.sort(uids);
    }

    public synchronized Optional<Integer> getMsn(MessageUid uid) {
        if (!uids.contains(uid)) {
            return Optional.absent();
        }
        int position = Ordering.explicit(uids).binarySearch(uids, uid);
        return Optional.of(position + 1);
    }

    public synchronized Optional<MessageUid> getUid(int msn) {
        if (msn <= uids.size() && msn > 0) {
            return Optional.of(uids.get(msn - 1));
        }
        return Optional.absent();
    }

    public synchronized Optional<MessageUid> getLastUid() {
        if (uids.isEmpty()) {
            return Optional.absent();
        }
        return getUid(getLastMsn());
    }

    public synchronized Optional<MessageUid> getFirstUid() {
        return getUid(FIRST_MSN);
    }

    public synchronized int getNumMessage() {
        return uids.size();
    }

    public synchronized void remove(MessageUid uid) {
        uids.remove(uid);
    }

    public synchronized boolean isEmpty() {
        return uids.isEmpty();
    }

    public synchronized void clear() {
        uids.clear();
    }

    public synchronized void addUid(MessageUid uid) {
        if (uids.contains(uid)) {
            return;
        }
        if (isLastUid(uid)) {
            uids.add(uid);
        } else {
            uids.add(uid);
            Collections.sort(uids);
        }
    }

    private boolean isLastUid(MessageUid uid) {
        Optional<MessageUid> lastUid = getLastUid();
        return !lastUid.isPresent() ||
            lastUid.get().compareTo(uid) < 0;
    }

    @VisibleForTesting
    ImmutableBiMap<Integer, MessageUid> getConvertion() {
        ImmutableBiMap.Builder<Integer, MessageUid> result = ImmutableBiMap.builder();
        for (int i = 0; i < uids.size(); i++) {
            result.put(i + 1, uids.get(i));
        }
        return result.build();
    }

    private int getLastMsn() {
        return getNumMessage();
    }
}
