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

import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionDAO;
import org.apache.james.mailbox.cassandra.mail.migration.Migration;
import org.apache.james.webadmin.dto.VersionResponse;

import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MigrationService {

    private static final String LATEST_VERSION = "latestVersion";
    private final CassandraSchemaVersionDAO schemaVersionDAO;
    private final Integer latestVersion;
    private final Map<Integer, Migration> allMigrationClazz;

    @Inject
    public MigrationService(CassandraSchemaVersionDAO schemaVersionDAO, Map<Integer, Migration> allMigrationClazz, @Named(LATEST_VERSION) Integer latestVersion) {
        this.schemaVersionDAO = schemaVersionDAO;
        this.latestVersion = latestVersion;
        this.allMigrationClazz = allMigrationClazz;
    }

    public VersionResponse getCurrentVersion() {
        return new VersionResponse(schemaVersionDAO.getCurrentSchemaVersion().join());
    }

    public VersionResponse getLatestVersion() {
        return new VersionResponse(Optional.ofNullable(latestVersion));
    }

    public void upgradeToVersion(Integer newVersion) {
        int currentVersion = schemaVersionDAO.getCurrentSchemaVersion().join().get();
        if (currentVersion >= newVersion) {
            throw new IllegalStateException("Current version is already up to date");
        }

        IntStream.range(currentVersion, newVersion)
            .boxed()
            .forEach(i -> {
                allMigrationClazz.get(i).runFullMigration();
            });
        schemaVersionDAO.updateVersion(newVersion);
    }

    public void upgradeToLastVersion() {
        upgradeToVersion(latestVersion);
    }

}
