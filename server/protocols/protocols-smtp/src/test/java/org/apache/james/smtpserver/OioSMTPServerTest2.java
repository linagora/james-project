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
package org.apache.james.smtpserver;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.smtp.SMTPClient;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.api.mock.SimpleDomainList;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.filesystem.api.mock.MockFileSystem;
import org.apache.james.mailrepository.api.MailRepositoryStore;
import org.apache.james.mailrepository.mock.MockMailRepositoryStore;
import org.apache.james.metrics.api.Metric;
import org.apache.james.protocols.api.utils.ProtocolServerUtils;
import org.apache.james.protocols.lib.mock.MockProtocolHandlerLoader;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.queue.api.mock.MockMailQueueFactory;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.lib.Mappings;
import org.apache.james.smtpserver.netty.OioSMTPServer;
import org.apache.james.smtpserver.netty.SMTPServer;
import org.apache.james.smtpserver.netty.SmtpMetricsImpl;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.lib.mock.InMemoryUsersRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OioSMTPServerTest2 {

    private static class AlterableDNSServer implements DNSService {

        private InetAddress localhostByName = null;

        @Override
        public Collection<String> findMXRecords(String hostname) {
            List<String> res = new ArrayList<String>();
            if (hostname == null) {
                return res;
            }
            if ("james.apache.org".equals(hostname)) {
                res.add("nagoya.apache.org");
            }
            return res;
        }

        @Override
        public InetAddress[] getAllByName(String host) throws UnknownHostException {
            return new InetAddress[]{getByName(host)};
        }

        @Override
        public InetAddress getByName(String host) throws UnknownHostException {
            if (getLocalhostByName() != null) {
                if ("127.0.0.1".equals(host)) {
                    return getLocalhostByName();
                }
            }

            if ("1.0.0.127.bl.spamcop.net.".equals(host)) {
                return InetAddress.getByName("localhost");
            }

            if ("james.apache.org".equals(host)) {
                return InetAddress.getByName("james.apache.org");
            }

            if ("abgsfe3rsf.de".equals(host)) {
                throw new UnknownHostException();
            }

            if ("128.0.0.1".equals(host) || "192.168.0.1".equals(host) || "127.0.0.1".equals(host) || "127.0.0.0".equals(
                    host) || "255.0.0.0".equals(host) || "255.255.255.255".equals(host)) {
                return InetAddress.getByName(host);
            }

            throw new UnsupportedOperationException("getByName not implemented in mock for host: " + host);
        }

        @Override
        public Collection<String> findTXTRecords(String hostname) {
            List<String> res = new ArrayList<String>();
            if (hostname == null) {
                return res;
            }

            if ("2.0.0.127.bl.spamcop.net.".equals(hostname)) {
                res.add("Blocked - see http://www.spamcop.net/bl.shtml?127.0.0.2");
            }
            return res;
        }

        public InetAddress getLocalhostByName() {
            return localhostByName;
        }

        @Override
        public String getHostName(InetAddress addr) {
            return addr.getHostName();
        }

        @Override
        public InetAddress getLocalHost() throws UnknownHostException {
            return InetAddress.getLocalHost();
        }
    }

    private static final long HALF_SECOND = 500;
    private static final int MAX_ITERATIONS = 10;

    private SMTPTestConfiguration smtpConfiguration;
    private final InMemoryUsersRepository usersRepository = new InMemoryUsersRepository();
    private AlterableDNSServer dnsServer;
    private MockMailRepositoryStore store;
    private MockFileSystem fileSystem;
    private MockProtocolHandlerLoader chain;
    private MockMailQueueFactory queueFactory;

    private SMTPServer smtpServer;

    @Before
    public void setUp() throws Exception {
        setUpFakeLoader();
        // slf4j can't set programmatically any log level. It's just a facade
        // log.setLevel(SimpleLog.LOG_LEVEL_ALL);
        smtpConfiguration = new SMTPTestConfiguration();
        setUpSMTPServer();
    }

    @After
    public void tearDown() throws Exception {
        smtpServer.destroy();
    }

    private SMTPServer createSMTPServer(SmtpMetricsImpl smtpMetrics) {
        return new OioSMTPServer(smtpMetrics);
    }

    private void setUpSMTPServer() {
        Logger log = LoggerFactory.getLogger("SMTP");
        // slf4j can't set programmatically any log level. It's just a facade
        // log.setLevel(SimpleLog.LOG_LEVEL_ALL);
        SmtpMetricsImpl smtpMetrics = mock(SmtpMetricsImpl.class);
        when(smtpMetrics.getCommandsMetric()).thenReturn(mock(Metric.class));
        when(smtpMetrics.getConnectionMetric()).thenReturn(mock(Metric.class));
        smtpServer = createSMTPServer(smtpMetrics);
        smtpServer.setDnsService(dnsServer);
        smtpServer.setFileSystem(fileSystem);
        smtpServer.setProtocolHandlerLoader(chain);
        smtpServer.setLog(log);

    }

    private void init(SMTPTestConfiguration testConfiguration) throws Exception {
        testConfiguration.init();
        initSMTPServer(testConfiguration);
        // m_mailServer.setMaxMessageSizeBytes(m_testConfiguration.getMaxMessageSize() * 1024);
    }

    private void initSMTPServer(SMTPTestConfiguration testConfiguration) throws Exception {
        smtpServer.configure(testConfiguration);
        smtpServer.init();
    }

    private void setUpFakeLoader() {

        chain = new MockProtocolHandlerLoader();
    
        chain.put("usersrepository", UsersRepository.class, usersRepository);
    
        dnsServer = new AlterableDNSServer();
        chain.put("dnsservice", DNSService.class, dnsServer);
    
        store = new MockMailRepositoryStore();
        chain.put("mailStore", MailRepositoryStore.class, store);
        fileSystem = new MockFileSystem();
    
        chain.put("fileSystem", FileSystem.class, fileSystem);
    
        chain.put("recipientrewritetable", RecipientRewriteTable.class, new RecipientRewriteTable() {
    
            @Override
            public void addRegexMapping(String user, String domain, String regex) throws RecipientRewriteTableException {
                throw new UnsupportedOperationException("Not implemented");
            }
    
            @Override
            public void removeRegexMapping(String user, String domain, String regex) throws
                    RecipientRewriteTableException {
                throw new UnsupportedOperationException("Not implemented");
            }
    
            @Override
            public void addAddressMapping(String user, String domain, String address) throws
                    RecipientRewriteTableException {
                throw new UnsupportedOperationException("Not implemented");
            }
    
            @Override
            public void removeAddressMapping(String user, String domain, String address) throws
                    RecipientRewriteTableException {
                throw new UnsupportedOperationException("Not implemented");
            }
    
            @Override
            public void addErrorMapping(String user, String domain, String error) throws RecipientRewriteTableException {
                throw new UnsupportedOperationException("Not implemented");
            }
    
            @Override
            public void removeErrorMapping(String user, String domain, String error) throws
                    RecipientRewriteTableException {
                throw new UnsupportedOperationException("Not implemented");
            }
    
            @Override
            public Mappings getUserDomainMappings(String user, String domain) throws
                    RecipientRewriteTableException {
                throw new UnsupportedOperationException("Not implemented");
            }
    
            @Override
            public void addMapping(String user, String domain, String mapping) throws RecipientRewriteTableException {
                throw new UnsupportedOperationException("Not implemented");
            }
    
            @Override
            public void removeMapping(String user, String domain, String mapping) throws RecipientRewriteTableException {
                throw new UnsupportedOperationException("Not implemented");
            }
    
            @Override
            public Map<String, Mappings> getAllMappings() throws RecipientRewriteTableException {
                throw new UnsupportedOperationException("Not implemented");
            }
    
            @Override
            public void addAliasDomainMapping(String aliasDomain, String realDomain) throws
                    RecipientRewriteTableException {
                throw new UnsupportedOperationException("Not implemented");
            }
    
            @Override
            public void removeAliasDomainMapping(String aliasDomain, String realDomain) throws
                    RecipientRewriteTableException {
                throw new UnsupportedOperationException("Not implemented");
            }
    
            @Override
            public Mappings getMappings(String user, String domain) throws ErrorMappingException,
                    RecipientRewriteTableException {
                throw new UnsupportedOperationException("Not implemented");
            }
        });
    
        queueFactory = new MockMailQueueFactory();
        chain.put("mailqueuefactory", MailQueueFactory.class, queueFactory);
        chain.put("domainlist", DomainList.class, new SimpleDomainList() {
    
            @Override
            public boolean containsDomain(String serverName) {
                return "localhost".equals(serverName);
            }
        });
        
    }

    @Test
    public void testConnectionLimit() throws Exception {
        smtpConfiguration.setConnectionLimit(2);
        init(smtpConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        InetSocketAddress bindedAddress = new ProtocolServerUtils(smtpServer).retrieveBindedAddress();
        smtpProtocol.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());
        SMTPClient smtpProtocol2 = new SMTPClient();
        smtpProtocol2.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());

        SMTPClient smtpProtocol3 = new SMTPClient();

        try {
            smtpProtocol3.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());
            Thread.sleep(3000);
            fail("Shold disconnect connection 3");
        } catch (Exception e) {
        }

        ensureIsDisconnected(smtpProtocol);
        ensureIsDisconnected(smtpProtocol2);

        smtpProtocol3.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());
        Thread.sleep(3000);

    }

    private void ensureIsDisconnected(SMTPClient client) throws IOException, InterruptedException {
        int initialConnections = smtpServer.getCurrentConnections();
        client.quit();
        client.disconnect();
        assertIsDisconnected(initialConnections);
    }

    private void assertIsDisconnected(int initialConnections) throws InterruptedException {
        int iterations = 0;
        while (smtpServer.getCurrentConnections() >= initialConnections && iterations++ < MAX_ITERATIONS) {
            Thread.sleep(HALF_SECOND);
        }
    }

    @Test
    public void test1() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test2() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test3() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test4() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test5() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test6() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test7() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test8() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test9() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test10() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test11() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test12() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test13() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test14() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test15() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test16() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test17() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test18() throws Exception {
        testConnectionLimit();
    }

    @Test
    public void test19() throws Exception {
        testConnectionLimit();
    }
}
