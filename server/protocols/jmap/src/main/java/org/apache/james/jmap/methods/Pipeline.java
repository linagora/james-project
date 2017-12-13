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

package org.apache.james.jmap.methods;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import javax.mail.MessagingException;

import org.apache.james.mailbox.exception.MailboxException;

import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.functions.FunctionChainer;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class Pipeline<T> {
    @FunctionalInterface
    public interface Step<U> {
        Optional<U> run(U u) throws MailboxException, MessagingException;
    }

    @FunctionalInterface
    public interface Operation<U> {
        U run(U u) throws MailboxException, MessagingException;
    }

    @FunctionalInterface
    public interface MailboxConditionSupplier {
        boolean get() throws MailboxException;
    }

    private static <V> Step<V> identity() {
        return v -> Optional.of(v);
    }

    public static class StepConbination<V> implements Step<V> {
        private final Step<V> step1;
        private final Step<V> step2;

        public StepConbination(Step<V> step1, Step<V> step2) {
            this.step1 = step1;
            this.step2 = step2;
        }

        @Override
        public Optional<V> run(V v) throws MailboxException, MessagingException {
            return step1.run(
                step2.run(v).orElse(v));
        }
    }

    public static class ConditionalStep<U> implements Step<U> {
        public static class Factory<V> {
            private MailboxConditionSupplier condition;

            public Factory when(MailboxConditionSupplier condition) {
                this.condition = condition;
                return this;
            }

            public ConditionalStep<V> then(Operation<V> operation) {
                Preconditions.checkNotNull(condition);
                Preconditions.checkNotNull(operation);
                return new ConditionalStep<>(condition, operation);
            }

            public ConditionalStep<V> then(Pipeline<V> pipeline) {
                Preconditions.checkNotNull(condition);
                Preconditions.checkNotNull(pipeline);
                return new ConditionalStep<>(condition, nested(pipeline));
            }
        }
        private final MailboxConditionSupplier condition;
        private final Operation<U> operation;

        public ConditionalStep(MailboxConditionSupplier condition, Operation operation) {
            this.condition = condition;
            this.operation = operation;
        }

        @Override
        public Optional<U> run(U U) throws MailboxException, MessagingException {
            if (condition.get()) {
                return Optional.of(operation.run(U));
            }
            return Optional.empty();
        }
    }

    public static <U> ConditionalStep.Factory<U> when(MailboxConditionSupplier condition) {
        return new ConditionalStep.Factory()
            .when(condition);
    }

    public static <U> ConditionalStep.Factory<U> when(boolean condition) {
        return new ConditionalStep.Factory<U>()
            .when(() -> condition);
    }

    public static <U> Operation<U> nested(Pipeline<U> pipeline) {
        return pipeline::executeFirst;
    }

    public static <U> Operation<U> nested(Step<U> pipeline) {
        return u -> pipeline.run(u).orElse(u);
    }

    public static <U> Pipeline<U> forOperations(Step<U>... steps) {
        return new Pipeline(ImmutableList.copyOf(Arrays.asList(steps)));
    }

    public static <U> Step<U> endWith(Operation<U> operation) {
        return builder -> Optional.of(operation.run(builder));
    }

    private final ImmutableList<Step> steps;

    private Pipeline(ImmutableList<Step> steps) {
        this.steps = steps;
    }

    public T executeFirst(T t) throws MailboxException, MessagingException {
        FunctionChainer<Step, Optional<T>> runOperation =
            Throwing.function(step -> step.run(t));

        return steps.stream()
            .map(runOperation.sneakyThrow())
            .filter(Optional::isPresent)
            .findFirst()
            .flatMap(Function.identity())
            .orElse(t);
    }

    public Step<T> executeAll() throws MailboxException, MessagingException {
        return steps.stream()
            .reduce((a, b) -> new StepConbination<T>(a, b))
            .orElse(Pipeline.<T>identity());
    }
}
