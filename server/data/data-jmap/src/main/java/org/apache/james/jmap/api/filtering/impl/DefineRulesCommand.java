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

package org.apache.james.jmap.api.filtering.impl;

import java.util.List;
import java.util.Objects;

import org.apache.james.core.User;
import org.apache.james.eventsourcing.Command;
import org.apache.james.jmap.api.filtering.Rule;

import com.google.common.base.MoreObjects;

public class DefineRulesCommand implements Command {

    private final User user;
    private final List<Rule> rules;

    public DefineRulesCommand(User user, List<Rule> rules) {
        this.user = user;
        this.rules = rules;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public User getUser() {
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefineRulesCommand that = (DefineRulesCommand) o;
        return Objects.equals(user, that.user) &&
            Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, rules);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("user", user)
            .add("rules", rules)
            .toString();
    }
}
