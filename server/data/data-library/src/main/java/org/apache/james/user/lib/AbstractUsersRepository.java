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
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.core.MailAddress;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.user.api.AlreadyExistInUsersRepositoryException;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.util.OptionalUtils;

import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

public abstract class AbstractUsersRepository implements UsersRepository, Configurable {
    public static final String ENABLE_VIRTUAL_HOSTING = "enableVirtualHosting";
    public static final String ADMINISTRATOR_ID = "administratorId";
    private boolean virtualHosting;
    private Optional<String> administratorId;
    private DomainList domainList;
    protected UsernameValidator usernameValidator = new UsernameValidatorAggregator(ImmutableSet.of());
    protected UsernameValidator additionalUsernameValidator;
    protected VirtualHostingNameValidator virtualHostingNameValidator;

    /**
     * @see
     * org.apache.james.lifecycle.api.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration configuration) throws ConfigurationException {
        virtualHosting = configuration.getBoolean(ENABLE_VIRTUAL_HOSTING, getDefaultVirtualHostingValue());
        administratorId = Optional.ofNullable(configuration.getString(ADMINISTRATOR_ID));
        doConfigure(configuration);
        this.virtualHostingNameValidator = new VirtualHostingNameValidator(virtualHosting, domainList);
        generateUserNameValidator();
    }

    protected boolean getDefaultVirtualHostingValue() {
        return false;
    }

    protected void doConfigure(HierarchicalConfiguration config) throws ConfigurationException {
    }

    public void setEnableVirtualHosting(boolean virtualHosting) {
        this.virtualHosting = virtualHosting;
    }

    @Inject
    public void setDomainList(DomainList domainList) {
        this.domainList = domainList;
    }

    @Inject
    public void setAdditionalUsernameValidator(UsernameValidator additionalUsernameValidator) {
        this.additionalUsernameValidator = additionalUsernameValidator;
        generateUserNameValidator();
    }

    private void generateUserNameValidator() {
        this.usernameValidator = new UsernameValidatorAggregator(
            Stream.concat(
                OptionalUtils.toStream(Optional.ofNullable(additionalUsernameValidator)),
                OptionalUtils.toStream(Optional.ofNullable(virtualHostingNameValidator)))
                .collect(Guavate.toImmutableSet()));
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#addUser(java.lang.String,
     * java.lang.String)
     */
    public void addUser(String username, String password) throws UsersRepositoryException {

        if (!contains(username)) {
            usernameValidator.validate(username);
            doAddUser(username, password);
        } else {
            throw new AlreadyExistInUsersRepositoryException("User with username " + username + " already exists!");
        }

    }

    /**
     * @see org.apache.james.user.api.UsersRepository#supportVirtualHosting()
     */
    public boolean supportVirtualHosting() {
        return virtualHosting;
    }

    /**
     * Add the user with the given username and password
     * 
     * @param username
     * @param password
     * @throws UsersRepositoryException
     *           If an error occurred
     */
    protected abstract void doAddUser(String username, String password) throws UsersRepositoryException;

    @Override
    public String getUser(MailAddress mailAddress) throws UsersRepositoryException {
        if (supportVirtualHosting()) {
            return mailAddress.asString();
        } else {
            return mailAddress.getLocalPart();
        }
    }

    @VisibleForTesting void setAdministratorId(Optional<String> username) {
        this.administratorId = username;
    }

    @Override
    public boolean isAdministrator(String username) throws UsersRepositoryException {
        if (administratorId.isPresent()) {
            return administratorId.get().equals(username);
        }
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }
}
