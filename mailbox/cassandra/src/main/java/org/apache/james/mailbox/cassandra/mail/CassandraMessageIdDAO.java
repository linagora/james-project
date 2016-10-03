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
import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.lte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageIdTable.FIELDS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageIdTable.TABLE_NAME;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageIds.IMAP_UID;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageIds.MAILBOX_ID;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageIds.MESSAGE_ID;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select.Where;

public class CassandraMessageIdDAO {

    private final Session session;

    public CassandraMessageIdDAO(Session session) {
        this.session = session;
    }

    public void delete(CassandraId mailboxId, MessageUid uid) {
        session.execute(QueryBuilder.delete()
                    .from(TABLE_NAME)
                    .where(eq(MAILBOX_ID, mailboxId.asUuid()))
                    .and(eq(IMAP_UID, uid.asLong())));
    }

    public void insert(CassandraId mailboxId, MessageUid uid, MessageId messageId) {
        session.execute(insertInto(TABLE_NAME)
                .value(MAILBOX_ID, mailboxId.asUuid())
                .value(IMAP_UID, uid.asLong())
                .value(MESSAGE_ID, messageId.serialize()));
    }

    public Stream<CassandraMessageId> retrieve(CassandraId mailboxId, MessageUid uid) {
        return CassandraUtils.convertToStream(session.execute(
                select(MESSAGE_ID)
                    .from(TABLE_NAME)
                    .where(eq(MAILBOX_ID, mailboxId.asUuid()))
                    .and(eq(IMAP_UID, uid.asLong()))))
            .map(row -> row.getString(MESSAGE_ID))
            .map(CassandraMessageId::of);
    }

    public List<CassandraMessageId> retrieveMessageIds(CassandraId mailboxId, MessageRange set, FetchType fetchType) {
        switch (set.getType()) {
        case ALL:
            return toMessageIds(session.execute(selectAll(mailboxId)));
        case FROM:
            return toMessageIds(session.execute(selectFrom(mailboxId, set.getUidFrom())));
        case RANGE:
            return toMessageIds(session.execute(selectRange(mailboxId, set.getUidFrom(), set.getUidTo())));
        case ONE:
            return toMessageIds(session.execute(selectMessage(mailboxId, set.getUidFrom())));
        }
        throw new UnsupportedOperationException();
    }

    private Where selectAll(CassandraId mailboxId) {
        return select(FIELDS)
            .from(TABLE_NAME)
            .where(eq(MAILBOX_ID, mailboxId.asUuid()));
    }

    private Where selectFrom(CassandraId mailboxId, MessageUid uid) {
        return select(FIELDS)
            .from(TABLE_NAME)
            .where(eq(MAILBOX_ID, mailboxId.asUuid()))
            .and(gte(IMAP_UID, uid.asLong()));
    }

    private Where selectRange(CassandraId mailboxId, MessageUid from, MessageUid to) {
        return select(FIELDS)
            .from(TABLE_NAME)
            .where(eq(MAILBOX_ID, mailboxId.asUuid()))
            .and(gte(IMAP_UID, from.asLong()))
            .and(lte(IMAP_UID, to.asLong()));
    }

    private Where selectMessage(CassandraId mailboxId, MessageUid uid) {
        Where where = select(FIELDS)
            .from(TABLE_NAME)
            .where(eq(MAILBOX_ID, mailboxId.asUuid()));
        return Optional.ofNullable(uid)
                .map(id -> where.and(eq(IMAP_UID, id.asLong())))
                .orElse(where);
    }

    private List<CassandraMessageId> toMessageIds(ResultSet resultSet) {
        return CassandraUtils.convertToStream(resultSet)
            .map(row -> CassandraMessageId.of(row.getString(MESSAGE_ID)))
            .collect(Collectors.toList());
    }
}
