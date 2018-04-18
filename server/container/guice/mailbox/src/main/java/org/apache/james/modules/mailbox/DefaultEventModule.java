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

package org.apache.james.modules.mailbox;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;

import javax.inject.Named;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.store.event.DefaultDelegatingMailboxListener;
import org.apache.james.mailbox.store.event.DelegatingMailboxListener;
import org.apache.james.mailbox.store.event.EventDelivery;
import org.apache.james.mailbox.store.event.SynchronousEventDelivery;
import org.apache.james.modules.GuiceMailboxListenerLoader;
import org.apache.james.modules.MailboxListenerProbe;
import org.apache.james.modules.Names;
import org.apache.james.utils.ConfigurationPerformer;
import org.apache.james.utils.GuiceProbe;
import org.apache.james.utils.PropertiesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class DefaultEventModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEventModule.class);

    @Override
    protected void configure() {
        bind(DefaultDelegatingMailboxListener.class).in(Scopes.SINGLETON);
        bind(DelegatingMailboxListener.class).to(DefaultDelegatingMailboxListener.class);

        bind(SynchronousEventDelivery.class).in(Scopes.SINGLETON);
        bind(EventDelivery.class).to(SynchronousEventDelivery.class);

        Multibinder.newSetBinder(binder(), ConfigurationPerformer.class).addBinding().to(ListenerRegistrationPerformer.class);
        Multibinder.newSetBinder(binder(), MailboxListener.class);

        Multibinder.newSetBinder(binder(), GuiceProbe.class).addBinding().to(MailboxListenerProbe.class);
    }

    @Singleton
    public static class ListenerRegistrationPerformer implements ConfigurationPerformer {
        private final MailboxManager mailboxManager;
        private final Set<MailboxListener> listeners;
        private final PropertiesProvider propertiesProvider;
        private final GuiceMailboxListenerLoader listenerLoader;

        @Inject
        public ListenerRegistrationPerformer(@Named(Names.MAILBOXMANAGER_NAME) MailboxManager mailboxManager,
                                             Set<MailboxListener> listeners, PropertiesProvider propertiesProvider,
                                             GuiceMailboxListenerLoader listenerLoader) {
            this.mailboxManager = mailboxManager;
            this.listeners = listeners;
            this.propertiesProvider = propertiesProvider;
            this.listenerLoader = listenerLoader;
        }

        @Override
        public void initModule() {
            try {
                MailboxSession systemSession = mailboxManager.createSystemSession("storeMailboxManager");

                loadGuiceDefinedListeners(systemSession);
                loadUserDefinedMailboxListeners(systemSession);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }

        public void loadUserDefinedMailboxListeners(MailboxSession systemSession) throws ConfigurationException {
            try {
                PropertiesConfiguration mailboxListeners = propertiesProvider.getConfiguration("mailboxListeners");

                List<String> listenerNames = Splitter.on(',')
                    .trimResults()
                    .omitEmptyStrings()
                    .splitToList(mailboxListeners.getString("listener.names", ""));

                listenerNames.stream()
                    .map(name -> mailboxListeners.getString(name + ".class"))
                    .peek(clazz -> LOGGER.info("Loading user defined listener {}", clazz))
                    .map(Throwing.function(listenerLoader::getListener))
                    .forEach(Throwing.consumer(listener -> mailboxManager.addGlobalListener(listener, systemSession)));
            } catch (FileNotFoundException e) {
                LOGGER.warn("Missing mailboxListeners.properties . User defined listeners will be ignored.");
            }
        }

        public void loadGuiceDefinedListeners(MailboxSession systemSession) {
            listeners.forEach(Throwing.consumer(listener ->
                mailboxManager.addGlobalListener(listener, systemSession)));
        }

        @Override
        public List<Class<? extends Configurable>> forClasses() {
            return ImmutableList.of();
        }
    }
}
