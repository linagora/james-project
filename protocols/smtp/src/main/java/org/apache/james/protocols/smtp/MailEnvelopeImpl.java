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


package org.apache.james.protocols.smtp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.google.common.base.Throwables;
import com.google.common.io.FileBackedOutputStream;

/**
 * MailEnvelope implementation which stores everything in memory, backed in files
 */
public class MailEnvelopeImpl implements MailEnvelope {

    public static final int FILE_THRESHOLD = 100 * 1024;

    private final List<MailAddress> recipients;
    private final MailAddress sender;

    private FileBackedOutputStream outputStream;

    public MailEnvelopeImpl(List<MailAddress> recipients, MailAddress sender) {
        this.recipients = recipients;
        this.sender = sender;
    }

    @Override
    public long getSize() {
        if (outputStream == null)
            return -1;
        try {
            return outputStream.asByteSource().size();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public List<MailAddress> getRecipients() {
        return recipients;
    }

    @Override
    public MailAddress getSender() {
        return sender;
    }

    @Override
    public OutputStream getMessageOutputStream() {
        if (outputStream == null) {
            this.outputStream = new FileBackedOutputStream(FILE_THRESHOLD);
        }
        return outputStream;
    }

    @Override
    public InputStream getMessageInputStream() {
        try {
            return outputStream.asByteSource().openStream();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public void close() throws IOException {
        if (outputStream != null) {
            outputStream.close();
        }
    }
}


