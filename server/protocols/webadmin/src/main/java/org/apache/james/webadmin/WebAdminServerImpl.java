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

package org.apache.james.webadmin;

import java.util.Optional;

import javax.annotation.PreDestroy;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.http.jetty.Configuration;
import org.apache.james.http.jetty.JettyHttpServer;
import org.apache.james.webadmin.servlet.DomainServlet;
import org.apache.james.webadmin.servlet.UserServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

public class WebAdminServerImpl implements WebAdminServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebAdminServerImpl.class);

    private final JettyHttpServer server;
    private boolean started;

    public WebAdminServerImpl(Optional<Integer> port, DomainServlet domainServlet, UserServlet userServlet) {
        this.server = JettyHttpServer.create(
            Configuration.builder()
                .port(port)
                .serve(Constants.DOMAIN + "/*")
                    .with(domainServlet)
                .serve(Constants.USER + "/*")
                    .with(userServlet)
                .build());
        this.started = false;
    }

    @Override
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        try {
            server.start();
            started = true;
            LOGGER.info("WebAdminServer started");
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @PreDestroy
    public void stop() {
        try {
            if (started) {
                server.stop();
                started = false;
                LOGGER.info("WebAdminServer stopped");
            } else {
                LOGGER.info("Attempt to shut WebAdminServer down but it was not started");
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    public int getPort() {
        return server.getPort();
    }

}
