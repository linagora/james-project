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
package org.apache.james.modules.spamassassin;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.james.mailbox.spamassassin.SpamAssassinConfiguration;
import org.apache.james.util.Host;
import org.junit.Test;

public class SpamAssassinConfigurationLoaderTest {

    @Test
    public void isEnableShouldReturnFalseWhenDisable() {
        SpamAssassinConfiguration configuration = SpamAssassinConfiguration.disabled();
        assertThat(configuration.isEnable()).isFalse();
    }

    @Test
    public void isEnableShouldReturnTrueWhenEnable() {
        SpamAssassinConfiguration configuration = SpamAssassinConfigurationLoader.fromProperties(new PropertiesConfiguration());
        assertThat(configuration.isEnable()).isTrue();
    }

    @Test
    public void hostShouldReturnDefaultWhenConfigurationIsEmpty() {
        SpamAssassinConfiguration configuration = SpamAssassinConfigurationLoader.fromProperties(new PropertiesConfiguration());

        assertThat(configuration)
            .isEqualTo(SpamAssassinConfiguration.builder()
                .host(Host.from(SpamAssassinConfigurationLoader.DEFAULT_HOST, SpamAssassinConfigurationLoader.DEFAULT_PORT))
                .isAsynchronous(SpamAssassinConfiguration.Builder.DEFAULT_ASYNCHRONOUS)
                .threadCount(SpamAssassinConfiguration.Builder.DEFAULT_THREAD_COUNT)
                .build());
    }

    @Test
    public void hostShouldReturnCustomWhenConfigurationIsProvided() {
        PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();
        String host = "10.69.1.123";
        int port = 1783;
        boolean asyncDisabled = false;
        int threadCount = 4;

        propertiesConfiguration.addProperty("spamassassin.host", host);
        propertiesConfiguration.addProperty("spamassassin.port", port);
        propertiesConfiguration.addProperty("spamassassin.asynchronous", asyncDisabled);
        propertiesConfiguration.addProperty("spamassassin.client.thread.count", threadCount);

        assertThat(SpamAssassinConfigurationLoader.fromProperties(propertiesConfiguration))
            .isEqualTo(SpamAssassinConfiguration.builder()
                .host(Host.from(host, port))
                .isAsynchronous(asyncDisabled)
                .threadCount(threadCount)
                .build());
    }
}
