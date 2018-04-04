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

import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

public class Unwrapper {
    public static ImmutableList<String> unwrap(String initialText) {
        Preconditions.checkNotNull(initialText);

        List<String> lines = Splitter.on("\r\n")
            .splitToList(initialText);

        return lines.stream()
            .reduce(new UnwrapAccumulator(), UnwrapAccumulator::addLine, UnwrapAccumulator::combine)
            .getUnwrappedLines();

    }

    public static class UnwrapAccumulator {
        private final ImmutableList<String> unwrappedLines;
        private final Optional<String> currentUnwrapedLine;

        public UnwrapAccumulator() {
            this(ImmutableList.of(), Optional.empty());
        }

        private UnwrapAccumulator(ImmutableList<String> unwrappedLine, Optional<String> currentUnwrapedLine) {
            this.unwrappedLines = unwrappedLine;
            this.currentUnwrapedLine = currentUnwrapedLine;
        }

        public ImmutableList<String> getUnwrappedLines() {
            if (currentUnwrapedLine.isPresent()) {
                return ImmutableList.<String>builder()
                    .addAll(unwrappedLines)
                    .add(currentUnwrapedLine.get())
                    .build();
            }
            return unwrappedLines;
        }

        public UnwrapAccumulator addLine(String line) {
            boolean isWrapped = line.startsWith(" ");
            if (isWrapped) {
                String newCurrentWrappedLine = currentUnwrapedLine.map(s -> s + "\n").orElse("") + line.substring(1);
                return new UnwrapAccumulator(unwrappedLines, Optional.of(newCurrentWrappedLine));
            }
            return new UnwrapAccumulator(getUnwrappedLines(), Optional.of(line));
        }

        public UnwrapAccumulator combine(UnwrapAccumulator other) {
            throw new UnsupportedOperationException();
        }
    }
}
