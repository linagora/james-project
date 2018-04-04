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

package org.apache.james.mdn.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UnwrapperTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void unwrapShouldThrowOnNull() {
        expectedException.expect(NullPointerException.class);

        Unwrapper.unwrap(null);
    }

    @Test
    public void unwrapShouldPreserveEmpty() {
        assertThat(Unwrapper.unwrap(""))
            .containsExactly("");
    }

    @Test
    public void unwrapShouldPreserveSingleValue() {
        assertThat(Unwrapper.unwrap("aaa"))
            .containsExactly("aaa");
    }

    @Test
    public void unwrapShouldPreserveUnwrappedLines() {
        assertThat(Unwrapper.unwrap("aaa\r\nbbb\r\nccc\r\nddd"))
            .containsExactly("aaa", "bbb", "ccc", "ddd");
    }

    @Test
    public void unwrapShouldPreserveBlankLine() {
        assertThat(Unwrapper.unwrap("aaa\r\n\r\nbbb\r\nccc\r\nddd"))
            .containsExactly("aaa", "", "bbb", "ccc", "ddd");
    }

    @Test
    public void unwrapShouldUnwrapWrappedLine() {
        assertThat(Unwrapper.unwrap("aaa\r\n bbb\r\n ccc\r\n ddd"))
            .containsExactly("aaa\nbbb\nccc\nddd");
    }

    @Test
    public void unwrapShouldUnwrapWrappedLines() {
        assertThat(Unwrapper.unwrap("aaa\r\n bbb\r\nccc\r\n ddd"))
            .containsExactly("aaa\nbbb", "ccc\nddd");
    }

    @Test
    public void unwrapShouldUnwrapComplexString() {
        assertThat(Unwrapper.unwrap("aaa\r\n aaa\r\nbbb\r\nccc\r\n ccc\r\n ccc\r\nddd\r\n ddd\r\neee"))
            .containsExactly("aaa\naaa", "bbb", "ccc\nccc\nccc", "ddd\nddd", "eee");
    }
}
