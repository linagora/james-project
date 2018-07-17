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

package org.apache.james.sieve.cassandra;

import static com.datastax.driver.core.DataType.bigint;
import static com.datastax.driver.core.DataType.cboolean;
import static com.datastax.driver.core.DataType.counter;
import static com.datastax.driver.core.DataType.text;
import static com.datastax.driver.core.DataType.timestamp;

import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.sieve.cassandra.tables.CassandraSieveActiveTable;
import org.apache.james.sieve.cassandra.tables.CassandraSieveClusterQuotaTable;
import org.apache.james.sieve.cassandra.tables.CassandraSieveQuotaTable;
import org.apache.james.sieve.cassandra.tables.CassandraSieveSpaceTable;
import org.apache.james.sieve.cassandra.tables.CassandraSieveTable;

import com.datastax.driver.core.schemabuilder.SchemaBuilder;

public class CassandraSieveRepositoryModule extends CassandraModuleComposite {

    public static final CassandraModule SIEVE_TABLE = CassandraModule.forTable(
        CassandraSieveTable.TABLE_NAME,
        SchemaBuilder.createTable(CassandraSieveTable.TABLE_NAME)
            .ifNotExists()
            .addPartitionKey(CassandraSieveTable.USER_NAME, text())
            .addClusteringColumn(CassandraSieveTable.SCRIPT_NAME, text())
            .addColumn(CassandraSieveTable.SCRIPT_CONTENT, text())
            .addColumn(CassandraSieveTable.IS_ACTIVE, cboolean())
            .addColumn(CassandraSieveTable.SIZE, bigint())
            .withOptions()
            .comment("Holds SIEVE scripts."));

    public static final CassandraModule SIEVE_SPACE_TABLE = CassandraModule.forTable(
        CassandraSieveSpaceTable.TABLE_NAME,
        SchemaBuilder.createTable(CassandraSieveSpaceTable.TABLE_NAME)
            .ifNotExists()
            .addPartitionKey(CassandraSieveSpaceTable.USER_NAME, text())
            .addColumn(CassandraSieveSpaceTable.SPACE_USED, counter())
            .withOptions()
            .comment("Holds per user current space occupied by SIEVE scripts."));

    public static final CassandraModule SIEVE_QUOTA_TABLE = CassandraModule.forTable(
        CassandraSieveQuotaTable.TABLE_NAME,
        SchemaBuilder.createTable(CassandraSieveQuotaTable.TABLE_NAME)
            .ifNotExists()
            .addPartitionKey(CassandraSieveQuotaTable.USER_NAME, text())
            .addColumn(CassandraSieveQuotaTable.QUOTA, bigint())
            .withOptions()
            .comment("Holds per user size limitations for SIEVE script storage."));

    public static final CassandraModule SIEVE_GLOBAL_QUOTA_TABLE = CassandraModule.forTable(
        CassandraSieveClusterQuotaTable.TABLE_NAME,
        SchemaBuilder.createTable(CassandraSieveClusterQuotaTable.TABLE_NAME)
            .ifNotExists()
            .addPartitionKey(CassandraSieveClusterQuotaTable.NAME, text())
            .addColumn(CassandraSieveClusterQuotaTable.VALUE, bigint())
            .withOptions()
            .comment("Holds default size limitations for SIEVE script storage."));

    public static final CassandraModule SIEVE_ACTIVE_TABLE = CassandraModule.forTable(
        CassandraSieveActiveTable.TABLE_NAME,
        SchemaBuilder.createTable(CassandraSieveActiveTable.TABLE_NAME)
            .ifNotExists()
            .addPartitionKey(CassandraSieveActiveTable.USER_NAME, text())
            .addColumn(CassandraSieveActiveTable.SCRIPT_NAME, text())
            .addColumn(CassandraSieveActiveTable.DATE, timestamp())
            .withOptions()
            .comment("Denormalisation table. Allows per user direct active SIEVE script retrieval."));

    public CassandraSieveRepositoryModule() {
        super(
            SIEVE_TABLE,
            SIEVE_SPACE_TABLE,
            SIEVE_QUOTA_TABLE,
            SIEVE_GLOBAL_QUOTA_TABLE,
            SIEVE_ACTIVE_TABLE);
    }

}
