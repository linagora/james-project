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

import javax.annotation.PreDestroy;

import org.apache.james.jmap.JMAPModule;
import org.apache.james.jmap.JMAPServer;
import org.apache.james.modules.CommonServicesModule;
import org.apache.james.modules.protocols.IMAPServerModule;
import org.apache.james.modules.protocols.JMAPServerModule;
import org.apache.james.modules.protocols.LMTPServerModule;
import org.apache.james.modules.protocols.ManageSieveServerModule;
import org.apache.james.modules.protocols.POP3ServerModule;
import org.apache.james.modules.protocols.ProtocolHandlerModule;
import org.apache.james.modules.protocols.SMTPServerModule;
import org.apache.james.modules.server.ActiveMQQueueModule;
import org.apache.james.modules.server.CamelMailetContainerModule;
import org.apache.james.modules.server.ConfigurationProviderModule;
import org.apache.james.modules.server.DNSServiceModule;
import org.apache.james.modules.server.MailStoreRepositoryModule;
import org.apache.james.modules.server.SieveModule;
import org.apache.james.utils.ConfigurationsPerformer;
import org.apache.james.utils.ExtendedServerProbe;
import org.apache.james.utils.GuiceServerProbe;
import org.apache.onami.lifecycle.core.Stager;
import org.apache.onami.lifecycle.jsr250.PreDestroyModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;

public class GuiceJamesServer {

    public static Module protocolsModule(Module methodsModule) {
        return Modules.combine(
            new JMAPServerModule(new JMAPModule(methodsModule)),
            new IMAPServerModule(),
            new ProtocolHandlerModule(),
            new POP3ServerModule(),
            new SMTPServerModule(),
            new LMTPServerModule(),
            new ManageSieveServerModule());
    }

    public static Module mailetProcessingModule(Module decoratorModule) {
        return Modules.combine(
            decoratorModule,
            new SieveModule(),
            new MailStoreRepositoryModule(),
            new CamelMailetContainerModule());
    }

    public static final Module commonUtilitiesModule = Modules.combine(
        new CommonServicesModule(),
        new ConfigurationProviderModule(),
        new PreDestroyModule(),
        new DNSServiceModule());

    private final Module serverModule;
    private Stager<PreDestroy> preDestroy;
    private GuiceServerProbe serverProbe;
    private int jmapPort;

    public GuiceJamesServer(Module serverModule) {
        this.serverModule = serverModule;
    }

    public void start() throws Exception {
        Injector injector = Guice.createInjector(serverModule);
        injector.getInstance(ConfigurationsPerformer.class).initModules();
        preDestroy = injector.getInstance(Key.get(new TypeLiteral<Stager<PreDestroy>>() {}));
        serverProbe = injector.getInstance(GuiceServerProbe.class);
        jmapPort = injector.getInstance(JMAPServer.class).getPort();
    }

    public void stop() {
        preDestroy.stage();
    }

    public ExtendedServerProbe serverProbe() {
        return serverProbe;
    }

    public int getJmapPort() {
        return jmapPort;
    }
}
