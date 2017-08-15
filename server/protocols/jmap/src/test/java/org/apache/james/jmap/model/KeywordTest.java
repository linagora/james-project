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
    public void keywordShouldCreateNewOneWhenFlagNameLengthEqualMaxLength() throws Exception {
        assertThat(new Keyword(StringUtils.repeat("a", Keyword.FLAG_NAME_MAX_LENTH), true)).isNotNull();
    }

    @Test
    public void keywordShouldCreateNewOneWhenFlagNameLengthEqualMinLength() throws Exception {
        assertThat(new Keyword("a", true)).isNotNull();
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainPercentageCharacter() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a%", true);
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainLeftBracket() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a[", true);
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainRightBracket() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a]", true);
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainLeftBrace() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a{", true);
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainSlash() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a\\", true);
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainStar() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a*", true);
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainQuote() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a\"", true);
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainOpeningParenthesis() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a(", true);
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainClosingParenthesis() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a)", true);
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

        assertThat(Keyword.toMapOfStringKeywords(Optional.of(flags)).get())
            .containsOnlyKeys(Keyword.FLAGGED.getFlagName(), Keyword.DRAFT.getFlagName());
    }

    @Test
    public void fromFlagsShouldReturnKeywordsWhenSystemStringFlagName() throws Exception {
        Flags flags = FlagsBuilder.builder()
            .add(new Flags("$Answered"))
            .build();

        assertThat(Keyword.toMapOfStringKeywords(Optional.of(flags)).get())
            .containsOnlyKeys(Keyword.ANSWERED.getFlagName());
    }

    @Test
    public void fromFlagsShouldReturnSetOfJmapFlagWhenUserFlag() throws Exception {
        Flags flags = FlagsBuilder.builder()
            .add(Flags.Flag.FLAGGED)
            .add(Flags.Flag.DRAFT)
            .add(FORWARDED)
            .build();

        assertThat(Keyword.toMapOfStringKeywords(Optional.of(flags)).get())
            .containsOnlyKeys(Keyword.FLAGGED.getFlagName(), Keyword.DRAFT.getFlagName(), FORWARDED);
    }

    @Test
    public void fromFlagsShouldSupportCustomJmapFlag() throws Exception {
        Flags flags = FlagsBuilder.builder()
            .add(Flags.Flag.FLAGGED)
            .add(Flags.Flag.DRAFT)
            .add("Unknown")
            .build();
        assertThat(Keyword.toMapOfStringKeywords(Optional.of(flags)).get())
            .containsOnlyKeys(Keyword.FLAGGED.getFlagName(), Keyword.DRAFT.getFlagName(), "Unknown");
    }

    @Test
    public void fromFlagsShouldRemoveUnsupportedJmapFlag() throws Exception {
        Flags flags = FlagsBuilder.builder()
            .add(Flags.Flag.RECENT)
            .add(Flags.Flag.DELETED)
            .add(Flags.Flag.DRAFT)
            .add(FORWARDED)
            .build();

        assertThat(Keyword.toMapOfStringKeywords(Optional.of(flags)).get())
            .containsOnlyKeys(Keyword.DRAFT.getFlagName(), FORWARDED);
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
        Flags imapFlags = Keyword.toFlagsWithFilterUnsupportedKeywords(ImmutableSet.of(Keyword.DRAFT, Keyword.DELETED));

        assertThat(imapFlags).isEqualToComparingFieldByField(expectedFlags);
    }

    @Test
    public void fromKeywordsShouldBuildImapFlagWithUserFlagFromJmapFlags() throws Exception {
        Flags expectedFlags = FlagsBuilder.builder()
            .add(Flags.Flag.DRAFT)
            .add(FORWARDED)
            .build();
        Flags imapFlags = Keyword.toFlagsWithFilterUnsupportedKeywords(ImmutableSet.of(Keyword.DRAFT, new Keyword(FORWARDED, true)));

        assertThat(imapFlags).isEqualToComparingFieldByField(expectedFlags);
    }

    @Test
    public void fromKeywordsShouldReturnFlagsFromSetOfStringOfKeyword() throws Exception {
        Flags expected = FlagsBuilder.builder()
                .add(Flags.Flag.FLAGGED)
                .add(FORWARDED)
                .build();
        assertThat(Keyword.toFlagsWithFilterUnsupportedKeywords(ImmutableSet.of(Keyword.FLAGGED, new Keyword(FORWARDED, true))))
            .isEqualTo(expected);
    }

    @Test
    public void toFlagsShouldReturnFlagsFromListOfStringOfKeyword() throws Exception {
        Flags expected = FlagsBuilder.builder()
                .add(Flags.Flag.FLAGGED)
                .add(FORWARDED)
                .build();
        assertThat(Keyword.toFlags("$Flagged", FORWARDED))
                .isEqualTo(expected);
    }

    @Test
    public void buildKeywordsReturnEmptyWhenEmpty() throws Exception {
        assertThat(Keyword.toSetOfKeywords(Optional.empty())).isEmpty();
    }

    @Test
    public void buildKeywordsShouldThrowWhenWrongFlagValue() throws Exception {
        expectedException.expect(IllegalArgumentException.class);

        Keyword.toSetOfKeywords(Optional.of(ImmutableMap.of("AnyKey", false)));
    }

    @Test
    public void buildKeywordsShouldThrowWhenWrongFlagName() throws Exception {
        expectedException.expect(IllegalArgumentException.class);

        Keyword.toSetOfKeywords(Optional.of(ImmutableMap.of("Any Key", true)));
    }

    @Test
    public void buildKeywordsShouldReturnSetOfKeywords() throws Exception {
        Optional<ImmutableSet<Keyword>> keywords = Keyword.toSetOfKeywords(
            Optional.of(
                ImmutableMap.of(
                    "AnyKey", true,
                    "OtherKey", true)));

        assertThat(keywords).isPresent();
        assertThat(keywords.get()).containsOnly(new Keyword("AnyKey", true), new Keyword("OtherKey", true));
    }
}