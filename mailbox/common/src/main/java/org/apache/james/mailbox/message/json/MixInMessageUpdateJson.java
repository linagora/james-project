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

package org.apache.james.mailbox.message.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class MixInMessageUpdateJson {

    @JsonProperty(JsonMessageConstants.IS_ANSWERED) abstract boolean isAnswered();
    @JsonProperty(JsonMessageConstants.IS_DELETED) abstract boolean isDeleted();
    @JsonProperty(JsonMessageConstants.IS_DRAFT) abstract boolean isDraft();
    @JsonProperty(JsonMessageConstants.IS_FLAGGED) abstract boolean isFlagged();
    @JsonProperty(JsonMessageConstants.IS_RECENT) abstract boolean isRecent();
    @JsonProperty(JsonMessageConstants.IS_UNREAD) abstract boolean isUnRead();
    @JsonProperty(JsonMessageConstants.USER_FLAGS) abstract String[] getUserFlags();
    @JsonProperty(JsonMessageConstants.MODSEQ) abstract long getModSeq();
}
