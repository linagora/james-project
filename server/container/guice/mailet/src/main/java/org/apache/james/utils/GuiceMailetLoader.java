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

package org.apache.james.utils;

import java.util.Objects;
import java.util.Set;

import javax.mail.MessagingException;

import org.apache.james.mailetcontainer.api.MailetLoader;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetConfig;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class GuiceMailetLoader implements MailetLoader {

    public static class DynamicConfiguration {
        private final Class<? extends Mailet> mailetClass;
        private final MailetConfigWrapper configuration;

        public DynamicConfiguration(Class<? extends Mailet> mailetClass, MailetConfigWrapper configuration) {
            this.mailetClass = mailetClass;
            this.configuration = configuration;
        }

        public Class<? extends Mailet> getMailetClass() {
            return mailetClass;
        }

        public MailetConfigWrapper getConfiguration() {
            return configuration;
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof DynamicConfiguration) {
                DynamicConfiguration that = (DynamicConfiguration) o;

                return Objects.equals(this.mailetClass, that.mailetClass);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(mailetClass, configuration);
        }
    }

    public interface MailetConfigWrapper {
        MailetConfig wrap(MailetConfig config);
    }

    private static final String STANDARD_PACKAGE = "org.apache.james.transport.mailets.";

    private final GuiceGenericLoader<Mailet> genericLoader;
    private final Set<DynamicConfiguration> dynamicConfigurations;

    @Inject
    public GuiceMailetLoader(Injector injector, ExtendedClassLoader extendedClassLoader, Set<DynamicConfiguration> dynamicConfigurations) {
        this.dynamicConfigurations = dynamicConfigurations;
        this.genericLoader = new GuiceGenericLoader<>(injector, extendedClassLoader, STANDARD_PACKAGE);
    }

    @Override
    public Mailet getMailet(MailetConfig config) throws MessagingException {
        try {
            Mailet result = genericLoader.instanciate(config.getMailetName());
            result.init(wrapConfig(config, result));
            return result;
        } catch (Exception e) {
            throw new MessagingException("Can not load mailet " + config.getMailetName(), e);
        }
    }

    public MailetConfig wrapConfig(MailetConfig config, Mailet mailet) {
        return dynamicConfigurations.stream()
            .filter(entry -> entry.mailetClass.equals(mailet.getClass()))
            .map(entry -> entry.getConfiguration().wrap(config))
            .findFirst()
            .orElse(config);
    }

}
