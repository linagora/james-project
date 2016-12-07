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

package org.apache.james.mailbox.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import javax.mail.util.SharedFileInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMessage;
import org.apache.james.mailbox.store.streaming.BodyOffsetInputStream;
import org.apache.james.mailbox.store.streaming.CountingInputStream;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.message.DefaultBodyDescriptorBuilder;
import org.apache.james.mime4j.message.HeaderImpl;
import org.apache.james.mime4j.message.MaximalBodyDescriptor;
import org.apache.james.mime4j.stream.EntityState;
import org.apache.james.mime4j.stream.MimeConfig;
import org.apache.james.mime4j.stream.MimeTokenStream;
import org.apache.james.mime4j.stream.RecursionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class MimeMessageReader {

    private static final Logger LOG = LoggerFactory.getLogger(MimeMessageReader.class);

    private final MessageParser messageParser;
    private final InputStream msgIn;
    private File file;
    private TeeInputStream tmpMsgIn;
    private BodyOffsetInputStream bIn;
    private FileOutputStream out;
    private SharedFileInputStream contentIn;

    public MimeMessageReader(MessageParser messageParser, InputStream msgIn) {
        Preconditions.checkNotNull(msgIn);
        this.messageParser = messageParser;
        this.msgIn = msgIn;
    }

    public Message read(MessageId messageId, Date internalDate) throws IOException, MimeException {
        init();
        PropertyBuilder propertyBuilder = buildProperties(bIn);
        finishCopyInitialStream();
        int bodyStartOctet = (int) bIn.getBodyStartOffset();
        if (bodyStartOctet == -1) {
            bodyStartOctet = 0;
        }
        contentIn = new SharedFileInputStream(file);
        int size = (int) file.length();
        List<MessageAttachment> attachments = extractAttachments(contentIn);

        return new SimpleMessage(messageId, contentIn, size, internalDate, propertyBuilder.getSubType(), propertyBuilder.getMediaType(),
            bodyStartOctet, propertyBuilder.getTextualLineCount(), propertyBuilder.toProperties(), attachments);
    }

    private void init() throws IOException {
        // Create a temporary file and copy the message to it. We will work
        // with the file as
        // source for the InputStream
        file = File.createTempFile("imap", ".msg");
        out = new FileOutputStream(file);
        tmpMsgIn = new TeeInputStream(msgIn, out);
        bIn = new BodyOffsetInputStream(tmpMsgIn);
    }

    private void finishCopyInitialStream() throws IOException {
        byte[] discard = new byte[4096];
        while (tmpMsgIn.read(discard) != -1) {
            // consume the rest of the stream so everything get copied to
            // the file now
            // via the TeeInputStream
        }
    }

    @VisibleForTesting
    PropertyBuilder buildProperties(InputStream bIn) throws IOException, MimeException {
        // Disable line length... This should be handled by the smtp server
        // component and not the parser itself
        // https://issues.apache.org/jira/browse/IMAP-122
        MimeConfig config = MimeConfig.custom().setMaxLineLen(-1).setMaxHeaderLen(-1).build();

        final MimeTokenStream parser = new MimeTokenStream(config, new DefaultBodyDescriptorBuilder());

        parser.setRecursionMode(RecursionMode.M_NO_RECURSE);
        parser.parse(bIn);
        final HeaderImpl header = new HeaderImpl();

        EntityState next = parser.next();
        while (next != EntityState.T_BODY && next != EntityState.T_END_OF_STREAM && next != EntityState.T_START_MULTIPART) {
            if (next == EntityState.T_FIELD) {
                header.addField(parser.getField());
            }
            next = parser.next();
        }
        final MaximalBodyDescriptor descriptor = (MaximalBodyDescriptor) parser.getBodyDescriptor();
        final PropertyBuilder propertyBuilder = new PropertyBuilder();
        final String mediaType;
        final String mediaTypeFromHeader = descriptor.getMediaType();
        final String subType;
        if (mediaTypeFromHeader == null) {
            mediaType = "text";
            subType = "plain";
        } else {
            mediaType = mediaTypeFromHeader;
            subType = descriptor.getSubType();
        }
        propertyBuilder.setMediaType(mediaType);
        propertyBuilder.setSubType(subType);
        propertyBuilder.setContentID(descriptor.getContentId());
        propertyBuilder.setContentDescription(descriptor.getContentDescription());
        propertyBuilder.setContentLocation(descriptor.getContentLocation());
        propertyBuilder.setContentMD5(descriptor.getContentMD5Raw());
        propertyBuilder.setContentTransferEncoding(descriptor.getTransferEncoding());
        propertyBuilder.setContentLanguage(descriptor.getContentLanguage());
        propertyBuilder.setContentDispositionType(descriptor.getContentDispositionType());
        propertyBuilder.setContentDispositionParameters(descriptor.getContentDispositionParameters());
        propertyBuilder.setContentTypeParameters(descriptor.getContentTypeParameters());
        // Add missing types
        final String codeset = descriptor.getCharset();
        if (codeset == null) {
            if ("TEXT".equalsIgnoreCase(mediaType)) {
                propertyBuilder.setCharset("us-ascii");
            }
        } else {
            propertyBuilder.setCharset(codeset);
        }

        final String boundary = descriptor.getBoundary();
        if (boundary != null) {
            propertyBuilder.setBoundary(boundary);
        }
        if ("text".equalsIgnoreCase(mediaType)) {
            final CountingInputStream bodyStream = new CountingInputStream(parser.getInputStream());
            bodyStream.readAll();
            long lines = bodyStream.getLineCount();
            bodyStream.close();
            next = parser.next();
            if (next == EntityState.T_EPILOGUE) {
                final CountingInputStream epilogueStream = new CountingInputStream(parser.getInputStream());
                epilogueStream.readAll();
                lines += epilogueStream.getLineCount();
                epilogueStream.close();

            }
            propertyBuilder.setTextualLineCount(lines);
        }
        return propertyBuilder;
    }

    private List<MessageAttachment> extractAttachments(SharedFileInputStream contentIn) {
        try {
            return messageParser.retrieveAttachments(contentIn);
        } catch (Exception e) {
            LOG.warn("Error while parsing mail's attachments: " + e.getMessage(), e);
            return ImmutableList.of();
        }
    }

    public void close() {
        IOUtils.closeQuietly(bIn);
        IOUtils.closeQuietly(tmpMsgIn);
        IOUtils.closeQuietly(out);
        IOUtils.closeQuietly(contentIn);

        // delete the temporary file if one was specified
        if (file != null) {
            file.delete();
        }
    }
}
