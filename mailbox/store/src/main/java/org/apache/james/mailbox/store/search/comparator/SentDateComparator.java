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
package org.apache.james.mailbox.store.search.comparator;

import java.io.IOException;
import java.util.Comparator;
import java.util.Date;

import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mime4j.dom.Message;

/**
 * {@link Comparator} which works like stated in RFC5256 2.2 Sent Date
 *
 */
public class SentDateComparator extends AbstractHeaderComparator {
    public final static Comparator<MailboxMessage> SENTDATE = new SentDateComparator();

    private SentDateComparator() {
        super();
    }

    @Override
    public int compare(MailboxMessage message1, MailboxMessage message2) {
        Date date1 = getSentDateOrInternalDate(message1);
        Date date2 = getSentDateOrInternalDate(message2);
        int diff = date1.compareTo(date2);
        
        // sent date was the same so use the uid as tie-breaker
        if (diff == 0) {
            return UidComparator.UID.compare(message1, message2);
        }

        return diff;
    }
    
    private Date getSentDateOrInternalDate(MailboxMessage message) {
        try {
            Message mime4jMessage = Message.Builder.of(message.getFullContent()).build();
            Date date = mime4jMessage.getDate();

            if (date == null) {
                date = message.getInternalDate();
            }

            return date;
        } catch (IOException e) {
            return message.getInternalDate();
        }
    }
}
