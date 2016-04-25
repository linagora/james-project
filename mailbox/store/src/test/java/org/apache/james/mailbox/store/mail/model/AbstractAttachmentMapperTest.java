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

package org.apache.james.mailbox.store.mail.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NullArgumentException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.Content;
import org.apache.james.mailbox.store.mail.AttachmentBlobNotFoundException;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.model.impl.SimpleAttachment;
import org.apache.james.mailbox.store.streaming.ByteContent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;

/**
 * Generic purpose tests for your implementation MailboxMapper.
 * 
 * You then just need to instantiate your mailbox mapper and an IdGenerator.
 */
public abstract class AbstractAttachmentMapperTest<Id extends MailboxId> {
    

    private final MapperProvider<Id> mapperProvider;
    private AttachmentMapper attachmentMapper;

    public AbstractAttachmentMapperTest(MapperProvider<Id> mapperProvider) {
        this.mapperProvider = mapperProvider;
    }

    @Before
    public void setUp() throws MailboxException {
        mapperProvider.ensureMapperPrepared();
        attachmentMapper = mapperProvider.createAttachmentMapper();
    }

    @After
    public void tearDown() throws MailboxException {
        mapperProvider.clearMapper();
    }

    @Test
    public void putShouldWork() throws IOException {
        // Given
        InMemoryAttachmentId blobId = InMemoryAttachmentId.of(1);
        String expectedContent = "content";
        Content content = new ByteContent(expectedContent.getBytes(Charsets.UTF_8));
        Attachment newAttachment = new SimpleAttachment(blobId, content);

        // When
        attachmentMapper.put(newAttachment);

        // Then
        Attachment actual = attachmentMapper.get(blobId);
        String actualContentAsString = IOUtils.toString(actual.getContent().getInputStream(), Charsets.UTF_8);
        assertThat(actualContentAsString).isEqualTo(expectedContent);
    }

    @Test
    public void getShouldWork() throws IOException {
        // Given
        String expectedContent = "content";
        InMemoryAttachmentId blobId = InMemoryAttachmentId.of(1);
        saveAsNewAttachment(blobId, expectedContent);

        // When
        Attachment actual = attachmentMapper.get(blobId);
        String actualContentAsString = IOUtils.toString(actual.getContent().getInputStream(), Charsets.UTF_8);

        // Then
        assertThat(actualContentAsString).isEqualTo(expectedContent);
    }

    @Test
    public void deleteShouldWork() throws IOException {
        // Given
        String expectedContent = "content";
        final InMemoryAttachmentId blobId = InMemoryAttachmentId.of(1);
        saveAsNewAttachment(blobId, expectedContent);

        // When
        attachmentMapper.delete(blobId);

        // Then
        try {
            attachmentMapper.get(blobId);
            fail("Should have thrown AttachmentBlobNotFoundException while accessing removed item !");
        }
        catch(AttachmentBlobNotFoundException e) {
        }
    }

    private void saveAsNewAttachment(AttachmentId blobId, String expectedContent) {
        Content content = new ByteContent(expectedContent.getBytes(Charsets.UTF_8));
        Attachment newAttachment = new SimpleAttachment(blobId, content);
        attachmentMapper.put(newAttachment);
    }

    @Test
    public void deleteShouldNotThrowWhenAttachmentIdNotFound() {
        InMemoryAttachmentId blobId = InMemoryAttachmentId.of(-1);
        attachmentMapper.delete(blobId);
    }

    @Test(expected = AttachmentBlobNotFoundException.class)
    public void getShouldThrowWhenAttachmentIdNotFound() {
        InMemoryAttachmentId blobId = InMemoryAttachmentId.of(-1);
        attachmentMapper.get(blobId);
    }

    @Test(expected = NullArgumentException.class)
    public void deleteShouldThrowWhenIdIsNull() {
        attachmentMapper.delete(null);
    }

    @Test(expected = NullArgumentException.class)
    public void getShouldThrowWhenIdIsNull() {
        attachmentMapper.get(null);
    }

    public static class InMemoryAttachmentId implements AttachmentId {

        public static InMemoryAttachmentId of(long value) {
            return new InMemoryAttachmentId(value);
        }

        private final long value;

        private InMemoryAttachmentId(long value) {
            this.value = value;
        }

        @Override
        public String serialize() {
            return String.valueOf(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            InMemoryAttachmentId that = (InMemoryAttachmentId) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }

}
