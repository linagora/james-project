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
package org.apache.james.modules.protocols;

import javax.servlet.Filter;

import org.apache.james.jmap.AuthenticationFilter;
import org.apache.james.jmap.AuthenticationServlet;
import org.apache.james.jmap.BypassOnPostFilter;
import org.apache.james.jmap.api.AccessTokenManager;
import org.apache.james.jmap.crypto.AccessTokenManagerImpl;
import org.apache.james.jmap.crypto.JamesSignatureHandler;
import org.apache.james.jmap.crypto.SignedContinuationTokenManager;
import org.apache.james.jmap.memory.access.MemoryAccessTokenRepository;
import org.apache.james.jmap.utils.DefaultZonedDateTimeProvider;
import org.apache.james.protocols.lib.KeystoreLoader;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.utils.ConfigurationPerformer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.ServletModule;

public class JMAPServerModule extends ServletModule {

    @Override
    protected void configureServlets() {
        bind(AccessTokenManager.class).toInstance(new AccessTokenManagerImpl(new MemoryAccessTokenRepository(100)));
        Multibinder.newSetBinder(binder(), ConfigurationPerformer.class).addBinding().to(JMAPModuleConfigurationPerformer.class);
    }

    @Singleton
    public static class JMAPModuleConfigurationPerformer implements ConfigurationPerformer {

        private final UsersRepository usersRepository;
        private final AccessTokenManager accessTokenManager;
        private final KeystoreLoader keystoreLoader;

        @Inject
        public JMAPModuleConfigurationPerformer(UsersRepository usersRepository, 
                AccessTokenManager accessTokenManager, 
                KeystoreLoader keystoreLoader) {
            
            this.usersRepository = usersRepository;
            this.accessTokenManager = accessTokenManager;
            this.keystoreLoader = keystoreLoader;
        }

        @Override
        public void initModule() throws Exception {
            Server server = new Server(8123);
            
            ServletHandler handler = new ServletHandler();
            server.setHandler(handler);
            
            AuthenticationServlet authenticationServlet = new AuthenticationServlet();
            authenticationServlet.setUsersRepository(usersRepository);
            authenticationServlet.setContinuationTokenManager(new SignedContinuationTokenManager(
                    new JamesSignatureHandler(keystoreLoader), new DefaultZonedDateTimeProvider()));
            authenticationServlet.setAccessTokenManager(accessTokenManager);
            ServletHolder servletHolder = new ServletHolder(authenticationServlet);
            handler.addServletWithMapping(servletHolder, "/*");

            AuthenticationFilter authenticationFilter = new AuthenticationFilter(new AccessTokenManagerImpl(new MemoryAccessTokenRepository(100)));
            Filter getAuthenticationFilter = new BypassOnPostFilter(authenticationFilter);
            FilterHolder authenticationFilterHolder = new FilterHolder(getAuthenticationFilter);
            handler.addFilterWithMapping(authenticationFilterHolder, "/*", null);
            
            server.start();
        }
    }
}