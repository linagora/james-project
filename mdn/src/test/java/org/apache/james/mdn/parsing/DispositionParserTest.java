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

import org.apache.james.mdn.action.mode.DispositionActionMode;
import org.apache.james.mdn.fields.Disposition;
import org.apache.james.mdn.modifier.DispositionModifier;
import org.apache.james.mdn.sending.mode.DispositionSendingMode;
import org.apache.james.mdn.type.DispositionType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DispositionParserTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private DispositionParser strict;
    private DispositionParser lenient;

    @Before
    public void setUp() {
        strict = new DispositionParser(true);
        lenient = new DispositionParser(false);
    }

    @Test
    public void parseShouldHandleWellFormattedDispositionField() {
        assertThat(strict.parse("automatic-action/MDN-sent-automatically;processed/error,failed"))
            .contains(Disposition.builder()
                .actionMode(DispositionActionMode.Automatic)
                .sendingMode(DispositionSendingMode.Automatic)
                .type(DispositionType.Processed)
                .addModifiers(DispositionModifier.Error, DispositionModifier.Failed)
                .build());
    }

    @Test
    public void parseShouldHandleWellWhenOnlyOneModifier() {
        assertThat(strict.parse("automatic-action/MDN-sent-automatically;processed/error"))
            .contains(Disposition.builder()
                .actionMode(DispositionActionMode.Automatic)
                .sendingMode(DispositionSendingMode.Automatic)
                .type(DispositionType.Processed)
                .addModifiers(DispositionModifier.Error)
                .build());
    }

    @Test
    public void parseShouldHandleWellWhenOnlyNoModifierButDelimiter() {
        assertThat(strict.parse("automatic-action/MDN-sent-automatically;processed/"))
            .contains(Disposition.builder()
                .actionMode(DispositionActionMode.Automatic)
                .sendingMode(DispositionSendingMode.Automatic)
                .type(DispositionType.Processed)
                .build());
    }

    @Test
    public void parseShouldHandleWellWhenOnlyNoModifier() {
        assertThat(strict.parse("automatic-action/MDN-sent-automatically;processed"))
            .contains(Disposition.builder()
                .actionMode(DispositionActionMode.Automatic)
                .sendingMode(DispositionSendingMode.Automatic)
                .type(DispositionType.Processed)
                .build());
    }

    @Test
    public void parseShouldThrowOnUnknownType() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse("automatic-action/MDN-sent-automatically;unknown");
    }

    @Test
    public void parseShouldThrowOnUnknownSendingMode() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse("automatic-action/unknown;processed");
    }

    @Test
    public void parseShouldThrowOnUnknownActionMode() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse("unknown/MDN-sent-automatically;processed");
    }

    @Test
    public void parseShouldThrowOnLineBreakInModifier() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse("automatic-action/MDN-sent-automatically;processed/aaa\nbbb");
    }

    @Test
    public void parseShouldThrowOnMissingType() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse("automatic-action/MDN-sent-automatically");
    }

    @Test
    public void parseShouldThrowOnMissingActionDelimiter() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse("automatic-action");
    }

    @Test
    public void parseLenientShouldNotThrowOnUnknownType() {
        assertThat(lenient.parse("automatic-action/MDN-sent-automatically;unknown"))
            .contains(Disposition.builder()
                .actionMode(DispositionActionMode.Automatic)
                .sendingMode(DispositionSendingMode.Automatic)
                .type(DispositionType.Processed)
                .build());
    }

    @Test
    public void parseLenientShouldNotThrowOnUnknownSendingMode() {
        assertThat(lenient.parse("automatic-action/unknown;processed"))
            .contains(Disposition.builder()
                .actionMode(DispositionActionMode.Automatic)
                .sendingMode(DispositionSendingMode.Automatic)
                .type(DispositionType.Processed)
                .build());
    }

    @Test
    public void parseLenientShouldNotThrowOnUnknownActionMode() {
        assertThat(lenient.parse("automatic-action/MDN-sent-automatically;processed"))
            .contains(Disposition.builder()
                .actionMode(DispositionActionMode.Automatic)
                .sendingMode(DispositionSendingMode.Automatic)
                .type(DispositionType.Processed)
                .build());
    }

    @Test
    public void parseLenientShouldNotThrowOnLineBreakInModifier() {
        assertThat(lenient.parse("automatic-action/MDN-sent-automatically;processed/aaa\nbbb"))
            .contains(Disposition.builder()
                .actionMode(DispositionActionMode.Automatic)
                .sendingMode(DispositionSendingMode.Automatic)
                .type(DispositionType.Processed)
                .build());
    }

    @Test
    public void parseLenientShouldNotThrowOnMissingType() {
        assertThat(lenient.parse("automatic-action/MDN-sent-automatically"))
            .contains(Disposition.builder()
                .actionMode(DispositionActionMode.Automatic)
                .sendingMode(DispositionSendingMode.Automatic)
                .type(DispositionType.Processed)
                .build());
    }

    @Test
    public void parseLenientShouldNotThrowOnActionModeDelimiter() {
        assertThat(lenient.parse("automatic-action"))
            .contains(Disposition.builder()
                .actionMode(DispositionActionMode.Automatic)
                .sendingMode(DispositionSendingMode.Automatic)
                .type(DispositionType.Processed)
                .build());
    }

}
