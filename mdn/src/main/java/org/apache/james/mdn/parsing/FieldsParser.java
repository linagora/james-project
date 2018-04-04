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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.mdn.fields.Disposition;
import org.apache.james.mdn.fields.Error;
import org.apache.james.mdn.fields.Field;
import org.apache.james.mdn.fields.FinalRecipient;
import org.apache.james.mdn.fields.Gateway;
import org.apache.james.mdn.fields.OriginalMessageId;
import org.apache.james.mdn.fields.OriginalRecipient;
import org.apache.james.mdn.fields.ReportingUserAgent;
import org.apache.james.util.OptionalUtils;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public class FieldsParser {
    public interface FieldParser {
        Optional<Field> parse(String value);
    }

    private static final ImmutableMap<String, Function<Boolean, FieldParser>> BUILT_IN_FIELD_PARSERS =
        ImmutableMap.<String, Function<Boolean, FieldParser>>builder()
            .put(Error.FIELD_NAME, ErrorParser::new)
            .put(OriginalMessageId.FIELD_NAME, OriginalMessageIdParser::new)
            .put(OriginalRecipient.FIELD_NAME, OriginalRecipientParser::new)
            .put(FinalRecipient.FIELD_NAME, FinalRecipientParser::new)
            .put(Gateway.FIELD_NAME, GatewayParser::new)
            .put(Disposition.FIELD_NAME, DispositionParser::new)
            .put(ReportingUserAgent.FIELD_NAME, ReportingUserAgentParser::new)
            .build();

    private final ImmutableMap<String, FieldParser> fieldParsers;

    public FieldsParser(boolean strict) {
        this.fieldParsers = BUILT_IN_FIELD_PARSERS.entrySet()
            .stream()
            .map(entry -> Pair.of(
                entry.getKey().toLowerCase(Locale.US),
                entry.getValue().apply(strict)))
            .collect(Guavate.toImmutableMap(Pair::getKey, Pair::getValue));
    }

    public List<Field> parse(List<UnparsedField> unparsedFields) {
        Preconditions.checkNotNull(unparsedFields);

        return unparsedFields.stream()
            .map(field -> getFieldParser(field.getName()).parse(field.getValue()))
            .flatMap(OptionalUtils::toStream)
            .collect(Guavate.toImmutableList());
    }

    private FieldParser getFieldParser(String name) {
        return Optional.ofNullable(fieldParsers.get(name.toLowerCase(Locale.US)))
            .orElseGet(() -> new ExtensionFieldParser(name));
    }
}
