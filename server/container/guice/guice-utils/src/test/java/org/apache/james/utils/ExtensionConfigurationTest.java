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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.commons.configuration2.Configuration;
import org.junit.jupiter.api.Test;

public class ExtensionConfigurationTest {
    @Test
    void shouldReturnEmptyWhenNoField() throws Exception {
        Configuration configuration = getConfiguration("extensions-none.properties");

        assertThat(ExtensionConfiguration.from(configuration).getAdditionalGuiceModulesForExtensions())
            .isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenEmptyField() throws Exception {
        Configuration configuration = getConfiguration("extensions-empty.properties");

        assertThat(ExtensionConfiguration.from(configuration).getAdditionalGuiceModulesForExtensions())
            .isEmpty();
    }

    @Test
    void shouldReturnOneRoutes() throws Exception {
        Configuration configuration = getConfiguration("extensions-one.properties");

        assertThat(ExtensionConfiguration.from(configuration).getAdditionalGuiceModulesForExtensions())
            .containsOnly(new ClassName("org.apache.custom.extensions.CustomExtension"));
    }

    @Test
    void shouldReturnSeveralRoutes() throws Exception {
        Configuration configuration = getConfiguration("extensions-two.properties");

        assertThat(ExtensionConfiguration.from(configuration).getAdditionalGuiceModulesForExtensions())
            .containsOnly(new ClassName("org.apache.custom.extensions.CustomExtension"),
                new ClassName("org.apache.custom.extension.AnotherCustomExtension"));
    }

    private Configuration getConfiguration(String name) throws Exception {
        return PropertiesProvider.getConfiguration(new File(ClassLoader.getSystemResource(name).toURI()));
    }
}