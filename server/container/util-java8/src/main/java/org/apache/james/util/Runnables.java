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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.steveash.guavate.Guavate;

public class Runnables {
    public static void runParallel(Runnable... runnables) {
        runParallelRunnables(Arrays.stream(runnables));
    }

    public static void runParallelRunnables(Stream<Runnable> runnables) {
        runParallel(runnables.map(Runnables::toSupplier));
    }

    public static <T> List<T> runParallel(Supplier<T>... suppliers) {
        return runParallel(Arrays.stream(suppliers));
    }

    public static <T> List<T> runParallel(Stream<Supplier<T>> supplierStream) {
        return FluentFutureStream.of(
            supplierStream
                .map(CompletableFuture::supplyAsync))
            .join()
            .collect(Guavate.toImmutableList());
    }

    private static Supplier<Object> toSupplier(Runnable runnable) {
        return () -> {
            runnable.run();
            return new Object();
        };
    }
}
