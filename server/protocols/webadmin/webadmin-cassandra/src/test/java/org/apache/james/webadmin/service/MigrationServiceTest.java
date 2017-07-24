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

package org.apache.james.webadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionDAO;
import org.apache.james.mailbox.cassandra.mail.migration.Migration;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MigrationServiceTest {
    private static final int LATEST_VERSION = 3;
    private static final int CURRENT_VERSION = 2;
    private static final int OLDER_VERSION = 1;
    private MigrationService testee;
    private CassandraSchemaVersionDAO schemaVersionDAO;
    private Map<Integer, Migration> allMigrationClazz;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        ImmutableMap.Builder<Integer, Migration> builder = ImmutableMap.builder();
        allMigrationClazz = builder.put(OLDER_VERSION, mock(Migration.class))
                .put(CURRENT_VERSION, mock(Migration.class))
                .put(LATEST_VERSION, mock(Migration.class))
                .build();
        schemaVersionDAO = mock(CassandraSchemaVersionDAO.class);
        testee = new MigrationService(schemaVersionDAO, allMigrationClazz, LATEST_VERSION);
    }

    @Test
    public void getCurrentVersionShouldReturnCurrentVersion() throws Exception {
        when(schemaVersionDAO.getCurrentSchemaVersion()).thenReturn(CompletableFuture.completedFuture(Optional.of(CURRENT_VERSION)));

        assertThat(testee.getCurrentVersion().getversion().get()).isEqualTo(CURRENT_VERSION);
    }

    @Test
    public void getLatestVersionShouldReturnTheLatestVersion() throws Exception {
        assertThat(testee.getLatestVersion().getversion().get()).isEqualTo(LATEST_VERSION);
    }

    @Test
    public void upgradeToVersionShouldThrowWhenCurrentVersionIsUpToDate() throws Exception {
        expectedException.expect(IllegalStateException.class);

        when(schemaVersionDAO.getCurrentSchemaVersion()).thenReturn(CompletableFuture.completedFuture(Optional.of(CURRENT_VERSION)));

        testee.upgradeToVersion(OLDER_VERSION);
    }

    @Test
    public void upgradeToVersionShouldUpdateToVersion() throws Exception {
        when(schemaVersionDAO.getCurrentSchemaVersion()).thenReturn(CompletableFuture.completedFuture(Optional.of(OLDER_VERSION)));

        testee.upgradeToVersion(CURRENT_VERSION);

        verify(schemaVersionDAO, times(1)).updateVersion(eq(CURRENT_VERSION));
    }

    @Test
    public void upgradeToLastVersionShouldThrowWhenVersionIsUpToDate() throws Exception {
        expectedException.expect(IllegalStateException.class);

        when(schemaVersionDAO.getCurrentSchemaVersion()).thenReturn(CompletableFuture.completedFuture(Optional.of(LATEST_VERSION)));

        testee.upgradeToLastVersion();
    }

    @Test
    public void upgradeToLastVersionShouldUpdateToLatestVersion() throws Exception {
        when(schemaVersionDAO.getCurrentSchemaVersion()).thenReturn(CompletableFuture.completedFuture(Optional.of(OLDER_VERSION)));

        testee.upgradeToLastVersion();

        verify(schemaVersionDAO, times(1)).updateVersion(eq(LATEST_VERSION));
    }
}