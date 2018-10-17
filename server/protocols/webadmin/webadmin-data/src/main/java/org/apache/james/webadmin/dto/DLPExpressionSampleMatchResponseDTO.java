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

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DLPExpressionSampleMatchResponseDTO {

    public static DLPExpressionSampleMatchResponseDTO invalid() {
        return new DLPExpressionSampleMatchResponseDTO(!DLPExpressionValidationResponseDTO.VALID, Optional.empty());
    }

    public static DLPExpressionSampleMatchResponseDTO matches(boolean matches) {
        return new DLPExpressionSampleMatchResponseDTO(
            DLPExpressionValidationResponseDTO.VALID,
            Optional.of(matches));
    }

    private final boolean isValid;
    private final Optional<Boolean> isMatched;

    private DLPExpressionSampleMatchResponseDTO(boolean isValid, Optional<Boolean> isMatched) {
        this.isValid = isValid;
        this.isMatched = isMatched;
    }

    @JsonProperty("isValid")
    public boolean isValid() {
        return isValid;
    }

    @JsonProperty("isMatched")
    public Optional<Boolean> isMatched() {
        return isMatched;
    }
}
