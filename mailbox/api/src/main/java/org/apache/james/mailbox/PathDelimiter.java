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
package org.apache.james.mailbox;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

public class PathDelimiter {

    private final Joiner joiner;
    private final Splitter splitter;
    private final char charDelimiter;

    public PathDelimiter(char charDelimiter) {
        this.charDelimiter = charDelimiter;
        this.joiner = Joiner.on(charDelimiter);
        this.splitter = Splitter.on(charDelimiter);
    }

    public String join(String... paths) {
        return joiner.join(paths);
    }

    public String join(Iterable<String> paths) {
        return joiner.join(paths);
    }

    public List<String> split(String path) {
        return splitter.splitToList(path);
    }

    public String getSimpleName(String path) {
        return Iterables.getLast(split(path));
    }

    public boolean containsPathDelimiter(String name) {
        return name.indexOf(charDelimiter) > -1;
    }

    public int lastIndex(String name) {
        return name.lastIndexOf(charDelimiter);
    }

    public String appendDelimiter(String name) {
        return name + charDelimiter;
    }

    public String removeTrailingSeparatorAtTheEnd(String name) {
        if (name.endsWith(String.valueOf(charDelimiter))) {
            return name.substring(0, name.length() - 1);
        }
        return name;
    }

    public String removeTrailingSeparatorAtTheBeginning(String name) {
        if (name.startsWith(String.valueOf(charDelimiter))) {
            return name.substring(1, name.length());
        }
        return name;
    }

    public char getPathDelimiter() {
        return charDelimiter;
    }
    
    @Override
    public final int hashCode() {
        return Objects.hashCode(charDelimiter);
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof PathDelimiter) {
            PathDelimiter other = (PathDelimiter) obj;
            return this.charDelimiter == other.charDelimiter;
        }
        return false;
    }

    @Override
    public final String toString() {
        return "{\"charDelimiter\": '" + charDelimiter + "'}";
    }
}
