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

package org.apache.james.mailbox.message;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.james.mailbox.store.extractor.TextExtractor;
import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.Property;
import org.apache.james.mime4j.MimeException;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Multimap;

public class IndexableMessage {

    public static IndexableMessage from(MailboxMessage<? extends MailboxId> message, TextExtractor textExtractor, ZoneId zoneId) {
        Preconditions.checkNotNull(message.getMailboxId());
        IndexableMessage indexableMessage = new IndexableMessage();
        try {
            MimePart parsingResult = new MimePartParser(message, textExtractor).parse();
            indexableMessage.bodyText = parsingResult.locateFirstTextualBody();
            indexableMessage.setFlattenedAttachments(parsingResult);
            indexableMessage.copyHeaderFields(parsingResult.getHeaderCollection(), getSanitizedInternalDate(message, zoneId));
        } catch (IOException | MimeException e) {
            throw Throwables.propagate(e);
        }
        indexableMessage.copyMessageFields(message, zoneId);
        return indexableMessage;
    }

    private void setFlattenedAttachments(MimePart parsingResult) {
        attachments = parsingResult.getAttachmentsStream()
            .collect(Collectors.toList());
    }

    private void copyHeaderFields(HeaderCollection headerCollection, ZonedDateTime internalDate) {
        this.headers = headerCollection.getHeaders();
        this.subjects = headerCollection.getSubjectSet();
        this.from = headerCollection.getFromAddressSet();
        this.to = headerCollection.getToAddressSet();
        this.replyTo = headerCollection.getReplyToAddressSet();
        this.cc = headerCollection.getCcAddressSet();
        this.bcc = headerCollection.getBccAddressSet();
        this.sentDate = DateResolutionFormater.DATE_TIME_FOMATTER.format(headerCollection.getSentDate().orElse(internalDate));
    }

    private void copyMessageFields(MailboxMessage<? extends MailboxId> message, ZoneId zoneId) {
        this.id = message.getUid();
        this.mailboxId = message.getMailboxId().serialize();
        this.modSeq = message.getModSeq();
        this.size = message.getFullContentOctets();
        this.date = DateResolutionFormater.DATE_TIME_FOMATTER.format(getSanitizedInternalDate(message, zoneId));
        this.mediaType = message.getMediaType();
        this.subType = message.getSubType();
        this.isAnswered = message.isAnswered();
        this.isDeleted = message.isDeleted();
        this.isDraft = message.isDraft();
        this.isFlagged = message.isFlagged();
        this.isRecent = message.isRecent();
        this.isUnRead = ! message.isSeen();
        this.userFlags = message.createFlags().getUserFlags();
        this.properties = message.getProperties();
    }

    private static ZonedDateTime getSanitizedInternalDate(MailboxMessage<? extends MailboxId> message, ZoneId zoneId) {
        if (message.getInternalDate() == null) {
            return ZonedDateTime.now();
        }
        return ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(message.getInternalDate().getTime()),
            zoneId);
    }

    private Long id;
    private String mailboxId;
    private long modSeq;
    private long size;
    private String date;
    private String mediaType;
    private String subType;
    private boolean isUnRead;
    private boolean isRecent;
    private boolean isFlagged;
    private boolean isDeleted;
    private boolean isDraft;
    private boolean isAnswered;
    private String[] userFlags;
    private Multimap<String, String> headers;
    private Set<EMailer> from;
    private Set<EMailer> to;
    private Set<EMailer> cc;
    private Set<EMailer> bcc;
    private Set<EMailer> replyTo;
    private Set<String> subjects;
    private String sentDate;
    private List<Property> properties;
    private List<MimePart> attachments;
    private Optional<String> bodyText;

    public Long getId() {
        return id;
    }

    public String getMailboxId() {
        return mailboxId;
    }

    public long getModSeq() {
        return modSeq;
    }

    public long getSize() {
        return size;
    }

    public String getDate() {
        return date;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getSubType() {
        return subType;
    }

    public boolean isUnRead() {
        return isUnRead;
    }

    public boolean isRecent() {
        return isRecent;
    }

    public boolean isFlagged() {
        return isFlagged;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public boolean isDraft() {
        return isDraft;
    }

    public boolean isAnswered() {
        return isAnswered;
    }

    public String[] getUserFlags() {
        return userFlags;
    }

    public Multimap<String, String> getHeaders() {
        return headers;
    }

    public Set<String> getSubjects() {
        return subjects;
    }

    public Set<EMailer> getFrom() {
        return from;
    }

    public Set<EMailer> getTo() {
        return to;
    }

    public Set<EMailer> getCc() {
        return cc;
    }

    public Set<EMailer> getBcc() {
        return bcc;
    }

    public Set<EMailer> getReplyTo() {
        return replyTo;
    }

    public String getSentDate() {
        return sentDate;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public List<MimePart> getAttachments() {
        return attachments;
    }

    public Optional<String> getBodyText() {
        return bodyText;
    }

    public boolean getHasAttachment() {
        return attachments.size() > 0;
    }
}
