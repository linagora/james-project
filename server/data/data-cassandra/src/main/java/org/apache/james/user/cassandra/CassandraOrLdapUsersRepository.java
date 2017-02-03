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

package org.apache.james.user.cassandra;

import java.util.Iterator;

import javax.inject.Inject;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.api.model.User;
import org.apache.james.user.ldap.ReadOnlyUsersLDAPRepository;
import org.apache.mailet.MailAddress;
import org.slf4j.Logger;

public class CassandraOrLdapUsersRepository implements UsersRepository {

    private final CassandraUsersRepository cassandraUsersRepository;
    private final ReadOnlyUsersLDAPRepository ldapRepository;

    private Logger log;
    private UsersRepository realImplementation;

    @Inject
    private CassandraOrLdapUsersRepository(CassandraUsersRepository cassandraUsersRepository, ReadOnlyUsersLDAPRepository ldapRepository) {
        this.cassandraUsersRepository = cassandraUsersRepository;
        this.ldapRepository = ldapRepository;
    }

    @Override
    public void setLog(Logger log) {
        this.log = log;
    }

    @Override
    public void configure(HierarchicalConfiguration configuration) throws ConfigurationException {
        if (isLdapConfigured(configuration)) {
            realImplementation = ldapRepository;
        } else {
            realImplementation = cassandraUsersRepository;
        }
        realImplementation.setLog(log);
        realImplementation.configure(configuration);
    }

    @Override
    public void init() throws Exception {
        realImplementation.init();
    }

    private boolean isLdapConfigured(HierarchicalConfiguration configuration) {
        String usersRepositoryClass = configuration.getString("[@class]", "");
        return usersRepositoryClass.equals(ReadOnlyUsersLDAPRepository.class);
    }


    @Override
    public void addUser(String username, String password) throws UsersRepositoryException {
        realImplementation.addUser(username, password);
    }

    @Override
    public User getUserByName(String name) throws UsersRepositoryException {
        return realImplementation.getUserByName(name);
    }

    @Override
    public void updateUser(User user) throws UsersRepositoryException {
        realImplementation.updateUser(user);
    }

    @Override
    public void removeUser(String name) throws UsersRepositoryException {
        realImplementation.removeUser(name);
    }

    @Override
    public boolean contains(String name) throws UsersRepositoryException {
        return realImplementation.contains(name);
    }

    @Override
    public boolean test(String name, String password) throws UsersRepositoryException {
        return realImplementation.test(name, password);
    }

    @Override
    public int countUsers() throws UsersRepositoryException {
        return realImplementation.countUsers();
    }

    @Override
    public Iterator<String> list() throws UsersRepositoryException {
        return realImplementation.list();
    }

    @Override
    public boolean supportVirtualHosting() throws UsersRepositoryException {
        return realImplementation.supportVirtualHosting();
    }

    @Override
    public String getUser(MailAddress mailAddress) throws UsersRepositoryException {
        return realImplementation.getUser(mailAddress);
    }

}
