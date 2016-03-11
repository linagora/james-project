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

package org.apache.james.jmap.memory;

import org.apache.james.jmap.JmapServer;
import org.apache.james.jmap.methods.SetMessagesMethodTest;
import org.apache.james.jmap.servers.MemoryJmapServer;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

public class MemorySetMessagesMethodTest extends SetMessagesMethodTest {

    private TemporaryFolder temporaryFolder = new TemporaryFolder();
    private JmapServer jmapServer = new MemoryJmapServer(MemoryJmapServer.defaultOverrideModule(temporaryFolder));

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(temporaryFolder).around(jmapServer);

    @Override
    protected JmapServer getJmapServer() {
        return jmapServer;
    }

    @Override
    protected void await() {

    }
}
