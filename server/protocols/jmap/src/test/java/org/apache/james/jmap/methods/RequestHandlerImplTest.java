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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.james.jmap.model.ProtocolRequest;
import org.apache.james.jmap.model.ProtocolResponse;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

public class RequestHandlerImplTest {

    public static class MyJmapRequest implements JmapRequest {

        public String id;
        public String name;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class MyJmapResponse implements JmapResponse {

        private final String id;
        private final String name;
        private final String message;

        public MyJmapResponse(String id, String name, String message) {
            this.id = id;
            this.name = name;
            this.message = message;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class MyMethod implements Method {

        private ProtocolArgumentsManager protocolArgumentsManager;

        @Inject
        @VisibleForTesting MyMethod(ProtocolArgumentsManager protocolArgumentsManager) {
            this.protocolArgumentsManager = protocolArgumentsManager;
        }

        @Override
        public String methodName() {
            return "myMethod";
        }

        @Override
        public ProtocolResponse process(ProtocolRequest request) {
            try {
                MyJmapRequest typedRequest = protocolArgumentsManager.extractJmapRequest(request, MyJmapRequest.class);
                return protocolArgumentsManager.formatMethodResponse(request, 
                        new MyJmapResponse(typedRequest.getId(), typedRequest.getName(), "works"));
            } catch (IOException e) {
                return protocolArgumentsManager.formatErrorResponse(request);
            }
        }
    }

    private RequestHandler testee;
    private ProtocolArgumentsManager protocolArgumentsManager;

    @Before
    public void setup() {
        protocolArgumentsManager = new ProtocolArgumentsManagerImpl();
        testee = new RequestHandlerImpl(ImmutableSet.of(new MyMethod(protocolArgumentsManager)));
    }


    @Test(expected=IllegalStateException.class)
    public void processShouldThrowWhenUnknownMethod() {
        JsonNode[] nodes = new JsonNode[] { new ObjectNode(new JsonNodeFactory(false)).textNode("unknwonMethod"),
                new ObjectNode(new JsonNodeFactory(false)).putObject("{\"id\": \"id\"}"),
                new ObjectNode(new JsonNodeFactory(false)).textNode("#1")} ;

        RequestHandlerImpl requestHandlerImpl = new RequestHandlerImpl(ImmutableSet.of());
        requestHandlerImpl.handle(ProtocolRequest.fromProtocolSpecification(nodes));
    }

    @Test(expected=IllegalStateException.class)
    public void requestHandlerShouldThrowWhenAMethodIsRecordedTwice() {
        new RequestHandlerImpl(ImmutableSet.of(new MyMethod(protocolArgumentsManager), new MyMethod(protocolArgumentsManager)));
    }

    @Test(expected=IllegalStateException.class)
    public void requestHandlerShouldThrowWhenTwoMethodsWithSameName() {
        new RequestHandlerImpl(ImmutableSet.of(new NamedMethod("name"), new NamedMethod("name")));
    }

    @Test
    public void requestHandlerMayBeCreatedWhenTwoMethodsWithDifferentName() {
        new RequestHandlerImpl(ImmutableSet.of(new NamedMethod("name"), new NamedMethod("name2")));
    }

    private class NamedMethod implements Method {

        private final String methodName;

        public NamedMethod(String methodName) {
            this.methodName = methodName;
            
        }

        @Override
        public String methodName() {
            return methodName;
        }

        @Override
        public ProtocolResponse process(ProtocolRequest request) {
            return null;
        }
    }

    @Test
    public void processShouldWorkWhenKnownMethod() {
        ObjectNode parameters = new ObjectNode(new JsonNodeFactory(false));
        parameters.put("id", "myId");
        parameters.put("name", "myName");
        
        JsonNode[] nodes = new JsonNode[] { new ObjectNode(new JsonNodeFactory(false)).textNode("myMethod"),
                parameters,
                new ObjectNode(new JsonNodeFactory(false)).textNode("#1")} ;

        ProtocolResponse response = testee.handle(ProtocolRequest.fromProtocolSpecification(nodes));

        assertThat(response.getResults().findValue("id").asText()).isEqualTo("myId");
        assertThat(response.getResults().findValue("name").asText()).isEqualTo("myName");
        assertThat(response.getResults().findValue("message").asText()).isEqualTo("works");
    }
}
