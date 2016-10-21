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

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageIds.IMAP_UID;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageIds.MAILBOX_ID;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageIds.MESSAGE_ID;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.ANSWERED;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.DELETED;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.DRAFT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.FLAGGED;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.RECENT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.SEEN;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.USER;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.USER_FLAGS;
import static org.apache.james.mailbox.cassandra.table.MessageIdToImapUid.FIELDS;
import static org.apache.james.mailbox.cassandra.table.MessageIdToImapUid.TABLE_NAME;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.ComposedMessageIdWithFlags;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.collect.ImmutableSet;

public class CassandraMessageIdToImapUidDAO {

    private final CassandraAsyncExecutor cassandraAsyncExecutor;
    private final PreparedStatement delete;
    private final PreparedStatement insert;
    private final PreparedStatement selectAll;
    private final PreparedStatement select;

    @Inject
    public CassandraMessageIdToImapUidDAO(Session session) {
        this.cassandraAsyncExecutor = new CassandraAsyncExecutor(session);
        this.delete = prepareDelete(session);
        this.insert = prepareInsert(session);
        this.selectAll = prepareSelectAll(session);
        this.select = prepareSelect(session);
    }

    private PreparedStatement prepareDelete(Session session) {
        return session.prepare(QueryBuilder.delete()
                .from(TABLE_NAME)
                .where(eq(MESSAGE_ID, bindMarker(MESSAGE_ID)))
                .and(eq(MAILBOX_ID, bindMarker(MAILBOX_ID))));
    }

    private PreparedStatement prepareInsert(Session session) {
        return session.prepare(insertInto(TABLE_NAME)
                .value(MESSAGE_ID, bindMarker(MESSAGE_ID))
                .value(MAILBOX_ID, bindMarker(MAILBOX_ID))
                .value(IMAP_UID, bindMarker(IMAP_UID))
                .value(ANSWERED, bindMarker(ANSWERED))
                .value(DELETED, bindMarker(DELETED))
                .value(DRAFT, bindMarker(DRAFT))
                .value(FLAGGED, bindMarker(FLAGGED))
                .value(RECENT, bindMarker(RECENT))
                .value(SEEN, bindMarker(SEEN))
                .value(USER, bindMarker(USER))
                .value(USER_FLAGS, bindMarker(USER_FLAGS)));
    }

    private PreparedStatement prepareSelectAll(Session session) {
        return session.prepare(select(FIELDS)
                .from(TABLE_NAME)
                .where(eq(MESSAGE_ID, bindMarker(MESSAGE_ID))));
    }

    private PreparedStatement prepareSelect(Session session) {
        return session.prepare(select(FIELDS)
                .from(TABLE_NAME)
                .where(eq(MESSAGE_ID, bindMarker(MESSAGE_ID)))
                .and(eq(MAILBOX_ID, bindMarker(MAILBOX_ID))));
    }

    public CompletableFuture<Void> delete(CassandraMessageId messageId, CassandraId mailboxId) {
        return cassandraAsyncExecutor.executeVoid(delete.bind()
                .setUUID(MESSAGE_ID, messageId.get())
                .setUUID(MAILBOX_ID, mailboxId.asUuid()));
    }

    public CompletableFuture<Void> insert(ComposedMessageIdWithFlags composedMessageIdWithFlags) {
        ComposedMessageId composedMessageId = composedMessageIdWithFlags.getComposedMessageId();
        Flags flags = composedMessageIdWithFlags.getFlags();
        return cassandraAsyncExecutor.executeVoid(insert.bind()
                .setUUID(MESSAGE_ID, ((CassandraMessageId) composedMessageId.getMessageId()).get())
                .setUUID(MAILBOX_ID, ((CassandraId) composedMessageId.getMailboxId()).asUuid())
                .setLong(IMAP_UID, composedMessageId.getUid().asLong())
                .setBool(ANSWERED, flags.contains(Flag.ANSWERED))
                .setBool(DELETED, flags.contains(Flag.DELETED))
                .setBool(DRAFT, flags.contains(Flag.DRAFT))
                .setBool(FLAGGED, flags.contains(Flag.FLAGGED))
                .setBool(RECENT, flags.contains(Flag.RECENT))
                .setBool(SEEN, flags.contains(Flag.SEEN))
                .setBool(USER, flags.contains(Flag.USER))
                .setSet(USER_FLAGS, ImmutableSet.copyOf(flags.getUserFlags())));
    }

    public CompletableFuture<Stream<ComposedMessageIdWithFlags>> retrieve(CassandraMessageId messageId, Optional<CassandraId> mailboxId) {
        return selectStatement(messageId, mailboxId)
                .thenApply(resultSet -> {
                    return CassandraUtils.convertToStream(resultSet)
                            .map(this::toComposedMessageIdWithFlags);
                });
    }

    private ComposedMessageIdWithFlags toComposedMessageIdWithFlags(Row row) {
        return ComposedMessageIdWithFlags.builder()
                .composedMessageId(new ComposedMessageId(
                    CassandraId.of(row.getUUID(MAILBOX_ID)),
                    CassandraMessageId.of(row.getUUID(MESSAGE_ID)),
                    MessageUid.of(row.getLong(IMAP_UID))))
                .flags(new FlagsExtractor(row).getFlags())
                .build();
    }

    private CompletableFuture<ResultSet> selectStatement(CassandraMessageId messageId, Optional<CassandraId> mailboxId) {
        if (mailboxId.isPresent()) {
            return cassandraAsyncExecutor.execute(select.bind()
                .setUUID(MESSAGE_ID, messageId.get())
                .setUUID(MAILBOX_ID, mailboxId.get().asUuid()));
        }
        return cassandraAsyncExecutor.execute(selectAll.bind()
                .setUUID(MESSAGE_ID, messageId.get()));
    }
}
