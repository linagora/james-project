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

package org.apache.james.mpt.managesieve.cassandra;

import org.apache.james.backends.cassandra.CassandraTestRunner;
import org.apache.james.mpt.host.ManageSieveHostSystem;
import org.apache.james.mpt.testsuite.AuthenticateContract;
import org.apache.james.mpt.testsuite.CapabilityContract;
import org.apache.james.mpt.testsuite.CheckScriptContract;
import org.apache.james.mpt.testsuite.DeleteScriptContract;
import org.apache.james.mpt.testsuite.GetScriptContract;
import org.apache.james.mpt.testsuite.HaveSpaceContract;
import org.apache.james.mpt.testsuite.ListScriptsContract;
import org.apache.james.mpt.testsuite.LogoutContract;
import org.apache.james.mpt.testsuite.NoopContract;
import org.apache.james.mpt.testsuite.PutScriptContract;
import org.apache.james.mpt.testsuite.RenameScriptContract;
import org.apache.james.mpt.testsuite.SetActiveContract;
import org.apache.james.mpt.testsuite.StartTlsContract;
import org.apache.james.mpt.testsuite.UnauthenticatedContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import com.google.inject.Guice;
import com.google.inject.Injector;

class CassandraManageSieveMPTTestRunner extends CassandraTestRunner {
    @Nested
    class CassandraAuthenticateTest implements AuthenticateContract {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraCapabilityTest implements CapabilityContract {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraCheckScriptTest implements CheckScriptContract {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraDeleteScriptTest implements DeleteScriptContract {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraGetScriptTest implements GetScriptContract {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraHaveSpaceTest implements HaveSpaceContract {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraListScriptsTest implements ListScriptsContract {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraLogoutTest implements LogoutContract {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraNoopTest implements NoopContract {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraPutScriptTest implements PutScriptContract {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraRenameScriptTest implements RenameScriptContract {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraSetActiveTest implements SetActiveContract {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraStartTlsTest implements StartTlsContract {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraUnauthenticatedTest implements UnauthenticatedContract {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }
}
