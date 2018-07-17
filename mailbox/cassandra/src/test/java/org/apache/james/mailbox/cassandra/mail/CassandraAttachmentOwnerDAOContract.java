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

package org.apache.james.mailbox.cassandra.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import org.apache.james.mailbox.model.AttachmentId;
import org.apache.james.mailbox.store.mail.model.Username;
import org.junit.jupiter.api.Test;

public interface CassandraAttachmentOwnerDAOContract {
    AttachmentId ATTACHMENT_ID = AttachmentId.from("id1");
    Username OWNER_1 = Username.fromRawValue("owner1");
    Username OWNER_2 = Username.fromRawValue("owner2");

    CassandraAttachmentOwnerDAO testee();

    @Test
    default void retrieveOwnersShouldReturnEmptyByDefault() {
        assertThat(testee().retrieveOwners(ATTACHMENT_ID).join())
            .isEmpty();
    }

    @Test
    default void retrieveOwnersShouldReturnAddedOwner() {
        testee().addOwner(ATTACHMENT_ID, OWNER_1).join();

        assertThat(testee().retrieveOwners(ATTACHMENT_ID).join())
            .containsOnly(OWNER_1);
    }

    @Test
    default void retrieveOwnersShouldReturnAddedOwners() {
        testee().addOwner(ATTACHMENT_ID, OWNER_1).join();
        testee().addOwner(ATTACHMENT_ID, OWNER_2).join();

        assertThat(testee().retrieveOwners(ATTACHMENT_ID).join())
            .containsOnly(OWNER_1, OWNER_2);
    }

    @Test
    default void retrieveOwnersShouldNotThrowWhenMoreReferencesThanPaging() {
        int referenceCountExceedingPaging = 5050;
        IntStream.range(0, referenceCountExceedingPaging)
            .boxed()
            .forEach(i -> testee().addOwner(ATTACHMENT_ID, Username.fromRawValue("owner" + i)).join());

        assertThat(testee().retrieveOwners(ATTACHMENT_ID).join())
            .hasSize(referenceCountExceedingPaging);
    }
}