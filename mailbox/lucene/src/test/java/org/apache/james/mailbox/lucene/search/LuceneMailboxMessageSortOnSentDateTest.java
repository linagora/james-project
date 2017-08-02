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
package org.apache.james.mailbox.lucene.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.SearchQuery;
import org.apache.james.mailbox.model.SearchQuery.Sort.SortClause;
import org.apache.james.mailbox.model.SimpleMailboxACL;
import org.apache.james.mailbox.model.TestId;
import org.apache.james.mailbox.model.TestMessageId;
import org.apache.james.mailbox.store.SimpleMailboxMembership;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Before;
import org.junit.Test;

public class LuceneMailboxMessageSortOnSentDateTest {

    public static final long LIMIT = 100L;
    private LuceneMessageSearchIndex index;
    
    private SimpleMailbox mailbox = new SimpleMailbox(0);
    private MailboxSession session;

    private MessageUid uid1;
    private MessageUid uid2;
    private MessageId id1;
    private MessageId id2;

    private Date smallestDate;
    private Date biggestDate;

    protected boolean useLenient() {
        return true;
    }

    @Before
    public void setUp() throws Exception {
        session = new MockMailboxSession("username");
        TestMessageId.Factory factory = new TestMessageId.Factory();

        index = new LuceneMessageSearchIndex(null, new TestId.Factory(), new RAMDirectory(), true, useLenient(), factory);
        index.setEnableSuffixMatch(true);

        uid1 = MessageUid.of(1);
        uid2 = MessageUid.of(2);

        id1 = factory.generate();
        id2 = factory.generate();

        GregorianCalendar cal = new GregorianCalendar();

        cal.set(2017, 0, 10);
        smallestDate = cal.getTime();

        cal.set(2017, 0, 12);
        biggestDate = cal.getTime();
    }

    private void indexMailWithSentDateAndInternalDate(MessageId id, MessageUid uid, Date internalDate, String sentDate) throws Exception {
        Map<String, String> header = new HashMap<String, String>();
        if (sentDate != null) {
            header.put("Date", sentDate);
        }

        SimpleMailboxMembership membership = new SimpleMailboxMembership(
            id,
            mailbox.getMailboxId(),
            uid,
            0,
            internalDate,
            20,
            new Flags(Flag.DELETED),
            "My Otherbody2".getBytes(),
            header);
        index.add(session, mailbox, membership);
    }

    @Test
    public void sortOnSentDateShouldSortUsingSendDateIfPresent() throws Exception {
        indexMailWithSentDateAndInternalDate(id1, uid1, biggestDate, "Date: Fri, 2 Jun 2017 10:37:47 -0400");
        indexMailWithSentDateAndInternalDate(id2, uid2, smallestDate, "Date: Fri, 2 Jun 2017 11:37:47 -0400");

        SearchQuery query = new SearchQuery();
        query.andCriteria(SearchQuery.all());
        query.setSorts(Arrays.asList(new SearchQuery.Sort(SortClause.SentDate, false)));
        Iterator<MessageUid> result = index.search(session, mailbox, query);

        assertThat(result).containsExactly(uid1, uid2);
    }

    @Test
    public void sortOnSentDateShouldSortUsingInternalDateIfNoSentDate() throws Exception {
        indexMailWithSentDateAndInternalDate(id1, uid1, smallestDate, null);
        indexMailWithSentDateAndInternalDate(id2, uid2, biggestDate, null);

        SearchQuery query = new SearchQuery();
        query.andCriteria(SearchQuery.all());
        query.setSorts(Arrays.asList(new SearchQuery.Sort(SortClause.SentDate, false)));
        Iterator<MessageUid> result = index.search(session, mailbox, query);

        assertThat(result).containsExactly(uid1, uid2);
    }

    @Test
    public void sortOnSentDateShouldCompareInternalDateAgainstSentDateIfOneOfTheMessageHasNoSentDate() throws Exception {
        indexMailWithSentDateAndInternalDate(id1, uid1, biggestDate, null);
        indexMailWithSentDateAndInternalDate(id2, uid2, smallestDate, "Date: Fri, 2 Jun 2017 11:37:47 -0400");

        SearchQuery query = new SearchQuery();
        query.andCriteria(SearchQuery.all());
        query.setSorts(Arrays.asList(new SearchQuery.Sort(SortClause.SentDate, false)));
        Iterator<MessageUid> result = index.search(session, mailbox, query);

        assertThat(result).containsExactly(uid1, uid2);
    }

    private final class SimpleMailbox implements Mailbox {
        private final TestId id;

        public SimpleMailbox(long id) {
        	this.id = TestId.of(id);
        }

        public void setMailboxId(MailboxId id) {
        }

        @Override
        public MailboxPath generateAssociatedPath() {
            return new MailboxPath(getNamespace(), getUser(), getName());
        }

        public TestId getMailboxId() {
            return id;
        }

        public String getNamespace() {
            throw new UnsupportedOperationException("Not supported");
        }

        public void setNamespace(String namespace) {
            throw new UnsupportedOperationException("Not supported");
        }

        public String getUser() {
            throw new UnsupportedOperationException("Not supported");
        }

        public void setUser(String user) {
            throw new UnsupportedOperationException("Not supported");
        }

        public String getName() {
            return id.serialize();
        }

        public void setName(String name) {
            throw new UnsupportedOperationException("Not supported");

        }

        public long getUidValidity() {
            return 0;
        }

        @Override
        public MailboxACL getACL() {
            return SimpleMailboxACL.OWNER_FULL_ACL;
        }

        @Override
        public void setACL(MailboxACL acl) {
            throw new UnsupportedOperationException("Not supported");
        }


        @Override
        public boolean isChildOf(Mailbox potentialParent, MailboxSession mailboxSession) {
            throw new UnsupportedOperationException("Not supported");
        }
    }
}
