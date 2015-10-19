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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.apache.james.jmap.model.Protocol;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class JMAPServletTest {

    @Test(expected=IllegalStateException.class)
    public void deserializedRequestsShouldThrowWhenNotEnoughElements() throws Exception {
        new JMAPServlet().deserialize("[[\"getAccounts\", {}]]");
    }

    @Test(expected=IllegalStateException.class)
    public void deserializedRequestsShouldThrowWhenTooMuchElements() throws Exception {
        new JMAPServlet().deserialize("[[\"getAccounts\", {}, \"#0\", \"tooMuch\"]]");
    }

    @Test
    public void deserializedRequestsShouldReturnAListOfObjects() throws JsonParseException, JsonMappingException, IOException {
        assertThat(new JMAPServlet().deserialize("[[\"getAccounts\", {}, \"#0\"]]"))
            .isInstanceOf(List.class);
    }

    @Test(expected=IllegalStateException.class)
    public void deserializedRequestsShouldThrowWhenFirstParameterIsNotString() throws JsonParseException, JsonMappingException, IOException {
        new JMAPServlet().deserialize("[[true, {}, \"#0\"]]");
    }

    @Test(expected=IllegalStateException.class)
    public void deserializedRequestsShouldThrowWhenSecondParameterIsNotJson() throws JsonParseException, JsonMappingException, IOException {
        new JMAPServlet().deserialize("[[\"getAccounts\", true, \"#0\"]]");
    }

    @Test(expected=IllegalStateException.class)
    public void deserializedRequestsShouldThrowWhenThirdParameterIsNotString() throws JsonParseException, JsonMappingException, IOException {
        new JMAPServlet().deserialize("[[\"getAccounts\", {}, true]]");
    }

    @Test
    public void deserializedRequestsShouldWorkWhenSingleRequest() throws JsonParseException, JsonMappingException, IOException {
        List<Protocol> requests = new JMAPServlet().deserialize("[[\"getAccounts\", {}, \"#0\"]]");
        
        Protocol request = requests.stream().findFirst().get();
        assertThat(request.getMethod()).isEqualTo("getAccounts");
        assertThat(request.getJson()).isNotNull();
        assertThat(request.getClientId()).isEqualTo("#0");
    }

    @Test
    public void deserializedRequestsShouldWorkWhenDoubleRequest() throws JsonParseException, JsonMappingException, IOException {
        List<Protocol> requests = new JMAPServlet().deserialize("[[\"getAccounts\", {}, \"#1\"],[\"getMailBoxes\", {\"id\": \"id\"}, \"#2\"]]");
        
        assertThat(requests).hasSize(2);
        Protocol request1 = requests.get(0);
        assertThat(request1.getMethod()).isEqualTo("getAccounts");
        assertThat(request1.getJson()).isNotNull();
        assertThat(request1.getClientId()).isEqualTo("#1");
        Protocol request2 = requests.get(1);
        assertThat(request2.getMethod()).isEqualTo("getMailBoxes");
        assertThat(request2.getJson()).isNotNull();
        assertThat(request2.getClientId()).isEqualTo("#2");
    }
}
