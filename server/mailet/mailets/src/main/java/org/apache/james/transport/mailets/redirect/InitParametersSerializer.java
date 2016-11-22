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

package org.apache.james.transport.mailets.redirect;

public class InitParametersSerializer {

    public static String serialize(InitParameters initParameters) {
        return initParameters.getClass().getSimpleName() + "\n" +
                "static=" + initParameters.isStatic() + "\n" +
                ", passThrough=" + initParameters.getPassThrough() + "\n" +
                ", fakeDomainCheck=" + initParameters.getFakeDomainCheck() + "\n" +
                ", sender=" + initParameters.getSender() + "\n" +
                ", replyTo=" + initParameters.getReplyTo() + "\n" +
                ", reversePath=" + initParameters.getReversePath() + "\n" +
                ", message=" + initParameters.getMessage() + "\n" +
                ", recipients=" + initParameters.getRecipients() + "\n" +
                ", subject=" + initParameters.getSubject() + "\n" +
                ", subjectPrefix=" + initParameters.getSubjectPrefix() + "\n" +
                ", apparentlyTo=" + initParameters.getTo() + "\n" +
                ", attachError=" + initParameters.isAttachError() + "\n" +
                ", isReply=" + initParameters.isReply() + "\n" +
                ", attachmentType=" + initParameters.getAttachmentType() + "\n" +
                ", inLineType=" + initParameters.getInLineType() + "\n";
    }
}
