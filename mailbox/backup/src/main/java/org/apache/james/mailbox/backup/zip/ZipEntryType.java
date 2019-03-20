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

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

public enum ZipEntryType {

    MAILBOX(ZipEntryType.MAILBOX_VALUE),
    MAILBOX_ANNOTATION_DIR(ZipEntryType.MAILBOX_ANNOTATION_DIR_VALUE),
    MAILBOX_ANNOTATION(ZipEntryType.MAILBOX_ANNOTATION_VALUE),
    MESSAGE(ZipEntryType.MESSAGE_VALUE);

    private final long value;

    ZipEntryType(long value) {
        this.value = value;
    }

    private static final long MAILBOX_VALUE = 1L;
    private static final long MAILBOX_ANNOTATION_DIR_VALUE = 2L;
    private static final long MAILBOX_ANNOTATION_VALUE = 3L;
    private static final long MESSAGE_VALUE = 4L;

    private static final Map<Long, ZipEntryType> entryByValue = ImmutableMap.of(MAILBOX_VALUE, MAILBOX,
        MAILBOX_ANNOTATION_DIR_VALUE, MAILBOX_ANNOTATION_DIR,
        MAILBOX_ANNOTATION_VALUE, MAILBOX_ANNOTATION,
        MESSAGE_VALUE, MESSAGE
    );

    public long getValue() {
        return value;
    }

    public static Optional<ZipEntryType> getFromValue(long value) {
        return Optional.ofNullable(entryByValue.get(value));
    }
}
