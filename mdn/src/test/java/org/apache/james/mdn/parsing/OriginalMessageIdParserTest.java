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

import org.apache.james.mdn.fields.OriginalMessageId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class OriginalMessageIdParserTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private OriginalMessageIdParser strict;
    private OriginalMessageIdParser lenient;

    @Before
    public void setUp() {
        strict = new OriginalMessageIdParser(true);
        lenient = new OriginalMessageIdParser(false);
    }

    @Test
    public void strictShouldThrowOnNull() {
        expectedException.expect(NullPointerException.class);

        strict.parse(null);
    }

    @Test
    public void lenientShouldThrowOnNull() {
        expectedException.expect(NullPointerException.class);

        lenient.parse(null);
    }

    @Test
    public void strictShouldThrowOnEmpty() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse("");
    }

    @Test
    public void strictShouldThrowOnFoldingWhiteSpaces() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse("  ");
    }

    @Test
    public void strictShouldThrowOnLineBreak() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse("a\nb");
    }

    @Test
    public void lenientShouldReturnEmptyOnEmptyInput() {
        assertThat(lenient.parse(""))
            .isEmpty();
    }

    @Test
    public void lenientShouldReturnEmptyOnFoldingWhiteSpaceInput() {
        assertThat(lenient.parse("  "))
            .isEmpty();
    }

    @Test
    public void lenientShouldReturnEmptyOnLineBreak() {
        assertThat(lenient.parse("a\nb"))
            .isEmpty();
    }

    @Test
    public void strictShouldAcceptValidValue() {
        assertThat(strict.parse("ab"))
            .contains(new OriginalMessageId("ab"));
    }

    @Test
    public void strictShouldTrimValue() {
        assertThat(strict.parse(" ab "))
            .contains(new OriginalMessageId("ab"));
    }
}
