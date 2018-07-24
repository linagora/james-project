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

import java.util.Optional;
import java.util.stream.LongStream;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.fge.lambdas.Throwing;

public interface CassandraUidProviderContract {
    CassandraId CASSANDRA_ID = new CassandraId.Factory().fromString("e22b3ac0-a80b-11e7-bb00-777268d65503");

    int UID_VALIDITY = 1234;
    SimpleMailbox mailbox = new SimpleMailbox(new MailboxPath("gsoc", "ieugen", "Trash"), UID_VALIDITY);

    @BeforeAll
    static void setMailboxId() {
        mailbox.setMailboxId(CASSANDRA_ID);
    }

    CassandraUidProvider uidProvider();

    @Test
    default void lastUidShouldRetrieveValueStoredByNextUid() throws Exception {
        int nbEntries = 100;
        Optional<MessageUid> result = uidProvider().lastUid(null, mailbox);
        assertThat(result).isEmpty();
        LongStream.range(0, nbEntries)
            .forEach(Throwing.longConsumer(value -> {
                        MessageUid uid = uidProvider().nextUid(null, mailbox);
                        assertThat(uid).isEqualTo(uidProvider().lastUid(null, mailbox).get());
                })
            );
    }

    @Test
    default void nextUidShouldIncrementValueByOne() {
        int nbEntries = 100;
        LongStream.range(1, nbEntries)
            .forEach(Throwing.longConsumer(value -> {
                        MessageUid result = uidProvider().nextUid(null, mailbox);
                        assertThat(value).isEqualTo(result.asLong());
                })
            );
    }

    @Test
    default void nextUidShouldGenerateUniqueValuesWhenParallelCalls() {
        int nbEntries = 100;
        long nbValues = LongStream.range(0, nbEntries)
            .parallel()
            .boxed()
            .map(Throwing.function(x -> uidProvider().nextUid(null, mailbox)))
            .distinct()
            .count();
        assertThat(nbValues).isEqualTo(nbEntries);
    }
}
