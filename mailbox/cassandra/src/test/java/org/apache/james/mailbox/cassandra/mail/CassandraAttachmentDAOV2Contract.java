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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.cassandra.CassandraBlobId;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAOV2.DAOAttachment;
import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.AttachmentId;
import org.junit.jupiter.api.Test;

public interface CassandraAttachmentDAOV2Contract {
    AttachmentId ATTACHMENT_ID = AttachmentId.from("id1");
    CassandraBlobId.Factory BLOB_ID_FACTORY = new CassandraBlobId.Factory();

    CassandraAttachmentDAOV2 testee();

    @Test
    default void getAttachmentShouldReturnEmptyWhenAbsent() {
        Optional<DAOAttachment> attachment = testee().getAttachment(ATTACHMENT_ID).join();

        assertThat(attachment).isEmpty();
    }

    @Test
    default void getAttachmentShouldReturnAttachmentWhenStored() {
        Attachment attachment = Attachment.builder()
            .attachmentId(ATTACHMENT_ID)
            .type("application/json")
            .bytes("{\"property\":`\"value\"}".getBytes(StandardCharsets.UTF_8))
            .build();
        BlobId blobId = BLOB_ID_FACTORY.from("blobId");
        DAOAttachment daoAttachment = CassandraAttachmentDAOV2.from(attachment, blobId);
        testee().storeAttachment(daoAttachment).join();

        Optional<DAOAttachment> actual = testee().getAttachment(ATTACHMENT_ID).join();

        assertThat(actual).contains(daoAttachment);
    }
}
