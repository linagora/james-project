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

package org.apache.james.webadmin.utils;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public class PathAnalyzer {

    private final List<String> pathParts;

    public PathAnalyzer(String path) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(path));
        this.pathParts = ImmutableList.copyOf(Splitter.on('/').split(path));
    }

    public boolean validate(int expectedLength) {
        return validateLength(expectedLength) && ensurePartNotEmpty(expectedLength - 1);
    }

    private boolean validateLength(int expectedLength) {
        return pathParts.size() == expectedLength;
    }

    private boolean ensurePartNotEmpty(int expectedPosition) {
        Preconditions.checkArgument(expectedPosition < pathParts.size());
        return !Strings.isNullOrEmpty(pathParts.get(expectedPosition));
    }

    public String retrieveLastPart() {
        Preconditions.checkState(pathParts.size() > 0);
        return pathParts.get(pathParts.size() - 1);
    }
}
