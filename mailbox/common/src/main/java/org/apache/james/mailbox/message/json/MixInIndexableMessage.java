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

package org.apache.james.mailbox.message.json;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.james.mailbox.message.EMailer;
import org.apache.james.mailbox.message.MimePart;
import org.apache.james.mailbox.store.mail.model.Property;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Multimap;

public abstract class MixInIndexableMessage {

    @JsonProperty(JsonMessageConstants.MAILBOX_ID) abstract String getMailboxId();
    @JsonProperty(JsonMessageConstants.MODSEQ) abstract long getModSeq();
    @JsonProperty(JsonMessageConstants.SIZE) abstract long getSize();
    @JsonProperty(JsonMessageConstants.DATE) abstract String getDate();
    @JsonProperty(JsonMessageConstants.MEDIA_TYPE) abstract String getMediaType();
    @JsonProperty(JsonMessageConstants.SUBTYPE) abstract String getSubType();
    @JsonProperty(JsonMessageConstants.IS_UNREAD) abstract boolean isUnRead();
    @JsonProperty(JsonMessageConstants.IS_RECENT) abstract boolean isRecent();
    @JsonProperty(JsonMessageConstants.IS_FLAGGED) abstract boolean isFlagged();
    @JsonProperty(JsonMessageConstants.IS_DELETED) abstract boolean isDeleted();
    @JsonProperty(JsonMessageConstants.IS_DRAFT) abstract boolean isDraft();
    @JsonProperty(JsonMessageConstants.IS_ANSWERED) abstract boolean isAnswered();
    @JsonProperty(JsonMessageConstants.USER_FLAGS) abstract String[] getUserFlags();
    @JsonProperty(JsonMessageConstants.HEADERS) abstract Multimap<String, String> getHeaders();
    @JsonProperty(JsonMessageConstants.SUBJECT) abstract Set<String> getSubjects();
    @JsonProperty(JsonMessageConstants.FROM) abstract Set<EMailer> getFrom();
    @JsonProperty(JsonMessageConstants.TO) abstract Set<EMailer> getTo();
    @JsonProperty(JsonMessageConstants.CC) abstract Set<EMailer> getCc();
    @JsonProperty(JsonMessageConstants.BCC) abstract Set<EMailer> getBcc();
    @JsonProperty(JsonMessageConstants.REPLY_TO) abstract Set<EMailer> getReplyTo();
    @JsonProperty(JsonMessageConstants.SENT_DATE) abstract String getSentDate();
    @JsonProperty(JsonMessageConstants.PROPERTIES) abstract List<Property> getProperties();
    @JsonProperty(JsonMessageConstants.ATTACHMENTS) abstract List<MimePart> getAttachments();
    @JsonProperty(JsonMessageConstants.TEXT_BODY) abstract Optional<String> getBodyText();
    @JsonProperty(JsonMessageConstants.HAS_ATTACHMENT) abstract boolean getHasAttachment();
}
