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
import static com.datastax.driver.core.querybuilder.QueryBuilder.in;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageIds.MESSAGE_ID;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.ATTACHMENTS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.BODY;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.BODY_CONTENT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.BODY_OCTECTS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.BODY_START_OCTET;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.FIELDS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.FULL_CONTENT_OCTETS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.HEADERS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.HEADER_CONTENT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.INTERNAL_DATE;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.METADATA;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.MOD_SEQ;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.PROPERTIES;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.TABLE_NAME;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.TEXTUAL_LINE_COUNT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.ANSWERED;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.DELETED;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.DRAFT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.FLAGGED;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.RECENT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.SEEN;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.USER;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Flag.USER_FLAGS;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.mail.Flags.Flag;

import org.apache.james.backends.cassandra.init.CassandraTypesProvider;
import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.backends.cassandra.utils.CassandraConstants;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Attachments;
import org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Properties;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.Cid;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.common.io.ByteStreams;

public class CassandraMessageDAO {

    private final CassandraAsyncExecutor cassandraAsyncExecutor;
    private final CassandraTypesProvider typesProvider;
    private final PreparedStatement insertStatement;
    private final PreparedStatement updateStatement;
    private final PreparedStatement deleteStatement;

    @Inject
    public CassandraMessageDAO(Session session, CassandraTypesProvider typesProvider) {
        this.cassandraAsyncExecutor = new CassandraAsyncExecutor(session);
        this.typesProvider = typesProvider;
        this.insertStatement = insertStatement(session);
        this.updateStatement = updateStatement(session);
        this.deleteStatement = deleteStatement(session);
    }

    private PreparedStatement insertStatement(Session session) {
        return session.prepare(insertInto(TABLE_NAME)
                .value(MESSAGE_ID, bindMarker(MESSAGE_ID))
                .value(MOD_SEQ, bindMarker(MOD_SEQ))
                .value(INTERNAL_DATE, bindMarker(INTERNAL_DATE))
                .value(BODY_START_OCTET, bindMarker(BODY_START_OCTET))
                .value(FULL_CONTENT_OCTETS, bindMarker(FULL_CONTENT_OCTETS))
                .value(BODY_OCTECTS, bindMarker(BODY_OCTECTS))
                .value(ANSWERED, bindMarker(ANSWERED))
                .value(DELETED, bindMarker(DELETED))
                .value(DRAFT, bindMarker(DRAFT))
                .value(FLAGGED, bindMarker(FLAGGED))
                .value(RECENT, bindMarker(RECENT))
                .value(SEEN, bindMarker(SEEN))
                .value(USER, bindMarker(USER))
                .value(USER_FLAGS, bindMarker(USER_FLAGS))
                .value(BODY_CONTENT, bindMarker(BODY_CONTENT))
                .value(HEADER_CONTENT, bindMarker(HEADER_CONTENT))
                .value(PROPERTIES, bindMarker(PROPERTIES))
                .value(TEXTUAL_LINE_COUNT, bindMarker(TEXTUAL_LINE_COUNT))
                .value(ATTACHMENTS, bindMarker(ATTACHMENTS)));
    }

    private PreparedStatement updateStatement(Session session) {
        return session.prepare(update(TABLE_NAME)
                .with(set(ANSWERED, bindMarker(ANSWERED)))
                .and(set(DELETED, bindMarker(DELETED)))
                .and(set(DRAFT, bindMarker(DRAFT)))
                .and(set(FLAGGED, bindMarker(FLAGGED)))
                .and(set(RECENT, bindMarker(RECENT)))
                .and(set(SEEN, bindMarker(SEEN)))
                .and(set(USER, bindMarker(USER)))
                .and(set(USER_FLAGS, bindMarker(USER_FLAGS)))
                .and(set(MOD_SEQ, bindMarker(MOD_SEQ)))
                .where(eq(MESSAGE_ID, bindMarker(MESSAGE_ID)))
                .onlyIf(eq(MOD_SEQ, bindMarker(MOD_SEQ))));
    }

    private PreparedStatement deleteStatement(Session session) {
        return session.prepare(QueryBuilder.delete()
                .from(TABLE_NAME)
                .where(eq(MESSAGE_ID, bindMarker(MESSAGE_ID))));
    }

    public CompletableFuture<Void> save(Mailbox mailbox, MailboxMessage message) throws MailboxException {
        try {
            CassandraMessageId messageId = (CassandraMessageId) message.getMessageId();
            BoundStatement boundStatement = insertStatement.bind()
                .setUUID(MESSAGE_ID, messageId.get())
                .setLong(MOD_SEQ, message.getModSeq())
                .setDate(INTERNAL_DATE, message.getInternalDate())
                .setInt(BODY_START_OCTET, (int) (message.getFullContentOctets() - message.getBodyOctets()))
                .setLong(FULL_CONTENT_OCTETS, message.getFullContentOctets())
                .setLong(BODY_OCTECTS, message.getBodyOctets())
                .setBool(ANSWERED, message.isAnswered())
                .setBool(DELETED, message.isDeleted())
                .setBool(DRAFT, message.isDraft())
                .setBool(FLAGGED, message.isFlagged())
                .setBool(RECENT, message.isRecent())
                .setBool(SEEN, message.isSeen())
                .setBool(USER, message.createFlags().contains(Flag.USER))
                .setSet(USER_FLAGS, userFlagsSet(message))
                .setBytes(BODY_CONTENT, toByteBuffer(message.getBodyContent()))
                .setBytes(HEADER_CONTENT, toByteBuffer(message.getHeaderContent()))
                .setList(PROPERTIES, message.getProperties().stream()
                    .map(x -> typesProvider.getDefinedUserType(PROPERTIES)
                        .newValue()
                        .setString(Properties.NAMESPACE, x.getNamespace())
                        .setString(Properties.NAME, x.getLocalName())
                        .setString(Properties.VALUE, x.getValue()))
                    .collect(Collectors.toList()))
                .setList(ATTACHMENTS, message.getAttachments().stream()
                    .map(this::toUDT)
                    .collect(Collectors.toList()));

            setTextualLineCount(boundStatement, message.getTextualLineCount());

            return cassandraAsyncExecutor.executeVoid(boundStatement);

        } catch (IOException e) {
            throw new MailboxException("Error saving mail", e);
        }
    }

    private void setTextualLineCount(BoundStatement boundStatement, Long textualLineCount) {
        if (textualLineCount == null) {
            boundStatement.setToNull(TEXTUAL_LINE_COUNT);
        } else {
            boundStatement.setLong(TEXTUAL_LINE_COUNT, textualLineCount);
        }
    }

    private UDTValue toUDT(MessageAttachment messageAttachment) {
        return typesProvider.getDefinedUserType(ATTACHMENTS)
            .newValue()
            .setString(Attachments.ID, messageAttachment.getAttachmentId().getId())
            .setString(Attachments.NAME, messageAttachment.getName().orNull())
            .setString(Attachments.CID, messageAttachment.getCid().transform(Cid::getValue).orNull())
            .setBool(Attachments.IS_INLINE, messageAttachment.isInline());
    }

    private Set<String> userFlagsSet(MailboxMessage message) {
        return Arrays.stream(message.createFlags().getUserFlags()).collect(Collectors.toSet());
    }

    private ByteBuffer toByteBuffer(InputStream stream) throws IOException {
        return ByteBuffer.wrap(ByteStreams.toByteArray(stream));
    }
    
    public CompletableFuture<Boolean> conditionalSave(MailboxMessage message, long oldModSeq) {
        CassandraMessageId messageId = (CassandraMessageId) message.getMessageId();
        return cassandraAsyncExecutor.executeSingleRow(
            updateStatement.bind()
                .setBool(ANSWERED, message.isAnswered())
                .setBool(DELETED, message.isDeleted())
                .setBool(DRAFT, message.isDraft())
                .setBool(FLAGGED, message.isFlagged())
                .setBool(RECENT, message.isRecent())
                .setBool(SEEN, message.isSeen())
                .setBool(USER, message.createFlags().contains(Flag.USER))
                .setSet(USER_FLAGS, userFlagsSet(message))
                .setLong(MOD_SEQ, message.getModSeq())
                .setUUID(MESSAGE_ID, messageId.get())
                .setLong(MOD_SEQ, oldModSeq))
            .thenApply(optional -> optional
                    .map(row -> row.getBool(CassandraConstants.LIGHTWEIGHT_TRANSACTION_APPLIED))
                    .orElse(false));
    }
    
    public CompletableFuture<ResultSet> retrieveMessages(List<CassandraMessageId> messageIds, FetchType fetchType, Optional<Integer> limit) {
        return cassandraAsyncExecutor.execute(buildQuery(messageIds, fetchType, limit));
    }
    
    private Where buildQuery(List<CassandraMessageId> messageIds, FetchType fetchType, Optional<Integer> limit) {
        Where where = select(retrieveFields(fetchType))
                .from(TABLE_NAME)
                .where(in(MESSAGE_ID, messageIds.stream()
                        .map(CassandraMessageId::get)
                        .collect(Collectors.toList())));
        if (limit.isPresent()) {
            where.limit(limit.get());
        }
        return where;
    }

    public CompletableFuture<ResultSet> selectMessageData(List<CassandraMessageId> messageIds, FetchType fetchType) {
        return cassandraAsyncExecutor.execute(buildQuery(messageIds, fetchType, Optional.empty()));
    }

    private String[] retrieveFields(FetchType fetchType) {
        switch (fetchType) {
            case Body:
                return BODY;
            case Full:
                return FIELDS;
            case Headers:
                return HEADERS;
            case Metadata:
                return METADATA;
            default:
                throw new RuntimeException("Unknown FetchType " + fetchType);
        }
    }

    public Statement buildSelectQueryWithLimit(Select.Where selectStatement, int max) {
        if (max <= 0) {
            return selectStatement;
        }
        return selectStatement.limit(max);
    }

    public CompletableFuture<Void> delete(CassandraMessageId messageId) {
        return cassandraAsyncExecutor.executeVoid(deleteStatement.bind()
            .setUUID(MESSAGE_ID, messageId.get()));
    }
}
