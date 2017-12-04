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

package org.apache.james.util.streams;

import java.net.Socket;

import javax.net.SocketFactory;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;

public class FakeSmtp extends SwarmGenericContainer {
    public FakeSmtp() {
        super("weave/rest-smtp-sink:latest");
    }

    public static void await(SwarmGenericContainer container) {
        Duration slowPacedPollInterval = com.jayway.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS;
        ConditionFactory calmlyAwait = Awaitility.with()
            .pollInterval(slowPacedPollInterval)
            .and()
            .with()
            .pollDelay(slowPacedPollInterval)
            .await();

        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> {
            try {
                Socket socket = SocketFactory.getDefault().createSocket(container.getContainerIp(), 25);
                socket.close();
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }
}
