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

package org.apache.james.jmap.methods;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.james.jmap.model.ProtocolRequest;
import org.apache.james.jmap.model.ProtocolResponse;

@Singleton
public class RequestHandlerImpl implements RequestHandler {

    private final Map<String, Method> methods;

    @Inject
    public RequestHandlerImpl(Set<Method> methods) {
        this.methods = methods.stream()
                .collect(Collectors.toMap(Method::methodName, method -> method));
    }

    @Override
    public ProtocolResponse handle(ProtocolRequest request) {
        String methodName = request.getMethod();
        if (!methods.containsKey(methodName)) {
            throw new IllegalStateException("unknown method");
        }

        return methods.get(methodName).process(request);
    }
}
