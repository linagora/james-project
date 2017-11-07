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
package org.apache.james.imap.processor;

import static org.apache.james.imap.api.ImapConstants.SUPPORTS_NAMESPACES;

import java.io.Closeable;
import java.util.List;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.message.request.NamespaceRequest;
import org.apache.james.imap.message.response.NamespaceResponse;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.PathDelimiter;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.util.MDCBuilder;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;

/**
 * Processes a NAMESPACE command into a suitable set of responses.
 */
public class NamespaceProcessor extends AbstractMailboxProcessor<NamespaceRequest> implements CapabilityImplementingProcessor {
    private static final List<String> CAPS = ImmutableList.of(SUPPORTS_NAMESPACES);


    public NamespaceProcessor(ImapProcessor next, MailboxManager mailboxManager, StatusResponseFactory factory,
                              MetricFactory metricFactory) {
        super(NamespaceRequest.class, next, mailboxManager, factory, metricFactory);
    }

    @Override
    protected void doProcess(NamespaceRequest request, ImapSession session, String tag, ImapCommand command, Responder responder) {
        PathDelimiter pathDelimiter = ImapSessionUtils.getMailboxSession(session).getPathDelimiter();
        responder.respond( new NamespaceResponse(
            toNamespaces(session.getNamespaceConfiguration().personalNamespace(), pathDelimiter),
            toNamespaces(session.getNamespaceConfiguration().otherUsersNamespace(), pathDelimiter),
            toNamespaces(session.getNamespaceConfiguration().sharedNamespacesNamespaces(), pathDelimiter)));
        unsolicitedResponses(session, responder, false);
        okComplete(command, tag, responder);
    }

    public List<NamespaceResponse.Namespace> toNamespaces(String namespace, PathDelimiter pathDelimiter) {
        return ImmutableList.of(new NamespaceResponse.Namespace(namespace, pathDelimiter));
    }

    public List<NamespaceResponse.Namespace> toNamespaces(List<String> namespaces, PathDelimiter pathDelimiter) {
        return namespaces.stream()
            .map(namespace -> new NamespaceResponse.Namespace(namespace, pathDelimiter))
            .collect(Guavate.toImmutableList());
    }

    @Override
    public List<String> getImplementedCapabilities(ImapSession session) {
        return CAPS;
    }

    @Override
    protected Closeable addContextToMDC(NamespaceRequest message) {
        return MDCBuilder.create()
            .addContext(MDCBuilder.ACTION, "NAMESPACE")
            .build();
    }
}
