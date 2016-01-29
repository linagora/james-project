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

package org.apache.james.mailbox.store;

import static org.assertj.core.api.Assertions.assertThat;

import javax.mail.Flags;

import org.apache.james.mailbox.FlagsBuilder;
import org.apache.james.mailbox.MessageManager;
import org.junit.Test;

public class FlagsUpdateTest {

    @Test(expected = NullPointerException.class)
    public void constructorShouldThrowWhenNullFlags() {
        new FlagsUpdate(null, MessageManager.FlagsUpdateMode.ADD);
    }

    @Test(expected = NullPointerException.class)
    public void constructorShouldThrowWhenNullMode() {
        new FlagsUpdate(new FlagsBuilder().add(Flags.Flag.ANSWERED).build(), null);
    }

    @Test
    public void applyShouldNotHaveSideEffects() {
        Flags originalFlags = new FlagsBuilder().add(Flags.Flag.RECENT, Flags.Flag.FLAGGED).build();
        Flags newFlags = new FlagsBuilder().add(Flags.Flag.RECENT).build();
        FlagsUpdate flagsUpdate = new FlagsUpdate(newFlags, MessageManager.FlagsUpdateMode.REPLACE);

        flagsUpdate.apply(originalFlags);

        assertThat(originalFlags).isEqualTo(new FlagsBuilder().add(Flags.Flag.RECENT, Flags.Flag.FLAGGED).build());
    }

    @Test
    public void applyShouldReturnWhenNoFlagsToAdd() {
        Flags originalFlags = new FlagsBuilder().add(Flags.Flag.RECENT, Flags.Flag.FLAGGED).build();
        assertThat(new FlagsUpdate(new Flags(), MessageManager.FlagsUpdateMode.ADD).apply(originalFlags)).isEqualTo(originalFlags);
    }

    @Test
    public void applyShouldReturnWhenNoFlagsToRemove() {
        Flags originalFlags = new FlagsBuilder().add(Flags.Flag.RECENT, Flags.Flag.FLAGGED).build();
        assertThat(new FlagsUpdate(new Flags(), MessageManager.FlagsUpdateMode.ADD).apply(originalFlags)).isEqualTo(originalFlags);
    }

    @Test
    public void applyShouldReplaceFlagsWhenReplaceMode() {
        Flags originalFlags = new FlagsBuilder().add(Flags.Flag.RECENT, Flags.Flag.FLAGGED).build();
        Flags newFlags = new FlagsBuilder().add(Flags.Flag.DELETED, Flags.Flag.FLAGGED).add("userflag").build();
        FlagsUpdate flagsUpdate = new FlagsUpdate(newFlags, MessageManager.FlagsUpdateMode.REPLACE);

        assertThat(flagsUpdate.apply(originalFlags)).isEqualTo(newFlags);
    }

    @Test
    public void applyShouldAddFlagsWhenAddMode() {
        Flags originalFlags = new FlagsBuilder().add(Flags.Flag.RECENT, Flags.Flag.FLAGGED).build();
        Flags flagsToAdd = new FlagsBuilder().add(Flags.Flag.DELETED, Flags.Flag.FLAGGED).add("userflag").build();

        FlagsUpdate flagsUpdate = new FlagsUpdate(flagsToAdd, MessageManager.FlagsUpdateMode.ADD);

        assertThat(flagsUpdate.apply(originalFlags))
            .isEqualTo(new FlagsBuilder().add(Flags.Flag.DELETED, Flags.Flag.FLAGGED, Flags.Flag.RECENT).add("userflag").build());
    }

    @Test
    public void applyShouldRemovedFlagsWhenRemoveMode() {
        Flags originalFlags = new FlagsBuilder().add(Flags.Flag.RECENT, Flags.Flag.FLAGGED).build();
        Flags flagsToRemove = new FlagsBuilder().add(Flags.Flag.DELETED, Flags.Flag.FLAGGED).add("userflag").build();
        FlagsUpdate flagsUpdate = new FlagsUpdate(flagsToRemove, MessageManager.FlagsUpdateMode.REMOVE);

        assertThat(flagsUpdate.apply(originalFlags)).isEqualTo(new Flags(Flags.Flag.RECENT));
    }

}
