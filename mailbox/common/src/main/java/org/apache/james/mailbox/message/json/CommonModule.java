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

import org.apache.james.mailbox.message.EMailer;
import org.apache.james.mailbox.message.IndexableMessage;
import org.apache.james.mailbox.message.MessageUpdateJson;
import org.apache.james.mailbox.message.MimePart;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.Module;

public class CommonModule extends Module {

    @Override
    public String getModuleName() {
        return "CommonModule";
    }

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(EMailer.class, MixInEMailer.class);
        context.setMixInAnnotations(MimePart.class, MixInMimePart.class);
        context.setMixInAnnotations(IndexableMessage.class, MixInIndexableMessage.class);
        context.setMixInAnnotations(MessageUpdateJson.class, MixInMessageUpdateJson.class);
    }

}
