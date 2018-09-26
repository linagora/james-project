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

package org.apache.james.modules.rabbitmq;

import java.io.FileNotFoundException;

import javax.inject.Singleton;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.eventsourcing.eventstore.cassandra.dto.EventDTOModule;
import org.apache.james.queue.rabbitmq.view.cassandra.configuration.CassandraMailQueueViewConfiguration;
import org.apache.james.queue.rabbitmq.view.cassandra.configuration.CassandraMailQueueViewConfigurationModule;
import org.apache.james.queue.rabbitmq.view.cassandra.configuration.EventsourcingConfigurationManagement;
import org.apache.james.utils.PropertiesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;

public class CassandraMailQueueViewModule extends AbstractModule {

    private static final String CASSANDRA_MAIL_QUEUE_VIEW_CONFIGURATION_NAME = "mailqueueview";

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraMailQueueViewModule.class);

    @Override
    protected void configure() {
        bind(EventsourcingConfigurationManagement.class).in(Scopes.SINGLETON);

        Multibinder<EventDTOModule> eventDTOModuleBinder = Multibinder.newSetBinder(binder(), EventDTOModule.class);
        eventDTOModuleBinder.addBinding().toInstance(CassandraMailQueueViewConfigurationModule.MAIL_QUEUE_VIEW_CONFIGURATION);
    }

    @Provides
    @Singleton
    private CassandraMailQueueViewConfiguration getMailQueueViewConfiguration(EventsourcingConfigurationManagement configurationManagement,
                                                                              PropertiesProvider propertiesProvider) {
        CassandraMailQueueViewConfiguration configurationFromPropertiesFile = loadConfiguration(propertiesProvider);
        configurationManagement.applyNewConfiguration(configurationFromPropertiesFile);
        return configurationFromPropertiesFile;
    }

    private CassandraMailQueueViewConfiguration loadConfiguration(PropertiesProvider propertiesProvider) {
        try {
            Configuration configuration = propertiesProvider.getConfiguration(CASSANDRA_MAIL_QUEUE_VIEW_CONFIGURATION_NAME);
            return CassandraMailQueueViewConfiguration.from(configuration);
        } catch (FileNotFoundException e) {
            LOGGER.error("Could not find " + CASSANDRA_MAIL_QUEUE_VIEW_CONFIGURATION_NAME + " configuration file. Using the default one");
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }

        return CassandraMailQueueViewConfiguration.DEFAULT;
    }
}
