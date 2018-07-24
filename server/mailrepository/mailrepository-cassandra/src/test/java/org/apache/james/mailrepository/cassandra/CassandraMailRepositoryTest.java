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

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.DockerCassandraExtension;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.blob.cassandra.CassandraBlobId;
import org.apache.james.blob.cassandra.CassandraBlobModule;
import org.apache.james.blob.cassandra.CassandraBlobsDAO;
import org.apache.james.mailrepository.MailRepositoryContract;
import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.mailrepository.api.MailRepositoryUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DockerCassandraExtension.class)
class CassandraMailRepositoryTest implements MailRepositoryContract, CassandraMailRepositoryCountDAOContract,
                                             CassandraMailRepositoryKeysDAOContract, CassandraMailRepositoryMailDAOContract {

    private MailRepositoryUrl URL = MailRepositoryUrl.from("proto://url");
    private CassandraBlobId.Factory BLOB_ID_FACTORY = new CassandraBlobId.Factory();

    private CassandraCluster cassandra;

    private CassandraMailRepository cassandraMailRepository;
    private CassandraMailRepositoryMailDAO mailDAO;
    private CassandraMailRepositoryKeysDAO keysDAO;
    private CassandraMailRepositoryCountDAO countDAO;

    @BeforeEach
    void setUp(DockerCassandraExtension.DockerCassandra dockerCassandra) {
        cassandra = CassandraCluster.create(
            new CassandraModuleComposite(
                CassandraMailRepositoryModule.MODULE,
                CassandraBlobModule.MODULE),
            dockerCassandra.getHost());

        mailDAO = new CassandraMailRepositoryMailDAO(cassandra.getConf(), CassandraMailRepositoryMailDAOContract.BLOB_ID_FACTORY, cassandra.getTypesProvider());

        CassandraMailRepositoryMailDAO realMailDAO = new CassandraMailRepositoryMailDAO(cassandra.getConf(), BLOB_ID_FACTORY, cassandra.getTypesProvider());
        keysDAO = new CassandraMailRepositoryKeysDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        countDAO = new CassandraMailRepositoryCountDAO(cassandra.getConf());
        CassandraBlobsDAO blobsDAO = new CassandraBlobsDAO(cassandra.getConf());

        cassandraMailRepository = new CassandraMailRepository(URL,
            keysDAO, countDAO, realMailDAO, blobsDAO);
    }

    @AfterEach
    void teardown() {
        cassandra.close();
    }

    @Override
    public MailRepository retrieveRepository() {
        return cassandraMailRepository;
    }

    @Override
    public CassandraMailRepositoryCountDAO countDAO() {
        return countDAO;
    }

    @Override
    public CassandraMailRepositoryKeysDAO keysDAO() {
        return keysDAO;
    }

    @Override
    public CassandraMailRepositoryMailDAO mailDAO() {
        return mailDAO;
    }

    @Test
    @Disabled("key is unique in Cassandra")
    @Override
    public void sizeShouldBeIncrementedByOneWhenDuplicates() {
    }
}