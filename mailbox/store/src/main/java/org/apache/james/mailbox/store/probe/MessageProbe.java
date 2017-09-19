package org.apache.james.mailbox.store.probe;

import java.util.Collection;

import org.apache.james.mailbox.model.AttachmentId;
import org.apache.james.mailbox.model.MessageId;

public interface MessageProbe {

    Collection<MessageId> getRelatedMessageIds(AttachmentId attachmentId, String user) throws Exception;

}
