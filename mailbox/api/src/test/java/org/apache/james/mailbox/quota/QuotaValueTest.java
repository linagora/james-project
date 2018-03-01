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
package org.apache.james.mailbox.quota;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public interface QuotaValueTest<T extends QuotaValue<T>> {

    T instance(int i);

    T unlimited();

    @Test
    default void greaterThanShouldReturnFalseWhenFirstEqualToSecond() {
        assertThat(instance(1).isGreaterThan(instance(1))).isFalse();
    }

    @Test
    default void greaterThanShouldReturnFalseWhenFirstSmallerThanSecond() {
        assertThat(instance(1).isGreaterThan(instance(2))).isFalse();
    }

    @Test
    default void greaterThanShouldReturnTrueWhenFirstGreaterThanSecond() {
        assertThat(instance(2).isGreaterThan(instance(1))).isTrue();
    }

    @Test
    default void greaterThanShouldReturnFalseWhenFirstIsFiniteAndSecondInfinite() {
        assertThat(instance(1).isGreaterThan(unlimited())).isFalse();
    }

    @Test
    default void greaterThanShouldReturnFalseWhenBothAreInfinite() {
        assertThat(unlimited().isGreaterThan(unlimited())).isFalse();
    }

    @Test
    default void greaterThanShouldReturnTrueWhenFirstIsInfiniteAndSecondFinite() {
        assertThat(unlimited().isGreaterThan(instance(1))).isTrue();
    }

    @Test
    default void addShouldReturnUnlimitedWhenThisIsInfinite() {
        assertThat(unlimited().add(2)).isEqualTo(unlimited());
    }

    @Test
    default void addShouldReturnUnlimitedWhenBothAre() {
        assertThat(unlimited().add(unlimited())).isEqualTo(unlimited());
    }

    @Test
    default void addShouldReturnSumResult() {
        assertThat(instance(12).add(instance(23))).isEqualTo(instance(35));
    }
}
