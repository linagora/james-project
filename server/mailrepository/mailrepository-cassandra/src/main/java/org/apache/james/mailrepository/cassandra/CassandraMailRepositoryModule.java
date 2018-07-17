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

package org.apache.james.mailrepository.cassandra;

import static com.datastax.driver.core.DataType.bigint;
import static com.datastax.driver.core.DataType.blob;
import static com.datastax.driver.core.DataType.counter;
import static com.datastax.driver.core.DataType.list;
import static com.datastax.driver.core.DataType.map;
import static com.datastax.driver.core.DataType.text;
import static com.datastax.driver.core.DataType.timestamp;
import static com.datastax.driver.core.schemabuilder.SchemaBuilder.frozen;

import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;

import com.datastax.driver.core.schemabuilder.SchemaBuilder;

public class CassandraMailRepositoryModule extends CassandraModuleComposite {

    public static final CassandraModule COUNT_TABLE = CassandraModule.forTable(
        MailRepositoryTable.COUNT_TABLE,
        SchemaBuilder.createTable(MailRepositoryTable.COUNT_TABLE)
            .ifNotExists()
            .addPartitionKey(MailRepositoryTable.REPOSITORY_NAME, text())
            .addColumn(MailRepositoryTable.COUNT, counter()));

    public static final CassandraModule KEYS_TABLE = CassandraModule.forTable(
        MailRepositoryTable.KEYS_TABLE_NAME,
        SchemaBuilder.createTable(MailRepositoryTable.KEYS_TABLE_NAME)
            .ifNotExists()
            .addPartitionKey(MailRepositoryTable.REPOSITORY_NAME, text())
            .addClusteringColumn(MailRepositoryTable.MAIL_KEY, text()));

    public static final CassandraModule MAIL_REPOSITORY_TABLE = CassandraModule.forTable(
        MailRepositoryTable.CONTENT_TABLE_NAME,
        SchemaBuilder.createTable(MailRepositoryTable.CONTENT_TABLE_NAME)
            .ifNotExists()
            .addPartitionKey(MailRepositoryTable.REPOSITORY_NAME, text())
            .addPartitionKey(MailRepositoryTable.MAIL_KEY, text())
            .addColumn(MailRepositoryTable.MESSAGE_SIZE, bigint())
            .addColumn(MailRepositoryTable.STATE, text())
            .addColumn(MailRepositoryTable.HEADER_BLOB_ID, text())
            .addColumn(MailRepositoryTable.BODY_BLOB_ID, text())
            .addColumn(MailRepositoryTable.ATTRIBUTES, map(text(), blob()))
            .addColumn(MailRepositoryTable.ERROR_MESSAGE, text())
            .addColumn(MailRepositoryTable.SENDER, text())
            .addColumn(MailRepositoryTable.RECIPIENTS, list(text()))
            .addColumn(MailRepositoryTable.REMOTE_HOST, text())
            .addColumn(MailRepositoryTable.REMOTE_ADDR, text())
            .addColumn(MailRepositoryTable.LAST_UPDATED, timestamp())
            .addUDTMapColumn(MailRepositoryTable.PER_RECIPIENT_SPECIFIC_HEADERS, text(), frozen(MailRepositoryTable.HEADER_TYPE))
            .withOptions()
            .comment("Stores the mails for a given repository. " +
                "Content is stored with other blobs"));

    public static final CassandraModule HEADER_TYPE = CassandraModule.forType(
        MailRepositoryTable.HEADER_TYPE,
        SchemaBuilder.createType(MailRepositoryTable.HEADER_TYPE)
            .ifNotExists()
            .addColumn(MailRepositoryTable.HEADER_NAME, text())
            .addColumn(MailRepositoryTable.HEADER_VALUE, text()));

    public CassandraMailRepositoryModule() {
        super(COUNT_TABLE,
            KEYS_TABLE,
            MAIL_REPOSITORY_TABLE,
            HEADER_TYPE);
    }
}
