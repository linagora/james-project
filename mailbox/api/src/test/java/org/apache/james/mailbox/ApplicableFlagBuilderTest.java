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
package org.apache.james.mailbox;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.Flags;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicableFlagBuilderTest {
    ApplicableFlagBuilder builder;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Before
    public void setUp() throws Exception {
        builder = ApplicableFlagBuilder.builder();
    }

    @Test
    public void shouldAtLeastContainAllDefaultApplicativeFlag() {
        assertThat(builder.build()).isEqualTo(getDefaultApplicativeFlags());
    }

    @Test
    public void shouldNeverRetainRecentAndUserFlag() {
        Flags result = builder
            .add(new Flags(Flags.Flag.RECENT))
            .add(new Flags(Flags.Flag.USER))
            .build();

        softly.assertThat(result.contains(Flags.Flag.RECENT)).isFalse();
        softly.assertThat(result.contains(Flags.Flag.USER)).isFalse();
    }

    @Test
    public void shouldAddCustomUserFlagIfProvidenToDefaultFlag() {
        Flags result = builder
            .add("yolo")
            .add("vibe")
            .build();

        softly.assertThat(result.contains(getDefaultApplicativeFlags())).isTrue();
        softly.assertThat(result.contains("yolo")).isTrue();
        softly.assertThat(result.contains("vibe")).isTrue();
    }

    @Test
    public void shouldAcceptUserCustomFlagInsideFlags() {
        Flags result = builder
            .add(new Flags("yolo"))
            .build();

        assertThat(result.contains("yolo")).isTrue();
    }

    @Test
    public void shouldAcceptFlagsThatContainMultipleFlag() {
        Flags flags = new Flags("yolo");
        flags.add("vibes");

        Flags result = builder
            .add(flags)
            .build();

        softly.assertThat(result.contains("yolo")).isTrue();
        softly.assertThat(result.contains("vibes")).isTrue();
    }

    @Test
    public void shouldAcceptMultipleFlagAtOnce() {
        Flags result = builder
            .add("cartman", "butters")
            .add("chef", "randy")
            .build();

        softly.assertThat(result.contains("cartman")).isTrue();
        softly.assertThat(result.contains("butters")).isTrue();
        softly.assertThat(result.contains("chef")).isTrue();
        softly.assertThat(result.contains("randy")).isTrue();
    }

    private Flags getDefaultApplicativeFlags() {
        Flags defaults = new Flags();

        defaults.add(Flags.Flag.DELETED);
        defaults.add(Flags.Flag.ANSWERED);
        defaults.add(Flags.Flag.DRAFT);
        defaults.add(Flags.Flag.FLAGGED);
        defaults.add(Flags.Flag.SEEN);

        return defaults;
    }
}