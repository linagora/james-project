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

import java.util.List;
import java.util.UUID;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.model.MessageRange;
import org.junit.jupiter.api.Test;

import com.github.steveash.guavate.Guavate;

public interface CassandraDeletedMessageDAOContract {
    CassandraId MAILBOX_ID = CassandraId.of(UUID.fromString("110e8400-e29b-11d4-a716-446655440000"));
    MessageUid UID_1 = MessageUid.of(1);
    MessageUid UID_2 = MessageUid.of(2);
    MessageUid UID_3 = MessageUid.of(3);
    MessageUid UID_4 = MessageUid.of(4);
    MessageUid UID_7 = MessageUid.of(7);
    MessageUid UID_8 = MessageUid.of(8);

    CassandraDeletedMessageDAO deletedMessageDAO();

    @Test
    default void retrieveDeletedMessageShouldReturnEmptyByDefault() {
        List<MessageUid> result = deletedMessageDAO()
            .retrieveDeletedMessage(MAILBOX_ID, MessageRange.all())
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(result).isEmpty();
    }

    @Test
    default void addDeletedMessageShouldThenBeReportedAsDeletedMessage() {
        deletedMessageDAO().addDeleted(MAILBOX_ID, UID_1).join();
        deletedMessageDAO().addDeleted(MAILBOX_ID, UID_2).join();

        List<MessageUid> result = deletedMessageDAO().retrieveDeletedMessage(MAILBOX_ID, MessageRange.all())
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(result).containsExactly(UID_1, UID_2);
    }

    @Test
    default void addDeletedMessageShouldBeIdempotent() {
        deletedMessageDAO().addDeleted(MAILBOX_ID, UID_1).join();
        deletedMessageDAO().addDeleted(MAILBOX_ID, UID_1).join();

        List<MessageUid> result = deletedMessageDAO().retrieveDeletedMessage(MAILBOX_ID, MessageRange.all())
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(result).containsExactly(UID_1);
    }


    @Test
    default void removeUnreadShouldReturnEmptyWhenNoData() {
        deletedMessageDAO().removeDeleted(MAILBOX_ID, UID_1).join();

        List<MessageUid> result = deletedMessageDAO()
            .retrieveDeletedMessage(MAILBOX_ID, MessageRange.all())
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(result).isEmpty();
    }

    @Test
    default void removeDeletedMessageShouldNotAffectOtherMessage() {
        deletedMessageDAO().addDeleted(MAILBOX_ID, UID_2).join();
        deletedMessageDAO().addDeleted(MAILBOX_ID, UID_1).join();

        deletedMessageDAO().removeDeleted(MAILBOX_ID, UID_1).join();

        List<MessageUid> result = deletedMessageDAO()
            .retrieveDeletedMessage(MAILBOX_ID, MessageRange.all())
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(result).containsExactly(UID_2);
    }

    @Test
    default void removeDeletedShouldRemoveSpecifiedUID() {
        deletedMessageDAO().addDeleted(MAILBOX_ID, UID_2).join();

        deletedMessageDAO().removeDeleted(MAILBOX_ID, UID_2).join();

        List<MessageUid> result = deletedMessageDAO()
            .retrieveDeletedMessage(MAILBOX_ID, MessageRange.all())
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(result).isEmpty();
    }

    default void addMessageForRetrieveTest() {
        deletedMessageDAO().addDeleted(MAILBOX_ID, UID_1).join();
        deletedMessageDAO().addDeleted(MAILBOX_ID, UID_2).join();
        deletedMessageDAO().addDeleted(MAILBOX_ID, UID_3).join();
        deletedMessageDAO().addDeleted(MAILBOX_ID, UID_4).join();
        deletedMessageDAO().addDeleted(MAILBOX_ID, UID_7).join();
        deletedMessageDAO().addDeleted(MAILBOX_ID, UID_8).join();
    }

    @Test
    default void retrieveDeletedMessageShouldReturnAllMessageForMessageRangeAll() {
        addMessageForRetrieveTest();

        List<MessageUid> result = deletedMessageDAO()
            .retrieveDeletedMessage(MAILBOX_ID, MessageRange.all())
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(result).containsExactly(UID_1, UID_2, UID_3, UID_4, UID_7, UID_8);
    }

    @Test
    default void retrieveDeletedMessageShouldReturnOneMessageForMessageRangeOneIfThisMessageIsPresent() {
        addMessageForRetrieveTest();

        List<MessageUid> result = deletedMessageDAO()
            .retrieveDeletedMessage(MAILBOX_ID, MessageRange.one(UID_1))
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(result).containsExactly(UID_1);
    }

    @Test
    default void retrieveDeletedMessageShouldReturnNoMessageForMessageRangeOneIfThisMessageIsNotPresent() {
        addMessageForRetrieveTest();

        List<MessageUid> result = deletedMessageDAO()
            .retrieveDeletedMessage(MAILBOX_ID, MessageRange.one(MessageUid.of(42)))
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(result).isEmpty();
    }

    @Test
    default void retrieveDeletedMessageShouldReturnMessageInRangeForMessageRangeRange() {
        addMessageForRetrieveTest();

        List<MessageUid> result = deletedMessageDAO()
            .retrieveDeletedMessage(MAILBOX_ID, MessageRange.range(MessageUid.of(3), MessageUid.of(7)))
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(result).containsExactly(UID_3, UID_4, UID_7);
    }

    @Test
    default void retrieveDeletedMessageShouldReturnNoMessageForMessageRangeRangeIfNoDeletedMessageInThatRange() {
        addMessageForRetrieveTest();

        List<MessageUid> result = deletedMessageDAO()
            .retrieveDeletedMessage(MAILBOX_ID, MessageRange.range(MessageUid.of(5), MessageUid.of(6)))
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(result).isEmpty();
    }

    @Test
    default void retrieveDeletedMessageShouldReturnNoMessageForMessageRangeFromIfNoDeletedMessageWithIdBiggerOrSameThanFrom() {
        addMessageForRetrieveTest();

        List<MessageUid> result = deletedMessageDAO()
            .retrieveDeletedMessage(MAILBOX_ID, MessageRange.from(MessageUid.of(9)))
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(result).isEmpty();
    }

    @Test
    default void retrieveDeletedMessageShouldReturnDeletedMessageWithIdBiggerOrSameThanFrom() {
        addMessageForRetrieveTest();

        List<MessageUid> result = deletedMessageDAO()
            .retrieveDeletedMessage(MAILBOX_ID, MessageRange.from(MessageUid.of(4)))
            .join()
            .collect(Guavate.toImmutableList());

        assertThat(result).containsExactly(UID_4, UID_7, UID_8);
    }
}
