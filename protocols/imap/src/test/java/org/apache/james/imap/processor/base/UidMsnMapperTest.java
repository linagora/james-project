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

package org.apache.james.imap.processor.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

import org.apache.james.mailbox.MessageUid;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableBiMap;

public class UidMsnMapperTest {
    private UidMsnMapper testee;
    private MessageUid messageUid1;
    private MessageUid messageUid2;
    private MessageUid messageUid3;
    private MessageUid messageUid4;

    @Before
    public void setUp() {
        testee = new UidMsnMapper();
        messageUid1 = MessageUid.of(1);
        messageUid2 = MessageUid.of(2);
        messageUid3 = MessageUid.of(3);
        messageUid4 = MessageUid.of(4);
    }

    @Test
    public void getUidShouldReturnEmptyIfNoMessageWithTheGivenMessageNumber() {
        assertThat(testee.getUid(0))
            .isAbsent();
    }

    @Test
    public void getUidShouldTheCorrespondingUidIfItExist() {
        testee.addUid(messageUid1);

        assertThat(testee.getUid(1))
            .contains(messageUid1);
    }

    @Test
    public void getFirstUidShouldReturnEmptyIfNoMessage() {
        assertThat(testee.getFirstUid()).isAbsent();
    }

    @Test
    public void getLastUidShouldReturnEmptyIfNoMessage() {
        assertThat(testee.getLastUid()).isAbsent();
    }

    @Test
    public void getFirstUidShouldReturnFirstUidIfAtLeastOneMessage() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);

        assertThat(testee.getFirstUid()).contains(messageUid1);
    }

    @Test
    public void getLastUidShouldReturnLastUidIfAtLeastOneMessage() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);

        assertThat(testee.getLastUid()).contains(messageUid2);
    }

    @Test
    public void getMsnShouldReturnAbsentIfNoCorrespondingMessage() {
        testee.addUid(messageUid1);

        assertThat(testee.getMsn(messageUid2)).isAbsent();
    }

    @Test
    public void getMsnShouldReturnMessageNumberIfUidIsThere() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);

        assertThat(testee.getMsn(messageUid2))
            .contains(2);
    }

    @Test
    public void getNumMessageShouldReturnZeroIfNoMapping() {
        assertThat(testee.getNumMessage())
            .isEqualTo(0);
    }

    @Test
    public void getNumMessageShouldReturnTheNumOfMessage() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);

        assertThat(testee.getNumMessage())
            .isEqualTo(2);
    }

    @Test
    public void isEmptyShouldReturnTrueIfNoMapping() {
        assertThat(testee.isEmpty())
            .isTrue();
    }

    @Test
    public void isEmptyShouldReturnFalseIfNoMapping() {
        testee.addUid(messageUid1);

        assertThat(testee.isEmpty())
            .isFalse();
    }

    @Test
    public void clearShouldClearMapping() {
        testee.addUid(messageUid1);

        testee.clear();

        assertThat(testee.isEmpty())
            .isTrue();
    }

    @Test
    public void addUidShouldKeepMessageNumberContiguous() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);
        testee.addUid(messageUid3);
        testee.addUid(messageUid4);
        testee.addUid(messageUid2);

        assertThat(testee.getInternals())
            .isEqualTo(ImmutableBiMap.of(
                1, messageUid1,
                2, messageUid2,
                3, messageUid3,
                4, messageUid4));
    }

    @Test
    public void addUidShouldNotOverridePreviousMapping() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);
        testee.addUid(messageUid3);
        testee.addUid(messageUid2);

        assertThat(testee.getMsn(messageUid2))
            .contains(2);
    }

    @Test
    public void removeShouldKeepAValidMappingWhenDeletingBeginning() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);
        testee.addUid(messageUid3);
        testee.addUid(messageUid4);

        testee.remove(messageUid1);

        assertThat(testee.getInternals())
            .isEqualTo(ImmutableBiMap.of(
                1, messageUid2,
                2, messageUid3,
                3, messageUid4));
    }

    @Test
    public void removeShouldKeepAValidMappingWhenDeletingMiddle() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);
        testee.addUid(messageUid3);
        testee.addUid(messageUid4);

        testee.remove(messageUid4);

        assertThat(testee.getInternals())
            .isEqualTo(ImmutableBiMap.of(
                1, messageUid1,
                2, messageUid2,
                3, messageUid3));
    }

    @Test
    public void removeShouldKeepAValidMappingWhenDeletingEnd() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);
        testee.addUid(messageUid3);
        testee.addUid(messageUid4);

        testee.remove(messageUid3);

        assertThat(testee.getInternals())
            .isEqualTo(ImmutableBiMap.of(
                1, messageUid1,
                2, messageUid2,
                3, messageUid4));
    }

}