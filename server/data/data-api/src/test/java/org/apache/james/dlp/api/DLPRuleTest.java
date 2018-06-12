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

package org.apache.james.dlp.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class DLPRuleTest {

    private static final String EXPLANATION = "explanation";
    private static final String REGEX = "regex";

    @Test
    void shouldMatchBeanContract() {
        EqualsVerifier.forClass(DLPRule.class)
            .allFieldsShouldBeUsed()
            .verify();
    }

    @Test
    void innerClassTargetsShouldMatchBeanContract() {
        EqualsVerifier.forClass(DLPRule.Targets.class)
            .allFieldsShouldBeUsed()
            .verify();
    }

    @Test
    void expressionShouldBeMandatory() {
        assertThatThrownBy(() ->
            DLPRule.builder()
                .targetsRecipients()
                .targetsSender()
                .targetsContent()
                .explanation(EXPLANATION)
                .build())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void expressionShouldIsTheOnlyMandatoryField() {
        assertThatCode(() ->
            DLPRule.builder()
                .expression(REGEX)
                .build())
            .doesNotThrowAnyException();
    }

    @Test
    void builderShouldPreserveExpression() {
        DLPRule dlpRule = DLPRule.builder()
            .expression("regex")
            .build();

        assertThat(dlpRule.getRegexp()).isEqualTo(REGEX);
    }

    @Test
    void builderShouldPreserveExplanation() {
        DLPRule dlpRule = DLPRule.builder()
            .explanation("explanation")
            .expression("regex")
            .build();

        assertThat(dlpRule.getExplanation()).contains(EXPLANATION);
    }

    @Test
    void dlpRuleShouldHaveNoTargetsWhenNoneSpecified() {
        DLPRule dlpRule = DLPRule.builder()
            .expression("regex")
            .build();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(dlpRule.getTargets().isContentTargeted()).isFalse();
        softly.assertThat(dlpRule.getTargets().isRecipientTargeted()).isFalse();
        softly.assertThat(dlpRule.getTargets().isSenderTargeted()).isFalse();
        softly.assertAll();
    }

    @Test
    void targetsRecipientsShouldBeReportedInTargets() {
        DLPRule dlpRule = DLPRule.builder()
            .targetsRecipients()
            .expression("regex")
            .build();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(dlpRule.getTargets().isContentTargeted()).isFalse();
        softly.assertThat(dlpRule.getTargets().isRecipientTargeted()).isTrue();
        softly.assertThat(dlpRule.getTargets().isSenderTargeted()).isFalse();
        softly.assertAll();
    }

    @Test
    void targetsSenderShouldBeReportedInTargets() {
        DLPRule dlpRule = DLPRule.builder()
            .targetsSender()
            .expression("regex")
            .build();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(dlpRule.getTargets().isContentTargeted()).isFalse();
        softly.assertThat(dlpRule.getTargets().isRecipientTargeted()).isFalse();
        softly.assertThat(dlpRule.getTargets().isSenderTargeted()).isTrue();
        softly.assertAll();
    }

    @Test
    void targetsContentShouldBeReportedInTargets() {
        DLPRule dlpRule = DLPRule.builder()
            .targetsContent()
            .expression("regex")
            .build();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(dlpRule.getTargets().isContentTargeted()).isTrue();
        softly.assertThat(dlpRule.getTargets().isRecipientTargeted()).isFalse();
        softly.assertThat(dlpRule.getTargets().isSenderTargeted()).isFalse();
        softly.assertAll();
    }

}