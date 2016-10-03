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
import java.util.stream.Collectors;

import javax.mail.Flags.Flag;

import org.apache.james.backends.cassandra.init.CassandraTypesProvider;
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

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.common.io.ByteStreams;

public class CassandraMessageDAO {

    private final Session session;
    private final CassandraTypesProvider typesProvider;

    public CassandraMessageDAO(Session session, CassandraTypesProvider typesProvider) {
        this.session = session;
        this.typesProvider = typesProvider;
    }

    public void save(Mailbox mailbox, MailboxMessage message) throws MailboxException {
        try {
            session.execute(insertInto(TABLE_NAME)
                .value(MESSAGE_ID, message.getMessageId().serialize())
                .value(MOD_SEQ, message.getModSeq())
                .value(INTERNAL_DATE, message.getInternalDate())
                .value(BODY_START_OCTET, message.getFullContentOctets() - message.getBodyOctets())
                .value(FULL_CONTENT_OCTETS, message.getFullContentOctets())
                .value(BODY_OCTECTS, message.getBodyOctets())
                .value(ANSWERED, message.isAnswered())
                .value(DELETED, message.isDeleted())
                .value(DRAFT, message.isDraft())
                .value(FLAGGED, message.isFlagged())
                .value(RECENT, message.isRecent())
                .value(SEEN, message.isSeen())
                .value(USER, message.createFlags().contains(Flag.USER))
                .value(USER_FLAGS, userFlagsSet(message))
                .value(BODY_CONTENT, toByteBuffer(message.getBodyContent()))
                .value(HEADER_CONTENT, toByteBuffer(message.getHeaderContent()))
                .value(PROPERTIES, message.getProperties().stream()
                    .map(x -> typesProvider.getDefinedUserType(PROPERTIES)
                        .newValue()
                        .setString(Properties.NAMESPACE, x.getNamespace())
                        .setString(Properties.NAME, x.getLocalName())
                        .setString(Properties.VALUE, x.getValue()))
                    .collect(Collectors.toList()))
                .value(TEXTUAL_LINE_COUNT, message.getTextualLineCount())
                .value(ATTACHMENTS, message.getAttachments().stream()
                    .map(this::toUDT)
                    .collect(Collectors.toList())));

        } catch (IOException e) {
            throw new MailboxException("Error saving mail", e);
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
    
    

    public boolean conditionalSave(MailboxMessage message, long oldModSeq) {
        ResultSet resultSet = session.execute(
            update(TABLE_NAME)
                .with(set(ANSWERED, message.isAnswered()))
                .and(set(DELETED, message.isDeleted()))
                .and(set(DRAFT, message.isDraft()))
                .and(set(FLAGGED, message.isFlagged()))
                .and(set(RECENT, message.isRecent()))
                .and(set(SEEN, message.isSeen()))
                .and(set(USER, message.createFlags().contains(Flag.USER)))
                .and(set(USER_FLAGS, userFlagsSet(message)))
                .and(set(MOD_SEQ, message.getModSeq()))
                .where(eq(MESSAGE_ID, message.getMessageId().serialize()))
                .onlyIf(eq(MOD_SEQ, oldModSeq)));
        return resultSet.one().getBool(CassandraConstants.LIGHTWEIGHT_TRANSACTION_APPLIED);
    }
    
    public ResultSet retrieveMessages(List<CassandraMessageId> messageIds, FetchType fetchType, Optional<Integer> limit) {
        return session.execute(buildQuery(messageIds, fetchType, limit));
    }
    
    private Where buildQuery(List<CassandraMessageId> messageIds, FetchType fetchType, Optional<Integer> limit) {
        Where where = select(retrieveFields(fetchType))
                .from(TABLE_NAME)
                .where(in(MESSAGE_ID, messageIds.stream()
                        .map(CassandraMessageId::serialize)
                        .collect(Collectors.toList())));
        if (limit.isPresent()) {
            where.limit(limit.get());
        }
        return where;
    }

    public ResultSet selectMessageData(List<CassandraMessageId> messageIds, FetchType fetchType) {
        return session.execute(buildQuery(messageIds, fetchType, Optional.empty()));
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
}
