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

import org.apache.james.mdn.fields.Error;
import org.apache.james.mdn.fields.ExtensionField;
import org.apache.james.mdn.fields.Field;
import org.apache.james.mdn.fields.OriginalMessageId;
import org.apache.james.mdn.fields.Text;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableList;

public class FieldFinderTest {
    private static final boolean STRICT = true;
    private static final boolean LENIENT = false;
    private static final OriginalMessageId FIRST_DIPLICATED_FIELD = new OriginalMessageId("aa");
    private static final ImmutableList<Field> DUPLICATED_FIELDS = ImmutableList.of(
        FIRST_DIPLICATED_FIELD,
        new OriginalMessageId("bb"));
    private static final ImmutableList<Field> NO_FIELDS = ImmutableList.of();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void strictFieldFinderShouldThrowOnNull() {
        expectedException.expect(NullPointerException.class);

        new FieldFinder(STRICT, null);
    }

    @Test
    public void lenientFieldFinderShouldThrowOnNull() {
        expectedException.expect(NullPointerException.class);

        new FieldFinder(LENIENT, null);
    }

    @Test
    public void strictFieldFinderShouldThrowWhenRetrievingDuplicatedFields() {
        FieldFinder fieldFinder = new FieldFinder(STRICT, DUPLICATED_FIELDS);

        expectedException.expect(IllegalStateException.class);

        fieldFinder.retrieveOriginalMessageId();
    }

    @Test
    public void lenientFieldFinderShouldReturnFirstWhenRetrievingDuplicatedFields() {
        FieldFinder fieldFinder = new FieldFinder(LENIENT, DUPLICATED_FIELDS);

        assertThat(fieldFinder.retrieveOriginalMessageId())
            .contains(FIRST_DIPLICATED_FIELD);
    }

    @Test
    public void strictShouldThrowOnMissingDisposition() {
        FieldFinder fieldFinder = new FieldFinder(STRICT, NO_FIELDS);

        expectedException.expect(IllegalStateException.class);

        fieldFinder.retrieveDisposition();
    }

    @Test
    public void lenientShouldReturnDefaultOnMissingDisposition() {
        FieldFinder fieldFinder = new FieldFinder(LENIENT, NO_FIELDS);

        assertThat(fieldFinder.retrieveDisposition())
            .isEqualTo(FieldFinder.DEFAULT_DISPOSITION);
    }

    @Test
    public void strictShouldThrowOnMissingFinalRecipient() {
        FieldFinder fieldFinder = new FieldFinder(STRICT, NO_FIELDS);

        expectedException.expect(IllegalStateException.class);

        fieldFinder.retrieveFinalRecipient();
    }

    @Test
    public void lenientShouldReturnEmptyOnMissingFinalRecipient() {
        FieldFinder fieldFinder = new FieldFinder(LENIENT, NO_FIELDS);

        assertThat(fieldFinder.retrieveFinalRecipient())
            .isEmpty();
    }

    @Test
    public void strictShouldReturnEmptyOnMissingGateway() {
        FieldFinder fieldFinder = new FieldFinder(STRICT, NO_FIELDS);

        assertThat(fieldFinder.retrieveGateway())
            .isEmpty();
    }

    @Test
    public void strictShouldReturnEmptyOnMissingOriginalMessageId() {
        FieldFinder fieldFinder = new FieldFinder(STRICT, NO_FIELDS);

        assertThat(fieldFinder.retrieveOriginalMessageId())
            .isEmpty();
    }

    @Test
    public void strictShouldReturnEmptyOnMissingOriginalRecipient() {
        FieldFinder fieldFinder = new FieldFinder(STRICT, NO_FIELDS);

        assertThat(fieldFinder.retrieveOriginalRecipient())
            .isEmpty();
    }

    @Test
    public void strictShouldReturnEmptyOnMissingReportingUserAgent() {
        FieldFinder fieldFinder = new FieldFinder(STRICT, NO_FIELDS);

        assertThat(fieldFinder.retrieveReportingUserAgent())
            .isEmpty();
    }

    @Test
    public void strictShouldReturnEmptyOnMissingExtensions() {
        FieldFinder fieldFinder = new FieldFinder(STRICT, NO_FIELDS);

        assertThat(fieldFinder.retrieveExtensions())
            .isEmpty();
    }

    @Test
    public void strictShouldReturnEmptyOnMissingErrors() {
        FieldFinder fieldFinder = new FieldFinder(STRICT, NO_FIELDS);

        assertThat(fieldFinder.retrieveErrors())
            .isEmpty();
    }

    @Test
    public void strictShouldReturnProvidedErrors() {
        Error error1 = new Error(Text.fromRawText("Message 1"));
        Error error2 = new Error(Text.fromRawText("Message 2"));
        FieldFinder fieldFinder = new FieldFinder(STRICT, ImmutableList.of(error1, error2));

        assertThat(fieldFinder.retrieveErrors())
            .containsExactly(error1, error2);
    }

    @Test
    public void strictShouldReturnProvidedExtensions() {
        ExtensionField extension1 = new ExtensionField("Extension1", "Message 1");
        ExtensionField extension2 = new ExtensionField("Extension2", "Message 2");
        FieldFinder fieldFinder = new FieldFinder(STRICT, ImmutableList.of(extension1, extension2));

        assertThat(fieldFinder.retrieveExtensions())
            .containsExactly(extension1, extension2);
    }
}
