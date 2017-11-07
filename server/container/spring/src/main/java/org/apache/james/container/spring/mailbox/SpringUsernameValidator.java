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

package org.apache.james.container.spring.mailbox;

import org.apache.james.core.User;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.lib.UsernameValidator;

public class SpringUsernameValidator implements UsernameValidator {
    public static final String DELIMITER_AS_STRING = String.valueOf(MailboxConstants.DEFAULT_DELIMITER);

    @Override
    public void validate(String username) throws UsersRepositoryException {
        String localPart = User.fromUsername(username).getLocalPart();

        if (localPart.contains(DELIMITER_AS_STRING)) {
            throw new UsersRepositoryException("User name local part can not contain '" + DELIMITER_AS_STRING + "'");
        }
    }
}
