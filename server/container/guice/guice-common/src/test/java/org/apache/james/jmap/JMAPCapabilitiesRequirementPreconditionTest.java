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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import org.apache.james.mailbox.MailboxManager;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class JMAPCapabilitiesRequirementPreconditionTest {
    @Test
    public void initModuleShouldThrowWhenMissingCapability() {
        // Given
        MailboxManager fakeMailboxManagerWithMissingCapabilities = mock(MailboxManager.class);
        when(fakeMailboxManagerWithMissingCapabilities.getSupportedCapabilities()).thenReturn(ImmutableList.of(MailboxManager.Capabilities.Basic, MailboxManager.Capabilities.UserFlags));
        // When
        JMAPModule.JMAPCapabilitiesRequirementPrecondition sut = new JMAPModule.JMAPCapabilitiesRequirementPrecondition(fakeMailboxManagerWithMissingCapabilities);
        // Then
        assertThatThrownBy(() -> sut.initModule()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void initModuleShouldNotThrow() throws Exception {
        MailboxManager fakeMailboxManagerWithMissingCapabilities = mock(MailboxManager.class);
        when(fakeMailboxManagerWithMissingCapabilities.getSupportedCapabilities()).thenReturn(Arrays.asList(MailboxManager.Capabilities.values()));
        JMAPModule.JMAPCapabilitiesRequirementPrecondition sut = new JMAPModule.JMAPCapabilitiesRequirementPrecondition(fakeMailboxManagerWithMissingCapabilities);
        sut.initModule();
    }
}