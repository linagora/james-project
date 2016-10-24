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

import static org.apache.james.mailbox.cassandra.table.CassandraMessageIds.MESSAGE_ID;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.ATTACHMENTS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.BODY_CONTENT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.BODY_START_OCTET;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.FULL_CONTENT_OCTETS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.HEADER_CONTENT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.INTERNAL_DATE;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.MOD_SEQ;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.PROPERTIES;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.TEXTUAL_LINE_COUNT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.mail.Flags;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.cassandra.table.CassandraMessageTable;
import org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Attachments;
import org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Properties;
import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.AttachmentId;
import org.apache.james.mailbox.model.Cid;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.SimpleProperty;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.UDTValue;
import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Bytes;

public class CassandraMessageRowHandler {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Row row;
        private ComposedMessageId messageId;
        private Function<List<AttachmentId>, List<Attachment>> attachmentsFunction;

        private Builder() {
        }

        public Builder row(Row row) {
            this.row = row;
            return this;
        }

        public Builder messageId(ComposedMessageId messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder loadingAttachmentsFunction(Function<List<AttachmentId>, List<Attachment>> attachmentsFunction) {
            this.attachmentsFunction = attachmentsFunction;
            return this;
        }

        public CassandraMessageRowHandler build() {
            return new CassandraMessageRowHandler(row, messageId, attachmentsFunction);
        }
    }

    private final Row row;
    private final ComposedMessageId messageId;
    private final Function<List<AttachmentId>, List<Attachment>> attachmentsFunction;

    public CassandraMessageRowHandler(Row row, ComposedMessageId messageId, Function<List<AttachmentId>, List<Attachment>> attachmentsFunction) {
        this.row = row;
        this.messageId = messageId;
        this.attachmentsFunction = attachmentsFunction;
    }

    public MailboxMessage toMailboxMessage(FetchType fetchType) {
        SimpleMailboxMessage message =
                new SimpleMailboxMessage(
                        CassandraMessageId.of(row.getUUID(MESSAGE_ID)),
                        row.getDate(INTERNAL_DATE),
                        row.getLong(FULL_CONTENT_OCTETS),
                        row.getInt(BODY_START_OCTET),
                        buildContent(row, fetchType),
                        getFlags(row),
                        getPropertyBuilder(row),
                        messageId.getMailboxId(),
                        getAttachments(row, fetchType));
        message.setUid(messageId.getUid());
        message.setModSeq(row.getLong(MOD_SEQ));
        return message;
    }

    private SharedByteArrayInputStream buildContent(Row row, FetchType fetchType) {
        switch (fetchType) {
            case Full:
                return new SharedByteArrayInputStream(getFullContent(row));
            case Headers:
                return new SharedByteArrayInputStream(getFieldContent(HEADER_CONTENT, row));
            case Body:
                return new SharedByteArrayInputStream(getBodyContent(row));
            case Metadata:
                return new SharedByteArrayInputStream(new byte[]{});
            default:
                throw new RuntimeException("Unknown FetchType " + fetchType);
        }
    }

    private byte[] getFullContent(Row row) {
        return Bytes.concat(getFieldContent(HEADER_CONTENT, row), getFieldContent(BODY_CONTENT, row));
    }

    private byte[] getBodyContent(Row row) {
        return Bytes.concat(new byte[row.getInt(BODY_START_OCTET)], getFieldContent(BODY_CONTENT, row));
    }

    private byte[] getFieldContent(String field, Row row) {
        byte[] headerContent = new byte[row.getBytes(field).remaining()];
        row.getBytes(field).get(headerContent);
        return headerContent;
    }

    private Flags getFlags(Row row) {
        Flags flags = new Flags();
        for (String flag : CassandraMessageTable.Flag.ALL) {
            if (row.getBool(flag)) {
                flags.add(CassandraMessageTable.Flag.JAVAX_MAIL_FLAG.get(flag));
            }
        }
        row.getSet(CassandraMessageTable.Flag.USER_FLAGS, String.class)
            .stream()
            .forEach(flags::add);
        return flags;
    }

    private PropertyBuilder getPropertyBuilder(Row row) {
        PropertyBuilder property = new PropertyBuilder(
            row.getList(PROPERTIES, UDTValue.class).stream()
                .map(x -> new SimpleProperty(x.getString(Properties.NAMESPACE), x.getString(Properties.NAME), x.getString(Properties.VALUE)))
                .collect(Collectors.toList()));
        property.setTextualLineCount(row.getLong(TEXTUAL_LINE_COUNT));
        return property;
    }

    private List<MessageAttachment> getAttachments(Row row, FetchType fetchType) {
        switch (fetchType) {
        case Full:
        case Body:
            List<UDTValue> udtValues = row.getList(ATTACHMENTS, UDTValue.class);
            Map<AttachmentId,Attachment> attachmentsById = attachmentsById(row, udtValues);

            return udtValues
                    .stream()
                    .map(Throwing.function(x -> 
                        MessageAttachment.builder()
                            .attachment(attachmentsById.get(attachmentIdFrom(x)))
                            .name(x.getString(Attachments.NAME))
                            .cid(com.google.common.base.Optional.fromNullable(x.getString(Attachments.CID)).transform(Cid::from))
                            .isInline(x.getBool(Attachments.IS_INLINE))
                            .build()))
                    .collect(Guavate.toImmutableList());
        default:
            return ImmutableList.of();
        }
    }

    private Map<AttachmentId,Attachment> attachmentsById(Row row, List<UDTValue> udtValues) {
        Map<AttachmentId, Attachment> map = new HashMap<>();
        List<AttachmentId> attachmentIds2 = attachmentIds(udtValues);
        attachmentsFunction
            .apply(attachmentIds2)
            .stream()
            .forEach(att -> map.put(att.getAttachmentId(), att));
        return map;
    }

    private List<AttachmentId> attachmentIds(List<UDTValue> udtValues) {
        return udtValues.stream()
            .map(this::attachmentIdFrom)
            .collect(Guavate.toImmutableList());
    }

    private AttachmentId attachmentIdFrom(UDTValue udtValue) {
        return AttachmentId.from(udtValue.getString(Attachments.ID));
    }
}
