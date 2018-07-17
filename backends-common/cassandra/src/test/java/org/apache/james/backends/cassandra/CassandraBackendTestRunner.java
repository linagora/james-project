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

package org.apache.james.backends.cassandra;

import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.CassandraTypeProviderContract;
import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.backends.cassandra.utils.PaggingContract;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionDAO;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionDAOContract;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

class CassandraBackendTestRunner extends CassandraTestRunner {
    @Nested
    class SchemaVersionDAOTest extends Runner implements CassandraSchemaVersionDAOContract {
        private CassandraSchemaVersionDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraSchemaVersionModule();
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraSchemaVersionDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        }

        @Override
        public CassandraSchemaVersionDAO testee() {
            return testee;
        }
    }

    @Nested
    class PaggingTestRunner extends Runner implements PaggingContract {
        private CassandraAsyncExecutor testee;

        @Override
        public CassandraModule module() {
            return MODULE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraAsyncExecutor(cassandra.getConf());
        }

        @Override
        public CassandraAsyncExecutor executor() {
            return testee;
        }
    }

    @Nested
    class TypeProviderTest extends Runner implements CassandraTypeProviderContract {
        @Override
        public CassandraModule module() {
            return MODULE;
        }

        @BeforeEach
        void setUp() {
            cassandra.getTypesProvider();
        }

        @Override
        public CassandraCluster cassandra() {
            return cassandra;
        }
    }
}
