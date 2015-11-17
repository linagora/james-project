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

package org.apache.james.jmap;

import org.apache.james.jmap.methods.ProtocolArgumentsManager;
import org.apache.james.jmap.methods.ProtocolArgumentsManagerImpl;
import org.apache.james.jmap.methods.GetMailboxesMethod;
import org.apache.james.jmap.methods.Method;
import org.apache.james.jmap.methods.RequestHandler;
import org.apache.james.jmap.methods.RequestHandlerImpl;
import org.apache.james.mailbox.cassandra.CassandraId;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

public class MethodsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RequestHandler.class).to(RequestHandlerImpl.class);
        bind(ProtocolArgumentsManager.class).to(ProtocolArgumentsManagerImpl.class);

        Multibinder<Method> methods = Multibinder.newSetBinder(binder(), Method.class);
        methods.addBinding().to(new TypeLiteral<GetMailboxesMethod<CassandraId>>(){});
    }

}
