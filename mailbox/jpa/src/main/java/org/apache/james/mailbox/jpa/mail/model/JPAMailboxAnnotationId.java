package org.apache.james.mailbox.jpa.mail.model;

import com.google.common.base.Objects;

import javax.persistence.Embeddable;

@Embeddable
public final class JPAMailboxAnnotationId {
    private long mailboxId;
    private String key;

    public JPAMailboxAnnotationId(long mailboxId, String key) {
        this.mailboxId = mailboxId;
        this.key = key;
    }

    public JPAMailboxAnnotationId() {
    }

    public long getMailboxId() {
        return mailboxId;
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JPAMailboxAnnotationId) {
            JPAMailboxAnnotationId that = (JPAMailboxAnnotationId) o;
            return Objects.equal(this.mailboxId, that.mailboxId) && Objects.equal(this.key, that.key);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mailboxId, key);
    }
}
