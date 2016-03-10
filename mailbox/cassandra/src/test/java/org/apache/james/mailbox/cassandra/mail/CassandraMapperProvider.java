package org.apache.james.mailbox.cassandra.mail;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
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

import com.google.common.collect.ImmutableList;
import com.nurkiewicz.asyncretry.AsyncRetryExecutor;

public class CassandraMapperProvider implements MapperProvider<CassandraId> {

    private static final CassandraCluster cassandra = CassandraCluster.create(new CassandraModuleComposite(
        new CassandraAclModule(),
        new CassandraMailboxModule(),
        new CassandraMessageModule(),
        new CassandraMailboxCounterModule(),
        new CassandraUidAndModSeqModule()));
    private ImmutableList.Builder<ScheduledExecutorService> schedulers = ImmutableList.builder();

    @Override
    public MailboxMapper<CassandraId> createMailboxMapper() throws MailboxException {
        return new CassandraMailboxSessionMapperFactory(
            new CassandraUidProvider(cassandra.getConf(), buildRetryer()),
            new CassandraModSeqProvider(cassandra.getConf(), buildRetryer()),
            cassandra.getConf(),
            cassandra.getTypesProvider(),
                createSingleThreadedScheduler()).getMailboxMapper(new MockMailboxSession("benwa"));
    }

    private AsyncRetryExecutor buildRetryer() {
        return new AsyncRetryExecutor(createSingleThreadedScheduler());
    }

    private ScheduledExecutorService createSingleThreadedScheduler() {
        ScheduledExecutorService newScheduler = Executors.newSingleThreadScheduledExecutor();
        schedulers.add(newScheduler);
        return newScheduler;
    }

    private void shutDownSchedulers() {
        schedulers.build().asList().stream()
                .filter(this::isNeitherNullNorShutdown)
                .forEach(ExecutorService::shutdown);
    }

    private boolean isNeitherNullNorShutdown(ScheduledExecutorService scheduler) {
        return scheduler != null && (!scheduler.isShutdown() || !scheduler.isTerminated());
    }

    @Override
    public MessageMapper<CassandraId> createMessageMapper() throws MailboxException {
        return new CassandraMailboxSessionMapperFactory(
            new CassandraUidProvider(cassandra.getConf(), buildRetryer()),
            new CassandraModSeqProvider(cassandra.getConf(), buildRetryer()),
            cassandra.getConf(),
            cassandra.getTypesProvider(),
                createSingleThreadedScheduler()).getMessageMapper(new MockMailboxSession("benwa"));
    }

    @Override
    public CassandraId generateId() {
        return CassandraId.timeBased();
    }

    @Override
    public void clearMapper() throws MailboxException {
        cassandra.clearAllTables();
        shutDownSchedulers();
        schedulers = ImmutableList.builder();
    }

    @Override
    public void ensureMapperPrepared() throws MailboxException {
        cassandra.ensureAllTables();
    }
}
