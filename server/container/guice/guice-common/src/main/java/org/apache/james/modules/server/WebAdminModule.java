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

package org.apache.james.modules.server;

import java.util.List;
import java.util.Optional;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.utils.ConfigurationPerformer;
import org.apache.james.utils.PropertiesProvider;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminServerImpl;
import org.apache.james.webadmin.servlet.DomainServlet;
import org.apache.james.webadmin.servlet.UserServlet;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class WebAdminModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), ConfigurationPerformer.class).addBinding().to(WebAdminServerConfigurationPerformer.class);
    }

    @Provides
    @Singleton
    @Inject
    WebAdminServer provideWebAdminServer(WebAdminConfiguration webAdminConfiguration,
                                         DomainServlet domainServlet,
                                         UserServlet userServlet) throws Exception {
        if (webAdminConfiguration.isEnabled()) {
            return new WebAdminServerImpl(webAdminConfiguration.getPort(), domainServlet, userServlet);
        } else {
            return new FakeWebAdminServer();
        }
    }

    @Provides
    @Inject
    WebAdminConfiguration provideConfiguration(PropertiesProvider propertiesProvider) throws Exception {
        PropertiesConfiguration configuration = propertiesProvider.getConfiguration("webadmin");
        return WebAdminConfiguration.ofProperties(
            configuration.getBoolean("enabled"),
            configuration.getInteger("port", null));
    }

    @Singleton
    public static class WebAdminServerConfigurationPerformer implements ConfigurationPerformer {

        private final WebAdminServer webAdminServer;

        @Inject
        public WebAdminServerConfigurationPerformer(WebAdminServer webAdminServer) {
            this.webAdminServer = webAdminServer;
        }

        public void initModule() {
            try {
                webAdminServer.configure(null);
            } catch (ConfigurationException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public List<Class<? extends Configurable>> forClasses() {
            return ImmutableList.of(WebAdminServer.class);
        }
    }

    public static class FakeWebAdminServer implements WebAdminServer {

        @Override
        public void configure(HierarchicalConfiguration config) throws ConfigurationException {

        }

    }

    public static class WebAdminConfiguration {

        public static WebAdminConfiguration ofProperties(boolean enabled, Integer port) {
            return new WebAdminConfiguration(enabled, Optional.ofNullable(port));
        }

        public static WebAdminConfiguration enabledRandomPort() {
            return new WebAdminConfiguration(true, Optional.empty());
        }

        private final boolean enabled;
        private final Optional<Integer> port;

        private WebAdminConfiguration(boolean enabled, Optional<Integer> port) {
            this.enabled = enabled;
            this.port = port;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public Optional<Integer> getPort() {
            return port;
        }
    }

}
