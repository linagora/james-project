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

package org.apache.james.blob.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.HashBlobId;
import org.apache.james.blob.api.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;

class MimeMessagePartsIdFactoryTest {

    private static final HashBlobId.Factory BLOB_ID_FACTORY = new HashBlobId.Factory();

    private MimeMessagePartsId.Factory factory;

    @BeforeEach
    void setUp() {
        factory = new MimeMessagePartsId.Factory();
    }

    @Test
    void generateShouldThrowWhenNull() {
        assertThatThrownBy(() -> factory.generate(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void generateShouldThrowWhenMissingHeaderBlobs() {
        assertThatThrownBy(() -> factory.generate(
            ImmutableMap.of(
                MimeMessagePartsId.HEADER_BLOB_TYPE, BLOB_ID_FACTORY.randomId())))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateShouldThrowWhenMissingBodyBlobs() {
        assertThatThrownBy(() -> factory.generate(
            ImmutableMap.of(
                MimeMessagePartsId.BODY_BLOB_TYPE, BLOB_ID_FACTORY.randomId())))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateShouldThrowWhenExtraBodyBlobs() {
        assertThatThrownBy(() -> factory.generate(
            ImmutableMap.of(
                MimeMessagePartsId.BODY_BLOB_TYPE, BLOB_ID_FACTORY.randomId(),
                MimeMessagePartsId.HEADER_BLOB_TYPE, BLOB_ID_FACTORY.randomId(),
                new Store.BlobType("Unknown"), BLOB_ID_FACTORY.randomId())))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateShouldReturnCorrectMimeMessagePartsId() {
        BlobId headerBlobId = BLOB_ID_FACTORY.randomId();
        BlobId bodyBlobId = BLOB_ID_FACTORY.randomId();

        assertThat(
            factory.generate(ImmutableMap.of(
                MimeMessagePartsId.BODY_BLOB_TYPE, bodyBlobId,
                MimeMessagePartsId.HEADER_BLOB_TYPE, headerBlobId)))
            .isEqualTo(
                MimeMessagePartsId.builder()
                    .headerBlobId(headerBlobId)
                    .bodyBlobId(bodyBlobId)
                    .build());
    }

    @Test
    void mimeMessagePartsIdShouldMatchBeanContract() {
        EqualsVerifier.forClass(MimeMessagePartsId.class).verify();
    }

}