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
import java.util.Optional;

import org.apache.james.mdn.action.mode.DispositionActionMode;
import org.apache.james.mdn.fields.Disposition;
import org.apache.james.mdn.fields.Error;
import org.apache.james.mdn.fields.ExtensionField;
import org.apache.james.mdn.fields.Field;
import org.apache.james.mdn.fields.FinalRecipient;
import org.apache.james.mdn.fields.Gateway;
import org.apache.james.mdn.fields.OriginalMessageId;
import org.apache.james.mdn.fields.OriginalRecipient;
import org.apache.james.mdn.fields.ReportingUserAgent;
import org.apache.james.mdn.sending.mode.DispositionSendingMode;
import org.apache.james.mdn.type.DispositionType;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class FieldFinder {
    private static final ImmutableList<String> RFC_DEFINED_FIELDS = ImmutableList.of(
        Disposition.FIELD_NAME,
        Error.FIELD_NAME,
        FinalRecipient.FIELD_NAME,
        Gateway.FIELD_NAME,
        OriginalRecipient.FIELD_NAME,
        OriginalMessageId.FIELD_NAME,
        ReportingUserAgent.FIELD_NAME);
    public static final Disposition DEFAULT_DISPOSITION = Disposition.builder()
        .actionMode(DispositionActionMode.Automatic)
        .sendingMode(DispositionSendingMode.Automatic)
        .type(DispositionType.Processed)
        .build();

    private final boolean strict;
    private final ImmutableList<Field> fields;

    public FieldFinder(boolean strict, List<Field> fields) {
        Preconditions.checkNotNull(fields);

        this.strict = strict;
        this.fields = ImmutableList.copyOf(fields);
    }

    public List<ExtensionField> retrieveExtensions() {
        return fields.stream()
            .filter(field -> !RFC_DEFINED_FIELDS.contains(field.getFieldName()))
            .map(field -> (ExtensionField) field)
            .collect(Guavate.toImmutableList());
    }

    public Disposition retrieveDisposition() {
        return retrieveMonoField(Disposition.FIELD_NAME)
            .map(field -> (Disposition) field)
            .orElseGet(this::handleMissingDisposition);
    }

    private Disposition handleMissingDisposition() {
        if (strict) {
            throw new IllegalStateException("Missing compulsory " + Disposition.FIELD_NAME + " field");
        }
        return DEFAULT_DISPOSITION;
    }

    public Optional<FinalRecipient> retrieveFinalRecipient() {
        Optional<Field> field = retrieveMonoField(FinalRecipient.FIELD_NAME);

        if (strict && !field.isPresent()) {
            throw new IllegalStateException("Missing compulsory " + FinalRecipient.FIELD_NAME + " field");
        }

        return field.map(value -> (FinalRecipient) value);
    }

    public Optional<OriginalMessageId> retrieveOriginalMessageId() {
        return retrieveMonoField(OriginalMessageId.FIELD_NAME)
            .map(field -> (OriginalMessageId) field);
    }

    public Optional<OriginalRecipient> retrieveOriginalRecipient() {
        return retrieveMonoField(OriginalRecipient.FIELD_NAME)
            .map(field -> (OriginalRecipient) field);
    }

    public Optional<Gateway> retrieveGateway() {
        return retrieveMonoField(Gateway.FIELD_NAME)
            .map(field -> (Gateway) field);
    }

    public Optional<ReportingUserAgent> retrieveReportingUserAgent() {
        return retrieveMonoField(ReportingUserAgent.FIELD_NAME)
            .map(field -> (ReportingUserAgent) field);
    }

    public List<Error> retrieveErrors() {
        return fields.stream()
            .filter(field -> field.getFieldName().equals(Error.FIELD_NAME))
            .map(field -> (Error) field)
            .collect(Guavate.toImmutableList());
    }

    private Optional<Field> retrieveMonoField(String fieldName) {
        ImmutableList<Field> matchingFields = fields.stream()
            .filter(field -> field.getFieldName().equals(fieldName))
            .collect(Guavate.toImmutableList());

        if (matchingFields.size() > 1) {
            if (strict) {
                throw new IllegalStateException(fieldName + " is duplicated.");
            }
        }

        return  matchingFields.stream().findFirst();
    }
}
