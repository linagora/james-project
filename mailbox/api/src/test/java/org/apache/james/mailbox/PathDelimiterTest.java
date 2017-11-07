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

package org.apache.james.mailbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.mailbox.model.MailboxConstants;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class PathDelimiterTest {

    @Test
    public void shouldMatchBeanContract() {
        EqualsVerifier.forClass(PathDelimiter.class)
            .verify();
    }

    @Test
    public void getSimpleNameShouldReturnMailboxNameWhenRootMailbox() throws Exception {
        String expected = "mailbox";
        String name = MailboxConstants.DEFAULT_DELIMITER.getSimpleName(expected);
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getSimpleNameShouldReturnMailboxNameWhenChildMailbox() throws Exception {
        String expected = "mailbox";
        String name = MailboxConstants.DEFAULT_DELIMITER.getSimpleName("inbox." + expected);
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getSimpleNameShouldReturnMailboxNameWhenChildOfChildMailbox() throws Exception {
        String expected = "mailbox";
        String name = MailboxConstants.DEFAULT_DELIMITER.getSimpleName("inbox.children." + expected);
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getSimpleNameShouldAcceptEmptyChildren() throws Exception {
        String expected = "";
        String name = MailboxConstants.DEFAULT_DELIMITER.getSimpleName("inbox.children." + expected);
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getSimpleNameShouldAcceptEmpty() throws Exception {
        String expected = "";
        String name = MailboxConstants.DEFAULT_DELIMITER.getSimpleName(expected);
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void splitShouldAcceptEmptyValue() {
        String mailboxName = "";
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .split(mailboxName))
            .containsOnly("");
    }

    @Test
    public void splitShouldHandleSeparatorOnly() {
        String mailboxName = ".";
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .split(mailboxName))
            .containsOnly("", "");
    }

    @Test
    public void splitShouldAcceptSimpleName() {
        String mailboxName = "name";
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .split(mailboxName))
            .containsOnly(mailboxName);
    }

    @Test
    public void splitShouldAcceptPath() {
        String mailboxName = "aa.bb";
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .split(mailboxName))
            .containsOnly("aa", "bb");
    }

    @Test
    public void splitShouldAcceptNamesStartingByDelimiter() {
        String mailboxName = ".aa";
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .split(mailboxName))
            .containsOnly("", "aa");
    }

    @Test
    public void splitShouldAcceptNamesFinishingByDelimiter() {
        String mailboxName = "aa.";
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .split(mailboxName))
            .containsOnly("aa", "");
    }

    @Test
    public void splitShouldAcceptNamesWithDoubleDelimiter() {
        String mailboxName = "aa..bb";
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .split(mailboxName))
            .containsOnly("aa", "", "bb");
    }

    @Test
    public void joinShouldReturnEmptyWhenNone() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join())
            .isEqualTo("");
    }

    @Test
    public void joinShouldReturnEmptyWhenOnlyEmpty() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join(""))
            .isEqualTo("");
    }

    @Test
    public void joinShouldReturnSimpleNameWhenOnlySimpleName() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join("aa"))
            .isEqualTo("aa");
    }

    @Test
    public void joinWithEmptyStringAsLastPartShouldAppendDelimiterAtTheEnd() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join("aa", ""))
            .isEqualTo("aa.");
    }

    @Test
    public void joinShouldReturnDelimiterWhenDoubleEmptyNames() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join("", ""))
            .isEqualTo(".");
    }

    @Test
    public void joinWithEmptyStringAsFirstPartShouldAppendDelimiterAtTheBeginning() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join("", "aa"))
            .isEqualTo(".aa");
    }

    @Test
    public void joinShouldReturnConcatenationWhendoubleNames() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join("aa", "bb"))
            .isEqualTo("aa.bb");
    }

    @Test
    public void joinShouldAllowDelimiterInParts() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join("a.a", "bb"))
            .isEqualTo("a.a.bb");
    }

    @Test
    public void containsPathDelimiterShouldReturnFalseWhenEmpty() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .containsPathDelimiter(""))
            .isFalse();
    }

    @Test
    public void containsPathDelimiterShouldReturnFalseWhenAbsent() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .containsPathDelimiter("name"))
            .isFalse();
    }

    @Test
    public void containsPathDelimiterShouldReturnTrueWhenPresent() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .containsPathDelimiter("na.me"))
            .isTrue();
    }

    @Test
    public void appendDelimiterShouldReturnDelimiterWhenEmpty() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .appendDelimiter(""))
            .isEqualTo(".");
    }

    @Test
    public void appendDelimiterShouldConcatenateValueAndDelimiter() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .appendDelimiter("aa"))
            .isEqualTo("aa.");
    }

    @Test
    public void appendDelimiterShouldAcceptValueSuffixedByDelimiter() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .appendDelimiter("aa."))
            .isEqualTo("aa..");
    }

    @Test
    public void removeTrailingSeparatorAtTheEndShouldAcceptEmpty() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparatorAtTheEnd(""))
            .isEqualTo("");
    }

    @Test
    public void removeTrailingSeparatorAtTheEndShouldDoNothingWhenNoSeparator() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparatorAtTheEnd("aa"))
            .isEqualTo("aa");
    }

    @Test
    public void removeTrailingSeparatorAtTheEndShouldDoNothingWhenMiddleSeparator() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparatorAtTheEnd("a.a"))
            .isEqualTo("a.a");
    }

    @Test
    public void removeTrailingSeparatorAtTheEndShouldDoNothingWhenBeginningSeparator() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparatorAtTheEnd(".aa"))
            .isEqualTo(".aa");
    }

    @Test
    public void removeTrailingSeparatorAtTheEndShouldReturnBaseNameWhenSeparatorAtTheEnd() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparatorAtTheEnd("aa."))
            .isEqualTo("aa");
    }

    @Test
    public void removeTrailingSeparatorAtTheEndShouldRemoveSeparatorOnlyOnce() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparatorAtTheEnd("aa.."))
            .isEqualTo("aa.");
    }

    @Test
    public void removeTrailingSeparatorAtTheBeginningShouldAcceptEmpty() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparatorAtTheBeginning(""))
            .isEqualTo("");
    }

    @Test
    public void removeTrailingSeparatorAtTheBeginningShouldDoNothingWhenNoSeparator() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparatorAtTheBeginning("aa"))
            .isEqualTo("aa");
    }

    @Test
    public void removeTrailingSeparatorAtTheBeginningShouldDoNothingWhenMiddleSeparator() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparatorAtTheBeginning("a.a"))
            .isEqualTo("a.a");
    }

    @Test
    public void removeTrailingSeparatorAtTheBeginningShouldReturnBaseNameWhenStartingByDelimiter() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparatorAtTheBeginning(".aa"))
            .isEqualTo("aa");
    }

    @Test
    public void removeTrailingSeparatorAtTheBeginningShouldRemoveSeparatorOnlyOnce() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparatorAtTheBeginning("..aa"))
            .isEqualTo(".aa");
    }

    @Test
    public void removeTrailingSeparatorAtTheBeginningShouldDoNothingWhenSeparatorAtTheEnd() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparatorAtTheBeginning("aa."))
            .isEqualTo("aa.");
    }

    @Test
    public void lastIndexShouldReturnLastDelimiterPosition() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .lastIndex("a.a"))
            .isEqualTo(1);
    }

    @Test
    public void lastIndexShouldAcceptSeparatorOnly() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .lastIndex("."))
            .isEqualTo(0);
    }

    @Test
    public void lastIndexShouldReturnMalusOneWhenNoDelimiter() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .lastIndex("aa"))
            .isEqualTo(-1);
    }

    @Test
    public void lastIndexShouldAcceptEmpty() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .lastIndex(""))
            .isEqualTo(-1);
    }
}