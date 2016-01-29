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
import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.lte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.BODY_CONTENT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.BODY_OCTECTS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.BODY_START_OCTET;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.FIELDS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.FULL_CONTENT_OCTETS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.HEADER_CONTENT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.IMAP_UID;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.INTERNAL_DATE;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.MAILBOX_ID;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.util.SharedByteArrayInputStream;

import com.datastax.driver.core.Statement;
import org.apache.james.backends.cassandra.init.CassandraTypesProvider;
import org.apache.james.backends.cassandra.utils.CassandraConstants;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.mailbox.FlagsBuilder;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.table.CassandraMessageTable;
import org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Properties;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.SimpleMessageMetaData;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.SimpleProperty;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;

public class CassandraMessageRepository {

    private final Session session;
    private final CassandraTypesProvider typesProvider;

    @Inject
    public CassandraMessageRepository(Session session, CassandraTypesProvider typesProvider) {
        this.session = session;
        this.typesProvider = typesProvider;
    }

    public CompletableFuture<Void> delete(Mailbox<CassandraId> mailbox, MailboxMessage<CassandraId> message) {
        return CassandraUtils.executeVoidAsync(session,
            QueryBuilder.delete()
                .from(TABLE_NAME)
                .where(eq(MAILBOX_ID, mailbox.getMailboxId().asUuid()))
                .and(eq(IMAP_UID, message.getUid())));
    }

    public CompletableFuture<Stream<MailboxMessage<CassandraId>>> loadMessageRange(Mailbox<CassandraId> mailbox, MessageRange set) {
        return CassandraUtils.executeAsync(session, buildQuery(mailbox, set))
                    .thenApply(CassandraUtils::convertToStream)
                    .thenApply(stream -> stream.map(this::message));
    }

    public CompletableFuture<Stream<Long>> findRecentMessageUids(Mailbox<CassandraId> mailbox) throws MailboxException {
        Statement query = select(IMAP_UID)
                            .from(TABLE_NAME)
                            .where(eq(MAILBOX_ID, mailbox.getMailboxId().asUuid()))
                            .and(eq(RECENT, true));
        return CassandraUtils
                    .executeAsync(session, query)
                    .thenApply(CassandraUtils::convertToStream)
                    .thenApply(stream -> stream.map(row -> row.getLong(IMAP_UID)));
    }

    public CompletableFuture<Stream<Long>> findUnseenMessageUids(Mailbox<CassandraId> mailbox) throws MailboxException {
        Statement query = select(IMAP_UID)
                            .from(TABLE_NAME)
                            .where(eq(MAILBOX_ID, mailbox.getMailboxId().asUuid()))
                            .and(eq(SEEN, false));
        return CassandraUtils
            .executeAsync(session, query)
            .thenApply(CassandraUtils::convertToStream)
            .thenApply(stream -> stream.map(row -> row.getLong(IMAP_UID)));
    }

    public CompletableFuture<Stream<MailboxMessage<CassandraId>>> findDeletedMessages(Mailbox<CassandraId> mailbox, MessageRange set) {
        Statement query = buildQuery(mailbox, set).and(eq(DELETED, true));
        return CassandraUtils
            .executeAsync(session, query)
            .thenApply(CassandraUtils::convertToStream)
            .thenApply(stream -> stream.map(this::message));
    }

    private MailboxMessage<CassandraId> message(Row row) {
        SimpleMailboxMessage<CassandraId> message =
            new SimpleMailboxMessage<>(
                row.getDate(INTERNAL_DATE),
                row.getInt(FULL_CONTENT_OCTETS),
                row.getInt(BODY_START_OCTET),
                new SharedByteArrayInputStream(getFullContent(row)),
                getFlags(row),
                getPropertyBuilder(row),
                CassandraId.of(row.getUUID(MAILBOX_ID)));
        message.setUid(row.getLong(IMAP_UID));
        message.setModSeq(row.getLong(MOD_SEQ));
        return message;
    }

    private byte[] getFullContent(Row row) {
        byte[] headerContent = new byte[row.getBytes(HEADER_CONTENT).remaining()];
        byte[] bodyContent = new byte[row.getBytes(BODY_CONTENT).remaining()];
        row.getBytes(HEADER_CONTENT).get(headerContent);
        row.getBytes(BODY_CONTENT).get(bodyContent);
        return Bytes.concat(headerContent, bodyContent);
    }

    private Flags getFlags(Row row) {
        Set<String> userFlagSet = row.getSet(CassandraMessageTable.Flag.USER_FLAGS, String.class);

        List<Flag> flagList = Arrays.stream(CassandraMessageTable.Flag.ALL)
            .filter(row::getBool)
            .map(this::toJavaxFlag)
            .collect(Collectors.toList());

        return new FlagsBuilder().addFlags(flagList).addUserFlags(userFlagSet).build();
    }

    private Flags.Flag toJavaxFlag(String flag) {
        switch (flag) {
            case CassandraMessageTable.Flag.ANSWERED:
                return Flags.Flag.ANSWERED;
            case CassandraMessageTable.Flag.DELETED:
                return Flags.Flag.DELETED;
            case CassandraMessageTable.Flag.DRAFT:
                return Flags.Flag.DRAFT;
            case CassandraMessageTable.Flag.RECENT:
                return Flags.Flag.RECENT;
            case CassandraMessageTable.Flag.SEEN:
                return Flags.Flag.SEEN;
            case CassandraMessageTable.Flag.FLAGGED:
                return Flags.Flag.FLAGGED;
            case CassandraMessageTable.Flag.USER:
                return Flags.Flag.USER;
            default:
                return null;
        }
    }

    private PropertyBuilder getPropertyBuilder(Row row) {
        PropertyBuilder property = new PropertyBuilder(
            row.getList(PROPERTIES, UDTValue.class).stream()
                .map(x -> new SimpleProperty(x.getString(Properties.NAMESPACE), x.getString(Properties.NAME), x.getString(Properties.VALUE)))
                .collect(Collectors.toList()));
        property.setTextualLineCount(row.getLong(TEXTUAL_LINE_COUNT));
        return property;
    }

    public CompletableFuture<MessageMetaData> save(Mailbox<CassandraId> mailbox, MailboxMessage<CassandraId> message) throws MailboxException {
        try {
            Insert query = insertInto(TABLE_NAME)
                .value(MAILBOX_ID, mailbox.getMailboxId().asUuid())
                .value(IMAP_UID, message.getUid())
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
                .value(BODY_CONTENT, bindMarker())
                .value(HEADER_CONTENT, bindMarker())
                .value(PROPERTIES, message.getProperties().stream()
                    .map(x -> typesProvider.getDefinedUserType(PROPERTIES)
                        .newValue()
                        .setString(Properties.NAMESPACE, x.getNamespace())
                        .setString(Properties.NAME, x.getLocalName())
                        .setString(Properties.VALUE, x.getValue()))
                    .collect(Collectors.toList()))
                .value(TEXTUAL_LINE_COUNT, message.getTextualLineCount());
            PreparedStatement preparedStatement = session.prepare(query.toString());


            BoundStatement boundStatement = preparedStatement.bind(toByteBuffer(message.getBodyContent()), toByteBuffer(message.getHeaderContent()));
            return CassandraUtils.executeAsync(session, boundStatement)
                .thenApply(x -> new SimpleMessageMetaData(message));
        } catch (IOException e) {
            throw new MailboxException("Error saving mail", e);
        }
    }

    private Set<String> userFlagsSet(MailboxMessage<CassandraId> message) {
        return Sets.newHashSet(message.createFlags().getUserFlags());
    }

    public CompletableFuture<Boolean> conditionalSave(MailboxMessage<CassandraId> message, long oldModSeq) {
        Statement query =
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
                .where(eq(IMAP_UID, message.getUid()))
                .and(eq(MAILBOX_ID, message.getMailboxId().asUuid()))
                .onlyIf(eq(MOD_SEQ, oldModSeq));
        return CassandraUtils.executeAsync(session, query)
            .thenApply(resultSet -> resultSet.one().getBool(CassandraConstants.LIGHTWEIGHT_TRANSACTION_APPLIED));
    }

    private ByteBuffer toByteBuffer(InputStream stream) throws IOException {
        return ByteBuffer.wrap(ByteStreams.toByteArray(stream));
    }

    private Where buildQuery(Mailbox<CassandraId> mailbox, MessageRange set) {
        final MessageRange.Type type = set.getType();
        switch (type) {
        case ALL:
            return selectAll(mailbox);
        case FROM:
            return selectFrom(mailbox, set.getUidFrom());
        case RANGE:
            return selectRange(mailbox, set.getUidFrom(), set.getUidTo());
        case ONE:
            return selectMessage(mailbox, set.getUidFrom());
        }
        throw new UnsupportedOperationException();
    }

    private Where selectAll(Mailbox<CassandraId> mailbox) {
        return select(FIELDS)
            .from(TABLE_NAME)
            .where(eq(MAILBOX_ID, mailbox.getMailboxId().asUuid()));
    }

    private Where selectFrom(Mailbox<CassandraId> mailbox, long uid) {
        return select(FIELDS)
            .from(TABLE_NAME)
            .where(eq(MAILBOX_ID, mailbox.getMailboxId().asUuid()))
            .and(gte(IMAP_UID, uid));
    }

    private Where selectRange(Mailbox<CassandraId> mailbox, long from, long to) {
        return select(FIELDS)
            .from(TABLE_NAME)
            .where(eq(MAILBOX_ID, mailbox.getMailboxId().asUuid()))
            .and(gte(IMAP_UID, from))
            .and(lte(IMAP_UID, to));
    }

    private Where selectMessage(Mailbox<CassandraId> mailbox, long uid) {
        return select(FIELDS)
            .from(TABLE_NAME)
            .where(eq(MAILBOX_ID, mailbox.getMailboxId().asUuid()))
            .and(eq(IMAP_UID, uid));
    }

}
