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

package org.apache.james.jmap.cassandra;

import static org.mockito.Mockito.mock;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.DockerCassandraExtension;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.backends.cassandra.init.CassandraZonedDateTimeModule;
import org.apache.james.jmap.api.access.AccessTokenRepository;
import org.apache.james.jmap.api.access.AccessTokenRepositoryContract;
import org.apache.james.jmap.api.vacation.NotificationRegistry;
import org.apache.james.jmap.api.vacation.NotificationRegistryContract;
import org.apache.james.jmap.api.vacation.VacationRepository;
import org.apache.james.jmap.api.vacation.VacationRepositoryContract;
import org.apache.james.jmap.cassandra.access.CassandraAccessModule;
import org.apache.james.jmap.cassandra.access.CassandraAccessTokenDAO;
import org.apache.james.jmap.cassandra.access.CassandraAccessTokenRepository;
import org.apache.james.jmap.cassandra.vacation.CassandraNotificationRegistry;
import org.apache.james.jmap.cassandra.vacation.CassandraNotificationRegistryDAO;
import org.apache.james.jmap.cassandra.vacation.CassandraNotificationRegistryModule;
import org.apache.james.jmap.cassandra.vacation.CassandraVacationDAO;
import org.apache.james.jmap.cassandra.vacation.CassandraVacationModule;
import org.apache.james.jmap.cassandra.vacation.CassandraVacationRepository;
import org.apache.james.util.date.ZonedDateTimeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DockerCassandraExtension.class)
class CassadraDataJMAPTest implements AccessTokenRepositoryContract, NotificationRegistryContract, VacationRepositoryContract {
    private CassandraCluster cassandra;
    private ZonedDateTimeProvider zonedDateTimeProvider;
    private CassandraAccessTokenRepository accessTokenRepository;
    private CassandraNotificationRegistry notificationRegistry;
    private CassandraVacationRepository vacationRepository;

    @BeforeEach
    void setUp(DockerCassandraExtension.DockerCassandra dockerCassandra) {
        cassandra = CassandraCluster.create(
            new CassandraModuleComposite(
                CassandraVacationModule.MODULE,
                CassandraZonedDateTimeModule.MODULE,
                CassandraAccessModule.MODULE,
                CassandraNotificationRegistryModule.MODULE),
            dockerCassandra.getHost());

        zonedDateTimeProvider = mock(ZonedDateTimeProvider.class);
        accessTokenRepository = new CassandraAccessTokenRepository(
            new CassandraAccessTokenDAO(cassandra.getConf(), AccessTokenRepositoryContract.TTL_IN_MS));
        notificationRegistry = new CassandraNotificationRegistry(zonedDateTimeProvider, new CassandraNotificationRegistryDAO(cassandra.getConf()));
        vacationRepository = new CassandraVacationRepository(new CassandraVacationDAO(cassandra.getConf(), cassandra.getTypesProvider()));
    }

    @AfterEach
    void tearDown() {
        cassandra.close();
    }

    @Override
    public AccessTokenRepository accessTokenRepository() {
        return accessTokenRepository;
    }

    @Override
    public NotificationRegistry notificationRegistry() {
        return notificationRegistry;
    }

    @Override
    public VacationRepository vacationRepository() {
        return vacationRepository;
    }

    @Override
    public ZonedDateTimeProvider zonedDateTimeProvider() {
        return zonedDateTimeProvider;
    }
}
