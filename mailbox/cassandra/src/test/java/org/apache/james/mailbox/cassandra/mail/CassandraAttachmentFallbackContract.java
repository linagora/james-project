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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.cassandra.CassandraBlobsDAO;
import org.apache.james.mailbox.exception.AttachmentNotFoundException;
import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.AttachmentId;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

public interface CassandraAttachmentFallbackContract {
    AttachmentId ATTACHMENT_ID_1 = AttachmentId.from("id1");
    AttachmentId ATTACHMENT_ID_2 = AttachmentId.from("id2");

    class Testee {
        private final CassandraAttachmentDAOV2 attachmentDAOV2;
        private final CassandraAttachmentDAO attachmentDAO;
        private final CassandraAttachmentMapper attachmentMapper;
        private final CassandraBlobsDAO blobsDAO;

        public Testee(CassandraAttachmentDAOV2 attachmentDAOV2, CassandraAttachmentDAO attachmentDAO, CassandraAttachmentMapper attachmentMapper,
                      CassandraBlobsDAO blobsDAO) {
            this.attachmentDAOV2 = attachmentDAOV2;
            this.attachmentDAO = attachmentDAO;
            this.attachmentMapper = attachmentMapper;
            this.blobsDAO = blobsDAO;
        }
    }

    Testee attachmentFallbackTestee();

    @Test
    default void getAttachmentShouldThrowWhenAbsentFromV1AndV2() {
        assertThatThrownBy(() -> attachmentFallbackTestee().attachmentMapper.getAttachment(ATTACHMENT_ID_1))
            .isInstanceOf(AttachmentNotFoundException.class);
    }

    @Test
    default void getAttachmentsShouldReturnEmptyWhenAbsentFromV1AndV2() {
        assertThat(attachmentFallbackTestee().attachmentMapper.getAttachments(ImmutableList.of(ATTACHMENT_ID_1)))
            .isEmpty();
    }

    @Test
    default void getAttachmentShouldReturnV2WhenPresentInV1AndV2() throws Exception {
        Attachment attachment = Attachment.builder()
            .attachmentId(ATTACHMENT_ID_1)
            .type("application/json")
            .bytes("{\"property\":`\"value\"}".getBytes(StandardCharsets.UTF_8))
            .build();
        Attachment otherAttachment = Attachment.builder()
            .attachmentId(ATTACHMENT_ID_1)
            .type("application/json")
            .bytes("{\"property\":`\"different\"}".getBytes(StandardCharsets.UTF_8))
            .build();

        BlobId blobId = attachmentFallbackTestee().blobsDAO.save(attachment.getBytes()).join();
        attachmentFallbackTestee().attachmentDAOV2.storeAttachment(CassandraAttachmentDAOV2.from(attachment, blobId)).join();
        attachmentFallbackTestee().attachmentDAO.storeAttachment(otherAttachment).join();

        assertThat(attachmentFallbackTestee().attachmentMapper.getAttachment(ATTACHMENT_ID_1))
            .isEqualTo(attachment);
    }

    @Test
    default void getAttachmentShouldReturnV1WhenV2Absent() throws Exception {
        Attachment attachment = Attachment.builder()
            .attachmentId(ATTACHMENT_ID_1)
            .type("application/json")
            .bytes("{\"property\":`\"value\"}".getBytes(StandardCharsets.UTF_8))
            .build();

        attachmentFallbackTestee().attachmentDAO.storeAttachment(attachment).join();

        assertThat(attachmentFallbackTestee().attachmentMapper.getAttachment(ATTACHMENT_ID_1))
            .isEqualTo(attachment);
    }

    @Test
    default void getAttachmentsShouldReturnV2WhenV2AndV1() throws Exception {
        Attachment attachment = Attachment.builder()
            .attachmentId(ATTACHMENT_ID_1)
            .type("application/json")
            .bytes("{\"property\":`\"value\"}".getBytes(StandardCharsets.UTF_8))
            .build();
        Attachment otherAttachment = Attachment.builder()
            .attachmentId(ATTACHMENT_ID_1)
            .type("application/json")
            .bytes("{\"property\":`\"different\"}".getBytes(StandardCharsets.UTF_8))
            .build();

        BlobId blobId = attachmentFallbackTestee().blobsDAO.save(attachment.getBytes()).join();
        attachmentFallbackTestee().attachmentDAOV2.storeAttachment(CassandraAttachmentDAOV2.from(attachment, blobId)).join();
        attachmentFallbackTestee().attachmentDAO.storeAttachment(otherAttachment).join();

        assertThat(attachmentFallbackTestee().attachmentMapper.getAttachments(ImmutableList.of(ATTACHMENT_ID_1)))
            .containsExactly(attachment);
    }

    @Test
    default void getAttachmentsShouldReturnV1WhenV2Absent() throws Exception {
        Attachment attachment = Attachment.builder()
            .attachmentId(ATTACHMENT_ID_1)
            .type("application/json")
            .bytes("{\"property\":`\"value\"}".getBytes(StandardCharsets.UTF_8))
            .build();

        attachmentFallbackTestee().attachmentDAO.storeAttachment(attachment).join();

        assertThat(attachmentFallbackTestee().attachmentMapper.getAttachments(ImmutableList.of(ATTACHMENT_ID_1)))
            .containsExactly(attachment);
    }

    @Test
    default void getAttachmentsShouldCombineElementsFromV1AndV2() throws Exception {
        Attachment attachment = Attachment.builder()
            .attachmentId(ATTACHMENT_ID_1)
            .type("application/json")
            .bytes("{\"property\":`\"value\"}".getBytes(StandardCharsets.UTF_8))
            .build();
        Attachment otherAttachment = Attachment.builder()
            .attachmentId(ATTACHMENT_ID_2)
            .type("application/json")
            .bytes("{\"property\":`\"different\"}".getBytes(StandardCharsets.UTF_8))
            .build();

        BlobId blobId = attachmentFallbackTestee().blobsDAO.save(attachment.getBytes()).join();
        attachmentFallbackTestee().attachmentDAOV2.storeAttachment(CassandraAttachmentDAOV2.from(attachment, blobId)).join();
        attachmentFallbackTestee().attachmentDAO.storeAttachment(otherAttachment).join();

        assertThat(attachmentFallbackTestee().attachmentMapper.getAttachments(ImmutableList.of(ATTACHMENT_ID_1, ATTACHMENT_ID_2)))
            .containsExactly(attachment, otherAttachment);
    }
}
