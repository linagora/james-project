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

import org.apache.james.backends.cassandra.CassandraTestRunner;
import org.apache.james.backends.cassandra.components.CassandraModule;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

class CassadraDataJMAPTestRunner extends CassandraTestRunner {
    @Nested
    class VacationRepositoryTest extends Runner implements VacationRepositoryContract {
        private VacationRepository testee;

        @Override
        public CassandraModule module() {
            return  new CassandraModuleComposite(CassandraVacationModule.MODULE, CassandraZonedDateTimeModule.MODULE);
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraVacationRepository(new CassandraVacationDAO(cassandra.getConf(), cassandra.getTypesProvider()));
        }

        @Override
        public VacationRepository testee() {
            return testee;
        }
    }

    @Nested
    class AccessTokenRepositoryTest extends Runner implements AccessTokenRepositoryContract {
        private AccessTokenRepository testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(CassandraAccessModule.MODULE, CassandraZonedDateTimeModule.MODULE);
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraAccessTokenRepository(
                new CassandraAccessTokenDAO(cassandra.getConf(), AccessTokenRepositoryContract.TTL_IN_MS));
        }

        @Override
        public AccessTokenRepository testee() {
            return testee;
        }
    }

    @Nested
    class NotificationRegistryTest extends Runner implements NotificationRegistryContract {
        private NotificationRegistry testee;
        private ZonedDateTimeProvider zonedDateTimeProvider;

        @Override
        public CassandraModule module() {
            return CassandraNotificationRegistryModule.MODULE;
        }

        @BeforeEach
        void setUp() {
            zonedDateTimeProvider = mock(ZonedDateTimeProvider.class);
            testee = new CassandraNotificationRegistry(zonedDateTimeProvider, new CassandraNotificationRegistryDAO(cassandra.getConf()));
        }

        @Override
        public NotificationRegistry testee() {
            return testee;
        }

        @Override
        public ZonedDateTimeProvider zonedDateTimeProvider() {
            return zonedDateTimeProvider;
        }
    }
}
