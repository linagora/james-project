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

package org.apache.james.webadmin.routes;

import javax.inject.Inject;

import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.service.MigrationService;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Service;

public class MigrationRoutes implements Routes {

    private static final int NO_CONTENT = 204;
    private static final int CONFLICT = 409;

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationRoutes.class);

    public static final String VERSION_BASE = "/version";
    public static final String VERSION_UPGRADE_BASE = VERSION_BASE + "/upgrade";

    private final MigrationService migrationService;
    private final JsonTransformer jsonTransformer;
    private boolean isMigrationRunning;

    @Inject
    public MigrationRoutes(MigrationService migrationService, JsonTransformer jsonTransformer) {
        this.migrationService = migrationService;
        this.jsonTransformer = jsonTransformer;
        this.isMigrationRunning = false;
    }

    @Override
    public void define(Service service) {

        service.get(VERSION_BASE, (request, response) -> {
            return migrationService.getCurrentVersion();
        }, jsonTransformer);

        service.post(VERSION_UPGRADE_BASE, (request, response) -> {
            if (isMigrationRunning) {
                LOGGER.debug("Cassandra already running");
                response.status(CONFLICT);
            } else {
                LOGGER.debug("Cassandra upgrade launched");
                migrationService.upgradeToLastVersion();
                response.status(NO_CONTENT);
            }
            return Constants.EMPTY_BODY;
        });
    }
}
