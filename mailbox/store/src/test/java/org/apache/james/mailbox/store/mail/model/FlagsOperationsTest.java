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

package org.apache.james.mailbox.store.mail.model;

import static org.assertj.core.api.Assertions.assertThat;

import javax.mail.Flags;

import org.apache.james.mailbox.FlagsBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FlagsOperationsTest {

    private static final String USER_FLAG = "userFlag";
    private static final String USER_FLAG_2 = "userFlag2";
    private Flags flags;
    private Flags fullPermanentFlags;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        flags = new FlagsBuilder().add(Flags.Flag.ANSWERED, Flags.Flag.DRAFT)
            .add(USER_FLAG, USER_FLAG_2)
            .build();
        fullPermanentFlags = new FlagsBuilder().add(Flags.Flag.DRAFT, Flags.Flag.DELETED, Flags.Flag.ANSWERED, Flags.Flag.RECENT, Flags.Flag.USER, Flags.Flag.SEEN, Flags.Flag.FLAGGED).build();

    }

    @Test
    public void trimShouldAcceptNullPermanentFlags() {
        Flags permanentFlags = null;
        Flags flags = new Flags();

        expectedException.expect(NullPointerException.class);
        FlagsOperations.trim(flags, permanentFlags);
    }

    @Test
    public void trimShouldAcceptNullFlags() {
        Flags permanentFlags = new Flags();
        Flags flags = null;
        assertThat(FlagsOperations.trim(flags, permanentFlags)).isEqualTo(new Flags());
    }

    @Test
    public void trimShouldOnlyKeepPermanentFlags() {
        Flags permanentFlags = new FlagsBuilder().add(Flags.Flag.DRAFT)
            .add(USER_FLAG)
            .build();

        assertThat(FlagsOperations.trim(flags, permanentFlags)).isEqualTo(new FlagsBuilder()
            .add(Flags.Flag.DRAFT)
            .add(USER_FLAG)
            .build());
    }

    @Test
    public void trimShouldNotRemoveRecentFlags() {
        Flags permanentFlags = new Flags();
        Flags flags = new Flags(Flags.Flag.RECENT);

        assertThat(FlagsOperations.trim(flags, permanentFlags)).isEqualTo(flags);
    }

    @Test
    public void trimShouldNotRemoveUserFlagsWhenUserFlagsSupported() {
        Flags permanentFlags = new FlagsBuilder().add(Flags.Flag.USER)
            .add(USER_FLAG)
            .build();
        Flags flags = new FlagsBuilder()
            .add(USER_FLAG, USER_FLAG_2)
            .build();

        assertThat(FlagsOperations.trim(flags, permanentFlags)).isEqualTo(flags);
    }

    @Test
    public void prepareForAppendShouldAddRecentFlagWhenAsked() {
        boolean isRecent = true;

        assertThat(FlagsOperations.prepareForAppend(flags, isRecent, fullPermanentFlags))
            .isEqualTo(new FlagsBuilder()
                .add(flags)
                .add(Flags.Flag.RECENT)
                .build());
    }

    @Test
    public void prepareForAppendShouldNotAddRecentFlagWhenNotAsked() {
        boolean isRecent = false;

        assertThat(FlagsOperations.prepareForAppend(flags, isRecent, fullPermanentFlags))
            .isEqualTo(new FlagsBuilder()
                .add(flags)
                .build());
    }

    @Test
    public void prepareForAppendShouldNullValues() {
        boolean isRecent = false;

        assertThat(FlagsOperations.prepareForAppend(null, isRecent, fullPermanentFlags))
            .isEqualTo(new Flags());
    }

    @Test
    public void prepareForAppendShouldThrowOnNullPermanentFlags() {
        expectedException.expect(NullPointerException.class);
        boolean isRecent = false;

        FlagsOperations.prepareForAppend(new Flags(), isRecent, null);
    }

    @Test
    public void prepareForAppendShouldTrimResult() {
        boolean isRecent = false;

        Flags permanentFlags = new FlagsBuilder().add(Flags.Flag.DRAFT).add(USER_FLAG).build();
        assertThat(FlagsOperations.prepareForAppend(flags, isRecent, permanentFlags))
            .isEqualTo(permanentFlags);
    }
}
