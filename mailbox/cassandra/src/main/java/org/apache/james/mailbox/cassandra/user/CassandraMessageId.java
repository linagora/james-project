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
package org.apache.james.mailbox.cassandra.user;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.base.Preconditions;
import org.apache.james.mailbox.store.mail.model.MessageId;

import java.util.Objects;
import java.util.UUID;

public class CassandraMessageId implements MessageId {

    public static CassandraMessageId create() {
        return new CassandraMessageId(UUIDs.timeBased());
    }

    public static CassandraMessageId of(UUID timeUuid) {
        Preconditions.checkNotNull(timeUuid);
        Preconditions.checkArgument(isTimeUuid(timeUuid));
        return new CassandraMessageId(timeUuid);
    }

    private static boolean isTimeUuid(UUID timeUuid) {
        return timeUuid.version() == 1;
    }

    private final UUID timeUuid;

    private CassandraMessageId(UUID timeUuid) {
        this.timeUuid = timeUuid;
    }

    public UUID getTimeUuid() {
        return timeUuid;
    }

    @Override
    public String serialize() {
        return timeUuid.toString();
    }
    
    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof CassandraMessageId) {
            return Objects.equals(this.timeUuid, ((CassandraMessageId) obj).timeUuid);
        }
        return false;
    }
    
    @Override
    public final int hashCode() {
        return Objects.hash(this.timeUuid);
    }
}
