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

package org.apache.james.mdn;

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
import org.apache.james.mdn.modifier.DispositionModifier;
import org.apache.james.mdn.sending.mode.DispositionSendingMode;
import org.apache.james.mdn.type.DispositionType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MDNReportParserTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void parseStrictShouldAcceptMaximalSubset() {
        assertThat(new MDNReportParser()
            .strict()
            .parse("Reporting-UA: UA_name; UA_product\r\n" +
                "Original-Recipient: rfc822; originalRecipient\r\n" +
                "MDN-Gateway: dns;my.address.com\r\n" +
                "Final-Recipient: rfc822; final_recipient\r\n" +
                "Original-Message-ID: original_message_id\r\n" +
                "Disposition: automatic-action/MDN-sent-automatically;processed/error,failed\r\n" +
                "Error: Message1\r\n" +
                "Error: Message2\r\n" +
                "X-OPENPAAS-IP: 177.177.177.77\r\n" +
                "X-OPENPAAS-PORT: 8000\r\n"))
            .contains(MDNReport.builder()
                .dispositionField(Disposition.builder()
                    .actionMode(DispositionActionMode.Automatic)
                    .sendingMode(DispositionSendingMode.Automatic)
                    .type(DispositionType.Processed)
                    .addModifiers(DispositionModifier.Error, DispositionModifier.Failed)
                    .build())
                .gatewayField(new Gateway(Text.fromRawText("my.address.com")))
                .finalRecipientField(new FinalRecipient(Text.fromRawText("final_recipient")))
                .originalRecipientField(new OriginalRecipient(Text.fromRawText("originalRecipient")))
                .originalMessageIdField(new OriginalMessageId("original_message_id"))
                .reportingUserAgentField(new ReportingUserAgent("UA_name", "UA_product"))
                .addErrorFields(new Error(Text.fromRawText("Message1")), new Error(Text.fromRawText("Message2")))
                .withExtensionFields(
                    new ExtensionField("X-OPENPAAS-IP", "177.177.177.77"),
                    new ExtensionField("X-OPENPAAS-PORT", "8000"))
                .build());
    }

    @Test
    public void parseStrictShouldAcceptMinimalSubset() {
        assertThat(new MDNReportParser()
            .strict()
            .parse("Final-Recipient: rfc822; final_recipient\r\n" +
                "Disposition: automatic-action/MDN-sent-automatically;processed\r\n"))
            .contains(MDNReport.builder()
                .dispositionField(Disposition.builder()
                    .actionMode(DispositionActionMode.Automatic)
                    .sendingMode(DispositionSendingMode.Automatic)
                    .type(DispositionType.Processed)
                    .build())
                .finalRecipientField(new FinalRecipient(Text.fromRawText("final_recipient")))
                .build());
    }

    @Test
    public void parseStrictShouldThrowOnMissingFinalRecipient() {
        expectedException.expect(IllegalStateException.class);

        new MDNReportParser()
            .strict()
            .parse("Disposition: automatic-action/MDN-sent-automatically;processed\r\n");
    }


    @Test
    public void parseLenientShouldReturnEmptyOnMissingFinalRecipient() {
        assertThat(
            new MDNReportParser()
                .lenient()
                .parse("Disposition: automatic-action/MDN-sent-automatically;processed\r\n"))
            .isEmpty();
    }

    @Test
    public void parseStrictShouldThrowOnDuplicatedFields() {
        expectedException.expect(IllegalStateException.class);

        new MDNReportParser()
            .strict()
            .parse("Final-Recipient: rfc822; final_recipient\r\n" +
                "Final-Recipient: rfc822; final_recipient\r\n" +
                "Disposition: automatic-action/MDN-sent-automatically;processed\r\n");
    }

    @Test
    public void parseLenientShouldAcceptMaximalSubset() {
        assertThat(new MDNReportParser()
            .lenient()
            .parse("Reporting-UA: UA_name; UA_product\r\n" +
                "Original-Recipient: rfc822; originalRecipient\r\n" +
                "Final-Recipient: rfc822; final_recipient\r\n" +
                "Original-Message-ID: original_message_id\r\n" +
                "Disposition: automatic-action/MDN-sent-automatically;processed/error,failed\r\n" +
                "Error: Message1\r\n" +
                "Error: Message2\r\n" +
                "X-OPENPAAS-IP: 177.177.177.77\r\n" +
                "X-OPENPAAS-PORT: 8000\r\n"))
            .contains(MDNReport.builder()
                .dispositionField(Disposition.builder()
                    .actionMode(DispositionActionMode.Automatic)
                    .sendingMode(DispositionSendingMode.Automatic)
                    .type(DispositionType.Processed)
                    .addModifiers(DispositionModifier.Error, DispositionModifier.Failed)
                    .build())
                .finalRecipientField(new FinalRecipient(Text.fromRawText("final_recipient")))
                .originalRecipientField(new OriginalRecipient(Text.fromRawText("originalRecipient")))
                .originalMessageIdField(new OriginalMessageId("original_message_id"))
                .reportingUserAgentField(new ReportingUserAgent("UA_name", "UA_product"))
                .addErrorFields(new Error(Text.fromRawText("Message1")), new Error(Text.fromRawText("Message2")))
                .withExtensionFields(
                    new ExtensionField("X-OPENPAAS-IP", "177.177.177.77"),
                    new ExtensionField("X-OPENPAAS-PORT", "8000"))
                .build());
    }

    @Test
    public void parseLenientShouldAcceptMinimalSubset() {
        assertThat(new MDNReportParser()
            .lenient()
            .parse("Final-Recipient: rfc822; final_recipient\r\n" +
                "Disposition: automatic-action/MDN-sent-automatically;processed\r\n"))
            .contains(MDNReport.builder()
                .dispositionField(Disposition.builder()
                    .actionMode(DispositionActionMode.Automatic)
                    .sendingMode(DispositionSendingMode.Automatic)
                    .type(DispositionType.Processed)
                    .build())
                .finalRecipientField(new FinalRecipient(Text.fromRawText("final_recipient")))
                .build());
    }

    @Test
    public void parseLenientShouldThrowOnDuplicatedFields() {
        assertThat(new MDNReportParser()
            .lenient()
            .parse("Final-Recipient: rfc822; final_recipient\r\n" +
                "Final-Recipient: rfc822; final_recipient\r\n" +
                "Disposition: automatic-action/MDN-sent-automatically;processed\r\n"))
            .contains(MDNReport.builder()
                .dispositionField(Disposition.builder()
                    .actionMode(DispositionActionMode.Automatic)
                    .sendingMode(DispositionSendingMode.Automatic)
                    .type(DispositionType.Processed)
                    .build())
                .finalRecipientField(new FinalRecipient(Text.fromRawText("final_recipient")))
                .build());
    }
}
