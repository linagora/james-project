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
package org.apache.james.mailbox.backup.zip;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.google.common.io.FileBackedOutputStream;

public class ZipEntryIterator implements Iterator<ZipEntryWithContent>, Closeable {
    private static final int FILE_THRESHOLD = 32768;
    private static final boolean RESET_ON_FINALIZE = true;

    private final ZipInputStream zipInputStream;
    private Optional<ZipEntry> next;

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipEntryIterator.class);

    public ZipEntryIterator(ZipInputStream inputStream) {
        zipInputStream = inputStream;
        try {
            next = Optional.ofNullable(zipInputStream.getNextEntry());
        } catch (IOException e) {
            //EMPTY STREAM
            next = Optional.empty();
        }
    }

    @Override
    public boolean hasNext() {
        return next.isPresent();
    }

    @Override
    public ZipEntryWithContent next() {
        Optional<ZipEntry> current = next;
        if (!current.isPresent()) {
            return null;
        }

        ZipEntry currentEntry = current.get();
        Optional<ByteSource> content = getCurrentEntryContent(currentEntry);

        readNextEntry();

        return new ZipEntryWithContent(currentEntry, content);
    }

    private void readNextEntry() {
        try {
            next = Optional.ofNullable(zipInputStream.getNextEntry());
        } catch (IOException e) {
            LOGGER.error("Error when reading archive", e);
            next = Optional.empty();
        }
    }

    @Override
    public void close() throws IOException {
        zipInputStream.close();
    }

    private Optional<ByteSource> getCurrentEntryContent(ZipEntry current) {
        if (current.isDirectory()) {
            return Optional.empty();
        } else {
            FileBackedOutputStream currentContent = new FileBackedOutputStream(FILE_THRESHOLD, RESET_ON_FINALIZE);
            try {
                IOUtils.copy(zipInputStream, currentContent);
                return Optional.of(currentContent.asByteSource());
            } catch (IOException e) {
                LOGGER.error("ERROR when copying content of current entry", e);
                try {
                    currentContent.close();
                } catch (IOException closeException) {
                    LOGGER.error("Error when closing entry output stream", closeException);
                }
                return Optional.empty();
            }
        }
    }

}
