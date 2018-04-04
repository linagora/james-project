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

package org.apache.james.mdn;

import java.util.List;
import java.util.Optional;

import org.apache.james.mdn.fields.Field;
import org.apache.james.mdn.parsing.FieldFinder;
import org.apache.james.mdn.parsing.FieldsParser;
import org.apache.james.mdn.parsing.UnparsedField;
import org.apache.james.mdn.parsing.Unwrapper;
import org.apache.james.util.OptionalUtils;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Strings;

public class MDNReportParser {
    private static final boolean STRICT = true;
    private static final boolean LENIENT = false;
    public static final boolean DEFAULT_STRICT = true;

    private final Optional<Boolean> strict;

    public MDNReportParser() {
        this(Optional.empty());
    }

    MDNReportParser(Optional<Boolean> strict) {
        this.strict = strict;
    }

    public MDNReportParser strict() {
        return new MDNReportParser(Optional.of(STRICT));
    }

    public MDNReportParser lenient() {
        return new MDNReportParser(Optional.of(LENIENT));
    }

    public Optional<MDNReport> parse(String mdnReport) {
        boolean strict = this.strict.orElse(DEFAULT_STRICT);
        List<String> unwrappedLines = Unwrapper.unwrap(mdnReport);
        List<UnparsedField> unparsedFields = toUnparsedFields(strict, unwrappedLines);
        List<Field> fields = new FieldsParser(strict).parse(unparsedFields);
        FieldFinder fieldFinder = new FieldFinder(strict, fields);

        return fieldFinder.retrieveFinalRecipient()
            .map(finalRecipient ->
                MDNReport.builder()
                    .dispositionField(fieldFinder.retrieveDisposition())
                    .finalRecipientField(finalRecipient)
                    .reportingUserAgentField(fieldFinder.retrieveReportingUserAgent())
                    .originalRecipientField(fieldFinder.retrieveOriginalRecipient())
                    .originalMessageIdField(fieldFinder.retrieveOriginalMessageId())
                    .gatewayField(fieldFinder.retrieveGateway())
                    .addErrorFields(fieldFinder.retrieveErrors())
                    .withExtensionFields(fieldFinder.retrieveExtensions())
                    .build())

        ;
    }

    private List<UnparsedField> toUnparsedFields(boolean strict, List<String> unwrappedLines) {
        UnparsedField.Factory unparsedFieldFactory = new UnparsedField.Factory(strict);
        return unwrappedLines.stream()
            .filter(s -> !Strings.isNullOrEmpty(s.trim()))
            .map(unparsedFieldFactory::parse)
            .flatMap(OptionalUtils::toStream)
            .collect(Guavate.toImmutableList());
    }
}
