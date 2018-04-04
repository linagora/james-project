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
import java.util.function.Predicate;

import org.apache.james.mdn.fields.Field;
import org.apache.james.mdn.fields.ReportingUserAgent;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class ReportingUserAgentParser implements FieldsParser.FieldParser {
    private static final Predicate<String> IS_NULL_OR_EMPTY = Strings::isNullOrEmpty;

    private final boolean strict;

    public ReportingUserAgentParser(boolean strict) {
        this.strict = strict;
    }

    @Override
    public Optional<Field> parse(String value) {
        Preconditions.checkNotNull(value);
        String trimedValue = value.trim();

        int semiColonPosition = trimedValue.indexOf(';');
        if (semiColonPosition < 1) {
            if (semiColonPosition == 0 && strict) {
                throw new IllegalArgumentException("Name can not be empty");
            }
            if (trimedValue.isEmpty()) {
                if (strict) {
                    throw new IllegalArgumentException("Expecting the filed not to be only folding white spaces");
                }
                return Optional.empty();
            }
            return Optional.of(new ReportingUserAgent(trimedValue.substring(semiColonPosition + 1)));
        }
        String name = trimedValue.substring(0, semiColonPosition).trim();
        String product = trimedValue.substring(semiColonPosition + 1).trim();

        return Optional.of(
            new ReportingUserAgent(
                name,
                Optional.of(product)
                    .filter(IS_NULL_OR_EMPTY.negate())));
    }
}
