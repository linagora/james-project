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

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;
import javax.mail.Flags;

import org.apache.james.mailbox.FlagsBuilder;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class KeywordTest {
    private final static String FORWARDED = "forwarded";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldRespectBeanContract() {
        EqualsVerifier.forClass(Keyword.class).verify();
    }

    @Test
    public void keywordShouldThrowWhenFalseValue() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("Anyvalue", false);
    }

    @Test
    public void keywordShouldThrowWhenFlagNameLengthLessThenMinLength() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("", false);
    }

    @Test
    public void keywordShouldThrowWhenFlagNameLengthMoreThenMaxLength() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword(StringUtils.repeat("a", Keyword.FLAG_NAME_MAX_LENTH + 1), true);
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainInvalidCharacter() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a%", true);
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainSpaceCharacter() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a b", true);
    }

    @Test
    public void fromFlagsShouldReturnSetOfJmapFlag() throws Exception {
        Flags flags = FlagsBuilder.builder()
                .add(Flags.Flag.FLAGGED)
                .add(Flags.Flag.DRAFT)
                .build();

        assertThat(Keyword.fromFlags(flags)).containsOnly(Keyword.FLAGGED, Keyword.DRAFT);
    }

    @Test
    public void fromFlagsShouldReturnSetOfJmapFlagWhenUserFlag() throws Exception {
        Flags flags = FlagsBuilder.builder()
                .add(Flags.Flag.FLAGGED)
                .add(Flags.Flag.DRAFT)
                .add(FORWARDED)
                .build();

        assertThat(Keyword.fromFlags(flags)).containsOnly(Keyword.FLAGGED, Keyword.DRAFT, new Keyword(FORWARDED, true));
    }

    @Test
    public void fromFlagsShouldSupportCustomJmapFlag() throws Exception {
        Flags flags = FlagsBuilder.builder()
                .add(Flags.Flag.FLAGGED)
                .add(Flags.Flag.DRAFT)
                .add("Unknown")
                .build();
        assertThat(Keyword.fromFlags(flags)).containsOnly(Keyword.FLAGGED, Keyword.DRAFT, new Keyword("Unknown", true));
    }

    @Test
    public void fromFlagsShouldRemoveUnsupportedJmapFlag() throws Exception {
        Flags flags = FlagsBuilder.builder()
                .add(Flags.Flag.RECENT)
                .add(Flags.Flag.DELETED)
                .add(Flags.Flag.DRAFT)
                .add(FORWARDED)
                .build();

        assertThat(Keyword.fromFlags(flags)).containsOnly(Keyword.DRAFT, new Keyword(FORWARDED, true));
    }

    @Test
    public void fromSystemFlagsShouldAllKeywordsOfImapSystemFlag() throws Exception {
        Flags flags = FlagsBuilder.builder()
                .add(Flags.Flag.RECENT)
                .add(Flags.Flag.DELETED)
                .add(Flags.Flag.DRAFT)
                .add(FORWARDED)
                .build();

        assertThat(Keyword.fromSystemFlags(flags)).containsOnly(Keyword.DRAFT, Keyword.RECENT, Keyword.DELETED);
    }

    @Test
    public void fromKeywordsShouldBuildImapFlagFromKeywordsWithoutUnsupportedKeywords() throws Exception {
        Flags expectedFlags = FlagsBuilder.builder()
            .add(Flags.Flag.DRAFT)
            .build();
        Flags imapFlags = Keyword.fromKeywordsWithFilterUnsupportedKeywords(ImmutableSet.of(Keyword.DRAFT, Keyword.DELETED));

        assertThat(imapFlags).isEqualToComparingFieldByField(expectedFlags);
    }

    @Test
    public void fromKeywordsShouldBuildImapFlagWithUserFlagFromJmapFlags() throws Exception {
        Flags expectedFlags = FlagsBuilder.builder()
            .add(Flags.Flag.DRAFT)
            .add(FORWARDED)
            .build();
        Flags imapFlags = Keyword.fromKeywordsWithFilterUnsupportedKeywords(ImmutableSet.of(Keyword.DRAFT, new Keyword(FORWARDED, true)));

        assertThat(imapFlags).isEqualToComparingFieldByField(expectedFlags);
    }

    @Test
    public void fromKeywordsShouldReturnFlagsFromSetOfStringOfKeyword() throws Exception {
        Flags expected = FlagsBuilder.builder()
                .add(Flags.Flag.FLAGGED)
                .add(FORWARDED)
                .build();
        assertThat(Keyword.fromKeywordsWithFilterUnsupportedKeywords(ImmutableSet.of(Keyword.FLAGGED, new Keyword(FORWARDED, true))))
            .isEqualTo(expected);
    }

    @Test
    public void fromKeywordsShouldReturnFlagsFromListOfStringOfKeyword() throws Exception {
        Flags expected = FlagsBuilder.builder()
                .add(Flags.Flag.FLAGGED)
                .add(FORWARDED)
                .build();
        assertThat(Keyword.fromKeywordsWithFilterUnsupportedKeywords("$Flagged", FORWARDED))
                .isEqualTo(expected);
    }

    @Test
    public void buildKeywordsReturnEmptyWhenEmpty() throws Exception {
        assertThat(Keyword.buildKeywords(Optional.empty())).isEmpty();
    }

    @Test
    public void buildKeywordsShouldThrowWhenWrongFlagValue() throws Exception {
        expectedException.expect(IllegalArgumentException.class);

        Keyword.buildKeywords(Optional.of(ImmutableMap.of("AnyKey", false)));
    }

    @Test
    public void buildKeywordsShouldThrowWhenWrongFlagName() throws Exception {
        expectedException.expect(IllegalArgumentException.class);

        Keyword.buildKeywords(Optional.of(ImmutableMap.of("Any Key", true)));
    }

    @Test
    public void buildKeywordsShouldReturnSetOfKeywords() throws Exception {
        Optional<ImmutableSet<Keyword>> keywords = Keyword.buildKeywords(
            Optional.of(
                ImmutableMap.of(
                    "AnyKey", true,
                    "OtherKey", true)));

        assertThat(keywords).isPresent();
        assertThat(keywords.get()).containsOnly(new Keyword("AnyKey", true), new Keyword("OtherKey", true));
    }
}