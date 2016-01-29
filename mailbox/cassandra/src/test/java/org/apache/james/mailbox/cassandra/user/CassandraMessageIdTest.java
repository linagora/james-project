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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class CassandraMessageIdTest {

    @Test
    public void createShouldReturnTimeUuidBasedObject() throws Exception {
        CassandraMessageId actual = CassandraMessageId.create();
        assertThat(actual).isNotNull();
        assertThat(actual.getTimeUuid().version()).isEqualTo(1);
    }

    @Test
    public void createShouldReturnUniqueValue() throws Exception {
        CassandraMessageId id = CassandraMessageId.create();
        CassandraMessageId actual = CassandraMessageId.create();
        assertThat(actual).isNotEqualTo(id);
    }
    
    @Test
    public void messageIdShouldRespectBeanContract() {
        EqualsVerifier.forClass(CassandraMessageId.class).verify();
    }
    

    @Test(expected=NullPointerException.class)
    public void ofShouldThrowWhenNullUuid() {
        CassandraMessageId.of(null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void ofShouldThrowWhenUuidIsNotTimedBased() {
        CassandraMessageId.of(UUID.randomUUID());
    }

    @Test
    public void getTimeUuidShouldReturnUnderlyingUuid() throws Exception {
        UUID timeUuid = UUID.fromString("8c8de280-baf9-11e5-ab09-235749ac29ff");
        assertThat(CassandraMessageId.of(timeUuid).getTimeUuid()).isEqualTo(timeUuid);
    }

    @Test
    public void serializeShouldReturnUuidAsString() throws Exception {
        String uuidAsString = "8c8de280-baf9-11e5-ab09-235749ac29ff";
        UUID timeUuid = UUID.fromString(uuidAsString);
        assertThat(CassandraMessageId.of(timeUuid).serialize()).isEqualTo(uuidAsString);

    }
}