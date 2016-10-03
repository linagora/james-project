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

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.apache.james.mailbox.cassandra.table.CassandraImapUidTable.FIELDS;
import static org.apache.james.mailbox.cassandra.table.CassandraImapUidTable.TABLE_NAME;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageIds.IMAP_UID;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageIds.MAILBOX_ID;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageIds.MESSAGE_ID;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.model.MessageId;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select.Where;

public class CassandraImapUidDAO {

    private final Session session;

    public CassandraImapUidDAO(Session session) {
        this.session = session;
    }

    public void delete(CassandraMessageId messageId, CassandraId mailboxId) {
        session.execute(QueryBuilder.delete()
                    .from(TABLE_NAME)
                    .where(eq(MESSAGE_ID, messageId.serialize()))
                    .and(eq(MAILBOX_ID, mailboxId.asUuid())));
    }

    public void insert(MessageId messageId, CassandraId mailboxId, MessageUid uid) {
        session.execute(insertInto(TABLE_NAME)
                .value(MESSAGE_ID, messageId.serialize())
                .value(MAILBOX_ID, mailboxId.asUuid())
                .value(IMAP_UID, uid.asLong()));
    }

    public Stream<UniqueMessageId> retrieve(CassandraMessageId messageId, Optional<CassandraId> mailboxId) {
        return CassandraUtils.convertToStream(session.execute(selectStatement(messageId, mailboxId)))
            .map(row -> UniqueMessageId.from(
                CassandraMessageId.of(row.getString(MESSAGE_ID)),
                CassandraId.of(row.getUUID(MAILBOX_ID)),
                MessageUid.of(row.getLong(IMAP_UID))));
    }

    private Where selectStatement(CassandraMessageId messageId, Optional<CassandraId> mailboxId) {
        Where where = select(FIELDS)
            .from(TABLE_NAME)
            .where(eq(MESSAGE_ID, messageId.serialize()));
        return mailboxId
                .map(id -> where.and(eq(MAILBOX_ID, id.asUuid())))
                .orElse(where);
    }
}
