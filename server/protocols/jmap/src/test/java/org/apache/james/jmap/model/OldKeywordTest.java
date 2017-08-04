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

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class OldKeywordTest {
    @Test
    public void shouldRespectBeanContract() {
        EqualsVerifier.forClass(OldKeyword.class).verify();
    }

    @Test
    public void fromFlagsShouldReturnOldKeywordOfFlags() throws Exception {
        Optional<Flags> flags = Optional.of(FlagsBuilder.builder()
            .build());
        OldKeyword expected = new OldKeyword(true, false, false, false);

        assertThat(OldKeyword.fromFlags(flags)).isEqualTo(expected);
    }

    @Test
    public void fromFlagsShouldReturnOldKeywordWhenNonFlags() throws Exception {
        OldKeyword expected = new OldKeyword(false, false, false, false);

        assertThat(OldKeyword.fromFlags(Optional.empty())).isEqualTo(expected);
    }

    @Test
    public void fromFlagsShouldReturnOldKeywordWhenFullFlags() throws Exception {
        Optional<Flags> flags = Optional.of(
            FlagsBuilder.builder()
            .add(Flags.Flag.FLAGGED)
            .add(Flags.Flag.ANSWERED)
            .add(Flags.Flag.DRAFT)
            .add(Flags.Flag.SEEN)
            .build());

        OldKeyword expected = new OldKeyword(false, true, true, true);

        assertThat(OldKeyword.fromFlags(flags)).isEqualTo(expected);
    }
}