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

import java.util.Objects;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class UnparsedField {

    public static class Factory {
        private final boolean strict;

        public Factory(boolean strict) {
            this.strict = strict;
        }

        public Optional<UnparsedField> parse(String rawField) {
            Preconditions.checkNotNull(rawField);

            int nameDelimiterPosition = rawField.indexOf(':');
            if (nameDelimiterPosition <= 0) {
                return handleError("Can not parse name delimiter ':'. A non empty string need to prefix the first compulsory ':'");
            }

            String name = rawField.substring(0, nameDelimiterPosition);
            String value = rawField.substring(nameDelimiterPosition + 1);
            if (name.contains("\n")) {
                return handleError("Field name can not contain line breaks.");
            }
            return Optional.of(new UnparsedField(name, value));
        }

        private Optional<UnparsedField> handleError(String errorMessage) {
            if (strict) {
                throw new IllegalArgumentException(errorMessage);
            } else {
                return Optional.empty();
            }
        }
    }

    private final String name;
    private final String value;

    @VisibleForTesting
    UnparsedField(String name, String value) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(value);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
        Preconditions.checkArgument(!name.contains("\n"));

        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof UnparsedField) {
            UnparsedField that = (UnparsedField) o;

            return Objects.equals(this.name, that.name)
                && Objects.equals(this.value, that.value);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(name, value);
    }
}
