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
import org.apache.james.webadmin.dto.VersionRequest;
import org.apache.james.webadmin.service.MigrationService;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Service;

public class MigrationRoutes implements Routes {

    private static final int NO_CONTENT = 204;

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationRoutes.class);

    public static final String VERSION_BASE = "/version";
    public static final String VERSION_BASE_LATEST = VERSION_BASE + "/latest";
    public static final String VERSION_UPGRADE_BASE = VERSION_BASE + "/upgrade";
    public static final String VERSION_UPGRADE_TO_LATEST_BASE = VERSION_UPGRADE_BASE + "/latest";
    public static final int CONTENT_UP_TO_DATE = 404;
    public static final int INVALID_VERSION = 400;

    private final MigrationService migrationService;
    private final JsonTransformer jsonTransformer;

    @Inject
    public MigrationRoutes(MigrationService migrationService, JsonTransformer jsonTransformer) {
        this.migrationService = migrationService;
        this.jsonTransformer = jsonTransformer;
    }

    @Override
    public void define(Service service) {
        service.get(VERSION_BASE, (request, response) -> {
            return migrationService.getCurrentVersion();
        }, jsonTransformer);

        service.get(VERSION_BASE_LATEST, (request, response) -> {
            return migrationService.getLatestVersion();
        }, jsonTransformer);

        service.post(VERSION_UPGRADE_BASE, (request, response) -> {
            LOGGER.debug("Cassandra upgrade launched");
            try {
                VersionRequest versionRequest = VersionRequest.parse(request.body());
                migrationService.upgradeToVersion(versionRequest.getValue());
                response.status(NO_CONTENT);
            } catch (NullPointerException e) {
                LOGGER.info("Invalid request for version upgrade");
                response.status(INVALID_VERSION);
            } catch (IllegalArgumentException e) {
                LOGGER.info("Invalid request for version upgrade");
                response.status(INVALID_VERSION);
            } catch (IllegalStateException e) {
                LOGGER.info("System is already up to date", e);
                response.status(CONTENT_UP_TO_DATE);
            }
            return Constants.EMPTY_BODY;
        });

        service.post(VERSION_UPGRADE_TO_LATEST_BASE, (request, response) -> {
            try {
                migrationService.upgradeToLastVersion();
            } catch (IllegalStateException e) {
                LOGGER.info("System is already up to date", e);
                response.status(CONTENT_UP_TO_DATE);
            }

            return Constants.EMPTY_BODY;
        });
    }
}
