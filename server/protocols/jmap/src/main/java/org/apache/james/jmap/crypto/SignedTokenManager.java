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

package org.apache.james.jmap.crypto;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;

import org.apache.james.jmap.api.SimpleTokenManager;
import org.apache.james.jmap.model.AttachmentAccessToken;
import org.apache.james.jmap.model.ContinuationToken;
import org.apache.james.jmap.model.SignedExpiringToken;
import org.apache.james.util.date.ZonedDateTimeProvider;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class SignedTokenManager implements SimpleTokenManager {

    private final SignatureHandler signatureHandler;
    private final ZonedDateTimeProvider zonedDateTimeProvider;

    @Inject
    public SignedTokenManager(SignatureHandler signatureHandler, ZonedDateTimeProvider zonedDateTimeProvider) {
        this.signatureHandler = signatureHandler;
        this.zonedDateTimeProvider = zonedDateTimeProvider;
    }

    @Override
    public ContinuationToken generateContinuationToken(String username) {
        Preconditions.checkNotNull(username);
        ZonedDateTime expirationTime = zonedDateTimeProvider.get().plusMinutes(15);
        return new ContinuationToken(username,
            expirationTime,
            signatureHandler.sign(username + ContinuationToken.SEPARATOR + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(expirationTime)));
    }

    @Override
    public AttachmentAccessToken generateAttachmentAccessToken(String blobId) {
        Preconditions.checkArgument(! Strings.isNullOrEmpty(blobId));
        ZonedDateTime expirationTime = zonedDateTimeProvider.get().plusMinutes(5);
        return AttachmentAccessToken.builder()
                .blobId(blobId)
                .expirationDate(expirationTime)
                .signature(signatureHandler.sign(blobId + AttachmentAccessToken.SEPARATOR + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(expirationTime)))
                .build();
    }

    @Override
    public TokenStatus getValidity(SignedExpiringToken token) {
        Preconditions.checkNotNull(token);
        if (! isCorrectlySigned(token)) {
            return TokenStatus.INVALID;
        }
        if (isExpired(token)) {
            return TokenStatus.EXPIRED;
        }
        return TokenStatus.OK;
    }
    
    @Override
    public boolean isValid(SignedExpiringToken token) {
        Preconditions.checkNotNull(token);
        return TokenStatus.OK.equals(getValidity(token));
    }

    private boolean isCorrectlySigned(SignedExpiringToken token) {
        return signatureHandler.verify(token.getContent(), token.getSignature());
    }

    private boolean isExpired(SignedExpiringToken token) {
        return token.getExpirationDate().isBefore(zonedDateTimeProvider.get());
    }
}
