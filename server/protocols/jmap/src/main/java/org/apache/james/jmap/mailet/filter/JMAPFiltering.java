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

package org.apache.james.jmap.mailet.filter;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.core.MailAddress;
import org.apache.james.core.User;
import org.apache.james.jmap.api.filtering.FilteringManagement;
import org.apache.james.jmap.api.filtering.Rule;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;

public class JMAPFiltering extends GenericMailet {

    private final FilteringManagement filteringManagement;
    private final UsersRepository usersRepository;
    private final ActionApplier.Factory actionApplierFactory;

    @Inject
    public JMAPFiltering(FilteringManagement filteringManagement,
                         UsersRepository usersRepository, ActionApplier.Factory actionApplierFactory) {

        this.filteringManagement = filteringManagement;
        this.usersRepository = usersRepository;
        this.actionApplierFactory = actionApplierFactory;
    }

    @Override
    public void service(Mail mail) {
        mail.getRecipients()
            .forEach(recipient -> filteringForRecipient(mail, recipient));
    }

    private void filteringForRecipient(Mail mail, MailAddress recipient) {
        User user = retrieveUser(recipient);
        List<Rule> filteringRules = filteringManagement.listRulesForUser(user);
        FilteringModel filteringModel = new FilteringModel(filteringRules);
        Optional<Rule.Action> maybeAction = filteringModel.computeAction(mail);

        maybeAction.ifPresent(action -> actionApplierFactory.forMail(mail)
                .forUser(user)
                .apply(action));
    }

    private User retrieveUser(MailAddress recipient) {
        try {
            return User.fromUsername(usersRepository.getUser(recipient));
        } catch (UsersRepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
