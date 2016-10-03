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

package org.apache.james.mailbox.cassandra;

import java.util.UUID;

import org.apache.james.mailbox.model.MessageId;

public class CassandraMessageId implements MessageId {

    public static CassandraMessageId of(UUID uuid) {
        return new CassandraMessageId(uuid);
    }
    
    public static CassandraMessageId of(String serialized) {
        return of(UUID.fromString(serialized));
    }

    private UUID uuid;
    
    private CassandraMessageId(UUID uuid) {
        this.uuid = uuid;
    }
    
    @Override
    public String serialize() {
        return uuid.toString();
    }
    
}
