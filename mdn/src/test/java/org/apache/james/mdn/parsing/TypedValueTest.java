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

import org.apache.james.mdn.fields.AddressType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.jqno.equalsverifier.EqualsVerifier;

public class TypedValueTest {
    public static final AddressType DEFAULT_TYPE = new AddressType("TYPE");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private TypedValue.Factory strict;
    private TypedValue.Factory lenientDefault;

    @Before
    public void setUp() {
        strict = new TypedValue.Factory(true, DEFAULT_TYPE);
        lenientDefault = new TypedValue.Factory(false, DEFAULT_TYPE);
    }

    @Test
    public void shouldMatchBeanContact() {
        EqualsVerifier.forClass(TypedValue.class)
            .allFieldsShouldBeUsed()
            .verify();
    }

    @Test
    public void strictShouldThrowOnNull() {
        expectedException.expect(NullPointerException.class);

        strict.parse(null);
    }

    @Test
    public void strictShouldThrowEmptyWhenEmpty() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse("");
    }

    @Test
    public void strictShouldThrowEmptyWhenOnlyType() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse("aaa");
    }

    @Test
    public void strictShouldThrowEmptyWhenLineBreakInType() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse("aa\na;bb");
    }

    @Test
    public void strictShouldParseValue() {
        assertThat(strict.parse("aa;bb"))
            .isEqualTo(new TypedValue(new AddressType("aa"), "bb"));
    }

    @Test
    public void strictShouldAllowMultipleSemicolon() {
        assertThat(strict.parse("aa;bb;cc"))
            .isEqualTo(new TypedValue(new AddressType("aa"), "bb;cc"));
    }

    @Test
    public void strictShouldAllowLineBreaksInValue() {
        assertThat(strict.parse("aa;bb\ncc"))
            .isEqualTo(new TypedValue(new AddressType("aa"), "bb\ncc"));
    }

    @Test
    public void strictShouldTrimTypeAndValue() {
        assertThat(strict.parse("  aa  ;  bb  "))
            .isEqualTo(new TypedValue(new AddressType("aa"), "bb"));
    }

    @Test
    public void strictShouldThrowOnFoldingWhiteSpaceType() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse("    ;  bb  ");
    }

    @Test
    public void lenientDefaultShouldThrowOnNull() {
        expectedException.expect(NullPointerException.class);

        lenientDefault.parse(null);
    }

    @Test
    public void lenientDefaultShouldReturnEmptyWhenEmpty() {
        assertThat(lenientDefault.parse(""))
            .isEqualTo(new TypedValue(DEFAULT_TYPE, ""));
    }

    @Test
    public void lenientDefaultShouldReturnEmptyWhenOnlyType() {
        assertThat(lenientDefault.parse("aaa"))
            .isEqualTo(new TypedValue(DEFAULT_TYPE, "aaa"));
    }

    @Test
    public void lenientDefaultShouldReturnEmptyWhenLineBreakInType() {
        assertThat(lenientDefault.parse("aa\na;bb"))
            .isEqualTo(new TypedValue(DEFAULT_TYPE, "bb"));
    }

    @Test
    public void lenientDefaultShouldReturnDefaultWhenFoldingWhiteSpaceType() {
        assertThat(lenientDefault.parse("  ;bb"))
            .isEqualTo(new TypedValue(DEFAULT_TYPE, "bb"));
    }

    @Test
    public void lenientDefaultShouldParseValue() {
        assertThat(lenientDefault.parse("aa;bb"))
            .isEqualTo(new TypedValue(new AddressType("aa"), "bb"));
    }

    @Test
    public void lenientDefaultShouldAllowMultipleSemicolon() {
        assertThat(lenientDefault.parse("aa;bb;cc"))
            .isEqualTo(new TypedValue(new AddressType("aa"), "bb;cc"));
    }

    @Test
    public void lenientDefaultShouldAllowLineBreaksInValue() {
        assertThat(lenientDefault.parse("aa;bb\ncc"))
            .isEqualTo(new TypedValue(new AddressType("aa"), "bb\ncc"));
    }

    @Test
    public void lenientDefaultShouldTrimTypeAndValue() {
        assertThat(lenientDefault.parse("  aa  ;  bb  "))
            .isEqualTo(new TypedValue(new AddressType("aa"), "bb"));
    }
}
