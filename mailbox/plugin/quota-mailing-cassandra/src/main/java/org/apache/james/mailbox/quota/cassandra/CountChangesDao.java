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
package org.apache.james.mailbox.quota.cassandra;

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

import java.time.Instant;

import javax.inject.Inject;

import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.core.User;
import org.apache.james.mailbox.quota.model.QuotaThreshold;
import org.apache.james.mailbox.quota.model.QuotaThresholdChange;
import org.apache.james.mailbox.quota.model.QuotaThresholdHistory;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.github.steveash.guavate.Guavate;

public class CountChangesDao {
    private final CassandraUtils cassandraUtils;
    private final CassandraAsyncExecutor cassandraAsyncExecutor;
    private final PreparedStatement insert;
    private final PreparedStatement select;

    @Inject
    public CountChangesDao(Session session, CassandraUtils cassandraUtils) {
        this.cassandraUtils = cassandraUtils;
        this.cassandraAsyncExecutor = new CassandraAsyncExecutor(session);
        this.insert = prepareInsert(session);
        this.select = prepareSelect(session);
    }

    private PreparedStatement prepareInsert(Session session) {
        return session.prepare(insertInto(CassandraQuotaThresholdHistoryStoreTable.COUNT_TABLE_NAME)
            .value(CassandraQuotaThresholdHistoryStoreTable.USER, bindMarker(CassandraQuotaThresholdHistoryStoreTable.USER))
            .value(CassandraQuotaThresholdHistoryStoreTable.INSTANT, bindMarker(CassandraQuotaThresholdHistoryStoreTable.INSTANT))
            .value(CassandraQuotaThresholdHistoryStoreTable.THRESHOLD, bindMarker(CassandraQuotaThresholdHistoryStoreTable.THRESHOLD)));
    }

    private PreparedStatement prepareSelect(Session session) {
        return session.prepare(select()
            .from(CassandraQuotaThresholdHistoryStoreTable.COUNT_TABLE_NAME)
            .where(eq(CassandraQuotaThresholdHistoryStoreTable.USER, bindMarker(CassandraQuotaThresholdHistoryStoreTable.USER))));
    }

    public QuotaThresholdHistory retrieve(User user) {
        return toHistory(
                cassandraAsyncExecutor.execute(
                    select.bind()
                        .setString(CassandraQuotaThresholdHistoryStoreTable.USER, user.asString()))
                .join());
    }

    private QuotaThresholdHistory toHistory(ResultSet resultSet) {
        return new QuotaThresholdHistory(
            cassandraUtils.convertToStream(resultSet)
                .map(this::toChange)
                .collect(Guavate.toImmutableList()));
    }

    private QuotaThresholdChange toChange(Row row) {
        QuotaThreshold quotaThreshold = new QuotaThreshold(row.getDouble(CassandraQuotaThresholdHistoryStoreTable.THRESHOLD));
        Instant instant = row.get(CassandraQuotaThresholdHistoryStoreTable.INSTANT, Instant.class);
        return new QuotaThresholdChange(quotaThreshold, instant);
    }

    public void add(User user, QuotaThresholdChange change) {
        cassandraAsyncExecutor.executeVoid(
                insert.bind()
                    .setString(CassandraQuotaThresholdHistoryStoreTable.USER, user.asString())
                    .set(CassandraQuotaThresholdHistoryStoreTable.INSTANT, change.getInstant(), Instant.class)
                    .setDouble(CassandraQuotaThresholdHistoryStoreTable.THRESHOLD, change.getQuotaThreshold().getQuotaOccupationRatio()))
            .join();
    }

}
