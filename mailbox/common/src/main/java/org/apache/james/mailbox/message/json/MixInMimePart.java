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
import java.util.stream.Stream;

import org.apache.james.mailbox.message.HeaderCollection;
import org.apache.james.mailbox.message.MimePart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public abstract class MixInMimePart {

    @JsonIgnore abstract List<MimePart> getAttachments();
    @JsonIgnore abstract HeaderCollection getHeaderCollection();
    @JsonProperty(JsonMessageConstants.HEADERS) abstract Multimap<String, String> getHeaders();
    @JsonProperty(JsonMessageConstants.Attachment.FILENAME) abstract Optional<String> getFileName();
    @JsonProperty(JsonMessageConstants.Attachment.FILE_EXTENSION) abstract Optional<String> getFileExtension();
    @JsonProperty(JsonMessageConstants.Attachment.MEDIA_TYPE) abstract Optional<String> getMediaType();
    @JsonProperty(JsonMessageConstants.Attachment.SUBTYPE) abstract Optional<String> getSubType();
    @JsonProperty(JsonMessageConstants.Attachment.CONTENT_DISPOSITION) abstract Optional<String> getContentDisposition();
    @JsonProperty(JsonMessageConstants.Attachment.TEXT_CONTENT) abstract Optional<String> getTextualBody();
    @JsonProperty(JsonMessageConstants.Attachment.FILE_METADATA) abstract ImmutableMultimap<String, String> getMetadata();
    @JsonIgnore abstract Optional<String> locateFirstTextualBody();
    @JsonIgnore abstract Stream<MimePart> getAttachmentsStream();
}
