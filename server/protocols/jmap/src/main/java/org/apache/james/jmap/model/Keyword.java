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

import java.util.Set;
import java.util.stream.Stream;
import javax.mail.Flags;

import org.apache.james.mailbox.FlagsBuilder;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@JsonSerialize(using = KeywordSerialization.class)
public class Keyword {
    public final static Keyword DRAFT = new Keyword("$Draft");
    public final static Keyword SEEN = new Keyword("$Seen");
    public final static Keyword FLAGGED = new Keyword("$Flagged");
    public final static Keyword ANSWERED = new Keyword("$Answered");
    public final static Keyword DELETED = new Keyword("$Deleted");
    public final static Keyword RECENT = new Keyword("$Recent");

    public static final ImmutableList<Keyword> UNSUPPORTED_KEYWORDS = ImmutableList.of(Keyword.RECENT, Keyword.DELETED);

    public static final ImmutableMap<Flags.Flag, Keyword> IMAP_SYSTEM_FLAGS = ImmutableMap.<Flags.Flag, Keyword>builder()
            .put(Flags.Flag.DRAFT, DRAFT)
            .put(Flags.Flag.SEEN, SEEN)
            .put(Flags.Flag.FLAGGED, FLAGGED)
            .put(Flags.Flag.ANSWERED, ANSWERED)
            .put(Flags.Flag.RECENT, RECENT)
            .put(Flags.Flag.DELETED, DELETED)
            .build();

    private final String flag;

    public Keyword(String flag) {
        Preconditions.checkNotNull(flag);
        this.flag = flag;
    }

    public String getFlag() {
        return flag;
    }

    public static FlagsBuilder accumulator(FlagsBuilder accumulator, Keyword keyword) {
        return accumulator.add(IMAP_SYSTEM_FLAGS.entrySet()
            .stream()
            .filter(entry -> entry.getValue().equals(keyword))
            .map(entry -> new Flags(entry.getKey()))
            .findAny()
            .orElse(new Flags(keyword.getFlag())));
    }

    public static FlagsBuilder combiner(FlagsBuilder firstBuilder, FlagsBuilder secondBuilder) {
        return firstBuilder.add(secondBuilder.build());
    }

    public static Set<Keyword> fromFlags(Flags flags) {
        return Stream.concat(
            Stream.of(flags.getUserFlags())
                .map(Keyword::new),
            Stream.of(flags.getSystemFlags())
                .map(flag -> IMAP_SYSTEM_FLAGS.get(flag))
                .filter(jmapFlag -> !UNSUPPORTED_KEYWORDS.contains(jmapFlag))
        ).collect(Guavate.toImmutableSet());
    }

    public static Set<Keyword> fromSystemFlags(Flags flags) {
        return Stream.of(flags.getSystemFlags())
                    .map(flag -> IMAP_SYSTEM_FLAGS.get(flag))
                    .collect(Guavate.toImmutableSet());
    }

    public static Flags fromKeywordsWithFilterUnsupportedKeywords(Set<Keyword> keywords) {
        return keywords.stream()
            .filter(keyword -> !UNSUPPORTED_KEYWORDS.contains(keyword))
            .reduce(FlagsBuilder.builder(), Keyword::accumulator, Keyword::combiner)
            .build();
    }

    public static Flags fromKeywords(Set<Keyword> keywords) {
        return keywords.stream()
            .reduce(FlagsBuilder.builder(), Keyword::accumulator, Keyword::combiner)
            .build();
    }

    public static Flags fromKeywordsWithFilterUnsupportedKeywords(String... keywords) {
        return fromKeywordsWithFilterUnsupportedKeywords(ImmutableSet.copyOf(keywords)
            .stream()
            .map(Keyword::new)
            .collect(Guavate.toImmutableSet()));
    }

    @Override
    public final boolean equals(Object other) {
        if (other instanceof Keyword) {
            return Objects.equal(flag, ((Keyword) other).flag);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(flag);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("flag", flag)
            .toString();
    }

}
