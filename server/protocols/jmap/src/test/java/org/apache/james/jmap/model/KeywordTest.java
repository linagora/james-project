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
import javax.mail.Flags;

import org.apache.james.mailbox.FlagsBuilder;

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

        assertThat(Keyword.fromFlags(flags)).containsOnly(Keyword.FLAGGED, Keyword.DRAFT, new Keyword(FORWARDED));
    }

    @Test
    public void fromFlagsShouldSupportCustomJmapFlag() throws Exception {
        Flags flags = FlagsBuilder.builder()
                .add(Flags.Flag.FLAGGED)
                .add(Flags.Flag.DRAFT)
                .add("Unknown")
                .build();
        assertThat(Keyword.fromFlags(flags)).containsOnly(Keyword.FLAGGED, Keyword.DRAFT, new Keyword("Unknown"));
    }

    @Test
    public void fromFlagsShouldRemoveUnsupportedJmapFlag() throws Exception {
        Flags flags = FlagsBuilder.builder()
                .add(Flags.Flag.RECENT)
                .add(Flags.Flag.DELETED)
                .add(Flags.Flag.DRAFT)
                .add(FORWARDED)
                .build();

        assertThat(Keyword.fromFlags(flags)).containsOnly(Keyword.DRAFT, new Keyword(FORWARDED));
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
        Flags imapFlags = Keyword.fromKeywordsWithFilterUnsupportedKeywords(ImmutableSet.of(Keyword.DRAFT, new Keyword(FORWARDED)));

        assertThat(imapFlags).isEqualToComparingFieldByField(expectedFlags);
    }

    @Test
    public void fromKeywordsShouldReturnFlagsFromSetOfStringOfKeyword() throws Exception {
        Flags expected = FlagsBuilder.builder()
                .add(Flags.Flag.FLAGGED)
                .add(FORWARDED)
                .build();
        assertThat(Keyword.fromKeywordsWithFilterUnsupportedKeywords(ImmutableSet.of(Keyword.FLAGGED, new Keyword(FORWARDED))))
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
}