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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.jqno.equalsverifier.EqualsVerifier;

public class UnparsedFieldTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private UnparsedField.Factory strictFactory;
    private UnparsedField.Factory lenientFactory;

    @Before
    public void setUp() {
        strictFactory = new UnparsedField.Factory(true);
        lenientFactory = new UnparsedField.Factory(false);
    }

    @Test
    public void shouldMatchBeanContract() {
        EqualsVerifier.forClass(UnparsedField.class)
            .allFieldsShouldBeUsed()
            .verify();
    }

    @Test
    public void strictParseShouldThrowWhenEmpty() {
        expectedException.expect(IllegalArgumentException.class);

        strictFactory.parse("");
    }

    @Test
    public void strictParseShouldThrowWhenNonName() {
        expectedException.expect(IllegalArgumentException.class);

        strictFactory.parse("aaa");
    }

    @Test
    public void strictParseShouldThrowWhenEmptyName() {
        expectedException.expect(IllegalArgumentException.class);

        strictFactory.parse(":");
    }

    @Test
    public void strictParseShouldThrowWhenEmptyNameWithValue() {
        expectedException.expect(IllegalArgumentException.class);

        strictFactory.parse(": aaa");
    }

    @Test
    public void strictParseShouldThrowWhenNamesContainsLineBreak() {
        expectedException.expect(IllegalArgumentException.class);

        strictFactory.parse("a\nb: aaa");
    }

    @Test
    public void lenientParseShouldReturnEmptyWhenEmpty() {
        assertThat(lenientFactory.parse(""))
            .isEmpty();
    }

    @Test
    public void lenientParseShouldReturnEmptyWhenNoName() {
        assertThat(lenientFactory.parse("aaa"))
            .isEmpty();
    }

    @Test
    public void lenientParseShouldReturnEmptyWhenEmptyName() {
        assertThat(lenientFactory.parse(":"))
            .isEmpty();
    }

    @Test
    public void lenientParseShouldReturnEmptyWhenEmptyNameWithValue() {
        assertThat(lenientFactory.parse(": aaa"))
            .isEmpty();
    }

    @Test
    public void lenientParseShouldReturnEmptyWhenNameContainsLineBreak() {
        assertThat(lenientFactory.parse("a\nb: aaa"))
            .isEmpty();
    }

    @Test
    public void strictParseShouldThrowOnNull() {
        expectedException.expect(NullPointerException.class);

        strictFactory.parse(null);
    }

    @Test
    public void lenientParseShouldThrowOnNull() {
        expectedException.expect(NullPointerException.class);

        lenientFactory.parse(null);
    }

    @Test
    public void lenientParseShouldSeparateNameFromValue() {
        assertThat(lenientFactory.parse("a:b"))
            .contains(new UnparsedField("a", "b"));
    }

    @Test
    public void strictParseShouldSeparateNameFromValue() {
        assertThat(strictFactory.parse("a:b"))
            .contains(new UnparsedField("a", "b"));
    }

    @Test
    public void strictParseShouldAcceptEmptyValue() {
        assertThat(strictFactory.parse("a:"))
            .contains(new UnparsedField("a", ""));
    }

    @Test
    public void lenientParseShouldAcceptEmptyValue() {
        assertThat(lenientFactory.parse("a:"))
            .contains(new UnparsedField("a", ""));
    }

    @Test
    public void lenientParseShouldAcceptMultilineValue() {
        assertThat(lenientFactory.parse("a:b\nc"))
            .contains(new UnparsedField("a", "b\nc"));
    }

    @Test
    public void strictParseShouldAcceptMultilineValue() {
        assertThat(strictFactory.parse("a:b\nc"))
            .contains(new UnparsedField("a", "b\nc"));
    }
}
