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

import org.apache.james.backends.cassandra.CassandraTestRunner;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.blob.cassandra.CassandraBlobId;
import org.apache.james.blob.cassandra.CassandraBlobModule;
import org.apache.james.blob.cassandra.CassandraBlobsDAO;
import org.apache.james.mailrepository.MailRepositoryContract;
import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.mailrepository.api.MailRepositoryUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CassandraMailRepositoryTestRunner extends CassandraTestRunner {
    @Nested
    class MailRepositoryCountDAOTest extends Runner implements CassandraMailRepositoryCountDAOContract {
        private CassandraMailRepositoryCountDAO testee;

        @Override
        public CassandraModule module() {
            return CassandraMailRepositoryModule.COUNT_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailRepositoryCountDAO(cassandra.getConf());
        }

        @Override
        public CassandraMailRepositoryCountDAO testee() {
            return testee;
        }
    }

    @Nested
    class MailRepositoryKeysDAOTest extends Runner implements CassandraMailRepositoryKeysDAOContract {
        private CassandraMailRepositoryKeysDAO testee;

        @Override
        public CassandraModule module() {
            return CassandraMailRepositoryModule.KEYS_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailRepositoryKeysDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        }

        @Override
        public CassandraMailRepositoryKeysDAO testee() {
            return testee;
        }
    }

    @Nested
    class MailRepositoryMailDAOTest extends Runner implements CassandraMailRepositoryMailDAOContract {
        private CassandraMailRepositoryMailDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                CassandraMailRepositoryModule.HEADER_TYPE,
                CassandraMailRepositoryModule.MAIL_REPOSITORY_TABLE);
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailRepositoryMailDAO(cassandra.getConf(), BLOB_ID_FACTORY, cassandra.getTypesProvider());
        }

        @Override
        public CassandraMailRepositoryMailDAO testee() {
            return testee;
        }
    }

    @Nested
    class CassandraMailRepositoryTest extends Runner implements MailRepositoryContract {
        MailRepositoryUrl URL = MailRepositoryUrl.from("proto://url");
        CassandraBlobId.Factory BLOB_ID_FACTORY = new CassandraBlobId.Factory();

        CassandraMailRepository cassandraMailRepository;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                new CassandraMailRepositoryModule(),
                new CassandraBlobModule());
        }

        @BeforeEach
        void setup() {
            CassandraMailRepositoryMailDAO mailDAO = new CassandraMailRepositoryMailDAO(cassandra.getConf(), BLOB_ID_FACTORY, cassandra.getTypesProvider());
            CassandraMailRepositoryKeysDAO keysDAO = new CassandraMailRepositoryKeysDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
            CassandraMailRepositoryCountDAO countDAO = new CassandraMailRepositoryCountDAO(cassandra.getConf());
            CassandraBlobsDAO blobsDAO = new CassandraBlobsDAO(cassandra.getConf());

            cassandraMailRepository = new CassandraMailRepository(URL,
                keysDAO, countDAO, mailDAO, blobsDAO);
        }

        @Override
        public MailRepository retrieveRepository() {
            return cassandraMailRepository;
        }

        @Test
        @Disabled("key is unique in Cassandra")
        @Override
        public void sizeShouldBeIncrementedByOneWhenDuplicates() {
        }

    }
}
