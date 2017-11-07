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

import java.util.Optional;

import org.apache.james.core.User;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.user.api.UsersRepositoryException;

public class VirtualHostingNameValidator implements UsernameValidator {

    private final boolean supportsVirtualHosting;
    private final DomainList domainList;

    public VirtualHostingNameValidator(boolean supportsVirtualHosting, DomainList domainList) {
        this.supportsVirtualHosting = supportsVirtualHosting;
        this.domainList = domainList;
    }

    public void validate(String username) throws UsersRepositoryException {
        Optional<String> domain = User.fromUsername(username).getDomainPart();

        if (supportsVirtualHosting) {
            validateUserDomain(domain.orElseThrow(() ->
                new UsersRepositoryException("Given Username needs to contain a @domainpart")));
        } else {
            // @ only allowed when virtualhosting is supported
            if (domain.isPresent()) {
                throw new UsersRepositoryException("Given Username contains a @domainpart but virtualhosting support is disabled");
            }
        }
    }

    private void validateUserDomain(String domain) throws UsersRepositoryException {
        try {
            if (!domainList.containsDomain(domain)) {
                throw new UsersRepositoryException("Domain does not exist in DomainList");
            }
        } catch (DomainListException e) {
            throw new UsersRepositoryException("Unable to query DomainList", e);
        }
    }
}
