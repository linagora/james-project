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
package org.apache.james;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.PreDestroy;

import org.apache.james.imapserver.netty.IMAPServerFactory;
import org.apache.james.jmap.JMAPServer;
import org.apache.james.lmtpserver.netty.LMTPServerFactory;
import org.apache.james.modules.CommonServicesModule;
import org.apache.james.modules.MailetProcessingModule;
import org.apache.james.modules.ProtocolsModule;
import org.apache.james.pop3server.netty.POP3ServerFactory;
import org.apache.james.protocols.lib.netty.AbstractConfigurableAsyncServer;
import org.apache.james.smtpserver.netty.SMTPServerFactory;
import org.apache.james.utils.ConfigurationsPerformer;
import org.apache.james.utils.ExtendedServerProbe;
import org.apache.james.utils.GuiceServerProbe;
import org.apache.james.webadmin.Port;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.onami.lifecycle.core.Stager;

import com.google.common.collect.Iterables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;

public class GuiceJamesServer {

    private final Module module;
    private Stager<PreDestroy> preDestroy;
    private GuiceServerProbe serverProbe;
    private int jmapPort;
    private int imapPort;
    private int imapPortSSl;
    private int pop3Port;
    private int smtpPort;
    private int lmtpPort;
    private Optional<Port> webadminPort;

    public GuiceJamesServer() {
        this(Modules.combine(
                        new CommonServicesModule(),
                        new ProtocolsModule(),
                        new MailetProcessingModule()));
    }

    private GuiceJamesServer(Module module) {
        this.module = module;
    }
    
    public GuiceJamesServer combineWith(Module... modules) {
        return new GuiceJamesServer(Modules.combine(Iterables.concat(Arrays.asList(module), Arrays.asList(modules))));
    }
    
    public GuiceJamesServer overrideWith(Module... overrides) {
        return new GuiceJamesServer(Modules.override(module).with(overrides));
    }
    
    public void start() throws Exception {
        Injector injector = Guice.createInjector(module);
        preDestroy = injector.getInstance(Key.get(new TypeLiteral<Stager<PreDestroy>>() {}));
        injector.getInstance(ConfigurationsPerformer.class).initModules();
        serverProbe = injector.getInstance(GuiceServerProbe.class);
        retrievePorts(injector);
        webadminPort =locateWebAdminPort(injector);
    }

    private void retrievePorts(Injector injector) {
        jmapPort = injector.getInstance(JMAPServer.class).getPort();
        List<AbstractConfigurableAsyncServer> imapServers = injector.getInstance(IMAPServerFactory.class).getServers();
        for (AbstractConfigurableAsyncServer server : imapServers) {
            if (server.useSSL()) {
                imapPortSSl = server.getPort();
            } else {
                imapPort = server.getPort();
            }
        }
        injector.getInstance(POP3ServerFactory.class).getServers()
            .stream()
            .findFirst()
            .ifPresent(server -> pop3Port = server.getPort());
        injector.getInstance(SMTPServerFactory.class).getServers()
            .stream()
            .findFirst()
            .ifPresent(server -> smtpPort = server.getPort());
        injector.getInstance(LMTPServerFactory.class).getServers()
            .stream()
            .findFirst()
            .ifPresent(server -> lmtpPort = server.getPort());
    }

    private Optional<Port> locateWebAdminPort(Injector injector) {
        try {
            return Optional.of(injector.getInstance(WebAdminServer.class).getPort());
        } catch(Exception e) {
            return Optional.empty();
        }
    }

    public void stop() {
        if (preDestroy != null) {
            preDestroy.stage();
        }
    }

    public ExtendedServerProbe serverProbe() {
        return serverProbe;
    }

    public int getJmapPort() {
        return jmapPort;
    }

    public int getImapPort() {
        return imapPort;
    }

    public int getImapPortSSl() {
        return imapPortSSl;
    }

    public int getPop3Port() {
        return pop3Port;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public int getLmtpPort() {
        return lmtpPort;
    }

    public Optional<Port> getWebadminPort() {
        return webadminPort;
    }

    public Module getModule() {
        return module;
    }

    public Stager<PreDestroy> getPreDestroy() {
        return preDestroy;
    }

    public GuiceServerProbe getServerProbe() {
        return serverProbe;
    }
}
