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

package org.apache.james.webadmin.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PathAnalyzerTest {

    @Test(expected = IllegalArgumentException.class)
    public void pathAnalyzerShouldNotAcceptNullPath() {
        new PathAnalyzer(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pathAnalyzerShouldNotAcceptEmptyPath() {
        new PathAnalyzer("");
    }

    @Test
    public void validateShouldReturnFalseWhenVariablesAreEmpty() {
        assertThat(new PathAnalyzer("/").validate(2)).isFalse();
    }

    @Test
    public void validateShouldReturnFalseWhenLastVariableIsEmpty() {
        assertThat(new PathAnalyzer("a/").validate(2)).isFalse();
    }

    @Test
    public void validateShouldReturnTrueWhenFirstVariableIsEmpty() {
        assertThat(new PathAnalyzer("/a").validate(2)).isTrue();
    }

    @Test
    public void validateShouldReturnFalseWhenCalledOnAnUndersizedString() {
        assertThat(new PathAnalyzer("aaa").validate(2)).isFalse();
    }

    @Test
    public void validateShouldWorkOnOneElementPath() {
        assertThat(new PathAnalyzer("aaa").validate(1)).isTrue();
    }

    @Test
    public void validateSHouldReturnTrueWhenBothVariablesSpecified() {
        assertThat(new PathAnalyzer("a/b").validate(2)).isTrue();
    }

    @Test
    public void retrieveLastPartShouldReturnLastPartOfThePath() {
        assertThat(new PathAnalyzer("a/b").retrieveLastPart()).isEqualTo("b");
    }

    @Test
    public void retrieveLastPartShouldReturnLastPartOfThePathWhenFirstPartIsEmpty() {
        assertThat(new PathAnalyzer("/b").retrieveLastPart()).isEqualTo("b");
    }

}
