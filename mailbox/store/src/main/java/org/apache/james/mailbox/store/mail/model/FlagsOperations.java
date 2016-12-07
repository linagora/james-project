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

import javax.mail.Flags;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class FlagsOperations {

    public static Flags createFlags(MailboxMessage mailboxMessage, String[] userFlags) {
        final Flags flags = new Flags();

        if (mailboxMessage.isAnswered()) {
            flags.add(Flags.Flag.ANSWERED);
        }
        if (mailboxMessage.isDeleted()) {
            flags.add(Flags.Flag.DELETED);
        }
        if (mailboxMessage.isDraft()) {
            flags.add(Flags.Flag.DRAFT);
        }
        if (mailboxMessage.isFlagged()) {
            flags.add(Flags.Flag.FLAGGED);
        }
        if (mailboxMessage.isRecent()) {
            flags.add(Flags.Flag.RECENT);
        }
        if (mailboxMessage.isSeen()) {
            flags.add(Flags.Flag.SEEN);
        }
        if (userFlags != null && userFlags.length > 0) {
            for (String userFlag : userFlags) {
                flags.add(userFlag);
            }
        }
        return flags;
    }

    /**
     * Check if the given {@link Flags} contains {@link Flags} which are not
     * included in the returned {@link Flags} of the permanentFlags.
     * If any are found, these are
     * removed from the given {@link Flags} instance. The only exception is the
     * {@link Flags.Flag#RECENT} flag.
     *
     * This flag is never removed!
     *
     * @param flags
     * @param session
     */
    public static Flags trim(Flags flags, Flags permanentFlags) {
        Preconditions.checkNotNull(permanentFlags);
        Flags result = new Flags(Optional.fromNullable(flags).or(new Flags()));
        Flags.Flag[] systemFlags = result.getSystemFlags();
        for (Flags.Flag f : systemFlags) {
            if (f != Flags.Flag.RECENT && !permanentFlags.contains(f)) {
                result.remove(f);
            }
        }
        // if the permFlags contains the special USER flag we can skip this as
        // all user flags are allowed
        if (!permanentFlags.contains(Flags.Flag.USER)) {
            String[] uFlags = result.getUserFlags();
            for (String uFlag : uFlags) {
                if (!permanentFlags.contains(uFlag)) {
                    result.remove(uFlag);
                }
            }
        }
        return result;
    }

    public static Flags prepareForAppend(Flags flags, boolean isRecent, Flags permanentFlags) {
        final Flags result = Optional.fromNullable(flags).or(new Flags());
        if (isRecent) {
            result.add(Flags.Flag.RECENT);
        }
        return FlagsOperations.trim(result, permanentFlags);
    }


}
