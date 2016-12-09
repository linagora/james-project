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

package org.apache.james.mailbox.store.mail.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.mailbox.model.FetchGroupImpl;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.junit.Test;

public class FetchTypeConversionTest {

    @Test
    public void getFetchTypeShouldConvertMinimal() {
        assertThat(MessageMapper.FetchType.getFetchType(FetchGroupImpl.MINIMAL)).isEqualTo(MessageMapper.FetchType.Metadata);
    }

    @Test
    public void getFetchTypeShouldConvertBodyContent() {
        assertThat(MessageMapper.FetchType.getFetchType(FetchGroupImpl.BODY_CONTENT)).isEqualTo(MessageMapper.FetchType.Body);
    }

    @Test
    public void getFetchTypeShouldConvertHeaders() {
        assertThat(MessageMapper.FetchType.getFetchType(FetchGroupImpl.HEADERS)).isEqualTo(MessageMapper.FetchType.Headers);
    }

    @Test
    public void getFetchTypeShouldConvertFullContent() {
        assertThat(MessageMapper.FetchType.getFetchType(FetchGroupImpl.FULL_CONTENT)).isEqualTo(MessageMapper.FetchType.Full);
    }

    @Test
    public void getFetchTypeShouldConvertMimeContent() {
        assertThat(MessageMapper.FetchType.getFetchType(new FetchGroupImpl(FetchGroupImpl.MIME_CONTENT))).isEqualTo(MessageMapper.FetchType.Metadata);
    }

    @Test
    public void getFetchTypeShouldConvertMimeDescriptor() {
        assertThat(MessageMapper.FetchType.getFetchType(new FetchGroupImpl(FetchGroupImpl.MIME_DESCRIPTOR))).isEqualTo(MessageMapper.FetchType.Full);
    }

    @Test
    public void getFetchTypeShouldConvertMimeHeaders() {
        assertThat(MessageMapper.FetchType.getFetchType(new FetchGroupImpl(FetchGroupImpl.MIME_HEADERS))).isEqualTo(MessageMapper.FetchType.Metadata);
    }

    @Test
    public void getFetchTypeShouldConvertAdditionBodyHeaders() {
        assertThat(MessageMapper.FetchType.getFetchType(new FetchGroupImpl(MessageResult.FetchGroup.HEADERS + MessageResult.FetchGroup.BODY_CONTENT)))
            .isEqualTo(MessageMapper.FetchType.Full);
    }

    @Test
    public void getFetchTypeShouldConvertAdditionMimeHeaders() {
        assertThat(MessageMapper.FetchType.getFetchType(new FetchGroupImpl(MessageResult.FetchGroup.MIME_HEADERS + MessageResult.FetchGroup.HEADERS)))
            .isEqualTo(MessageMapper.FetchType.Headers);
    }

}
