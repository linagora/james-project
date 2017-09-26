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

package org.apache.james.jmap.model.mailbox;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;

import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxACL.EntryKey;
import org.apache.james.mailbox.model.MailboxACL.NameType;
import org.apache.james.mailbox.model.MailboxACL.Rfc4314Rights;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

public class Rights {
    public enum Right {
        Administer('a', MailboxACL.Right.Administer),
        Expunge('e', MailboxACL.Right.PerformExpunge),
        Insert('i', MailboxACL.Right.Insert),
        Lookup('l', MailboxACL.Right.Lookup),
        Read('r', MailboxACL.Right.Read),
        Seen('s', MailboxACL.Right.WriteSeenFlag),
        T_Delete('t', MailboxACL.Right.DeleteMessages),
        Write('w', MailboxACL.Right.Write);

        private final char imapRight;
        private final MailboxACL.Right right;

        Right(char imapRight, MailboxACL.Right right) {
            this.imapRight = imapRight;
            this.right = right;
        }

        @JsonValue
        public char getImapRight() {
            return imapRight;
        }

        public MailboxACL.Right getRight() {
            return right;
        }

        public static boolean exists(char c) {
            return Arrays.stream(values())
                .anyMatch(right -> right.getImapRight() == c);
        }

   //     @JsonCreator
        public static Right forChar(char c) {
            return Arrays.stream(values())
                .filter(right -> right.getImapRight() == c)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No matching right for '" + c + "'"));
        }
    }

    public static class Username {
        private final String value;

     //   @JsonCreator
        public Username(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof Username) {
                Username username = (Username) o;

                return Objects.equals(this.value, username.value);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(value);
        }
    }

    public static class Builder {
        private Multimap<Username, Right> rights;

        public Builder() {
            rights = ArrayListMultimap.create();
        }

        public Builder delegateTo(Username username, Right... rights) {
            delegateTo(username, Arrays.asList(rights));
            return this;
        }

        public Builder delegateTo(Username username, Collection<Right> rights) {
            this.rights.putAll(username, rights);
            return this;
        }

        public Builder combine(Builder builder) {
            this.rights.putAll(builder.rights);
            return this;
        }

        public Rights build() {
            return new Rights(rights);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Rights fromACL(MailboxACL acl) {
        return acl.getEntries()
            .entrySet()
            .stream()
            .filter(entry -> isUser(entry.getKey()))
            .map(entry -> builder().delegateTo(
                new Username(entry.getKey().getName()),
                fromACL(entry.getValue())))
            .reduce(builder(), Builder::combine)
            .build();
    }

    private static List<Right> fromACL(Rfc4314Rights rights) {
        return rights.list()
            .stream()
            .map(MailboxACL.Right::asCharacter)
            .filter(Rights::existingChar)
            .map(Right::forChar)
            .collect(Guavate.toImmutableList());
    }

    private static boolean existingChar(Character c) {
        if (!Right.exists(c)) {
            LOGGER.warn("Non handled right '" + c + "'");
            return false;
        }
        return true;
    }

    private static boolean isUser(EntryKey key) {
        if (key.isNegative()) {
            LOGGER.warn("Negative keys are not supported");
            return false;
        }
        if (key.getNameType() != NameType.user) {
            LOGGER.warn(key.getNameType() + " is not sopported. Onlu 'user' is.");
            return false;
        }
        return true;
    }

    public static final Rights EMPTY = new Rights(ArrayListMultimap.create());

    private static final Logger LOGGER = LoggerFactory.getLogger(Rights.class);

    private final Multimap<Username, Right> rights;

    @JsonCreator
    public Rights(Multimap<Username, Right> rights) {
        this.rights = rights;
    }

    @JsonAnyGetter
    public Map<Username, Collection<Right>> getRights() {
        return rights.asMap();
    }

    public MailboxACL toMailboxAcl() {
        BinaryOperator<MailboxACL> union = Throwing.binaryOperator(MailboxACL::union);

        return rights.asMap()
            .entrySet()
            .stream()
            .map(entrie -> new MailboxACL(
                ImmutableMap.of(
                    EntryKey.createUser(entrie.getKey().value),
                    toMailboxAclRights(entrie.getValue()))))
            .reduce(new MailboxACL(), union);
    }

    private Rfc4314Rights toMailboxAclRights(Collection<Right> rights) {
        BinaryOperator<Rfc4314Rights> union = Throwing.binaryOperator(Rfc4314Rights::union);

        return rights.stream()
            .map(Right::getRight)
            .map(Throwing.function(Rfc4314Rights::new))
            .reduce(new Rfc4314Rights(), union);
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof Rights) {
            Rights that = (Rights) o;

            return Objects.equals(this.rights, that.rights);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(rights);
    }
}
