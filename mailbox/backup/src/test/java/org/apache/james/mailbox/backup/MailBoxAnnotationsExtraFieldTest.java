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

package org.apache.james.mailbox.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.james.mailbox.model.MailboxAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class MailBoxAnnotationsExtraFieldTest {

    private String annotation1Serialized = MailboxMessageFixture.ANNOTATION_1.getKey().asString() + ":" + MailboxMessageFixture.ANNOTATION_1.getValue().orElse("");
    private String annotation2Serialized = MailboxMessageFixture.ANNOTATION_2.getKey().asString() + ":" + MailboxMessageFixture.ANNOTATION_2.getValue().orElse("");

    private int sizeAnnotation1 = annotation1Serialized.length();
    private int sizeAnnotation2 = annotation2Serialized.length();
    private int sizeSeparator = 1;

    private List<MailboxAnnotation> oneThousandTimes = IntStream.range(0, 1000)
            .mapToObj(i -> MailboxMessageFixture.ANNOTATION_1).collect(Collectors.toList());

    private String bufferContentForOneThousand = oneThousandTimes.stream().map(a ->  a.getKey().asString() + ":" + a.getValue().orElse("")).collect(Collectors.joining("%"));

    @Test
    public void shouldMatchBeanContract() {
        EqualsVerifier.forClass(MailBoxAnnotationsExtraField.class)
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }

    @Nested
    class GetHeaderId {

        @Test
        void getHeaderIdShouldReturnSpecificStringInLittleEndian() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField();
            ByteBuffer byteBuffer = ByteBuffer.wrap(testee.getHeaderId().getBytes())
                .order(ByteOrder.LITTLE_ENDIAN);

            assertThat(Charsets.US_ASCII.decode(byteBuffer).toString())
                .isEqualTo("aq");
        }
    }

    @Nested
    class GetLocalFileDataLength {
        @Test
        void getLocalFileDataLengthShouldThrowWhenNoValue() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField();

            assertThatThrownBy(() -> testee.getLocalFileDataLength().getValue())
                .isInstanceOf(RuntimeException.class);
        }

        @Test
        void getLocalFileDataLengthShouldReturnIntegerSize() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(ImmutableList.of());

            assertThat(testee.getLocalFileDataLength().getValue())
                .isEqualTo(0);
        }

        @Test
        void getLocalFileDataLengthShouldReturnIntegerSizeWhenOneAnnotationSet() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(MailboxMessageFixture.WITH_ANNOTATION_1);

            assertThat(testee.getLocalFileDataLength().getValue())
                .isEqualTo(sizeAnnotation1);
        }

        @Test
        void getLocalFileDataLengthShouldReturnIntegerSizeWhenTwoAnnotationSet() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(MailboxMessageFixture.WITH_ANNOTATION_1_AND_2);

            assertThat(testee.getLocalFileDataLength().getValue())
                .isEqualTo(sizeAnnotation1 + sizeAnnotation2 + sizeSeparator);
        }

        @Test
        void getLocalFileDataLengthShouldReturnIntegerSizeWhenOneThousandAnnotationSet() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(oneThousandTimes);
            assertThat(testee.getLocalFileDataLength().getValue())
                .isEqualTo(sizeAnnotation1 * 1000 + 999 * sizeSeparator);
        }
    }

    @Nested
    class GetCentralDirectoryLength {
        @Test
        void getCentralDirectoryDataLengthShouldThrowWhenNoValue() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField();

            assertThatThrownBy(() -> testee.getCentralDirectoryLength().getValue())
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void getCentralDirectoryDataLengthShouldReturnIntegerSize() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(ImmutableList.of());

            assertThat(testee.getCentralDirectoryLength().getValue())
                    .isEqualTo(0);
        }

        @Test
        void getCentralDirectoryDataLengthShouldReturnIntegerSizeWhenOneAnnotationSet() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(MailboxMessageFixture.WITH_ANNOTATION_1);

            assertThat(testee.getCentralDirectoryLength().getValue())
                    .isEqualTo(sizeAnnotation1);
        }

        @Test
        void getCentralDirectoryDataLengthShouldReturnIntegerSizeWhenTwoAnnotationSet() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(MailboxMessageFixture.WITH_ANNOTATION_1_AND_2);

            assertThat(testee.getCentralDirectoryLength().getValue())
                    .isEqualTo(sizeAnnotation1 + sizeAnnotation2 + sizeSeparator);
        }

        @Test
        void getCentralDirectoryDataLengthShouldReturnIntegerSizeWhenOneThousandAnnotationSet() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(oneThousandTimes);
            assertThat(testee.getCentralDirectoryLength().getValue())
                    .isEqualTo(sizeAnnotation1 * 1000 + 999 * sizeSeparator);
        }
    }

    @Nested
    class GetLocalFileData {

        @Test
        void getLocalFileDataDataShouldThrowWhenNoValue() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField();

            assertThatThrownBy(() -> testee.getLocalFileDataData())
                .isInstanceOf(RuntimeException.class);
        }

        @Test
        void getLocalFileDataDataShouldReturnByteArraysOfAnnotation1() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(MailboxMessageFixture.WITH_ANNOTATION_1);
            assertThat(testee.getLocalFileDataData())
                .isEqualTo((MailboxMessageFixture.ANNOTATION_1.getKey().asString() + ":" + MailboxMessageFixture.ANNOTATION_1.getValue().orElse("")).getBytes(StandardCharsets.UTF_8));
        }

        @Test
        void getLocalFileDataDataShouldReturnByteArraysOfAnnotation1And2() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(MailboxMessageFixture.WITH_ANNOTATION_1_AND_2);
            assertThat(testee.getLocalFileDataData())
                .isEqualTo((MailboxMessageFixture.ANNOTATION_1.getKey().asString() + ":" + MailboxMessageFixture.ANNOTATION_1.getValue().orElse("") + "%" +
                        MailboxMessageFixture.ANNOTATION_2.getKey().asString() + ":" + MailboxMessageFixture.ANNOTATION_2.getValue().orElse("")
                        ).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Nested
    class GetCentralDirectoryData {

        @Test
        void getCentralDirectoryDataShouldThrowWhenNoValue() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField();

            assertThatThrownBy(() -> testee.getCentralDirectoryData())
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void getCentralDirectoryDataShouldReturnByteArraysOfAnnotation1() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(MailboxMessageFixture.WITH_ANNOTATION_1);
            assertThat(testee.getCentralDirectoryData())
                    .isEqualTo((MailboxMessageFixture.ANNOTATION_1.getKey().asString() + ":" + MailboxMessageFixture.ANNOTATION_1.getValue().orElse("")).getBytes(StandardCharsets.UTF_8));
        }

        @Test
        void getCentralDirectoryDataShouldReturnByteArraysOfAnnotation1And2() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(MailboxMessageFixture.WITH_ANNOTATION_1_AND_2);
            assertThat(testee.getCentralDirectoryData())
                    .isEqualTo((annotation1Serialized + "%" +
                            MailboxMessageFixture.ANNOTATION_2.getKey().asString() + ":" + MailboxMessageFixture.ANNOTATION_2.getValue().orElse("")
                    ).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Nested
    class ParseFromLocalFileData {

        @Test
        void parseFromLocalFileDataShouldParseByteData() {
            String bufferContent = annotation1Serialized;
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(MailboxMessageFixture.WITH_ANNOTATION_1);
            testee.parseFromLocalFileData(bufferContent
                .getBytes(StandardCharsets.UTF_8), 0, sizeAnnotation1);
            assertThat(testee.getValue()).contains(bufferContent);
        }

        @Test
        void parseFromLocalFileDataShouldParseByteDataWhenOffsetSet() {
            String bufferContent = annotation1Serialized;
            int offset = MailboxMessageFixture.ANNOTATION_1.getKey().asString().length() + 1;
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(MailboxMessageFixture.WITH_ANNOTATION_1);
            testee.parseFromLocalFileData(bufferContent
                    .getBytes(StandardCharsets.UTF_8), offset, sizeAnnotation1 - offset);
            assertThat(testee.getValue()).isEqualTo(MailboxMessageFixture.ANNOTATION_1.getValue());
        }

        @Test
        void getFromLocalFileDataWhenOneThousandAnnotationSet() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(oneThousandTimes);
            testee.parseFromLocalFileData(bufferContentForOneThousand
                    .getBytes(StandardCharsets.UTF_8), 0, sizeAnnotation1 * 1000 + 999 * sizeSeparator);

            assertThat(testee.getValue())
                    .contains(bufferContentForOneThousand);
        }
    }
    @Nested
    class ParseFromCentralDirectoryData {

        @Test
        void parseFromCentralDirectoryDataShouldParseByteData() {
            String bufferContent = annotation1Serialized;
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(MailboxMessageFixture.WITH_ANNOTATION_1);
            testee.parseFromCentralDirectoryData(bufferContent
                .getBytes(StandardCharsets.UTF_8), 0, sizeAnnotation1);
            assertThat(testee.getValue()).contains(bufferContent);
        }

        @Test
        void parseFromCentralDirectoryDataShouldParseByteDataWhenOffsetSet() {
            String bufferContent = annotation1Serialized;
            int offset = MailboxMessageFixture.ANNOTATION_1.getKey().asString().length() + 1;
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(MailboxMessageFixture.WITH_ANNOTATION_1);
            testee.parseFromCentralDirectoryData(bufferContent
                    .getBytes(StandardCharsets.UTF_8), offset, sizeAnnotation1 - offset);
            assertThat(testee.getValue()).isEqualTo(MailboxMessageFixture.ANNOTATION_1.getValue());
        }

        @Test
        void getFromCentralDirectoryDataWhenOneThousandAnnotationSet() {
            MailBoxAnnotationsExtraField testee = new MailBoxAnnotationsExtraField(oneThousandTimes);
            testee.parseFromCentralDirectoryData(bufferContentForOneThousand
                    .getBytes(StandardCharsets.UTF_8), 0, sizeAnnotation1 * 1000 + 999 * sizeSeparator);

            assertThat(testee.getValue())
                    .contains(bufferContentForOneThousand);
        }
    }
}
