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
package org.apache.james.mailbox.spamassassin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.util.Host;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class SpamAssassinConfigurationTest {

    @Test
    public void spamAssassinConfigurationShouldRespectBeanContract() {
        EqualsVerifier.forClass(SpamAssassinConfiguration.class)
            .verify();
    }

    @Test
    public void isEnableShouldReturnFalseWhenEmpty() {
        SpamAssassinConfiguration configuration = new SpamAssassinConfiguration(Optional.empty());
        assertThat(configuration.isEnable()).isFalse();
    }

    @Test
    public void isEnableShouldReturnTrueWhenConfigured() {
        int port = 1;
        SpamAssassinConfiguration configuration = new SpamAssassinConfiguration(Optional.of(Host.from("hostname", port)));
        assertThat(configuration.isEnable()).isTrue();
    }

    @Test
    public void isEnableShouldReturnFalseWhenNoConfiguration() {
        SpamAssassinConfiguration configuration = SpamAssassinConfiguration.fromXML(new DefaultConfigurationBuilder());

        assertThat(configuration.isEnable()).isFalse();
    }

    @Test
    public void hostShouldReturnCustomWhenConfigurationIsProvided() {
        String ip = "10.69.1.123";
        int port = 1783;
        Host host = Host.from(ip, port);
        HierarchicalConfiguration xmlCongiguration = SpamAssassinConfiguration.generateXMLForHost(host);

        SpamAssassinConfiguration configuration = SpamAssassinConfiguration.fromXML(xmlCongiguration);
        assertThat(configuration.getHost().get()).isEqualTo(host);
    }
}
