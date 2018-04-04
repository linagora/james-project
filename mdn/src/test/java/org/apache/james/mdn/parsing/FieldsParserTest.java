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
import org.apache.james.mdn.fields.Error;
import org.apache.james.mdn.fields.ExtensionField;
import org.apache.james.mdn.fields.FinalRecipient;
import org.apache.james.mdn.fields.Gateway;
import org.apache.james.mdn.fields.OriginalMessageId;
import org.apache.james.mdn.fields.OriginalRecipient;
import org.apache.james.mdn.fields.ReportingUserAgent;
import org.apache.james.mdn.fields.Text;
import org.apache.james.mdn.sending.mode.DispositionSendingMode;
import org.apache.james.mdn.type.DispositionType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableList;

public class FieldsParserTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private FieldsParser strict;
    private FieldsParser lenient;

    @Before
    public void setUp() {
        strict = new FieldsParser(true);
        lenient = new FieldsParser(false);
    }

    @Test
    public void parseShouldThrowOnNull() {
        expectedException.expect(NullPointerException.class);

        strict.parse(null);
    }

    @Test
    public void parseShouldAcceptEmpty() {
        assertThat(strict.parse(ImmutableList.of()))
            .isEmpty();
    }

    @Test
    public void parseShouldDefaultToExtensionWhenFieldNameUnknown() {
        String name = "UNKNOWN";
        String value = "value";

        assertThat(strict.parse(ImmutableList.of(new UnparsedField(name, value))))
            .containsExactly(new ExtensionField(name, value));
    }

    @Test
    public void parseShouldRecognizeError() {
        String message = "Message";
        assertThat(strict.parse(ImmutableList.of(new UnparsedField(Error.FIELD_NAME, message))))
            .containsExactly(new Error(Text.fromRawText(message)));
    }

    @Test
    public void parseShouldRecognizeDisposition() {
        assertThat(strict.parse(ImmutableList.of(
            new UnparsedField(
                Disposition.FIELD_NAME,
                "automatic-action/MDN-sent-automatically;processed"))))
            .containsExactly(
                Disposition.builder()
                    .actionMode(DispositionActionMode.Automatic)
                    .sendingMode(DispositionSendingMode.Automatic)
                    .type(DispositionType.Processed)
                    .build());
    }

    @Test
    public void parseShouldRecognizeFinalRecipient() {
        assertThat(strict.parse(ImmutableList.of(
            new UnparsedField(
                FinalRecipient.FIELD_NAME,
                "rfc822; a@b.com"))))
            .containsExactly(
                new FinalRecipient(Text.fromRawText("a@b.com")));
    }

    @Test
    public void parseShouldRecognizeOriginalRecipient() {
        assertThat(strict.parse(ImmutableList.of(
            new UnparsedField(
                OriginalRecipient.FIELD_NAME,
                "rfc822; a@b.com"))))
            .containsExactly(
                new OriginalRecipient(Text.fromRawText("a@b.com")));
    }

    @Test
    public void parseShouldRecognizeGateway() {
        assertThat(strict.parse(ImmutableList.of(
            new UnparsedField(
                Gateway.FIELD_NAME,
                "dns; b.com"))))
            .containsExactly(
                new Gateway(Text.fromRawText("b.com")));
    }

    @Test
    public void parseShouldRecognizeOriginalMessageId() {
        assertThat(strict.parse(ImmutableList.of(
            new UnparsedField(
                OriginalMessageId.FIELD_NAME,
                "id"))))
            .containsExactly(
                new OriginalMessageId("id"));
    }

    @Test
    public void parseShouldRecognizeReportingUserAgent() {
        assertThat(strict.parse(ImmutableList.of(
            new UnparsedField(
                ReportingUserAgent.FIELD_NAME,
                "name"))))
            .containsExactly(
                new ReportingUserAgent("name"));
    }

    @Test
    public void parseStrictShouldThrowOnInvalidErrorField() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse(ImmutableList.of(
            new UnparsedField(
                Error.FIELD_NAME,
                "")));
    }

    @Test
    public void parseStrictShouldThrowOnInvalidDispositionField() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse(ImmutableList.of(
            new UnparsedField(
                Disposition.FIELD_NAME,
                "")));
    }

    @Test
    public void parseStrictShouldThrowOnInvalidFinalRecipientField() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse(ImmutableList.of(
            new UnparsedField(
                FinalRecipient.FIELD_NAME,
                "")));
    }

    @Test
    public void parseStrictShouldThrowOnInvalidOriginalRecipientField() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse(ImmutableList.of(
            new UnparsedField(
                OriginalRecipient.FIELD_NAME,
                "")));
    }

    @Test
    public void parseStrictShouldThrowOnInvalidGatewayField() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse(ImmutableList.of(
            new UnparsedField(
                Gateway.FIELD_NAME,
                "")));
    }

    @Test
    public void parseStrictShouldThrowOnInvalidOriginalMessageIdField() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse(ImmutableList.of(
            new UnparsedField(
                OriginalMessageId.FIELD_NAME,
                "")));
    }

    @Test
    public void parseStrictShouldThrowOnInvalidReportingUserAgentField() {
        expectedException.expect(IllegalArgumentException.class);

        strict.parse(ImmutableList.of(
            new UnparsedField(
                ReportingUserAgent.FIELD_NAME,
                "")));
    }

    @Test
    public void parseLenientShouldReturnEmptyOnInvalidErrorField() {
        String message = "";
        assertThat(lenient.parse(ImmutableList.of(new UnparsedField(Error.FIELD_NAME, message))))
            .isEmpty();
    }

    @Test
    public void parseLenientShouldReturnEmptyOnInvalidDispositionField() {
        assertThat(lenient.parse(ImmutableList.of(
            new UnparsedField(
                Disposition.FIELD_NAME,
                ""))))
            .containsExactly(
                Disposition.builder()
                    .actionMode(DispositionActionMode.Automatic)
                    .sendingMode(DispositionSendingMode.Automatic)
                    .type(DispositionType.Processed)
                    .build());
    }

    @Test
    public void parseLenientShouldReturnEmptyOnInvalidFinalRecipientField() {
        assertThat(lenient.parse(ImmutableList.of(
            new UnparsedField(
                FinalRecipient.FIELD_NAME,
                ""))))
            .isEmpty();
    }

    @Test
    public void parseLenientShouldReturnEmptyOnInvalidOriginalRecipientField() {
        assertThat(lenient.parse(ImmutableList.of(
            new UnparsedField(
                OriginalRecipient.FIELD_NAME,
                ""))))
            .isEmpty();
    }

    @Test
    public void parseLenientShouldReturnEmptyOnInvalidGatewayField() {
        assertThat(lenient.parse(ImmutableList.of(
            new UnparsedField(
                Gateway.FIELD_NAME,
                ""))))
            .isEmpty();
    }

    @Test
    public void parseLenientShouldReturnEmptyOnInvalidOriginalMessageIdField() {
        assertThat(lenient.parse(ImmutableList.of(
            new UnparsedField(
                OriginalMessageId.FIELD_NAME,
                ""))))
            .isEmpty();
    }

    @Test
    public void parseLenientShouldReturnEmptyOnInvalidReportingUserAgentField() {
        assertThat(lenient.parse(ImmutableList.of(
            new UnparsedField(
                ReportingUserAgent.FIELD_NAME,
                ""))))
            .isEmpty();
    }
}
