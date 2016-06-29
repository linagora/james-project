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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

public class AttachmentAccessTokenTest {

    private static final String BLOB_ID = "blobId";
    private static final String EXPIRATION_DATE_STRING = "2011-12-03T10:15:30+01:00";
    private static final ZonedDateTime EXPIRATION_DATE = ZonedDateTime.parse(EXPIRATION_DATE_STRING, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    private static final String SIGNATURE = "signature";

    @Test
    public void getAsStringShouldNotContainBlobId() throws Exception {
        assertThat(new AttachmentAccessToken(BLOB_ID, EXPIRATION_DATE, SIGNATURE).serialize())
            .isEqualTo(EXPIRATION_DATE_STRING + AttachmentAccessToken.SEPARATOR + SIGNATURE);
    }

    @Test
    public void getContentShouldContainOnlyExpiration() throws Exception {
        assertThat(new AttachmentAccessToken(BLOB_ID, EXPIRATION_DATE, SIGNATURE).getContent())
            .isEqualTo(EXPIRATION_DATE_STRING);
    }

    @Test(expected = NullPointerException.class)
    public void buildWithNullBlobIdShouldThrow() {
        AttachmentAccessToken.builder()
            .blobId(null)
            .build();
    }

    @Test(expected = NullPointerException.class)
    public void buildWithNullExpirationDateShouldThrow() {
        AttachmentAccessToken.builder()
            .blobId(BLOB_ID)
            .expirationDate(null)
            .build();
    }

    @Test(expected = NullPointerException.class)
    public void buildWithNullSignatureShouldThrow() {
        AttachmentAccessToken.builder()
            .blobId(BLOB_ID)
            .expirationDate(EXPIRATION_DATE)
            .signature(null)
            .build();
    }

    public void buildWithValidArgumentsShouldBuild() {
        AttachmentAccessToken expected = new AttachmentAccessToken(BLOB_ID, EXPIRATION_DATE, SIGNATURE);
        AttachmentAccessToken actual = AttachmentAccessToken.builder()
            .blobId(BLOB_ID)
            .expirationDate(EXPIRATION_DATE)
            .signature(SIGNATURE)
            .build();
        assertThat(actual).isEqualToComparingFieldByField(expected);
    }
}
