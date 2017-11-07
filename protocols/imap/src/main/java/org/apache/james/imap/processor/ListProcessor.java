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

import java.io.Closeable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.james.core.User;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.CharsetUtil;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.response.ImapResponseMessage;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.MailboxType;
import org.apache.james.imap.api.process.MailboxTyper;
import org.apache.james.imap.main.PathConverter;
import org.apache.james.imap.message.request.ListRequest;
import org.apache.james.imap.message.response.ListResponse;
import org.apache.james.imap.processor.base.PrefixedRegex;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.PathDelimiter;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxMetaData.Children;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.search.MailboxQuery;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.util.MDCBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListProcessor extends AbstractMailboxProcessor<ListRequest> {
    public static class ListAnswer {
        private final PathDelimiter delimiter;
        private final Children inferiors;
        private final MailboxMetaData.Selectability selectability;
        private final String mailboxName;
        private final MailboxPath mailboxPath;

        public ListAnswer(PathDelimiter delimiter, Children inferiors, MailboxMetaData.Selectability selectability, String mailboxName, MailboxPath mailboxPath) {
            this.delimiter = delimiter;
            this.inferiors = inferiors;
            this.selectability = selectability;
            this.mailboxName = mailboxName;
            this.mailboxPath = mailboxPath;
        }

        public MailboxPath getMailboxPath() {
            return mailboxPath;
        }

        public PathDelimiter getDelimiter() {
            return delimiter;
        }

        public Children getInferiors() {
            return inferiors;
        }

        public MailboxMetaData.Selectability getSelectability() {
            return selectability;
        }

        public String getMailboxName() {
            return mailboxName;
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof ListAnswer) {
                ListAnswer that = (ListAnswer) o;

                return Objects.equals(this.mailboxName, that.mailboxName);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(mailboxName);
        }
    }

    public static ListAnswer forVirtualMailbox(PathDelimiter delimiter, String user, String mailboxName) {
        return new ListAnswer(delimiter,
            Children.CHILDREN_ALLOWED_BUT_UNKNOWN,
            MailboxMetaData.Selectability.NOSELECT,
            mailboxName,
            MailboxPath.forUser(user, mailboxName));
    }

    public static ListAnswer forVirtualMailboxWithChildren(PathDelimiter delimiter, String user, String mailboxName) {
        return new ListAnswer(delimiter,
            Children.HAS_CHILDREN,
            MailboxMetaData.Selectability.NOSELECT,
            mailboxName,
            MailboxPath.forUser(user, mailboxName));
    }

    public static Stream<ListAnswer> forMailboxMetadata(ImapSession session, PathConverter pathConverter, MailboxMetaData metaData) {
        String username = ImapSessionUtils.getUserName(session);
        if (username.equals(metaData.getPath().getUser())) {
            return Stream.of(fromMetadata(pathConverter, metaData));
        }
        PathDelimiter delimiter = metaData.getHierarchyDelimiter();
        String otherUsersNamespace = session.getNamespaceConfiguration().otherUsersNamespace();
        String sanitizedUserName = User.fromUsername(metaData.getPath().getUser()).getLocalPart();
        return Stream.of(
            fromMetadata(pathConverter, metaData),
            forVirtualMailboxWithChildren(delimiter, username, otherUsersNamespace),
            forVirtualMailboxWithChildren(delimiter,
                username,
                delimiter.join(otherUsersNamespace, sanitizedUserName)));
    }

    public static ListAnswer fromMetadata(PathConverter pathConverter, MailboxMetaData metaData) {
        return new ListAnswer(metaData.getHierarchyDelimiter(),
            metaData.inferiors(),
            metaData.getSelectability(),
            pathConverter.buildMailboxName(metaData.getPath()),
            metaData.getPath());
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ListProcessor.class);
    private static final MailboxTyper MAILBOX_TYPER = null;

    public ListProcessor(ImapProcessor next, MailboxManager mailboxManager, StatusResponseFactory factory,
            MetricFactory metricFactory) {
        super(ListRequest.class, next, mailboxManager, factory, metricFactory);
    }

    /**
     * @see
     * org.apache.james.imap.processor.AbstractMailboxProcessor
     * #doProcess(org.apache.james.imap.api.message.request.ImapRequest,
     * org.apache.james.imap.api.process.ImapSession, java.lang.String,
     * org.apache.james.imap.api.ImapCommand,
     * org.apache.james.imap.api.process.ImapProcessor.Responder)
     */
    protected void doProcess(ListRequest request, ImapSession session, String tag, ImapCommand command, Responder responder) {
        doProcess(request.getBaseReferenceName(),
            request.getMailboxPattern(),
            session,
            tag,
            command,
            responder,
            MAILBOX_TYPER);
    }

    protected ImapResponseMessage createResponse(boolean noInferior, boolean noSelect, boolean marked, boolean unmarked, boolean hasChildren, boolean hasNoChildren, String mailboxName, PathDelimiter delimiter, MailboxType type) {
        return new ListResponse(noInferior, noSelect, marked, unmarked, hasChildren, hasNoChildren, mailboxName, delimiter);
    }

    /**
     * (from rfc3501)<br>
     * The LIST command returns a subset of names from the complete set of all
     * names available to the client. Zero or more untagged LIST replies are
     * returned, containing the name attributes, hierarchy delimiter, and name;
     * see the description of the LIST reply for more detail.<br>
     * ...<br>
     * An empty ("" string) mailbox name argument is a special request to return
     * the hierarchy delimiter and the root name of the name given in the
     * reference. The value returned as the root MAY be the empty string if the
     * reference is non-rooted or is an empty string.
     */
    protected final void doProcess(String referenceName, String mailboxName, ImapSession session, String tag, ImapCommand command, Responder responder, MailboxTyper mailboxTyper) {
        String user = ImapSessionUtils.getUserName(session);
        PathConverter pathConverter = PathConverter.forSession(session);
        MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
        try {
            if (mailboxName.isEmpty()) {
                handleHierarchyInformationRequests(referenceName,
                    session,
                    responder,
                    mailboxTyper,
                    user,
                    mailboxSession);
            } else {
                handleListRequests(
                    referenceName,
                    mailboxName,
                    session,
                    responder,
                    mailboxTyper,
                    pathConverter,
                    mailboxSession);
            }
        } catch (MailboxException e) {
            LOGGER.error("List failed for mailboxName " + mailboxName + " and user" + user, e);
            no(command, tag, responder, HumanReadableText.SEARCH_FAILED);
        }
        okComplete(command, tag, responder);
    }

    private void handleListRequests(String referenceName, String mailboxName, ImapSession session, Responder responder, MailboxTyper mailboxTyper, PathConverter pathConverter, MailboxSession mailboxSession) throws MailboxException {
        PrefixedRegex expression = new PrefixedRegex(
            CharsetUtil.decodeModifiedUTF7(referenceName),
            CharsetUtil.decodeModifiedUTF7(mailboxName),
            mailboxSession.getPathDelimiter());

        getMailboxManager()
            .search(
                MailboxQuery.allMailboxes(),
                mailboxSession)
            .stream()
            .flatMap(metaData -> forMailboxMetadata(session, pathConverter, metaData))
            .filter(listAnswer -> expression.isExpressionMatch(listAnswer.getMailboxName()))
            .distinct()
            .forEach(listAnswer ->
                processResult(responder,
                    listAnswer,
                    getMailboxType(session, mailboxTyper, listAnswer.getMailboxPath())));
    }

    private void handleHierarchyInformationRequests(String referenceName, ImapSession session, Responder responder, MailboxTyper mailboxTyper, String user, MailboxSession mailboxSession) {
        // An empty mailboxName signifies a request for the hierarchy
        // delimiter and root name of the referenceName argument
        String otherUsersNamespace = session.getNamespaceConfiguration().otherUsersNamespace();
        if (referenceName.startsWith(otherUsersNamespace)) {
            MailboxPath rootPath = new MailboxPath(MailboxConstants.USER_NAMESPACE, user, otherUsersNamespace);
            processResult(responder,
                forVirtualMailbox(mailboxSession.getPathDelimiter(), user, rootPath.getName()),
                getMailboxType(session, mailboxTyper, rootPath));
        } else {
            // Get the mailbox for the user reference name.
            MailboxPath rootPath = new MailboxPath(MailboxConstants.USER_NAMESPACE, user, "");
            processResult(responder,
                forVirtualMailbox(mailboxSession.getPathDelimiter(), user, rootPath.getName()),
                getMailboxType(session, mailboxTyper, rootPath));
        }
    }

    void processResult(Responder responder, ListAnswer listAnswer, MailboxType mailboxType) {
        PathDelimiter delimiter = listAnswer.getDelimiter();
        Children inferiors = listAnswer.getInferiors();
        boolean noInferior = MailboxMetaData.Children.NO_INFERIORS.equals(inferiors);
        boolean hasChildren = MailboxMetaData.Children.HAS_CHILDREN.equals(inferiors);
        boolean hasNoChildren = MailboxMetaData.Children.HAS_NO_CHILDREN.equals(inferiors);
        boolean noSelect = listAnswer.getSelectability() == MailboxMetaData.Selectability.NOSELECT;
        boolean marked = listAnswer.getSelectability() == MailboxMetaData.Selectability.MARKED;
        boolean unmarked = listAnswer.getSelectability() == MailboxMetaData.Selectability.UNMARKED;

        responder.respond(createResponse(noInferior,
            noSelect,
            marked, unmarked,
            hasChildren,
            hasNoChildren,
            listAnswer.getMailboxName(),
            delimiter,
            mailboxType));
    }

    /**
     * retrieve mailboxType for specified mailboxPath using provided MailboxTyper
     * 
     * @param session current imap session
     * @param mailboxTyper provided MailboxTyper used to retrieve mailbox type
     * @param path mailbox's path
     * @return MailboxType value
     */
    private MailboxType getMailboxType(ImapSession session, MailboxTyper mailboxTyper, MailboxPath path) {
        return Optional.ofNullable(mailboxTyper)
            .map(typer -> typer.getMailboxType(session, path))
            .orElse(MailboxType.OTHER);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return ListRequest.class.equals(message.getClass());
    }

    @Override
    protected Closeable addContextToMDC(ListRequest message) {
        return MDCBuilder.create()
            .addContext(MDCBuilder.ACTION, "LIST")
            .addContext("base", message.getBaseReferenceName())
            .addContext("pattern", message.getMailboxPattern())
            .build();
    }
}
