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

package org.apache.james.mailbox.cassandra.mail.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.apache.james.backends.cassandra.migration.Migration;
import org.apache.james.blob.cassandra.CassandraBlobId;
import org.apache.james.blob.cassandra.CassandraBlobsDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAOV2;
import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.AttachmentId;
import org.junit.jupiter.api.Test;

public interface AttachmentV2MigrationContract {
    AttachmentId ATTACHMENT_ID = AttachmentId.from("id1");
    AttachmentId ATTACHMENT_ID_2 = AttachmentId.from("id2");
    CassandraBlobId.Factory BLOB_ID_FACTORY = new CassandraBlobId.Factory();
    Attachment attachment1 = Attachment.builder()
        .attachmentId(ATTACHMENT_ID)
        .type("application/json")
        .bytes("{\"property\":`\"value1\"}".getBytes(StandardCharsets.UTF_8))
        .build();
    Attachment attachment2 = Attachment.builder()
        .attachmentId(ATTACHMENT_ID_2)
        .type("application/json")
        .bytes("{\"property\":`\"value2\"}".getBytes(StandardCharsets.UTF_8))
        .build();

    class Testee {
        private final CassandraAttachmentDAO attachmentDAO;
        private final CassandraAttachmentDAOV2 attachmentDAOV2;
        private final CassandraBlobsDAO blobsDAO;
        private final AttachmentV2Migration migration;

        public Testee(CassandraAttachmentDAO attachmentDAO, CassandraAttachmentDAOV2 attachmentDAOV2, CassandraBlobsDAO blobsDAO, AttachmentV2Migration migration) {
            this.attachmentDAO = attachmentDAO;
            this.attachmentDAOV2 = attachmentDAOV2;
            this.blobsDAO = blobsDAO;
            this.migration = migration;
        }
    }

    Testee attachmentV2MigrationTestee();

    @Test
    default void emptyAttachmentV2MigrationShouldSucceed() {
        assertThat(attachmentV2MigrationTestee().migration.run())
            .isEqualTo(Migration.Result.COMPLETED);
    }

    @Test
    default void migrationShouldSucceed() throws Exception {
        attachmentV2MigrationTestee().attachmentDAO.storeAttachment(attachment1).join();
        attachmentV2MigrationTestee().attachmentDAO.storeAttachment(attachment2).join();

        assertThat(attachmentV2MigrationTestee().migration.run())
            .isEqualTo(Migration.Result.COMPLETED);
    }

    @Test
    default void migrationShouldMoveAttachmentsToV2() throws Exception {
        attachmentV2MigrationTestee().attachmentDAO.storeAttachment(attachment1).join();
        attachmentV2MigrationTestee().attachmentDAO.storeAttachment(attachment2).join();

        attachmentV2MigrationTestee().migration.run();

        assertThat(attachmentV2MigrationTestee().attachmentDAOV2.getAttachment(ATTACHMENT_ID).join())
            .contains(CassandraAttachmentDAOV2.from(attachment1, BLOB_ID_FACTORY.forPayload(attachment1.getBytes())));
        assertThat(attachmentV2MigrationTestee().attachmentDAOV2.getAttachment(ATTACHMENT_ID_2).join())
            .contains(CassandraAttachmentDAOV2.from(attachment2, BLOB_ID_FACTORY.forPayload(attachment2.getBytes())));
        assertThat(attachmentV2MigrationTestee().blobsDAO.read(BLOB_ID_FACTORY.forPayload(attachment1.getBytes())).join())
            .isEqualTo(attachment1.getBytes());
        assertThat(attachmentV2MigrationTestee().blobsDAO.read(BLOB_ID_FACTORY.forPayload(attachment2.getBytes())).join())
            .isEqualTo(attachment2.getBytes());
    }

    @Test
    default void migrationShouldRemoveAttachmentsFromV1() throws Exception {
        attachmentV2MigrationTestee().attachmentDAO.storeAttachment(attachment1).join();
        attachmentV2MigrationTestee().attachmentDAO.storeAttachment(attachment2).join();

        attachmentV2MigrationTestee().migration.run();

        assertThat(attachmentV2MigrationTestee().attachmentDAO.getAttachment(ATTACHMENT_ID).join())
            .isEmpty();
        assertThat(attachmentV2MigrationTestee().attachmentDAO.getAttachment(ATTACHMENT_ID_2).join())
            .isEmpty();
    }

    @Test
    default void runShouldReturnPartialWhenInitialReadFail() throws Exception {
        CassandraAttachmentDAO attachmentDAO = mock(CassandraAttachmentDAO.class);
        CassandraAttachmentDAOV2 attachmentDAOV2 = mock(CassandraAttachmentDAOV2.class);
        CassandraBlobsDAO blobsDAO = mock(CassandraBlobsDAO.class);
        AttachmentV2Migration migration = new AttachmentV2Migration(attachmentDAO, attachmentDAOV2, blobsDAO);

        when(attachmentDAO.retrieveAll()).thenThrow(new RuntimeException());

        assertThat(migration.run()).isEqualTo(Migration.Result.PARTIAL);
    }

    @Test
    default void runShouldReturnPartialWhenSavingBlobsFails() throws Exception {
        CassandraAttachmentDAO attachmentDAO = mock(CassandraAttachmentDAO.class);
        CassandraAttachmentDAOV2 attachmentDAOV2 = mock(CassandraAttachmentDAOV2.class);
        CassandraBlobsDAO blobsDAO = mock(CassandraBlobsDAO.class);
        AttachmentV2Migration migration = new AttachmentV2Migration(attachmentDAO, attachmentDAOV2, blobsDAO);

        when(attachmentDAO.retrieveAll()).thenReturn(Stream.of(
            attachment1,
            attachment2));
        when(blobsDAO.save(any())).thenThrow(new RuntimeException());

        assertThat(migration.run()).isEqualTo(Migration.Result.PARTIAL);
    }

    @Test
    default void runShouldReturnPartialWhenSavingAttachmentV2Fail() throws Exception {
        CassandraAttachmentDAO attachmentDAO = mock(CassandraAttachmentDAO.class);
        CassandraAttachmentDAOV2 attachmentDAOV2 = mock(CassandraAttachmentDAOV2.class);
        CassandraBlobsDAO blobsDAO = mock(CassandraBlobsDAO.class);
        AttachmentV2Migration migration = new AttachmentV2Migration(attachmentDAO, attachmentDAOV2, blobsDAO);

        when(attachmentDAO.retrieveAll()).thenReturn(Stream.of(
            attachment1,
            attachment2));
        when(blobsDAO.save(attachment1.getBytes()))
            .thenReturn(CompletableFuture.completedFuture(BLOB_ID_FACTORY.forPayload(attachment1.getBytes())));
        when(blobsDAO.save(attachment2.getBytes()))
            .thenReturn(CompletableFuture.completedFuture(BLOB_ID_FACTORY.forPayload(attachment2.getBytes())));
        when(attachmentDAOV2.storeAttachment(any())).thenThrow(new RuntimeException());

        assertThat(migration.run()).isEqualTo(Migration.Result.PARTIAL);
    }

    @Test
    default void runShouldReturnPartialWhenDeleteV1AttachmentFail() throws Exception {
        CassandraAttachmentDAO attachmentDAO = mock(CassandraAttachmentDAO.class);
        CassandraAttachmentDAOV2 attachmentDAOV2 = mock(CassandraAttachmentDAOV2.class);
        CassandraBlobsDAO blobsDAO = mock(CassandraBlobsDAO.class);
        AttachmentV2Migration migration = new AttachmentV2Migration(attachmentDAO, attachmentDAOV2, blobsDAO);

        when(attachmentDAO.retrieveAll()).thenReturn(Stream.of(
            attachment1,
            attachment2));
        when(blobsDAO.save(attachment1.getBytes()))
            .thenReturn(CompletableFuture.completedFuture(BLOB_ID_FACTORY.forPayload(attachment1.getBytes())));
        when(blobsDAO.save(attachment2.getBytes()))
            .thenReturn(CompletableFuture.completedFuture(BLOB_ID_FACTORY.forPayload(attachment2.getBytes())));
        when(attachmentDAOV2.storeAttachment(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(attachmentDAO.deleteAttachment(any())).thenThrow(new RuntimeException());

        assertThat(migration.run()).isEqualTo(Migration.Result.PARTIAL);
    }

    @Test
    default void runShouldReturnPartialWhenAtLeastOneAttachmentMigrationFails() throws Exception {
        CassandraAttachmentDAO attachmentDAO = mock(CassandraAttachmentDAO.class);
        CassandraAttachmentDAOV2 attachmentDAOV2 = mock(CassandraAttachmentDAOV2.class);
        CassandraBlobsDAO blobsDAO = mock(CassandraBlobsDAO.class);
        AttachmentV2Migration migration = new AttachmentV2Migration(attachmentDAO, attachmentDAOV2, blobsDAO);

        when(attachmentDAO.retrieveAll()).thenReturn(Stream.of(
            attachment1,
            attachment2));
        when(blobsDAO.save(attachment1.getBytes()))
            .thenReturn(CompletableFuture.completedFuture(BLOB_ID_FACTORY.forPayload(attachment1.getBytes())));
        when(blobsDAO.save(attachment2.getBytes()))
            .thenThrow(new RuntimeException());
        when(attachmentDAOV2.storeAttachment(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(attachmentDAO.deleteAttachment(any())).thenReturn(CompletableFuture.completedFuture(null));

        assertThat(migration.run()).isEqualTo(Migration.Result.PARTIAL);
    }

}