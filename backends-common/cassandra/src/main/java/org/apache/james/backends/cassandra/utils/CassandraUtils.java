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

package org.apache.james.backends.cassandra.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class CassandraUtils {

    private static Executor executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("Cassandra-CompletableFuture-Pool-%d").build());
    
    public static Stream<Row> convertToStream(ResultSet resultSet) {
        return StreamSupport.stream(resultSet.spliterator(), true);
    }

    public static CompletableFuture<ResultSet> executeAsync(Session session, Statement statement) {
        return FutureConverter.toCompletableFuture(session.executeAsync(statement), executor);
    }

    public static CompletableFuture<Void> executeVoidAsync(Session session, Statement statement) {
        return FutureConverter.toCompletableFuture(session.executeAsync(statement), executor)
            .thenAccept(CassandraUtils::consume);
    }

    private static <U> void consume(U value) {}

}
