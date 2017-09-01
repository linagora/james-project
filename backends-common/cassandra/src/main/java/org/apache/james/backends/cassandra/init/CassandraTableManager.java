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

import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.components.CassandraTable;
import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.util.FluentFutureStream;
import org.apache.james.util.streams.JamesCollectors;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;

public class CassandraTableManager {

    public static final int TABLE_MANAGEMENT_PARRALLEL_OPERATION_COUNT = 4;
    private final CassandraAsyncExecutor executor;
    private final CassandraModule module;

    public CassandraTableManager(CassandraModule module, Session session) {
        this.executor = new CassandraAsyncExecutor(session);
        this.module = module;
    }

    public CassandraTableManager ensureAllTables() {
        module.moduleTables()
            .stream()
            .map(CassandraTable::getCreateStatement)
            .collect(JamesCollectors.chunker(TABLE_MANAGEMENT_PARRALLEL_OPERATION_COUNT))
            .map(statementBatch -> statementBatch.stream()
                .map(executor::executeVoid))
            .map(FluentFutureStream::of)
            .forEach(FluentFutureStream::join);
        return this;
    }

    public void clearAllTables() {
        module.moduleTables()
            .stream()
            .map(CassandraTable::getName)
            .map(QueryBuilder::truncate)
            .collect(JamesCollectors.chunker(TABLE_MANAGEMENT_PARRALLEL_OPERATION_COUNT))
            .map(statementBatch -> statementBatch.stream()
                .map(executor::executeVoid))
            .map(FluentFutureStream::of)
            .forEach(FluentFutureStream::join);
    }
}
