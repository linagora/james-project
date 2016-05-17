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

package org.apache.james.mailbox.store.mail.model.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.james.mailbox.store.mail.model.Attachment;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.message.MessageServiceFactoryImpl;

import com.google.common.collect.ImmutableList;

public class MessageParser {

    private final MessageBuilder messageBuilder;
    private final MessageWriter messageWriter;

    public MessageParser() {
        MessageServiceFactoryImpl messageServiceFactoryImpl = new MessageServiceFactoryImpl();
        messageBuilder = messageServiceFactoryImpl.newMessageBuilder();
        messageWriter = messageServiceFactoryImpl.newMessageWriter();
    }

    public List<Attachment> retrieveAttachments(InputStream fullContent) throws MimeException, IOException {
        ImmutableList.Builder<Attachment> attachments = ImmutableList.builder();
        Body body = messageBuilder.parseMessage(fullContent)
                .getBody();
        if (body instanceof Multipart) {
            Multipart multipart = (Multipart) body;
            for (Entity entity : multipart.getBodyParts()) {
                if (isAttachmentEntity(entity)) {
                    attachments.add(createAttachment(entity.getBody()));
                }
            }
            body.dispose();
        }
        return attachments.build();
    }
    
    private Attachment createAttachment(Body body) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        messageWriter.writeBody(body, out);
        body.dispose();
        return new Attachment(out.toByteArray());
    }

    private boolean isAttachmentEntity(Entity part) {
        return ContentDispositionField.DISPOSITION_TYPE_ATTACHMENT.equalsIgnoreCase(part.getDispositionType());
    }
}
