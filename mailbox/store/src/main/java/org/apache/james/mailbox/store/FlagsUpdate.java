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

import javax.mail.Flags;

import org.apache.james.mailbox.MessageManager;
import org.msgpack.core.Preconditions;

public class FlagsUpdate {

    private final Flags providedFlags;
    private final MessageManager.FlagsUpdateMode mode;

    public FlagsUpdate(Flags providedFlags, MessageManager.FlagsUpdateMode mode) {
        Preconditions.checkNotNull(providedFlags);
        Preconditions.checkNotNull(mode);
        this.providedFlags = providedFlags;
        this.mode = mode;
    }

    public Flags apply(Flags flags) {
        Flags updatedFlags = new Flags(flags);
        switch (mode) {
        case REPLACE:
            return new Flags(providedFlags);
        case ADD:
            updatedFlags.add(providedFlags);
            return updatedFlags;
        case REMOVE:
            updatedFlags.remove(providedFlags);
            return updatedFlags;
        default:
            throw new IllegalStateException("mode not handled");
        }
    }

}
