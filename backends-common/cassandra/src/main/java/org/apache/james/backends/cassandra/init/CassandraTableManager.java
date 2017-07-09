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

package org.apache.james.backends.cassandra.init;

import static com.datastax.driver.core.querybuilder.QueryBuilder.truncate;

import java.util.concurrent.CompletableFuture;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.components.CassandraTable;
import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.util.FluentFutureStream;

public class CassandraTableManager {

    private final CassandraAsyncExecutor executor;
    private final CassandraModule module;

    public CassandraTableManager(CassandraModule module, Session session) {
        this.executor = new CassandraAsyncExecutor(session);
        this.module = module;
    }

    public CassandraTableManager ensureAllTables() {
        FluentFutureStream.of(
            module.moduleTables()
                .stream()
                .map(CassandraTable::getCreateStatement)
                .map(executor::execute))
            .join();
        return this;
    }

    public void clearAllTables() {
        FluentFutureStream.of(
            module.moduleTables()
                .stream()
                .map(CassandraTable::getName)
                .map(this::clearTable))
            .join();
    }

    private CompletableFuture<ResultSet> clearTable(String tableName) {
        return executor.execute(truncate(tableName));
    }
}
