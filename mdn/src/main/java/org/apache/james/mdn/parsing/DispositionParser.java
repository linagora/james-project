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
import java.util.stream.Stream;

import org.apache.james.mdn.action.mode.DispositionActionMode;
import org.apache.james.mdn.fields.Disposition;
import org.apache.james.mdn.fields.Field;
import org.apache.james.mdn.modifier.DispositionModifier;
import org.apache.james.mdn.sending.mode.DispositionSendingMode;
import org.apache.james.mdn.type.DispositionType;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

public class DispositionParser implements FieldsParser.FieldParser {
    private final boolean strict;

    public DispositionParser(boolean strict) {
        this.strict = strict;
    }

    @Override
    public Optional<Field> parse(String value) {
        Preconditions.checkNotNull(value);

        int slashPosition = value.indexOf('/');
        Optional<DispositionActionMode> actionMode = parseActionMode(value, slashPosition);
        String afterActionMode = value.substring(slashPosition + 1);

        int semiColonPosition = afterActionMode.indexOf(';');
        Optional<DispositionSendingMode> sendingMode = parseSendingMode(afterActionMode, semiColonPosition);
        String afterSendingMode = afterActionMode.substring(semiColonPosition + 1);


        int secondSlashPosition = afterSendingMode.indexOf('/');
        Optional<DispositionType> type = parseType(afterSendingMode, secondSlashPosition);
        String afterType = getModifierString(afterSendingMode, secondSlashPosition);

        ImmutableList<DispositionModifier> modifiers = parseModifiers(afterType);

        return Optional.of(Disposition.builder()
            .actionMode(getActionMode(actionMode))
            .sendingMode(getSendingMode(sendingMode))
            .type(getType(type))
            .addModifiers(modifiers)
            .build());
    }

    private DispositionType getType(Optional<DispositionType> type) {
        if (strict) {
            return type.orElseThrow(
                () -> new IllegalArgumentException("Type not recognized"));
        }
        return type.orElse(DispositionType.Processed);
    }

    private DispositionSendingMode getSendingMode(Optional<DispositionSendingMode> sendingMode) {
        if (strict) {
            return sendingMode.orElseThrow(
                () -> new IllegalArgumentException("Sending mode not recognized"));
        }
        return sendingMode.orElse(DispositionSendingMode.Automatic);
    }

    private DispositionActionMode getActionMode(Optional<DispositionActionMode> actionMode) {
        if (strict) {
            return actionMode.orElseThrow(
                () -> new IllegalArgumentException("Action mode not recognized"));
        }
        return actionMode.orElse(DispositionActionMode.Automatic);
    }

    private String getModifierString(String afterSendingMode, int secondSlashPosition) {
        if (secondSlashPosition < 0) {
            return "";
        }
        return afterSendingMode.substring(secondSlashPosition + 1);
    }

    private ImmutableList<DispositionModifier> parseModifiers(String afterType) {
        return Splitter.on(',')
            .trimResults()
            .omitEmptyStrings()
            .splitToList(afterType)
            .stream()
            .flatMap(this::evictLineBreaks)
            .map(DispositionModifier::new)
            .collect(Guavate.toImmutableList());
    }

    private Stream<String> evictLineBreaks(String s) {
        if (s.contains("\n")) {
            if (strict) {
                throw new IllegalArgumentException("Modifiers can not contain line breaks");
            } else {
                return Stream.of();
            }
        }
        return Stream.of(s);
    }

    private Optional<DispositionType> parseType(String afterSendingMode, int secondSlashPosition) {
        if (secondSlashPosition < 1) {
            return DispositionType.fromString(afterSendingMode.trim());
        } else {
            String typeString = afterSendingMode.substring(0, secondSlashPosition).trim();
            return DispositionType.fromString(typeString);
        }
    }

    private Optional<DispositionSendingMode> parseSendingMode(String value, int semiColonPosition) {
        if (semiColonPosition < 1) {
            if (strict) {
                throw new IllegalArgumentException("No sending mode specified");
            }
            return Optional.empty();
        } else {
            String sendingModeString = value.substring(0, semiColonPosition).trim();
            return DispositionSendingMode.fromString(sendingModeString);
        }
    }

    private Optional<DispositionActionMode> parseActionMode(String value, int slashPosition) {
        if (slashPosition < 1) {
            if (strict) {
                throw new IllegalArgumentException("No action mode specified");
            }
            return Optional.empty();
        } else {
            String actionModeString = value.substring(0, slashPosition).trim();
            return DispositionActionMode.fromString(actionModeString);
        }
    }
}
