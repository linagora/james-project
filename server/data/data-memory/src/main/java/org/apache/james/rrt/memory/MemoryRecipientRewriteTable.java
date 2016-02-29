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

package org.apache.james.rrt.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.lib.AbstractRecipientRewriteTable;
import org.apache.james.rrt.lib.Mappings;
import org.apache.james.rrt.lib.MappingsImpl;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;

public class MemoryRecipientRewriteTable extends AbstractRecipientRewriteTable {

    static class InMemroyMappingEntry {
        private final String user;
        private final String domain;
        private final String mapping;

        public InMemroyMappingEntry(String user, String domain, String mapping) {
            this.user = user;
            this.domain = domain;
            this.mapping = mapping;
        }

        public String getUser() {
            return user;
        }

        public String getDomain() {
            return domain;
        }

        public String getMapping() {
            return mapping;
        }

        public String asKey() {
            return getUser() + "@" + getDomain();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InMemroyMappingEntry that = (InMemroyMappingEntry) o;

            if (user != null ? !user.equals(that.user) : that.user != null) return false;
            if (domain != null ? !domain.equals(that.domain) : that.domain != null) return false;
            return mapping != null ? mapping.equals(that.mapping) : that.mapping == null;

        }

        @Override
        public int hashCode() {
            int result = user != null ? user.hashCode() : 0;
            result = 31 * result + (domain != null ? domain.hashCode() : 0);
            result = 31 * result + (mapping != null ? mapping.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "InMemroyMappingEntry{" +
                "user='" + user + '\'' +
                ", domain='" + domain + '\'' +
                ", mapping='" + mapping + '\'' +
                '}';
        }
    }

    private final List<InMemroyMappingEntry> mappingEntries;

    public MemoryRecipientRewriteTable() {
        mappingEntries = new ArrayList<InMemroyMappingEntry>();
    }

    @Override
    protected void addMappingInternal(String user, String domain, String mapping) throws RecipientRewriteTableException {
        mappingEntries.add(new InMemroyMappingEntry(getFixedUser(user), getFixedDomain(domain), mapping));
    }

    @Override
    protected void removeMappingInternal(String user, String domain, String mapping) throws RecipientRewriteTableException {
        mappingEntries.remove(new InMemroyMappingEntry(user, domain, mapping));
    }

    @Override
    protected Mappings getUserDomainMappingsInternal(String user, String domain) throws RecipientRewriteTableException {
        return retrieveMappings(user, domain)
            .orNull();
    }

    @Override
    protected String mapAddressInternal(String user, String domain) throws RecipientRewriteTableException {
        Mappings mappings = retrieveMappings(user, domain)
            .or(retrieveMappings(WILDCARD, domain)
                .or(retrieveMappings(user, WILDCARD)
                    .or(MappingsImpl.empty())));

        return !mappings.isEmpty() ? mappings.serialize() : null;
    }

    @Override
    protected Map<String, Mappings> getAllMappingsInternal() throws RecipientRewriteTableException {
        if (mappingEntries.isEmpty()) {
            return null;
        }
        Map<String, Collection<Mappings>> userMappingsMap = Multimaps.transformEntries(
            Multimaps.index(mappingEntries, new Function<InMemroyMappingEntry, String>() {
                public String apply(InMemroyMappingEntry mappingEntry) {
                    return mappingEntry.asKey();
                }
            }), new Maps.EntryTransformer<String, InMemroyMappingEntry, Mappings>() {
                public Mappings transformEntry(String s, InMemroyMappingEntry mappingEntry) {
                    return MappingsImpl.fromRawString(mappingEntry.getMapping());
                }
            }).asMap();
        return Maps.transformEntries(userMappingsMap, new Maps.EntryTransformer<String, Collection<Mappings>, Mappings>() {
            public Mappings transformEntry(String s, Collection<Mappings> mappingsList) {
                Mappings result = MappingsImpl.empty();
                for (Mappings mappings : mappingsList) {
                    result = result.union(mappings);
                }
                return result;
            }
        });
    }

    private Optional<Mappings> retrieveMappings(final String user, final String domain) {
        List<String> userEntries = Lists.newArrayList(
            Iterables.transform(
                Iterables.filter(mappingEntries, new Predicate<InMemroyMappingEntry>() {
                    public boolean apply(InMemroyMappingEntry mappingEntry) {
                        return user.equals(mappingEntry.getUser()) && domain.equals(mappingEntry.getDomain());
                    }
                }), new Function<InMemroyMappingEntry, String>() {
                    @Override
                    public String apply(InMemroyMappingEntry mappingEntry) {
                        return mappingEntry.getMapping();
                    }
                }));
        return MappingsImpl.fromCollection(userEntries).toOptional();
    }

}
