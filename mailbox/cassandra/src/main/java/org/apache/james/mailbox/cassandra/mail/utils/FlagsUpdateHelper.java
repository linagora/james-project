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

package org.apache.james.mailbox.cassandra.mail.utils;

import static com.datastax.driver.core.querybuilder.QueryBuilder.addAll;
import static com.datastax.driver.core.querybuilder.QueryBuilder.removeAll;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static org.apache.james.mailbox.cassandra.table.Flag.USER_FLAGS;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;

import javax.mail.Flags;

import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.cassandra.table.Flag;
import org.apache.james.mailbox.store.FlagsUpdateCalculator;

import com.datastax.driver.core.querybuilder.Update;
import com.github.steveash.guavate.Guavate;

public class FlagsUpdateHelper {

    public static final boolean SET = true;
    public static final boolean UNSET = false;

    public static Function<Update.Assignments, Update.Assignments> updateFlags(FlagsUpdateCalculator flagsUpdateCalculator) {
        MessageManager.FlagsUpdateMode mode = flagsUpdateCalculator.getMode();

        return updateSystemFlags(flagsUpdateCalculator, mode)
            .compose(updateUserFlags(flagsUpdateCalculator));
    }

    private static Function<Update.Assignments, Update.Assignments> updateSystemFlags(FlagsUpdateCalculator flagsUpdateCalculator, MessageManager.FlagsUpdateMode mode) {
        return Flag.FLAG_TO_STRING_MAP.keySet().stream()
            .filter(flagsUpdateCalculator::isFlagModified)
            .map(flag -> flagAssignment(flagsUpdateCalculator, mode, flag))
            .reduce(Function::compose)
            .orElse(Function.identity());
    }

    private static Function<Update.Assignments, Update.Assignments> updateUserFlags(FlagsUpdateCalculator flagsUpdateCalculator) {
        MessageManager.FlagsUpdateMode mode = flagsUpdateCalculator.getMode();

        Set<String> userFlags = Arrays.stream(flagsUpdateCalculator.getProvidedFlags().getUserFlags())
            .collect(Guavate.toImmutableSet());

        switch (mode) {
            case ADD:
                if (!userFlags.isEmpty()) {
                    return update -> update.and(addAll(USER_FLAGS, userFlags));
                } else {
                    return Function.identity();
                }
            case REMOVE:
                if (!userFlags.isEmpty()) {
                    return update -> update.and(removeAll(USER_FLAGS, userFlags));
                } else {
                    return Function.identity();
                }
            case REPLACE:
                return update -> update.and(set(USER_FLAGS, userFlags));
            default:
                throw new RuntimeException("Unknown update mode " + mode);
        }
    }

    private static Function<Update.Assignments, Update.Assignments> flagAssignment(FlagsUpdateCalculator flagsUpdateCalculator, MessageManager.FlagsUpdateMode mode, Flags.Flag flag) {
        switch (mode) {
            case ADD:
                return update -> update.and(set(Flag.FLAG_TO_STRING_MAP.get(flag), SET));
            case REMOVE:
                return update -> update.and(set(Flag.FLAG_TO_STRING_MAP.get(flag), UNSET));
            case REPLACE:
                return update -> update.and(set(Flag.FLAG_TO_STRING_MAP.get(flag),
                    flagsUpdateCalculator.getProvidedFlags().contains(flag)));
            default:
                throw new IllegalStateException("Unknown flags update mode " + mode);
        }
    }

}
