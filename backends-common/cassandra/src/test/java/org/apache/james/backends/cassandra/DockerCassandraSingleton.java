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

package org.apache.james.backends.cassandra;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.util.Host;

public class DockerCassandraSingleton {
    public static DockerCassandra singleton = new DockerCassandra();

    private static final int maxItemCount = 1000;
    private static final AtomicInteger pastCreatedItemCount = new AtomicInteger(0);

    static {
        singleton.start();
    }

    public static Host getManagedHost(CassandraModule module) {
        int itemCount = module.moduleTables().size() + module.moduleTypes().size();

        if (tooManyCassandraItemsCreated(itemCount)) {
            reinitItemCount(itemCount);
            restart();
        }
        return singleton.getHost();
    }

    private static void reinitItemCount(int itemCount) {
        pastCreatedItemCount.set(itemCount);
    }

    private static boolean tooManyCassandraItemsCreated(int itemCount) {
        return pastCreatedItemCount.addAndGet(itemCount) > maxItemCount;
    }

    public static void restart() {
        singleton.stop();
        singleton.start();
    }

    // Cleanup will be performed by test container resource reaper
}
