package org.apache.james.mailbox.cassandra.mail;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.backends.cassandra.init.CassandraTypesProvider;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMailboxSessionMapperFactory;
import org.apache.james.mailbox.cassandra.modules.CassandraAclModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxCounterModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMessageModule;
import org.apache.james.mailbox.cassandra.modules.CassandraUidAndModSeqModule;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.MapperProvider;

import com.datastax.driver.core.Session;

public class CassandraMapperProvider implements MapperProvider<CassandraId> {

    private static final CassandraCluster cassandra = CassandraCluster.create(new CassandraModuleComposite(
        new CassandraAclModule(),
        new CassandraMailboxModule(),
        new CassandraMessageModule(),
        new CassandraMailboxCounterModule(),
        new CassandraUidAndModSeqModule()));

    @Override
    public MailboxMapper<CassandraId> createMailboxMapper() throws MailboxException {
        Session session = cassandra.getConf();
        CassandraTypesProvider typesProvider = cassandra.getTypesProvider();
        CassandraMessageRepository messageRepository = new CassandraMessageRepository(session, typesProvider);
        CassandraMailboxCountersRepository mailboxCountersRepository = new CassandraMailboxCountersRepository(session);
        return new CassandraMailboxSessionMapperFactory(
            new CassandraUidProvider(session),
            new CassandraModSeqProvider(session),
            session,
            typesProvider,
            messageRepository,
            mailboxCountersRepository).getMailboxMapper(new MockMailboxSession("benwa"));
    }

    @Override
    public MessageMapper<CassandraId> createMessageMapper() throws MailboxException {
        Session session = cassandra.getConf();
        CassandraTypesProvider typesProvider = cassandra.getTypesProvider();
        CassandraMessageRepository messageRepository = new CassandraMessageRepository(session, typesProvider);
        CassandraMailboxCountersRepository mailboxCountersRepository = new CassandraMailboxCountersRepository(session);
        return new CassandraMailboxSessionMapperFactory(
            new CassandraUidProvider(session),
            new CassandraModSeqProvider(session),
            session,
            typesProvider,
            messageRepository,
            mailboxCountersRepository).getMessageMapper(new MockMailboxSession("benwa"));
    }

    @Override
    public CassandraId generateId() {
        return CassandraId.timeBased();
    }

    @Override
    public void clearMapper() throws MailboxException {
        cassandra.clearAllTables();
    }

    @Override
    public void ensureMapperPrepared() throws MailboxException {
        cassandra.ensureAllTables();
    }
}
