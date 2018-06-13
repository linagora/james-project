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

import java.util.Optional;

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class ListenerConfiguration {

    public static ListenerConfiguration from(HierarchicalConfiguration configuration) {
        String listenerClass = configuration.getString("class");
        String listenerFactoryClass = configuration.getString("factoryclass");
        Preconditions.checkState(!(Strings.isNullOrEmpty(listenerClass) && Strings.isNullOrEmpty(listenerFactoryClass)),
            "class name or factoryclass name is mandatory");
        Optional<Boolean> isAsync = Optional.ofNullable(configuration.getBoolean("async", null));
        return new ListenerConfiguration(
            Optional.ofNullable(listenerClass), Optional.ofNullable(listenerFactoryClass),
            extractSubconfiguration(configuration), isAsync);
    }

    public static ListenerConfiguration forClass(String clazz) {
        return new ListenerConfiguration(Optional.of(clazz), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static ListenerConfiguration forFactory(String clazz) {
        return new ListenerConfiguration(Optional.empty(), Optional.of(clazz), Optional.empty(), Optional.empty());
    }

    private static Optional<HierarchicalConfiguration> extractSubconfiguration(HierarchicalConfiguration configuration) {
        return configuration.configurationsAt("configuration")
            .stream()
            .findFirst();
    }

    private final Optional<String> clazz;
    private final Optional<String> factoryClazz;
    private final Optional<HierarchicalConfiguration> configuration;
    private final Optional<Boolean> isAsync;

    private ListenerConfiguration(Optional<String> clazz, Optional<String> factoryClazz,
                                  Optional<HierarchicalConfiguration> configuration, Optional<Boolean> isAsync) {
        this.clazz = clazz;
        this.factoryClazz = factoryClazz;
        this.configuration = configuration;
        this.isAsync = isAsync;
    }

    public Optional<String> getClazz() {
        return clazz;
    }

    public Optional<String> getFactoryClass() {
        return factoryClazz;
    }

    public Optional<HierarchicalConfiguration> getConfiguration() {
        return configuration;
    }

    public Optional<Boolean> isAsync() {
        return isAsync;
    }
}