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

package org.apache.james.webadmin.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

public class DLPExpressionSampleMatchRequestDTO {
    private final String expression;
    private final String sampleValue;

    @JsonCreator
    public DLPExpressionSampleMatchRequestDTO(@JsonProperty("expression") String expression,
                                              @JsonProperty("sampleValue") String sampleValue) {
        Preconditions.checkNotNull(expression, "The regex expression can not be null");
        Preconditions.checkNotNull(sampleValue, "The sample value can not be null");

        this.expression = expression;
        this.sampleValue = sampleValue;
    }

    public String getExpression() {
        return expression;
    }

    public String getSampleValue() {
        return sampleValue;
    }
}
