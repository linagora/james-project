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

package org.apache.james.util;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class MemoizedSupplier<T> implements Supplier<T> {

    public static <T> MemoizedSupplier<T> of(Supplier<T> supplier) {
        return new MemoizedSupplier<>(supplier);
    }

    private final Supplier<T> originalSupplier;
    private Optional<AtomicReference<T>> cachedValue;

    public MemoizedSupplier(Supplier<T> originalSupplier) {
        this.originalSupplier = originalSupplier;
        this.cachedValue = Optional.empty();
    }

    @Override
    public T get() {
        return cachedValue
            .orElseGet(
            () -> {
                T value = originalSupplier.get();
                AtomicReference<T> atomicReference = new AtomicReference<>(value);
                cachedValue = Optional.of(atomicReference);
                return atomicReference;
            })
            .get();
    }
}
