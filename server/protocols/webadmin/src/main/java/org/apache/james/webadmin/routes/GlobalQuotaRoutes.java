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

import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.quota.MaxQuotaManager;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.dto.QuotaRequest;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Service;

public class GlobalQuotaRoutes implements Routes {

    public static final String QUOTA_PREFIX = "/quota";
    public static final String COUNT_ENDPOINT = QUOTA_PREFIX + "/count";
    public static final String SIZE_ENDPOINT = QUOTA_PREFIX + "/size";
    private static final Logger LOGGER = LoggerFactory.getLogger(Routes.class);

    private final MaxQuotaManager maxQuotaManager;
    private final JsonTransformer jsonTransformer;

    @Inject
    public GlobalQuotaRoutes(MaxQuotaManager maxQuotaManager, JsonTransformer jsonTransformer) {
        this.maxQuotaManager = maxQuotaManager;
        this.jsonTransformer = jsonTransformer;
    }

    @Override
    public void define(Service service) {
        service.get(COUNT_ENDPOINT, (request, response) -> {
            long value = maxQuotaManager.getDefaultMaxMessage();
            response.status(200);
            return value;
        }, jsonTransformer);

        service.delete(COUNT_ENDPOINT, (request, response) -> {
            maxQuotaManager.setDefaultMaxMessage(Quota.UNLIMITED);
            response.status(204);
            return "";
        });

        service.put(COUNT_ENDPOINT, (request, response) -> {
            try {
                QuotaRequest quotaRequest = QuotaRequest.parse(request.body());
                maxQuotaManager.setDefaultMaxMessage(quotaRequest.getValue());
                response.status(204);
            } catch (IllegalArgumentException e) {
                LOGGER.info("Invalid quota. Need to be an integer value greater than 0");
                response.status(400);
            }
            return "";
        });

        service.get(SIZE_ENDPOINT, (request, response) -> {
            long value = maxQuotaManager.getDefaultMaxStorage();
            response.status(200);
            return value;
        }, jsonTransformer);

        service.delete(SIZE_ENDPOINT, (request, response) -> {
            maxQuotaManager.setDefaultMaxStorage(Quota.UNLIMITED);
            response.status(204);
            return "";
        });

        service.put(SIZE_ENDPOINT, (request, response) -> {
            try {
                QuotaRequest quotaRequest = QuotaRequest.parse(request.body());
                maxQuotaManager.setDefaultMaxStorage(quotaRequest.getValue());
                response.status(204);
            } catch (IllegalArgumentException e) {
                LOGGER.info("Invalid quota. Need to be an integer value greater than 0");
                response.status(400);
            }
            return "";
        });
    }
}
