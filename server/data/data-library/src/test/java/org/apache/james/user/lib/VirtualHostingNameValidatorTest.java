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

package org.apache.james.user.lib;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.user.api.UsersRepositoryException;
import org.junit.Before;
import org.junit.Test;

public class VirtualHostingNameValidatorTest {

    public static final boolean SUPPORTS_VIRTUAL_HOSTING = true;
    private DomainList domainList;

    @Before
    public void setUp() throws DomainListException {
        domainList = mock(DomainList.class);
        when(domainList.containsDomain(anyString())).thenReturn(true);
    }

    @Test
    public void validateShouldThrowOnNull() throws Exception {
        UsernameValidator usernameValidator = new VirtualHostingNameValidator(SUPPORTS_VIRTUAL_HOSTING, domainList);

        assertThatThrownBy(() -> usernameValidator.validate(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void validateShouldThrowOnEmptyWhenVirtualHosting() throws Exception {
        UsernameValidator usernameValidator = new VirtualHostingNameValidator(SUPPORTS_VIRTUAL_HOSTING, domainList);

        assertThatThrownBy(() -> usernameValidator.validate(""))
            .isInstanceOf(UsersRepositoryException.class);
    }

    @Test
    public void validateShouldThrowOnMissingLocalPartWhenVirtualHosting() throws Exception {
        UsernameValidator usernameValidator = new VirtualHostingNameValidator(SUPPORTS_VIRTUAL_HOSTING, domainList);

        assertThatThrownBy(() -> usernameValidator.validate("@domain.com"))
            .isInstanceOf(UsersRepositoryException.class);
    }

    @Test
    public void validateShouldThrowOnMissingDomainPartWhenVirtualHosting() throws Exception {
        UsernameValidator usernameValidator = new VirtualHostingNameValidator(SUPPORTS_VIRTUAL_HOSTING, domainList);

        assertThatThrownBy(() -> usernameValidator.validate("any"))
            .isInstanceOf(UsersRepositoryException.class);
    }

    @Test
    public void validateShouldAcceptInternetAddressWhenVirtualHosting() throws Exception {
        UsernameValidator usernameValidator = new VirtualHostingNameValidator(SUPPORTS_VIRTUAL_HOSTING, domainList);

        usernameValidator.validate("any@domain.com");
    }

    @Test
    public void validateShouldAcceptMissingDomainPartWhenVirtualHosting() throws Exception {
        UsernameValidator usernameValidator = new VirtualHostingNameValidator(SUPPORTS_VIRTUAL_HOSTING, domainList);

        usernameValidator.validate("any@d");
    }

    @Test
    public void validateShouldThrowOnEmptyWhenNoVirtualHosting() throws Exception {
        UsernameValidator usernameValidator = new VirtualHostingNameValidator(!SUPPORTS_VIRTUAL_HOSTING, domainList);

        assertThatThrownBy(() -> usernameValidator.validate(""))
            .isInstanceOf(UsersRepositoryException.class);
    }

    @Test
    public void validateShouldThrowOnInternetAddressWhenNoVirtualHosting() throws Exception {
        UsernameValidator usernameValidator = new VirtualHostingNameValidator(!SUPPORTS_VIRTUAL_HOSTING, domainList);

        assertThatThrownBy(() -> usernameValidator.validate("any@domain.com"))
            .isInstanceOf(UsersRepositoryException.class);
    }

    @Test
    public void validateShouldAcceptSimpleNamesWhenNoVirtualHosting() throws Exception {
        UsernameValidator usernameValidator = new VirtualHostingNameValidator(!SUPPORTS_VIRTUAL_HOSTING, domainList);

        usernameValidator.validate("any");
    }

}