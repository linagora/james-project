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

package org.apache.james.mdn.parsing;

import java.util.Optional;

import org.apache.james.mdn.fields.Field;
import org.apache.james.mdn.fields.OriginalMessageId;

import com.google.common.base.Preconditions;

public class OriginalMessageIdParser implements FieldsParser.FieldParser {
    private final boolean strict;

    public OriginalMessageIdParser(boolean strict) {
        this.strict = strict;
    }

    @Override
    public Optional<Field> parse(String value) {
        Preconditions.checkNotNull(value);

        if (value.contains("\n")) {
            return handleBreakLineFailure();
        }
        if (value.trim().isEmpty()) {
            return handleEmptyFailure();
        }
        return Optional.of(new OriginalMessageId(value.trim()));
    }

    private Optional<Field> handleEmptyFailure() {
        if (strict) {
            throw new IllegalArgumentException(OriginalMessageId.FIELD_NAME + " value can not be empty or folding white spaces");
        }
        return Optional.empty();
    }

    private Optional<Field> handleBreakLineFailure() {
        if (strict) {
            throw new IllegalArgumentException(OriginalMessageId.FIELD_NAME + " value can not contain line breaks");
        }
        return Optional.empty();
    }
}
