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

package org.apache.james.util.io;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import com.google.common.primitives.Ints;

public class ExposedByteArrayOutputStream extends ByteArrayOutputStream {

    public static final int DEFAULT_ARRAY_SIZE = 32;

    public ExposedByteArrayOutputStream(int size) {
        super(sanitizeSize(size));
    }

    public ExposedByteArrayOutputStream(long size) {
        super(Ints.checkedCast(sanitizeSize(size)));
    }

    private static int sanitizeSize(int size) {
        if (size < 0) {
            return DEFAULT_ARRAY_SIZE;
        }
        return size;
    }

    private static long sanitizeSize(long size) {
        if (size < 0) {
            return DEFAULT_ARRAY_SIZE;
        }
        return size;
    }

    @Override
    public synchronized byte[] toByteArray() {
        return ByteBuffer.wrap(buf, 0, count).array();
    }

    byte[] getUnderlying() {
        return buf;
    }
}
