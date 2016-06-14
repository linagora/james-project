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

import java.net.InetAddress;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.containers.GenericContainer;

import com.google.common.net.InetAddresses;

public class ForwardSmtpTest {

    private final TemporaryFolder folder = new TemporaryFolder();
    private final GenericContainer fakeSmtp = new GenericContainer("weave/rest-smtp-sink:latest");
    
    @Rule
    public final RuleChain chain = RuleChain.outerRule(folder).around(fakeSmtp);

    @Test
    public void forwardingAnEmailShouldWork() throws Exception {
        InetAddress containerIp = InetAddresses.forString(fakeSmtp.getContainerInfo().getNetworkSettings().getIpAddress());
        System.out.println("containerIp: " + containerIp);
    }
}
