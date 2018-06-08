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

import java.util.Optional;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.james.mailbox.spamassassin.SpamAssassinConfiguration;
import org.apache.james.util.Host;

public class SpamAssassinConfigurationLoader {

    private static final String SPAMASSASSIN_HOST = "spamassassin.host";
    private static final String SPAMASSASSIN_PORT = "spamassassin.port";
    private static final String SPAMASSASSIN_ASYNCHRONOUS = "spamassassin.asynchronous";
    private static final String SPAMASSASSIN_THREAD_COUNT = "spamassassin.client.thread.count";
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 783;

    public static SpamAssassinConfiguration fromProperties(PropertiesConfiguration configuration) {
        return SpamAssassinConfiguration.builder()
            .host(getHost(configuration))
            .isAsynchronous(isAsynchronous(configuration))
            .threadCount(getThreadCount(configuration))
            .build();
    }

    private static Host getHost(PropertiesConfiguration propertiesReader) {
        return Host.from(propertiesReader.getString(SPAMASSASSIN_HOST, DEFAULT_HOST), 
                propertiesReader.getInteger(SPAMASSASSIN_PORT, DEFAULT_PORT));
    }

    private static Optional<Boolean> isAsynchronous(PropertiesConfiguration propertiesReader) {
        return Optional.ofNullable(propertiesReader.getBoolean(SPAMASSASSIN_ASYNCHRONOUS, null));
    }

    private static Optional<Integer> getThreadCount(PropertiesConfiguration propertiesReader) {
        return Optional.ofNullable(propertiesReader.getInteger(SPAMASSASSIN_THREAD_COUNT, null));
    }
}
