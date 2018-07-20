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

package org.apache.james.mailbox.cassandra.modules;

import static com.datastax.driver.core.DataType.bigint;
import static com.datastax.driver.core.DataType.text;
import static com.datastax.driver.core.DataType.timeuuid;

import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.backends.cassandra.utils.CassandraConstants;
import org.apache.james.mailbox.cassandra.table.CassandraMailboxPathTable;
import org.apache.james.mailbox.cassandra.table.CassandraMailboxPathV2Table;
import org.apache.james.mailbox.cassandra.table.CassandraMailboxTable;

import com.datastax.driver.core.schemabuilder.SchemaBuilder;

public interface CassandraMailboxModule {

    CassandraModule MAILBOX_BASE_TYPE = CassandraModule.forType(
        CassandraMailboxTable.MAILBOX_BASE,
        SchemaBuilder.createType(CassandraMailboxTable.MAILBOX_BASE)
            .ifNotExists()
            .addColumn(CassandraMailboxTable.MailboxBase.NAMESPACE, text())
            .addColumn(CassandraMailboxTable.MailboxBase.USER, text()));

    CassandraModule MAILBOX_TABLE = CassandraModule.forTable(
        CassandraMailboxTable.TABLE_NAME,
        SchemaBuilder.createTable(CassandraMailboxTable.TABLE_NAME)
            .ifNotExists()
            .addPartitionKey(CassandraMailboxTable.ID, timeuuid())
            .addUDTColumn(CassandraMailboxTable.MAILBOX_BASE, SchemaBuilder.frozen(CassandraMailboxTable.MAILBOX_BASE))
            .addColumn(CassandraMailboxTable.NAME, text())
            .addColumn(CassandraMailboxTable.UIDVALIDITY, bigint())
            .withOptions()
            .comment("Holds the mailboxes information.")
            .caching(SchemaBuilder.KeyCaching.ALL,
                SchemaBuilder.rows(CassandraConstants.DEFAULT_CACHED_ROW_PER_PARTITION)));

    CassandraModule MAILBOX_PATH_TABLE = CassandraModule.forTable(
        CassandraMailboxPathTable.TABLE_NAME,
        SchemaBuilder.createTable(CassandraMailboxPathTable.TABLE_NAME)
            .ifNotExists()
            .addUDTPartitionKey(CassandraMailboxPathTable.NAMESPACE_AND_USER, SchemaBuilder.frozen(CassandraMailboxTable.MAILBOX_BASE))
            .addClusteringColumn(CassandraMailboxPathTable.MAILBOX_NAME, text())
            .addColumn(CassandraMailboxPathTable.MAILBOX_ID, timeuuid())
            .withOptions()
            .comment("Denormalisation table. Allow to retrieve mailboxes belonging to a certain user. This is a " +
                "LIST optimisation.")
            .caching(SchemaBuilder.KeyCaching.ALL,
                SchemaBuilder.rows(CassandraConstants.DEFAULT_CACHED_ROW_PER_PARTITION)));

    CassandraModule MAILBOX_PATH_V2_TABLE = CassandraModule.forTable(
        CassandraMailboxPathV2Table.TABLE_NAME,
        SchemaBuilder.createTable(CassandraMailboxPathV2Table.TABLE_NAME)
            .ifNotExists()
            .addPartitionKey(CassandraMailboxPathV2Table.NAMESPACE, text())
            .addPartitionKey(CassandraMailboxPathV2Table.USER, text())
            .addClusteringColumn(CassandraMailboxPathV2Table.MAILBOX_NAME, text())
            .addColumn(CassandraMailboxPathV2Table.MAILBOX_ID, timeuuid())
            .withOptions()
            .comment("Denormalisation table. Allow to retrieve mailboxes belonging to a certain user. This is a " +
                "LIST optimisation.")
            .caching(SchemaBuilder.KeyCaching.ALL,
                SchemaBuilder.rows(CassandraConstants.DEFAULT_CACHED_ROW_PER_PARTITION)));

    CassandraModule MODULE = new CassandraModuleComposite(
        MAILBOX_TABLE,
        MAILBOX_PATH_TABLE,
        MAILBOX_PATH_V2_TABLE,
        MAILBOX_BASE_TYPE);
}
