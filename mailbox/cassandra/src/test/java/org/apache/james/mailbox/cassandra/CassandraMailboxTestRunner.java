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
import org.apache.james.backends.cassandra.CassandraTestRunner;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.backends.cassandra.init.configuration.CassandraConfiguration;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.blob.cassandra.CassandraBlobId;
import org.apache.james.blob.cassandra.CassandraBlobModule;
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
import org.apache.james.mailbox.cassandra.mail.migration.AttachmentMessageIdCreation;
import org.apache.james.mailbox.cassandra.mail.migration.AttachmentMessageIdCreationContract;
import org.apache.james.mailbox.cassandra.mail.migration.AttachmentV2Migration;
import org.apache.james.mailbox.cassandra.mail.migration.AttachmentV2MigrationContract;
import org.apache.james.mailbox.cassandra.mail.migration.CassandraMailboxMapperContract;
import org.apache.james.mailbox.cassandra.mail.utils.GuiceUtils;
import org.apache.james.mailbox.cassandra.modules.CassandraAclModule;
import org.apache.james.mailbox.cassandra.modules.CassandraAnnotationModule;
import org.apache.james.mailbox.cassandra.modules.CassandraApplicableFlagsModule;
import org.apache.james.mailbox.cassandra.modules.CassandraAttachmentModule;
import org.apache.james.mailbox.cassandra.modules.CassandraDeletedMessageModule;
import org.apache.james.mailbox.cassandra.modules.CassandraFirstUnseenModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxCounterModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxRecentsModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMessageModule;
import org.apache.james.mailbox.cassandra.modules.CassandraModSeqModule;
import org.apache.james.mailbox.cassandra.modules.CassandraQuotaModule;
import org.apache.james.mailbox.cassandra.modules.CassandraRegistrationModule;
import org.apache.james.mailbox.cassandra.modules.CassandraSubscriptionModule;
import org.apache.james.mailbox.cassandra.modules.CassandraUidModule;
import org.apache.james.mailbox.cassandra.quota.CassandraCurrentQuotaManager;
import org.apache.james.mailbox.cassandra.quota.CassandraPerUserMaxQuotaManager;
import org.apache.james.mailbox.cassandra.user.CassandraSubscriptionMapper;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.AnnotationMapper;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.MailboxMapper;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

class CassandraMailboxTestRunner extends CassandraTestRunner {
    @Nested
    class ApplicableFlagDAOTest extends Runner implements CassandraApplicableFlagDAOContract {
        private CassandraApplicableFlagDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraApplicableFlagsModule();
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraApplicableFlagDAO(cassandra.getConf());
        }

        @Override
        public CassandraApplicableFlagDAO testee() {
            return testee;
        }
    }

    @Nested
    class AttachmentDAOTest extends Runner implements CassandraAttachmentDAOContract {
        private CassandraAttachmentDAO testee;

        @Override
        public CassandraModule module() {
            return CassandraAttachmentModule.CASSANDRA_ATTACHMENT_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraAttachmentDAO(cassandra.getConf(),
                CassandraUtils.WITH_DEFAULT_CONFIGURATION,
                CassandraConfiguration.DEFAULT_CONFIGURATION);
        }

        @Override
        public CassandraAttachmentDAO testee() {
            return testee;
        }
    }

    @Nested
    class AttachmentDAOV2Test extends Runner implements CassandraAttachmentDAOV2Contract {
        private CassandraAttachmentDAOV2 testee;

        @Override
        public CassandraModule module() {
            return CassandraAttachmentModule.CASSANDRA_ATTACHMENT_V2_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraAttachmentDAOV2(BLOB_ID_FACTORY, cassandra.getConf());
        }

        @Override
        public CassandraAttachmentDAOV2 testee() {
            return testee;
        }
    }

    @Nested
    class AttachmentOwnerDAOTest extends Runner implements CassandraAttachmentOwnerDAOContract {
        private CassandraAttachmentOwnerDAO testee;

        @Override
        public CassandraModule module() {
            return CassandraAttachmentModule.CASSANDRA_ATTACHMENT_OWNER_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraAttachmentOwnerDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        }

        @Override
        public CassandraAttachmentOwnerDAO testee() {
            return testee;
        }
    }

    @Nested
    class DeletedMessageDAOTest extends Runner implements CassandraDeletedMessageDAOContract {
        private CassandraDeletedMessageDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraDeletedMessageModule();
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraDeletedMessageDAO(cassandra.getConf());
        }

        @Override
        public CassandraDeletedMessageDAO testee() {
            return testee;
        }
    }

    @Nested
    class FirstUnseenDAOTest extends Runner implements CassandraFirstUnseenDAOContract {
        private CassandraFirstUnseenDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraFirstUnseenModule();
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraFirstUnseenDAO(cassandra.getConf());
        }

        @Override
        public CassandraFirstUnseenDAO testee() {
            return testee;
        }
    }

    @Nested
    class MailboxCounterDAOTest extends Runner implements CassandraMailboxCounterDAOContract {
        private CassandraMailboxCounterDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraMailboxCounterModule();
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailboxCounterDAO(cassandra.getConf());
        }

        @Override
        public CassandraMailboxCounterDAO testee() {
            return testee;
        }
    }

    @Nested
    class MailboxDAOTest extends Runner implements CassandraMailboxDAOContract {
        private CassandraMailboxDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                CassandraMailboxModule.MAILBOX_BASE_TYPE,
                CassandraMailboxModule.MAILBOX_TABLE);
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailboxDAO(cassandra.getConf(), cassandra.getTypesProvider());
        }

        @Override
        public CassandraMailboxDAO testee() {
            return testee;
        }

    }

    @Nested
    class UserMailboxRightsDAOTest extends Runner implements CassandraUserMailboxRightsDAOContract {
        private CassandraUserMailboxRightsDAO testee;

        @Override
        public CassandraModule module() {
            return CassandraAclModule.CASSANDRA_USER_MAILBOX_RIGHTS_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraUserMailboxRightsDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        }

        @Override
        public CassandraUserMailboxRightsDAO testee() {
            return testee;
        }
    }

    @Nested
    class UidProviderTest extends Runner implements CassandraUidProviderContract {
        private CassandraUidProvider uidProvider;

        @Override
        public CassandraModule module() {
            return new CassandraUidModule();
        }

        @BeforeEach
        void setUp() {
            uidProvider = new CassandraUidProvider(cassandra.getConf());
        }

        @Override
        public CassandraUidProvider testee() {
            return uidProvider;
        }
    }

    @Nested
    class ModSedProviderTest extends Runner implements CassandraModSeqProviderContract {
        private CassandraModSeqProvider modSeqProvider;

        @Override
        public CassandraModule module() {
            return new CassandraModSeqModule();
        }

        @BeforeEach
        void setUp() {
            modSeqProvider = new CassandraModSeqProvider(cassandra.getConf());
        }

        @Override
        public CassandraModSeqProvider testee() {
            return modSeqProvider;
        }
    }

    @Nested
    class MessageIdToImapUidDAOTest extends Runner implements CassandraMessageIdToImapUidDAOContract {
        private CassandraMessageIdToImapUidDAO testee;

        @Override
        public CassandraModule module() {
            return CassandraMessageModule.MESSAGE_ID_TO_IMAP_UID_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMessageIdToImapUidDAO(cassandra.getConf(), messageIdFactory);
        }

        @Override
        public CassandraMessageIdToImapUidDAO testee() {
            return testee;
        }
    }

    @Nested
    class MessageIdDAOTest extends Runner implements CassandraMessageIdDAOContract {
        private CassandraMessageIdDAO testee;

        @Override
        public CassandraModule module() {
            return CassandraMessageModule.CASSANDRA_MESSAGE_ID_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMessageIdDAO(cassandra.getConf(), messageIdFactory);
        }

        @Override
        public CassandraMessageIdDAO testee() {
            return testee;
        }
    }

    @Nested
    class MessageDAOTest extends Runner implements CassandraMessageDAOContract {
        private CassandraMessageDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(CassandraMessageModule.ATTACHMENT_TYPE,
                CassandraMessageModule.PROPERTY_TYPE,
                CassandraMessageModule.CASSANDRA_MESSAGE_V2_TABLE,
                new CassandraBlobModule());
        }

        @BeforeEach
        void setUp() {
            CassandraBlobsDAO blobsDAO = new CassandraBlobsDAO(cassandra.getConf());
            CassandraBlobId.Factory blobIdFactory = new CassandraBlobId.Factory();
            testee = new CassandraMessageDAO(cassandra.getConf(), cassandra.getTypesProvider(), blobsDAO, blobIdFactory,
                CassandraUtils.WITH_DEFAULT_CONFIGURATION, new CassandraMessageId.Factory());
        }

        @Override
        public CassandraMessageDAO testee() {
            return testee;
        }
    }

    @Nested
    class MailboxRecentDAOTest extends Runner implements CassandraMailboxRecentDAOContract {
        private CassandraMailboxRecentsDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraMailboxRecentsModule();
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailboxRecentsDAO(cassandra.getConf());
        }

        @Override
        public CassandraMailboxRecentsDAO testee() {
            return testee;
        }
    }

    @Nested
    class MailboxPathDAOImplTest extends Runner implements CassandraMailboxPathDAOImplContract {
        protected CassandraMailboxPathDAOImpl testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                CassandraMailboxModule.MAILBOX_PATH_TABLE,
                CassandraMailboxModule.MAILBOX_BASE_TYPE);
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailboxPathDAOImpl(cassandra.getConf(), cassandra.getTypesProvider());
        }

        @Override
        public CassandraMailboxPathDAOImpl testee() {
            return testee;
        }
    }

    @Nested
    class MailboxPathDAOV2Test extends Runner implements CassandraMailboxPathDAOContract {
        protected CassandraMailboxPathV2DAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                CassandraMailboxModule.MAILBOX_PATH_V2_TABLE,
                CassandraMailboxModule.MAILBOX_BASE_TYPE);
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailboxPathV2DAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        }

        @Override
        public CassandraMailboxPathV2DAO testee() {
            return testee;
        }
    }

    @Nested
    class MailboxMapperConcurrencyTest extends Runner implements CassandraMailboxMapperConcurrencyContract {
        private CassandraMailboxMapper testee;

        @Override
        public CassandraModule module() {
            return  new CassandraModuleComposite(new CassandraMailboxModule(), new CassandraAclModule());
        }

        @BeforeEach
        void setUp() {
            testee = GuiceUtils.testInjector(cassandra)
                .getInstance(CassandraMailboxMapper.class);
        }

        @Override
        public CassandraMailboxMapper testee() {
            return testee;
        }
    }

    @Nested
    class MailboxMapperTest extends Runner implements org.apache.james.mailbox.cassandra.mail.CassandraMailboxMapperContract {

        private Testee testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(new CassandraMailboxModule(), new CassandraAclModule());
        }

        @BeforeEach
        void setUp() {
            CassandraUserMailboxRightsDAO userMailboxRightsDAO = new CassandraUserMailboxRightsDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);

            CassandraMailboxDAO mailboxDAO = new CassandraMailboxDAO(cassandra.getConf(), cassandra.getTypesProvider());
            CassandraMailboxPathDAOImpl mailboxPathDAO = new CassandraMailboxPathDAOImpl(cassandra.getConf(), cassandra.getTypesProvider());
            CassandraMailboxPathV2DAO mailboxPathV2DAO = new CassandraMailboxPathV2DAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);

            CassandraMailboxMapper mailboxMapper = new CassandraMailboxMapper(
                mailboxDAO,
                mailboxPathDAO,
                mailboxPathV2DAO,
                userMailboxRightsDAO,
                new CassandraACLMapper(cassandra.getConf(),
                    new CassandraUserMailboxRightsDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION),
                    CassandraConfiguration.DEFAULT_CONFIGURATION));

            testee = new Testee(
                mailboxDAO,
                mailboxPathDAO,
                mailboxPathV2DAO,
                mailboxMapper);
        }

        @Override
        public Testee testee() {
            return testee;
        }

    }

    @Nested
    class ACLMapperTest extends Runner implements CassandraACLMapperContract {

        private CassandraACLMapper testee;
        private ExecutorService executor;

        @Override
        public CassandraModule module() {
            return new CassandraAclModule();
        }

        @BeforeEach
        void setUp() {
            testee = GuiceUtils.testInjector(cassandra())
                .getInstance(CassandraACLMapper.class);
            executor = Executors.newFixedThreadPool(2);
        }

        @AfterEach
        void tearDown() {
            executor.shutdownNow();
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
        public CassandraACLMapper testee() {
            return testee;
        }
    }

    @Nested
    class MailboxPathRegisterMapperTest extends Runner implements CassandraMailboxPathRegistrerMapperContract {
        private CassandraMailboxPathRegisterMapper testee;

        @Override
        public CassandraModule module() {
            return new CassandraRegistrationModule();
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailboxPathRegisterMapper(cassandra().getConf(),
                cassandra().getTypesProvider(),
                CassandraUtils.WITH_DEFAULT_CONFIGURATION,
                CASSANDRA_TIME_OUT_IN_S);
        }

        @Override
        public CassandraCluster cassandra() {
            return cassandra;
        }

        @Override
        public CassandraMailboxPathRegisterMapper testee() {
            return testee;
        }
    }

    @Nested
    class IndexTableHandlerTest extends Runner implements CassandraIndexTableHandlerContract {
        private Testee testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                new CassandraMailboxCounterModule(),
                new CassandraMailboxRecentsModule(),
                new CassandraFirstUnseenModule(),
                new CassandraApplicableFlagsModule(),
                new CassandraDeletedMessageModule());
        }

        @BeforeEach
        void setUp() {
            CassandraMailboxCounterDAO mailboxCounterDAO = new CassandraMailboxCounterDAO(cassandra.getConf());
            CassandraMailboxRecentsDAO mailboxRecentsDAO = new CassandraMailboxRecentsDAO(cassandra.getConf());
            CassandraFirstUnseenDAO firstUnseenDAO = new CassandraFirstUnseenDAO(cassandra.getConf());
            CassandraApplicableFlagDAO applicableFlagDAO = new CassandraApplicableFlagDAO(cassandra.getConf());
            CassandraDeletedMessageDAO deletedMessageDAO = new CassandraDeletedMessageDAO(cassandra.getConf());

            testee = new Testee(
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
        }

        @Override
        public Testee testee() {
            return testee;
        }
    }

    @Nested
    class AttachmentMapperTestRunner extends Runner implements AttachmentMapperContract {
        private AttachmentMapper testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                new CassandraAttachmentModule(),
                new CassandraBlobModule());
        }

        @BeforeEach
        void setUp() {
            testee = GuiceUtils.testInjector(cassandra)
                .getInstance(CassandraAttachmentMapper.class);
        }

        @Override
        public AttachmentMapper testee() {
            return testee;
        }

        @Override
        public MessageId generateMessageId() {
            return new CassandraMessageId.Factory().generate();
        }
    }

    @Nested
    class AnnotationMapperTestRunner extends Runner implements AnnotationMapperContract {
        private AnnotationMapper testee;
        private CassandraId cassandraId;

        @Override
        public CassandraModule module() {
            return new CassandraAnnotationModule();
        }

        @BeforeEach
        void setUp() {
            testee = GuiceUtils.testInjector(cassandra)
                .getInstance(CassandraAnnotationMapper.class);
            cassandraId = CassandraId.timeBased();
        }

        @Override
        public AnnotationMapper testee() {
            return testee;
        }

        @Override
        public MailboxId mailboxId() {
            return cassandraId;
        }
    }

    @Nested
    class SubscriptionMapperTestRunner extends Runner implements SubscriptionMapperContract {
        private SubscriptionMapper testee;

        @Override
        public CassandraModule module() {
            return new CassandraSubscriptionModule();
        }

        @BeforeEach
        void setUp() {
            testee = GuiceUtils.testInjector(cassandra)
                .getInstance(CassandraSubscriptionMapper.class);
        }

        @Override
        public SubscriptionMapper testee() {
            return testee;
        }

    }

    @Nested
    class MailboxMapperAclTestRunner extends Runner implements MailboxMapperACLContract {
        private MailboxMapper testee;
        private SimpleMailbox mailbox;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                new CassandraAclModule(),
                new CassandraMailboxModule());
        }

        @BeforeEach
        void setUp() throws MailboxException {
            testee = GuiceUtils.testInjector(cassandra)
                .getInstance(CassandraMailboxMapper.class);
            mailbox = new SimpleMailbox(benwaInboxPath, UID_VALIDITY);
            mailbox.setMailboxId(CassandraId.timeBased());
            testee().save(mailbox());
        }

        @Override
        public MailboxMapper testee() {
            return testee;
        }

        @Override
        public Mailbox mailbox() {
            return mailbox;
        }
    }

    @Nested
    class AttachmentMessageIdCreationTestRunner extends Runner implements AttachmentMessageIdCreationContract {
        private AttachmentMessageIdCreationContract.Testee testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                new CassandraMessageModule(),
                new CassandraAttachmentModule(),
                new CassandraBlobModule());
        }

        @BeforeEach
        void setUp() {
            CassandraBlobsDAO blobsDAO = new CassandraBlobsDAO(cassandra.getConf());
            CassandraMessageDAO cassandraMessageDAO = new CassandraMessageDAO(cassandra.getConf(), cassandra.getTypesProvider(),
                blobsDAO, new CassandraBlobId.Factory(), CassandraUtils.WITH_DEFAULT_CONFIGURATION, messageIdFactory);

            CassandraAttachmentMessageIdDAO attachmentMessageIdDAO = new CassandraAttachmentMessageIdDAO(cassandra.getConf(),
                new CassandraMessageId.Factory(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);

            AttachmentMessageIdCreation migration = new AttachmentMessageIdCreation(cassandraMessageDAO, attachmentMessageIdDAO);

            testee = new AttachmentMessageIdCreationContract.Testee(blobsDAO, cassandraMessageDAO, attachmentMessageIdDAO, migration);
        }

        @Override
        public AttachmentMessageIdCreationContract.Testee testee() {
            return testee;
        }
    }

    @Nested
    class AttachmentV2MigrationTestRunner extends Runner implements AttachmentV2MigrationContract {
        private AttachmentV2MigrationContract.Testee testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                new CassandraAttachmentModule(),
                new CassandraBlobModule());
        }

        @BeforeEach
        void setUp() {
            CassandraAttachmentDAO attachmentDAO = new CassandraAttachmentDAO(cassandra.getConf(),
                CassandraUtils.WITH_DEFAULT_CONFIGURATION,
                CassandraConfiguration.DEFAULT_CONFIGURATION);
            CassandraAttachmentDAOV2 attachmentDAOV2 = new CassandraAttachmentDAOV2(BLOB_ID_FACTORY, cassandra.getConf());
            CassandraBlobsDAO blobsDAO = new CassandraBlobsDAO(cassandra.getConf());
            AttachmentV2Migration migration = new AttachmentV2Migration(attachmentDAO, attachmentDAOV2, blobsDAO);

            testee = new AttachmentV2MigrationContract.Testee(attachmentDAO, attachmentDAOV2, blobsDAO, migration);

        }

        @Override
        public AttachmentV2MigrationContract.Testee testee() {
            return testee;
        }
    }

    @Nested
    class MailboxPathV2MigrationTestRunner extends Runner implements CassandraMailboxMapperContract {
        private CassandraMailboxMapperContract.Testee testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                new CassandraMailboxModule(),
                new CassandraAclModule());
        }

        @BeforeEach
        void setUp() {
            CassandraMailboxPathDAOImpl daoV1 = new CassandraMailboxPathDAOImpl(
                cassandra.getConf(),
                cassandra.getTypesProvider(),
                CassandraUtils.WITH_DEFAULT_CONFIGURATION);
            CassandraMailboxPathV2DAO daoV2 = new CassandraMailboxPathV2DAO(
                cassandra.getConf(),
                CassandraUtils.WITH_DEFAULT_CONFIGURATION);

            CassandraUserMailboxRightsDAO userMailboxRightsDAO = new CassandraUserMailboxRightsDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
            CassandraMailboxDAO mailboxDAO = new CassandraMailboxDAO(cassandra.getConf(), cassandra.getTypesProvider());
            CassandraMailboxMapper mailboxMapper = new CassandraMailboxMapper(
                mailboxDAO,
                daoV1,
                daoV2,
                userMailboxRightsDAO,
                new CassandraACLMapper(cassandra.getConf(), userMailboxRightsDAO, CassandraConfiguration.DEFAULT_CONFIGURATION));

            testee = new Testee(daoV1, daoV2, mailboxMapper, mailboxDAO);
        }

        @Override
        public CassandraMailboxMapperContract.Testee testee() {
            return testee;
        }
    }

    @Nested
    class AttachmentFallbackTest extends Runner implements CassandraAttachmentFallbackContract {
        private CassandraAttachmentFallbackContract.Testee testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                new CassandraAttachmentModule(),
                new CassandraBlobModule());
        }

        @BeforeEach
        void setUp() {
            CassandraAttachmentDAOV2 attachmentDAOV2 = new CassandraAttachmentDAOV2(BLOB_ID_FACTORY, cassandra.getConf());
            CassandraAttachmentDAO attachmentDAO = new CassandraAttachmentDAO(cassandra.getConf(),
                CassandraUtils.WITH_DEFAULT_CONFIGURATION,
                CassandraConfiguration.DEFAULT_CONFIGURATION);
            CassandraBlobsDAO blobsDAO = new CassandraBlobsDAO(cassandra.getConf());
            CassandraAttachmentMessageIdDAO attachmentMessageIdDAO = new CassandraAttachmentMessageIdDAO(cassandra.getConf(), new CassandraMessageId.Factory(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
            CassandraAttachmentOwnerDAO ownerDAO = new CassandraAttachmentOwnerDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
            CassandraAttachmentMapper attachmentMapper = new CassandraAttachmentMapper(attachmentDAO, attachmentDAOV2, blobsDAO, attachmentMessageIdDAO, ownerDAO);

            testee = new Testee(attachmentDAOV2, attachmentDAO, attachmentMapper, blobsDAO);
        }

        @Override
        public CassandraAttachmentFallbackContract.Testee testee() {
            return testee;
        }
    }

    @Nested
    class GenericMaxQuotaManagerTestRunner extends Runner implements GenericMaxQuotaManagerTest {
        private CassandraPerUserMaxQuotaManager testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                CassandraQuotaModule.MAX_QUOTA_TABLE,
                CassandraQuotaModule.DOMAIN_QUOTA_TABLE,
                CassandraQuotaModule.USER_QUOTA_TABLE);
        }

        @BeforeEach
        void setUp() {
            testee = GuiceUtils.testInjector(cassandra)
                .getInstance(CassandraPerUserMaxQuotaManager.class);
        }

        @Override
        public CassandraPerUserMaxQuotaManager testee() {
            return testee;
        }
    }

    @Nested
    class StoreCurrentQuotaManagerTestRunner extends Runner implements StoreCurrentQuotaManagerContract {
        private StoreCurrentQuotaManager testee;

        @Override
        public CassandraModule module() {
            return CassandraQuotaModule.CURRENT_QUOTA_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraCurrentQuotaManager(cassandra.getConf());
        }

        @Override
        public StoreCurrentQuotaManager testee() {
            return testee;
        }
    }
}
