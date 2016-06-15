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

package org.apache.james.webadmin.utils;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.james.webadmin.Constants;

import com.github.fge.lambdas.consumers.ThrowingConsumer;
import com.google.common.base.Strings;

public class ResourceAccessor {

    public static void applyOnResource(HttpServletRequest req, HttpServletResponse resp, ThrowingConsumer<String> operation) throws Exception {
        if (Strings.isNullOrEmpty(req.getPathInfo())) {
            resp.setStatus(SC_BAD_REQUEST);
        } else {
            applyOnResource(resp, new PathAnalyzer(req.getPathInfo()), operation);
        }
    }

    private static void applyOnResource(HttpServletResponse resp, PathAnalyzer pathInfo, ThrowingConsumer<String> operation) throws Exception {
        if (pathInfo.validate(2)) {
            operation.accept(pathInfo.retrieveLastPart());

            resp.setContentType(Constants.JSON_CONTENT_TYPE);
            resp.setStatus(SC_OK);
        } else {
            resp.setStatus(SC_BAD_REQUEST);
        }
    }

}
