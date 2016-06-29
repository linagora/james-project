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

package org.apache.james.jmap.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

public class AttachmentAccessToken implements SignedExpiringToken {

    public static final String SEPARATOR = "_";

    public static Builder builder() {
        return new Builder();
    }

    public static AttachmentAccessToken from(String serializedAttachmentAccessToken) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(serializedAttachmentAccessToken), "'AttachmentAccessToken' is mandatory");
        List<String> split = Splitter.on(SEPARATOR).splitToList(serializedAttachmentAccessToken);
        Preconditions.checkArgument(split.size() == 3, "Wrong 'AttachmentAccessToken'");
        return builder()
                .blobId(Iterables.get(split, 0, null))
                .expirationDate(ZonedDateTime.parse(Iterables.get(split, 1, null)))
                .signature(Iterables.get(split, 2, null))
                .build();
    }

    public static class Builder {
        private String blobId;
        private ZonedDateTime expirationDate;
        private String signature;

        private Builder() {}
        
        public Builder blobId(String blobId) {
            this.blobId = blobId;
            return this;
        }

        public Builder expirationDate(ZonedDateTime expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public Builder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public AttachmentAccessToken build() {
            Preconditions.checkNotNull(blobId);
            Preconditions.checkArgument(! blobId.isEmpty());
            Preconditions.checkNotNull(expirationDate);
            Preconditions.checkNotNull(signature);
            return new AttachmentAccessToken(blobId, expirationDate, signature);
        }
    }
    
    private final String blobId;
    private final ZonedDateTime expirationDate;
    private final String signature;

    @VisibleForTesting
    AttachmentAccessToken(String blobId, ZonedDateTime expirationDate, String signature) {
        this.blobId = blobId;
        this.expirationDate = expirationDate;
        this.signature = signature;
    }

    public String getBlobId() {
        return blobId;
    }

    @Override
    public ZonedDateTime getExpirationDate() {
        return expirationDate;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    public String serialize() {
        return getContent()
            + SEPARATOR
            + signature;
    }
    
    @Override
    public String getContent() {
        return blobId
            + SEPARATOR
            + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(expirationDate);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        AttachmentAccessToken attachmentAccessToken = (AttachmentAccessToken) other;
        return Objects.equals(blobId, attachmentAccessToken.blobId)
            && expirationDate.isEqual(attachmentAccessToken.expirationDate)
            && Objects.equals(signature, attachmentAccessToken.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blobId, expirationDate, signature);
    }

    @Override
    public String toString() {
        return "ContinuationToken{" +
            "blobId='" + blobId + '\'' +
            ", expirationDate=" + expirationDate +
            ", signature='" + signature + '\'' +
            '}';
    }
}
