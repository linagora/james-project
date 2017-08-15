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

package org.apache.james.jmap.model;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.mail.Flags;

import org.apache.james.mailbox.FlagsBuilder;
import org.apache.commons.lang.StringUtils;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class Keyword {
    public final static boolean FLAG_VALUE = true;
    private final static int FLAG_NAME_MIN_LENTH = 1;
    protected final static int FLAG_NAME_MAX_LENTH = 255;
    private static final CharMatcher FLAG_NAME_PATTERN = CharMatcher.JAVA_LETTER_OR_DIGIT
            .or(CharMatcher.is('$'));

    public final static Keyword DRAFT = new Keyword("$Draft", FLAG_VALUE);
    public final static Keyword SEEN = new Keyword("$Seen", FLAG_VALUE);
    public final static Keyword FLAGGED = new Keyword("$Flagged", FLAG_VALUE);
    public final static Keyword ANSWERED = new Keyword("$Answered", FLAG_VALUE);
    public final static Keyword DELETED = new Keyword("$Deleted", FLAG_VALUE);
    public final static Keyword RECENT = new Keyword("$Recent", FLAG_VALUE);

    public static final ImmutableList<Keyword> UNSUPPORTED_KEYWORDS = ImmutableList.of(Keyword.RECENT, Keyword.DELETED);

    public static final ImmutableBiMap<Flags, Keyword> IMAP_SYSTEM_FLAGS = ImmutableBiMap.<Flags, Keyword>builder()
            .put(new Flags(Flags.Flag.DRAFT), DRAFT)
            .put(new Flags(Flags.Flag.SEEN), SEEN)
            .put(new Flags(Flags.Flag.FLAGGED), FLAGGED)
            .put(new Flags(Flags.Flag.ANSWERED), ANSWERED)
            .put(new Flags(Flags.Flag.RECENT), RECENT)
            .put(new Flags(Flags.Flag.DELETED), DELETED)
            .build();

    private final String flagName;
    private final boolean flagValue;

    public Keyword(String flagName) {
        this(flagName, FLAG_VALUE);
    }
    public Keyword(String flagName, Boolean flagValue) {
        this.flagName = flagName;
        this.flagValue = flagValue;

        Preconditions.checkArgument(flagValue, "Flag value must be true");
        Preconditions.checkArgument(isValid(),
                "Flagname must not be null or empty, must have length form 1-255, must not contain charater with hex from '\u0000' to '\u00019' or {'(' ')' '{' ']' '%' '*' '\"' '\\'} ");
    }

    private boolean isValid() {
        if (StringUtils.isBlank(flagName)) {
            return false;
        }
        if (flagName.length() < FLAG_NAME_MIN_LENTH || flagName.length() > FLAG_NAME_MAX_LENTH) {
            return false;
        }
        if (!FLAG_NAME_PATTERN.matchesAllOf(flagName)) {
            return false;
        }
        return true;
    }

    public String getFlagName() {
        return flagName;
    }

    public Boolean getFlagValue() {
        return flagValue;
    }

    public static FlagsBuilder accumulator(FlagsBuilder accumulator, Keyword keyword) {
        return accumulator.add(IMAP_SYSTEM_FLAGS.inverse()
                .getOrDefault(keyword, new Flags(keyword.getFlagName())));
    }

    public static FlagsBuilder combiner(FlagsBuilder firstBuilder, FlagsBuilder secondBuilder) {
        return firstBuilder.add(secondBuilder.build());
    }


    public static Set<Keyword> fromSystemFlags(Flags flags) {
        return Stream.of(flags.getSystemFlags())
                .map(flag -> IMAP_SYSTEM_FLAGS.get(new Flags(flag)))
                .collect(Guavate.toImmutableSet());
    }

    public static Flags toFlagsWithFilterUnsupportedKeywords(Set<Keyword> keywords) {
        return keywords.stream()
            .filter(keyword -> !UNSUPPORTED_KEYWORDS.contains(keyword))
            .reduce(FlagsBuilder.builder(), Keyword::accumulator, Keyword::combiner)
            .build();
    }

    public static Flags toFlags(Set<Keyword> keywords) {
        return keywords.stream()
            .reduce(FlagsBuilder.builder(), Keyword::accumulator, Keyword::combiner)
            .build();
    }

    public static Flags toFlags(String... keywords) {
        return toFlags(
            ImmutableSet.copyOf(keywords)
            .stream()
            .map(keyword -> new Keyword(keyword, FLAG_VALUE))
            .collect(Guavate.toImmutableSet()));
    }

    public static Optional<ImmutableSet<Keyword>> toSetOfKeywords(Optional<Map<String, Boolean>> keywords) {
        return Optional.of(keywords.map(allKeywords -> allKeywords.entrySet()
            .stream()
            .map(entry -> new Keyword(entry.getKey(), entry.getValue()))
            .collect(Guavate.toImmutableSet())))
            .orElse(Optional.empty());
    }

    public static Optional<ImmutableMap<String, Boolean>> toMapOfStringKeywords(Optional<Flags> flags) {
        return Optional.of(flags.map(allFlags -> fromFlags(allFlags)
            .stream()
            .collect(Guavate.toImmutableMap(Keyword::getFlagName, Keyword::getFlagValue))))
            .orElse(Optional.empty());
    }

    private static Set<Keyword> fromFlags(Flags flags) {
        return Stream.concat(
            Stream.of(flags.getUserFlags())
                .map(flag -> new Keyword(flag, FLAG_VALUE)),
            Stream.of(flags.getSystemFlags())
                .map(flag -> IMAP_SYSTEM_FLAGS.get(new Flags(flag)))
                .filter(jmapFlag -> !UNSUPPORTED_KEYWORDS.contains(jmapFlag))
        ).collect(Guavate.toImmutableSet());
    }

    @Override
    public final boolean equals(Object other) {
        if (other instanceof Keyword) {
            Keyword otherKeyword = (Keyword) other;
            return Objects.equal(flagName, otherKeyword.flagName)
                && Objects.equal(flagValue, otherKeyword.flagValue);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(flagName, flagValue);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("flagName", flagName)
            .add("flagValue", flagValue)
            .toString();
    }

}
