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

package org.apache.james.mailbox.cassandra;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.DockerCassandraExtension;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.backends.cassandra.init.CassandraTableManager;
import org.apache.james.backends.cassandra.init.configuration.CassandraConfiguration;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.blob.cassandra.CassandraBlobId;
import org.apache.james.blob.cassandra.CassandraBlobsDAO;
import org.apache.james.mailbox.cassandra.event.distributed.CassandraMailboxPathRegisterMapper;
import org.apache.james.mailbox.cassandra.event.distributed.CassandraMailboxPathRegistrerMapperContract;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.cassandra.ids.CassandraMessageId;
import org.apache.james.mailbox.cassandra.mail.CassandraACLMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraACLMapperContract;
import org.apache.james.mailbox.cassandra.mail.CassandraAnnotationMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraApplicableFlagDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraApplicableFlagDAOContract;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAOContract;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAOV2;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAOV2Contract;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentFallbackContract;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentMessageIdDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentOwnerDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentOwnerDAOContract;
import org.apache.james.mailbox.cassandra.mail.CassandraDeletedMessageDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraDeletedMessageDAOContract;
import org.apache.james.mailbox.cassandra.mail.CassandraFirstUnseenDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraFirstUnseenDAOContract;
import org.apache.james.mailbox.cassandra.mail.CassandraIndexTableHandler;
import org.apache.james.mailbox.cassandra.mail.CassandraIndexTableHandlerContract;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxCounterDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxCounterDAOContract;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxDAOContract;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxMapperConcurrencyContract;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxMapperContract;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathDAOContract;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathDAOImpl;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathDAOImplContract;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathV2DAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxRecentDAOContract;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxRecentsDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageDAOContract;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageIdDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageIdDAOContract;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageIdToImapUidDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageIdToImapUidDAOContract;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProviderContract;
import org.apache.james.mailbox.cassandra.mail.CassandraUidProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraUidProviderContract;
import org.apache.james.mailbox.cassandra.mail.CassandraUserMailboxRightsDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraUserMailboxRightsDAOContract;
import org.apache.james.mailbox.cassandra.mail.MailboxAggregateModule;
import org.apache.james.mailbox.cassandra.mail.migration.AttachmentMessageIdCreation;
import org.apache.james.mailbox.cassandra.mail.migration.AttachmentMessageIdCreationContract;
import org.apache.james.mailbox.cassandra.mail.migration.AttachmentV2Migration;
import org.apache.james.mailbox.cassandra.mail.migration.AttachmentV2MigrationContract;
import org.apache.james.mailbox.cassandra.mail.migration.CassandraMailboxMapperFallbackContract;
import org.apache.james.mailbox.cassandra.modules.CassandraQuotaModule;
import org.apache.james.mailbox.cassandra.modules.CassandraRegistrationModule;
import org.apache.james.mailbox.cassandra.quota.CassandraCurrentQuotaManager;
import org.apache.james.mailbox.cassandra.quota.CassandraGlobalMaxQuotaDao;
import org.apache.james.mailbox.cassandra.quota.CassandraPerDomainMaxQuotaDao;
import org.apache.james.mailbox.cassandra.quota.CassandraPerUserMaxQuotaDao;
import org.apache.james.mailbox.cassandra.quota.CassandraPerUserMaxQuotaManager;
import org.apache.james.mailbox.cassandra.user.CassandraSubscriptionMapper;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.quota.MaxQuotaManager;
import org.apache.james.mailbox.store.mail.AnnotationMapper;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.model.AnnotationMapperContract;
import org.apache.james.mailbox.store.mail.model.AttachmentMapperContract;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMapperACLContract;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.apache.james.mailbox.store.quota.GenericMaxQuotaManagerTest;
import org.apache.james.mailbox.store.quota.StoreCurrentQuotaManager;
import org.apache.james.mailbox.store.quota.StoreCurrentQuotaManagerContract;
import org.apache.james.mailbox.store.user.SubscriptionMapper;
import org.apache.james.mailbox.store.user.SubscriptionMapperContract;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DockerCassandraExtension.class)
class CassandraMailboxTestRunner implements CassandraUidProviderContract, CassandraModSeqProviderContract, CassandraMailboxDAOContract ,
    CassandraApplicableFlagDAOContract, CassandraAttachmentDAOContract, CassandraAttachmentDAOV2Contract, CassandraAttachmentOwnerDAOContract,
    CassandraDeletedMessageDAOContract, CassandraFirstUnseenDAOContract, CassandraMailboxCounterDAOContract, CassandraUserMailboxRightsDAOContract,
    CassandraMessageIdToImapUidDAOContract, CassandraMessageIdDAOContract, CassandraMessageDAOContract, CassandraMailboxRecentDAOContract,
    CassandraMailboxPathDAOImplContract, CassandraMailboxPathDAOContract, CassandraIndexTableHandlerContract, SubscriptionMapperContract,
    GenericMaxQuotaManagerTest, CassandraACLMapperContract, CassandraMailboxPathRegistrerMapperContract, CassandraMailboxMapperConcurrencyContract,
    StoreCurrentQuotaManagerContract, MailboxMapperACLContract, AnnotationMapperContract, AttachmentMapperContract, AttachmentV2MigrationContract,
    CassandraMailboxMapperFallbackContract, CassandraAttachmentFallbackContract, CassandraMailboxMapperContract, AttachmentMessageIdCreationContract {

    private static final CassandraBlobId.Factory BLOB_ID_FACTORY = new CassandraBlobId.Factory();
    private static final CassandraModuleComposite MODULES = new CassandraModuleComposite(
        MailboxAggregateModule.MODULE,
        CassandraQuotaModule.MODULE,
        CassandraRegistrationModule.MODULE);

    private static CassandraCluster cassandra;

    private CassandraUidProvider uidProvider;
    private CassandraModSeqProvider modSeqProvider;
    private CassandraMailboxDAO cassandraMailboxDAO;
    private CassandraApplicableFlagDAO applicableFlagDAO;
    private CassandraAttachmentDAO attachmentDAO;
    private CassandraDeletedMessageDAO deletedMessageDAO;
    private CassandraFirstUnseenDAO firstUnseenDAO;
    private CassandraAttachmentDAOV2 attachmentDAOV2;
    private CassandraAttachmentOwnerDAO attachmentOwnerDAO;
    private CassandraMailboxCounterDAO mailboxCounterDAO;
    private CassandraUserMailboxRightsDAO userMailboxRightsDAO;
    private CassandraMessageIdToImapUidDAO messageIdToImapUidDAO;
    private CassandraMessageIdDAO messageIdDAO;
    private CassandraMessageDAO messageDAO;
    private CassandraMailboxRecentsDAO mailboxRecentsDAO;
    private CassandraMailboxPathDAOImpl mailboxPathDAOImpl;
    private CassandraMailboxPathV2DAO mailboxPathDAOV2;
    private CassandraIndexTableHandlerContract.Testee indexTableHandlerTestee;
    private CassandraSubscriptionMapper subscriptionMapper;
    private CassandraPerUserMaxQuotaManager maxQuotaManager;
    private ExecutorService executor;
    private CassandraACLMapper aclMapper;
    private CassandraMailboxPathRegisterMapper mailboxPathRegisterMapper;
    private CassandraMailboxMapper mailboxMapper;
    private CassandraCurrentQuotaManager currentQuotaManager;
    private SimpleMailbox mailbox;
    private CassandraId cassandraId;
    private CassandraAnnotationMapper annotationMapper;
    private CassandraAttachmentMapper attachmentMapper;
    private CassandraMailboxMapperFallbackContract.Testee mailboxMapperFallbackTestee;
    private CassandraAttachmentMessageIdDAO attachmentMessageIdDAO;
    private CassandraBlobsDAO blobsDAO;
    private CassandraAttachmentFallbackContract.Testee attachmentFallbackTestee;
    private CassandraMailboxMapperContract.Testee mailboxMapperTestee;
    private AttachmentMessageIdCreationContract.Testee attachmentMessageIdCreationTestee;
    private AttachmentV2MigrationContract.Testee attachmentVMigrationTestee;

    @BeforeAll
    static void setUpConnection(DockerCassandraExtension.DockerCassandra dockerCassandra) {
        cassandra = CassandraCluster.create(MODULES, dockerCassandra.getHost());
    }

    @BeforeEach
    void setUp() {
        uidProvider = new CassandraUidProvider(cassandra.getConf());
        modSeqProvider = new CassandraModSeqProvider(cassandra.getConf());
        cassandraMailboxDAO = new CassandraMailboxDAO(cassandra.getConf(), cassandra.getTypesProvider());
        applicableFlagDAO = new CassandraApplicableFlagDAO(cassandra.getConf());
        attachmentDAO = new CassandraAttachmentDAO(cassandra.getConf(),
            CassandraUtils.WITH_DEFAULT_CONFIGURATION,
            CassandraConfiguration.DEFAULT_CONFIGURATION);
        deletedMessageDAO = new CassandraDeletedMessageDAO(cassandra.getConf());
        firstUnseenDAO = new CassandraFirstUnseenDAO(cassandra.getConf());
        attachmentDAOV2 = new CassandraAttachmentDAOV2(CassandraAttachmentDAOV2Contract.BLOB_ID_FACTORY, cassandra.getConf());
        attachmentOwnerDAO = new CassandraAttachmentOwnerDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        mailboxCounterDAO = new CassandraMailboxCounterDAO(cassandra.getConf());
        userMailboxRightsDAO = new CassandraUserMailboxRightsDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        messageIdToImapUidDAO = new CassandraMessageIdToImapUidDAO(cassandra.getConf(), CassandraMessageIdToImapUidDAOContract.messageIdFactory);
        messageIdDAO = new CassandraMessageIdDAO(cassandra.getConf(), CassandraMessageIdDAOContract.messageIdFactory);
        blobsDAO = new CassandraBlobsDAO(cassandra.getConf());
        messageDAO = new CassandraMessageDAO(cassandra.getConf(), cassandra.getTypesProvider(), blobsDAO, BLOB_ID_FACTORY,
            CassandraUtils.WITH_DEFAULT_CONFIGURATION, new CassandraMessageId.Factory());
        mailboxRecentsDAO = new CassandraMailboxRecentsDAO(cassandra.getConf());
        mailboxPathDAOImpl = new CassandraMailboxPathDAOImpl(cassandra.getConf(), cassandra.getTypesProvider());
        mailboxPathDAOV2 = new CassandraMailboxPathV2DAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        indexTableHandlerTestee = new CassandraIndexTableHandlerContract.Testee(
            mailboxCounterDAO,
            mailboxRecentsDAO,
            applicableFlagDAO,
            firstUnseenDAO,
            new CassandraIndexTableHandler(mailboxRecentsDAO,
                mailboxCounterDAO,
                firstUnseenDAO,
                applicableFlagDAO,
                deletedMessageDAO),
            deletedMessageDAO);
        subscriptionMapper = new CassandraSubscriptionMapper(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        maxQuotaManager = new CassandraPerUserMaxQuotaManager(
            new CassandraPerUserMaxQuotaDao(cassandra.getConf()),
            new CassandraPerDomainMaxQuotaDao(cassandra.getConf()),
            new CassandraGlobalMaxQuotaDao(cassandra.getConf()));
        executor = Executors.newFixedThreadPool(2);
        aclMapper = new CassandraACLMapper(
            cassandra.getConf(),
            new CassandraUserMailboxRightsDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION),
            CassandraConfiguration.DEFAULT_CONFIGURATION);
        mailboxPathRegisterMapper = new CassandraMailboxPathRegisterMapper(cassandra().getConf(),
            cassandra().getTypesProvider(),
            CassandraUtils.WITH_DEFAULT_CONFIGURATION,
            CASSANDRA_TIME_OUT_IN_S);
        mailboxMapper = new CassandraMailboxMapper(
            cassandraMailboxDAO,
            mailboxPathDAOImpl,
            mailboxPathDAOV2,
            userMailboxRightsDAO,
            aclMapper);;
        currentQuotaManager = new CassandraCurrentQuotaManager(cassandra.getConf());
        mailbox = new SimpleMailbox(benwaInboxPath, MailboxMapperACLContract.UID_VALIDITY);
        mailbox.setMailboxId(CassandraId.timeBased());
        cassandraId = CassandraId.timeBased();
        annotationMapper = new CassandraAnnotationMapper(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        attachmentMessageIdDAO = new CassandraAttachmentMessageIdDAO(cassandra.getConf(), new CassandraMessageId.Factory(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        attachmentMapper = new CassandraAttachmentMapper(
            attachmentDAO,
            attachmentDAOV2,
            blobsDAO,
            attachmentMessageIdDAO,
            attachmentOwnerDAO);
        mailboxMapperFallbackTestee = new CassandraMailboxMapperFallbackContract.Testee(
            mailboxPathDAOImpl,
            mailboxPathDAOV2,
            mailboxMapper,
            cassandraMailboxDAO);
        attachmentFallbackTestee = new CassandraAttachmentFallbackContract.Testee(attachmentDAOV2, attachmentDAO, attachmentMapper, blobsDAO);        mailboxMapperTestee = new CassandraMailboxMapperContract.Testee(cassandraMailboxDAO, mailboxPathDAOImpl, mailboxPathDAOV2, mailboxMapper);
        mailboxMapperTestee = new CassandraMailboxMapperContract.Testee(cassandraMailboxDAO, mailboxPathDAOImpl, mailboxPathDAOV2, mailboxMapper);
        attachmentMessageIdCreationTestee = new AttachmentMessageIdCreationContract.Testee(
            blobsDAO, messageDAO, attachmentMessageIdDAO,
            new AttachmentMessageIdCreation(messageDAO, attachmentMessageIdDAO));
        attachmentVMigrationTestee = new AttachmentV2MigrationContract.Testee(attachmentDAO, attachmentDAOV2, blobsDAO,
            new AttachmentV2Migration(attachmentDAO, attachmentDAOV2, blobsDAO));
    }

    @AfterEach
    void tearDown() {
        new CassandraTableManager(MODULES, cassandra.getConf()).clearAllTables();
    }

    @AfterAll
    static void closeConnection() {
        cassandra.close();
    }

    @Override
    public CassandraUidProvider uidProvider() {
        return uidProvider;
    }

    @Override
    public CassandraModSeqProvider modSeqProvider() {
        return modSeqProvider;
    }

    @Override
    public CassandraMailboxDAO mailboxDAO() {
        return cassandraMailboxDAO;
    }

    @Override
    public CassandraApplicableFlagDAO applicableFlagsDAO() {
        return applicableFlagDAO;
    }

    @Override
    public CassandraAttachmentDAO attachmentDAO() {
        return attachmentDAO;
    }

    @Override
    public CassandraAttachmentDAOV2 attachmentDAOV2() {
        return attachmentDAOV2;
    }

    @Override
    public CassandraAttachmentOwnerDAO attachmentOwnerDAO() {
        return attachmentOwnerDAO;
    }

    @Override
    public CassandraDeletedMessageDAO deletedMessageDAO() {
        return deletedMessageDAO;
    }

    @Override
    public CassandraFirstUnseenDAO firstUnseenDAO() {
        return firstUnseenDAO;
    }

    @Override
    public CassandraMailboxCounterDAO mailboxCounterDAO() {
        return mailboxCounterDAO;
    }

    @Override
    public CassandraUserMailboxRightsDAO userMailboxRightsDAO() {
        return userMailboxRightsDAO;
    }

    @Override
    public CassandraMessageIdToImapUidDAO messageIdToImapUidDAO() {
        return messageIdToImapUidDAO;
    }

    @Override
    public CassandraMessageIdDAO messageIdDAO() {
        return messageIdDAO;
    }

    @Override
    public CassandraMessageDAO messageDAO() {
        return messageDAO;
    }

    @Override
    public CassandraMailboxRecentsDAO mailboxRecentsDAO() {
        return mailboxRecentsDAO;
    }

    @Override
    public CassandraMailboxPathDAOImpl mailboxPathDAOImpl() {
        return mailboxPathDAOImpl;
    }

    @Override
    public CassandraMailboxPathDAO mailboxPathDao() {
        return mailboxPathDAOV2;
    }

    @Override
    public CassandraIndexTableHandlerContract.Testee indexTableHandlerTestee() {
        return indexTableHandlerTestee;
    }

    @Override
    public SubscriptionMapper subscriptionMapper() {
        return subscriptionMapper;
    }

    @Override
    public MaxQuotaManager maxQuotaManager() {
        return maxQuotaManager;
    }

    @Override
    public ExecutorService executor() {
        return executor;
    }

    @Override
    public CassandraCluster cassandra() {
        return cassandra;
    }

    @Override
    public CassandraACLMapper aclMapper() {
        return aclMapper;
    }

    @Override
    public CassandraMailboxPathRegisterMapper mailboxPathRegisterMapper() {
        return mailboxPathRegisterMapper;
    }

    @Override
    public CassandraMailboxMapper mailboxMapper() {
        return mailboxMapper;
    }

    @Override
    public StoreCurrentQuotaManager currentQuotaManager() {
        return currentQuotaManager;
    }

    @Override
    public Mailbox mailbox() {
        return mailbox;
    }

    @Override
    public MailboxId mailboxId() {
        return cassandraId;
    }

    @Override
    public AnnotationMapper annotationMapper() {
        return annotationMapper;
    }

    @Override
    public AttachmentMapper attachmentMapper() {
        return attachmentMapper;
    }

    @Override
    public MessageId generateMessageId() {
        return new CassandraMessageId.Factory().generate();
    }

    @Override
    public CassandraMailboxMapperFallbackContract.Testee mailboxMapperFallbackTestee() {
        return mailboxMapperFallbackTestee;
    }

    @Override
    public CassandraAttachmentFallbackContract.Testee attachmentFallbackTestee() {
        return attachmentFallbackTestee;
    }

    @Override
    public CassandraMailboxMapperContract.Testee mailboxMapperTestee() {
        return mailboxMapperTestee;
    }

    @Override
    public AttachmentMessageIdCreationContract.Testee attachmentMessageIdCreationTestee() {
        return attachmentMessageIdCreationTestee;
    }

    @Override
    public AttachmentV2MigrationContract.Testee attachmentV2MigrationTestee() {
        return attachmentVMigrationTestee;
    }
}
