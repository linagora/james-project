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

import java.util.Optional;
import javax.mail.Flags;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class OldKeyword {
    private final boolean isUnread;
    private final boolean isFlagged;
    private final boolean isAnswered;
    private final boolean isDraft;

    @VisibleForTesting
    OldKeyword(boolean isUnread, boolean isFlagged, boolean isAnswered, boolean isDraft) {
        this.isUnread = isUnread;
        this.isFlagged = isFlagged;
        this.isAnswered = isAnswered;
        this.isDraft = isDraft;
    }

    public boolean isUnread() {
        return isUnread;
    }

    public boolean isFlagged() {
        return isFlagged;
    }

    public boolean isAnswered() {
        return isAnswered;
    }

    public boolean isDraft() {
        return isDraft;
    }

    public static OldKeyword fromFlags(Optional<Flags> flags) {
        return new OldKeyword(
            flags.map(imapFlags -> !imapFlags.contains(Flags.Flag.SEEN)).orElse(false),
            flags.map(imapFlags -> imapFlags.contains(Flags.Flag.FLAGGED)).orElse(false),
            flags.map(imapFlags -> imapFlags.contains(Flags.Flag.ANSWERED)).orElse(false),
            flags.map(imapFlags -> imapFlags.contains(Flags.Flag.DRAFT)).orElse(false)
        );
    }

    @Override
    public final boolean equals(Object other) {
        if (other instanceof OldKeyword) {
            OldKeyword oldKeyword = (OldKeyword) other;
            return Objects.equal(isUnread, oldKeyword.isUnread)
                && Objects.equal(isFlagged, oldKeyword.isFlagged)
                && Objects.equal(isAnswered, oldKeyword.isAnswered)
                && Objects.equal(isDraft, oldKeyword.isDraft);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(isUnread, isFlagged, isAnswered, isDraft);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("isUnread", isUnread)
                .add("isFlagged", isFlagged)
                .add("isAnswered", isAnswered)
                .add("isDraft", isDraft)
                .toString();
    }

}
