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

package org.apache.james.mailrepository.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class MailRepositoryUrlTest {
    @Test
    public void shouldMatchBeanContract() {
        EqualsVerifier.forClass(MailRepositoryUrl.class)
            .verify();
    }

    @Test
    public void constructorShouldThrowWhenNull() {
        assertThatThrownBy(() -> new MailRepositoryUrl(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void constructorShouldThrowWhenNoSeparator() {
        assertThatThrownBy(() -> new MailRepositoryUrl("invalid"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void getProtocolShouldReturnValue() {
        assertThat(new MailRepositoryUrl("proto://abc").getProtocol())
            .isEqualTo(new Protocol("proto"));
    }

    @Test
    public void getProtocolShouldReturnValueWhenEmpty() {
        assertThat(new MailRepositoryUrl("://abc").getProtocol())
            .isEqualTo(new Protocol(""));
    }

    @Test
    public void fromEncodedShouldReturnDecodedValue() throws Exception {
        assertThat(MailRepositoryUrl.fromEncoded("url%3A%2F%2FmyRepo"))
            .isEqualTo(new MailRepositoryUrl("url://myRepo"));
    }

    @Test
    public void encodedValueShouldEncodeUnderlyingValue() throws Exception {
        assertThat(new MailRepositoryUrl("url://myRepo").encodedValue())
            .isEqualTo("url%3A%2F%2FmyRepo");
    }
}
