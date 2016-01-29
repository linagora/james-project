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

import javax.mail.Flags;
import java.util.Arrays;

public class FlagsBuilder {

    private final Flags internalFlags;

    public FlagsBuilder() {
        internalFlags = new Flags();
    }

    public FlagsBuilder add(Flags.Flag... flags) {
        return addFlags(Arrays.asList(flags));
    }

    public FlagsBuilder add(String... flags) {
        return addUserFlags(Arrays.asList(flags));
    }

    public FlagsBuilder add(Flags flags) {
        internalFlags.add(flags);
        return this;
    }

    public FlagsBuilder addFlags(Iterable<Flags.Flag> flags) {
        for(Flags.Flag flag : flags) {
            internalFlags.add(flag);
        }
        return this;
    }

    public FlagsBuilder addUserFlags(Iterable<String> userFlags) {
        for(String userFlag : userFlags) {
            internalFlags.add(userFlag);
        }
        return this;
    }

    public Flags build() {
        return new Flags(internalFlags);
    }

}
