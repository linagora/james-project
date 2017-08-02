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

package org.apache.james.util.io;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Condition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ExposedByteArrayOutputStreamTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void initialisationShouldNotFailOnNegativeValue() {
        new ExposedByteArrayOutputStream(-1);
    }

    @Test
    public void initialisationShouldNotFailOnZeroValue() {
        new ExposedByteArrayOutputStream(0);
    }

    @Test
    public void initialisationShouldNotFailOnPositiveValue() {
        new ExposedByteArrayOutputStream(1024);
    }

    @Test
    public void initialisationShouldNotFailOnNegativeLongValue() {
        new ExposedByteArrayOutputStream(-1L);
    }

    @Test
    public void initialisationShouldNotFailOnZeroLongValue() {
        new ExposedByteArrayOutputStream(0L);
    }

    @Test
    public void initialisationShouldNotFailOnPositiveLongValue() {
        new ExposedByteArrayOutputStream(1024L);
    }

    @Test
    public void initialisationOnTooBigLongs() {
        expectedException.expect(IllegalArgumentException.class);

        new ExposedByteArrayOutputStream(Long.MAX_VALUE);
    }

    @Test
    public void initialisationShouldNotFailOnTooLittleLong() {
        new ExposedByteArrayOutputStream(Long.MIN_VALUE);
    }

    @Test
    public void exactSizeShouldAvoidResize() throws Exception {
        int size = 527;
        ExposedByteArrayOutputStream exposedByteArrayOutputStream = new ExposedByteArrayOutputStream(size);

        exposedByteArrayOutputStream.write(new byte[size]);

        assertThat(exposedByteArrayOutputStream.toByteArray()).hasSize(size);
    }

    @Test
    public void overSizeShouldResize() throws Exception {
        final int size = 1024;
        ExposedByteArrayOutputStream exposedByteArrayOutputStream = new ExposedByteArrayOutputStream(size);

        exposedByteArrayOutputStream.write(new byte[size + 1]);

        assertThat(exposedByteArrayOutputStream.toByteArray()).has(biggerSize(size));
    }

    @Test
    public void toByteArrayShouldNotCopyTheUnderlyingArray() throws Exception {
        final int size = 1024;
        ExposedByteArrayOutputStream exposedByteArrayOutputStream = new ExposedByteArrayOutputStream(size);

        exposedByteArrayOutputStream.write(new byte[size]);

        assertThat(exposedByteArrayOutputStream.toByteArray())
            .isSameAs(exposedByteArrayOutputStream.toByteArray());
    }

    private Condition<byte[]> biggerSize(final int size) {
        return new Condition<byte[]>() {
            @Override
            public boolean matches(byte[] value) {
                return value.length > size;
            }
        };
    }


}
