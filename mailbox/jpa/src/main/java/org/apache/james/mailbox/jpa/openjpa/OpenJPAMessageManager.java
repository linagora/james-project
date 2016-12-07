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

package org.apache.james.mailbox.jpa.openjpa;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxPathLocker;
import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.jpa.JPAMessageManager;
import org.apache.james.mailbox.jpa.mail.model.JPAMailbox;
import org.apache.james.mailbox.jpa.mail.model.openjpa.JPAEncryptedMailboxMessage;
import org.apache.james.mailbox.jpa.mail.model.openjpa.JPAStreamingMailboxMessage;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.quota.QuotaManager;
import org.apache.james.mailbox.quota.QuotaRootResolver;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.event.MailboxEventDispatcher;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;
import org.apache.james.mailbox.store.search.MessageSearchIndex;

/**
 * OpenJPA implementation of Mailbox
 */
public class OpenJPAMessageManager extends JPAMessageManager {

    private final AdvancedFeature feature;

    public enum AdvancedFeature {
        None,
        Streaming,
        Encryption
    }
    
    public OpenJPAMessageManager(MailboxSessionMapperFactory mapperFactory, 
    		MessageSearchIndex index,MailboxEventDispatcher dispatcher, 
    		MailboxPathLocker locker, Mailbox mailbox, MailboxACLResolver aclResolver, 
    		GroupMembershipResolver groupMembershipResolver,
            QuotaManager quotaManager, QuotaRootResolver quotaRootResolver, MessageParser messageParser,
            MessageId.Factory messageIdFactory) throws MailboxException {
        this(mapperFactory, index, dispatcher, locker,  mailbox, AdvancedFeature.None, aclResolver, 
                groupMembershipResolver, quotaManager, quotaRootResolver, messageParser, messageIdFactory);
    }

    public OpenJPAMessageManager(MailboxSessionMapperFactory mapperFactory, 
    		MessageSearchIndex index, MailboxEventDispatcher dispatcher, 
    		MailboxPathLocker locker, Mailbox mailbox, AdvancedFeature f, 
    		MailboxACLResolver aclResolver, GroupMembershipResolver groupMembershipResolver,
            QuotaManager quotaManager, QuotaRootResolver quotaRootResolver, MessageParser messageParser,
            MessageId.Factory messageIdFactory) throws MailboxException {
    	
        super(mapperFactory,  index, dispatcher, locker, mailbox, aclResolver, groupMembershipResolver, quotaManager, quotaRootResolver, messageParser, messageIdFactory);
        this.feature = f;
    }

    @Override
    protected MailboxMessage createMailboxMessage(Message message, Flags flags) throws MailboxException {
        switch (feature) {
            case Streaming:
                return new JPAStreamingMailboxMessage((JPAMailbox) getMailboxEntity(), message, flags);
            case Encryption:
                return new JPAEncryptedMailboxMessage((JPAMailbox) getMailboxEntity(), message, flags);
            default:
                return super.createMailboxMessage(message, flags);
        }
       
    }

    

}
