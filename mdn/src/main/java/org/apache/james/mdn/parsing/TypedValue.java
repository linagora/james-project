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
import java.util.function.Function;

import org.apache.james.mdn.fields.AddressType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public class TypedValue {

    public static class Factory {
        private final boolean strict;
        private final AddressType defaultType;

        public Factory(boolean strict, AddressType defaultType) {
            this.strict = strict;
            this.defaultType = defaultType;
        }

        public TypedValue parse(String input) {
            if (strict) {
                return strict(input);
            }
            return lenient(input);
        }

        public TypedValue strict(String input) {
            return parse(input, errorMessage -> {
                throw new IllegalArgumentException(errorMessage);
            });
        }

        public TypedValue lenient(String input) {
            return parse(input,
                errorMessage ->
                    new TypedValue(defaultType, sanitizeValue(input)));
        }

        private String sanitizeValue(String input) {
            int typeSeparatorPosition = input.indexOf(';');
            return input.substring(typeSeparatorPosition + 1);
        }

        private TypedValue parse(String input, Function<String, TypedValue> fail) {
            Preconditions.checkNotNull(input);
            int typeSeparatorPosition = input.indexOf(';');
            if (typeSeparatorPosition <= 1) {
                return fail.apply("Type can not be empty");
            }
            String type = input.substring(0, typeSeparatorPosition);
            if (type.contains("\n")) {
                return fail.apply("Type can not contain line breaks");
            }
            if (type.trim().isEmpty()) {
                return fail.apply("Type can not be empty or only composed of white spaces");
            }
            String value = input.substring(typeSeparatorPosition + 1);
            return new TypedValue(new AddressType(type), value.trim());
        }

    }

    private final AddressType type;
    private final String value;

    @VisibleForTesting
    TypedValue(AddressType type, String value) {
        this.type = type;
        this.value = value;
    }

    public AddressType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof TypedValue) {
            TypedValue that = (TypedValue) o;

            return Objects.equals(this.type, that.type)
                && Objects.equals(this.value, that.value);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(type, value);
    }
}
