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

package org.apache.james.jmap.methods;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.apache.james.jmap.model.GetMessagesRequest;
import org.apache.james.jmap.model.GetMessagesResponse;
import org.apache.james.jmap.model.Message;
import org.apache.james.jmap.model.Message.Builder;
import org.apache.james.jmap.model.MessageId;
import org.apache.james.jmap.model.Property;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.FetchGroupImpl;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.mailbox.model.MessageResultIterator;
import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.javatuples.Pair;

import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.functions.ThrowingBiFunction;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class GetMessagesMethod<Id extends MailboxId> implements Method {

    private static final Method.Name METHOD_NAME = Method.name("getMessages");
    private final MailboxManager mailboxManager;
    

    @Inject
    @VisibleForTesting GetMessagesMethod(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }
    
    @Override
    public Method.Name methodName() {
        return METHOD_NAME;
    }
    
    @Override
    public Class<? extends JmapRequest> requestType() {
        return GetMessagesRequest.class;
    }
    
    @Override
    public GetMessagesResponse process(JmapRequest request, MailboxSession mailboxSession) {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(mailboxSession);
        Preconditions.checkArgument(request instanceof GetMessagesRequest);
        GetMessagesRequest getMessagesRequest = (GetMessagesRequest) request;
        
        Function<MessageId, Stream<Pair<MessageResult, MailboxPath>>> loadMessages = loadMessage(mailboxSession);
        Function<Pair<MessageResult, MailboxPath>, Message> toJmapMessage = new JmapMessageFactory(getMessagesRequest, mailboxSession);
        
        List<Message> result = getMessagesRequest.getMessageIds().stream()
            .flatMap(loadMessages)
            .map(toJmapMessage)
            .collect(Collectors.toList());

        return new GetMessagesResponse(result);
    }

    private Function<MessageId, Stream<Pair<MessageResult, MailboxPath>>> loadMessage(MailboxSession mailboxSession) {
        return Throwing
                .function((MessageId messageId) -> {
                     MailboxPath mailboxPath = messageId.getMailboxPath(mailboxSession);
                     MessageManager mailbox = mailboxManager.getMailbox(mailboxPath, mailboxSession);
                     return Pair.with(
                             mailbox.getMessages(MessageRange.one(messageId.getUid()), FetchGroupImpl.MINIMAL, mailboxSession),
                             mailboxPath
                             );
         })
                .andThen(this::iteratorToStream);
    }
    
    private Stream<Pair<MessageResult, MailboxPath>> iteratorToStream(Pair<MessageResultIterator, MailboxPath> value) {
        Iterable<MessageResult> iterable = () -> value.getValue0();
        Stream<MessageResult> targetStream = StreamSupport.stream(iterable.spliterator(), false);
        
        MailboxPath mailboxPath = value.getValue1();
        return targetStream.map(x -> Pair.with(x, mailboxPath));
    }

    private static class JmapMessageFactory implements Function<Pair<MessageResult, MailboxPath>, Message> {
        
        public ImmutableMap<Property, ThrowingBiFunction<Pair<MessageResult, MailboxPath>, Message.Builder, Message.Builder>> fieldCopiers = 
                ImmutableMap.of(
                        Property.id, this::copyId,
                        Property.subject, this::copySubject
                        );
        
        private final MailboxSession session;
        private final ImmutableList<Property> selectedProperties;
        
        public JmapMessageFactory(GetMessagesRequest messagesRequest, MailboxSession session) {
            this.session = session;
            this.selectedProperties = messagesRequest.getProperties().orElse(Property.all());
        }
        
        private Message.Builder copyId(Pair<MessageResult, MailboxPath> element, Message.Builder builder) {
            MessageResult messageResult = element.getValue0();
            MailboxPath mailboxPath = element.getValue1();
            long mailUid = messageResult.getUid();
            return builder.messageId(new MessageId(session.getUser(), mailboxPath , mailUid));
        }
        
        private Message.Builder copySubject(Pair<MessageResult, MailboxPath> element, Message.Builder builder) throws MailboxException {
            MessageResult messageResult = element.getValue0();
            return builder.subject(getMessageSubject(messageResult));
        }
        
        private Optional<String> getMessageSubject(MessageResult messageResult) throws MailboxException {
            Iterator<MessageResult.Header> headers = messageResult.getHeaders().headers();
            Iterable<MessageResult.Header> iterable = () -> headers;
            return StreamSupport.stream(iterable.spliterator(), true)
                .filter(Throwing.predicate(x -> x.getName().equalsIgnoreCase("subject")))
                .map(Throwing.function(MessageResult.Header::getValue))
                .findFirst();
        }

        @Override
        public Message apply(Pair<MessageResult, MailboxPath> t) {
            Message.Builder builder = Message.builder();
            
            selectCopiers().forEach(f -> f.apply(t, builder));
            
            return builder.build();
        }

        private Stream<ThrowingBiFunction<Pair<MessageResult, MailboxPath>, Builder, Builder>> selectCopiers() {
            return Stream.concat(selectedProperties.stream(), Stream.of(Property.id))
                .filter(fieldCopiers::containsKey)
                .map(fieldCopiers::get);
        }   
    }
}
