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

import javax.mail.Flags;

import org.apache.james.mailbox.MessageUid;

public interface MutableMailboxContext extends HasMailboxContext {
    /**
     * Set the uid for the message. This must be called before the message is added to the store
     * and must be unique / sequential.
     */
    void setUid(MessageUid uid);

    /**
     * Set the mod-sequence for the message. This must be called before the message is added to the store 
     * or any flags are changed. This must be unique / sequential.
     * 
     * @param modSeq
     */
    void setModSeq(long modSeq);
    
    /**
     * Set the Flags 
     * 
     * @param flags
     */
    void setFlags(Flags flags);
}