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
package org.apache.james.rrt.cassandra;

import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.apache.james.rrt.cassandra.tables.CassandraRecipientRewriteTableTable.DOMAIN;
import static org.apache.james.rrt.cassandra.tables.CassandraRecipientRewriteTableTable.MAPPING;
import static org.apache.james.rrt.cassandra.tables.CassandraRecipientRewriteTableTable.TABLE_NAME;
import static org.apache.james.rrt.cassandra.tables.CassandraRecipientRewriteTableTable.USER;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.lib.AbstractRecipientRewriteTable;
import org.apache.james.rrt.lib.Mappings;
import org.apache.james.rrt.lib.MappingsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Session;
import com.google.common.annotations.VisibleForTesting;

public class CassandraRecipientRewriteTable extends AbstractRecipientRewriteTable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraRecipientRewriteTable.class);
    private final Session session;

    @Inject
    @VisibleForTesting CassandraRecipientRewriteTable(Session session) {
        this.session = session;
    }

    @Override
    protected void addMappingInternal(String user, String domain, String mapping) throws RecipientRewriteTableException {
        session.execute(insertInto(TABLE_NAME)
            .value(USER, getFixedUser(user))
            .value(DOMAIN, getFixedDomain(domain))
            .value(MAPPING, mapping));
    }

    @Override
    protected void removeMappingInternal(String user, String domain, String mapping) throws RecipientRewriteTableException {
        session.execute(delete()
            .from(TABLE_NAME)
            .where(eq(USER, getFixedUser(user)))
            .and(eq(DOMAIN, getFixedDomain(domain)))
            .and(eq(MAPPING, mapping)));
    }

    @Override
    protected Mappings getUserDomainMappingsInternal(String user, String domain) throws RecipientRewriteTableException {
        List<String> mappings = session.execute(select(MAPPING)
                .from(TABLE_NAME)
                .where(eq(USER, getFixedUser(user)))
                .and(eq(DOMAIN, getFixedDomain(domain))))
            .all()
            .stream()
            .map(row -> row.getString(MAPPING))
            .collect(Collectors.toList());
        return mappings.isEmpty() ? null : MappingsImpl.fromCollection(mappings);
    }

    @Override
    protected Map<String, Mappings> getAllMappingsInternal() throws RecipientRewriteTableException {
        Map<String, Mappings> map = new HashMap<>();
        session.execute(select(USER, DOMAIN)
            .from(TABLE_NAME))
            .all()
            .stream()
            .forEach(row -> putUserDomainMappings(map, row.getString(USER), row.getString(DOMAIN)));
        return map.isEmpty() ? null : map;
    }

    private void putUserDomainMappings(Map<String, Mappings> map, String user, String domain) {
        try {
            Mappings userDomainMappingsInternal = getUserDomainMappingsInternal(user, domain);
            if (userDomainMappingsInternal != null) {
                map.put(user + "@" + domain, userDomainMappingsInternal);
            }
        } catch (Exception e) {
            LOGGER.error("Unhandled error occurs", e);
        }
    }

    @Override
    protected String mapAddressInternal(String user, String domain) throws RecipientRewriteTableException {
        Mappings userDomainMappingsInternal = getUserDomainMappingsInternal(user, domain);
        if (userDomainMappingsInternal != null) {
            return userDomainMappingsInternal.serialize();
        }

        Mappings wildcardUserMappingsInternal = getUserDomainMappingsInternal(WILDCARD, domain);
        if (wildcardUserMappingsInternal != null) {
            return wildcardUserMappingsInternal.serialize();
        }

        Mappings wildcardDomainMappingsInternal = getUserDomainMappingsInternal(user, WILDCARD);
        if (wildcardDomainMappingsInternal != null) {
            return wildcardDomainMappingsInternal.serialize();
        }
        return null;
    }

}
