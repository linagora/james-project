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

package org.apache.james.mailets.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;

public class SmtpConfigurationTest {
    @Test
    public void defaultSmtpConfigurationAcceptsAllHosts() throws IOException {
        assertThat(SmtpConfiguration.DEFAULT.serializeAsXml())
            .isEqualTo("<?xml version=\"1.0\"?>\n" +
                "\n" +
                "<!--\n" +
                "  Licensed to the Apache Software Foundation (ASF) under one\n" +
                "  or more contributor license agreements.  See the NOTICE file\n" +
                "  distributed with this work for additional information\n" +
                "  regarding copyright ownership.  The ASF licenses this file\n" +
                "  to you under the Apache License, Version 2.0 (the\n" +
                "  \"License\"); you may not use this file except in compliance\n" +
                "  with the License.  You may obtain a copy of the License at\n" +
                "\n" +
                "    http://www.apache.org/licenses/LICENSE-2.0\n" +
                "\n" +
                "  Unless required by applicable law or agreed to in writing,\n" +
                "  software distributed under the License is distributed on an\n" +
                "  \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
                "  KIND, either express or implied.  See the License for the\n" +
                "  specific language governing permissions and limitations\n" +
                "  under the License.\n" +
                " -->\n" +
                "\n" +
                "<smtpservers>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-global</jmxName>\n" +
                "        <bind>0.0.0.0:1025</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <authRequired>false</authRequired>\n" +
                "        <verifyIdentity>true</verifyIdentity>\n" +
                "        <maxmessagesize>0</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>true</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-TLS</jmxName>\n" +
                "        <bind>0.0.0.0:10465</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <!--\n" +
                "           Authorize only local users\n" +
                "        -->\n" +
                "        <authRequired>true</authRequired>\n" +
                "        <!-- Trust authenticated users -->\n" +
                "        <verifyIdentity>false</verifyIdentity>\n" +
                "        <maxmessagesize>0</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>true</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-authenticated</jmxName>\n" +
                "        <bind>0.0.0.0:1587</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <!--\n" +
                "           Authorize only local users\n" +
                "        -->\n" +
                "        <authRequired>true</authRequired>\n" +
                "        <!-- Trust authenticated users -->\n" +
                "        <verifyIdentity>false</verifyIdentity>\n" +
                "        <maxmessagesize>0</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>true</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "</smtpservers>\n\n\n");
    }


    @Test
    public void smtpBracketEnforcementCanBeCustomized() throws IOException {
        assertThat(SmtpConfiguration.builder()
            .doNotRequireBracketEnforcement()
            .build().serializeAsXml())
            .isEqualTo("<?xml version=\"1.0\"?>\n" +
                "\n" +
                "<!--\n" +
                "  Licensed to the Apache Software Foundation (ASF) under one\n" +
                "  or more contributor license agreements.  See the NOTICE file\n" +
                "  distributed with this work for additional information\n" +
                "  regarding copyright ownership.  The ASF licenses this file\n" +
                "  to you under the Apache License, Version 2.0 (the\n" +
                "  \"License\"); you may not use this file except in compliance\n" +
                "  with the License.  You may obtain a copy of the License at\n" +
                "\n" +
                "    http://www.apache.org/licenses/LICENSE-2.0\n" +
                "\n" +
                "  Unless required by applicable law or agreed to in writing,\n" +
                "  software distributed under the License is distributed on an\n" +
                "  \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
                "  KIND, either express or implied.  See the License for the\n" +
                "  specific language governing permissions and limitations\n" +
                "  under the License.\n" +
                " -->\n" +
                "\n" +
                "<smtpservers>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-global</jmxName>\n" +
                "        <bind>0.0.0.0:1025</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <authRequired>false</authRequired>\n" +
                "        <verifyIdentity>true</verifyIdentity>\n" +
                "        <maxmessagesize>0</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>false</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-TLS</jmxName>\n" +
                "        <bind>0.0.0.0:10465</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <!--\n" +
                "           Authorize only local users\n" +
                "        -->\n" +
                "        <authRequired>true</authRequired>\n" +
                "        <!-- Trust authenticated users -->\n" +
                "        <verifyIdentity>false</verifyIdentity>\n" +
                "        <maxmessagesize>0</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>false</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-authenticated</jmxName>\n" +
                "        <bind>0.0.0.0:1587</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <!--\n" +
                "           Authorize only local users\n" +
                "        -->\n" +
                "        <authRequired>true</authRequired>\n" +
                "        <!-- Trust authenticated users -->\n" +
                "        <verifyIdentity>false</verifyIdentity>\n" +
                "        <maxmessagesize>0</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>false</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "</smtpservers>\n\n\n");
    }

    @Test
    public void smtpMessageSizeCanBeCustomized() throws IOException {
        assertThat(SmtpConfiguration.builder()
            .withMaxMessageSizeInKb(36)
            .build()
            .serializeAsXml())
            .isEqualTo("<?xml version=\"1.0\"?>\n" +
                "\n" +
                "<!--\n" +
                "  Licensed to the Apache Software Foundation (ASF) under one\n" +
                "  or more contributor license agreements.  See the NOTICE file\n" +
                "  distributed with this work for additional information\n" +
                "  regarding copyright ownership.  The ASF licenses this file\n" +
                "  to you under the Apache License, Version 2.0 (the\n" +
                "  \"License\"); you may not use this file except in compliance\n" +
                "  with the License.  You may obtain a copy of the License at\n" +
                "\n" +
                "    http://www.apache.org/licenses/LICENSE-2.0\n" +
                "\n" +
                "  Unless required by applicable law or agreed to in writing,\n" +
                "  software distributed under the License is distributed on an\n" +
                "  \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
                "  KIND, either express or implied.  See the License for the\n" +
                "  specific language governing permissions and limitations\n" +
                "  under the License.\n" +
                " -->\n" +
                "\n" +
                "<smtpservers>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-global</jmxName>\n" +
                "        <bind>0.0.0.0:1025</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <authRequired>false</authRequired>\n" +
                "        <verifyIdentity>true</verifyIdentity>\n" +
                "        <maxmessagesize>36</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>true</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-TLS</jmxName>\n" +
                "        <bind>0.0.0.0:10465</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <!--\n" +
                "           Authorize only local users\n" +
                "        -->\n" +
                "        <authRequired>true</authRequired>\n" +
                "        <!-- Trust authenticated users -->\n" +
                "        <verifyIdentity>false</verifyIdentity>\n" +
                "        <maxmessagesize>36</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>true</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-authenticated</jmxName>\n" +
                "        <bind>0.0.0.0:1587</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <!--\n" +
                "           Authorize only local users\n" +
                "        -->\n" +
                "        <authRequired>true</authRequired>\n" +
                "        <!-- Trust authenticated users -->\n" +
                "        <verifyIdentity>false</verifyIdentity>\n" +
                "        <maxmessagesize>36</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>true</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "</smtpservers>\n\n\n");
    }

    @Test
    public void smtpConfigurationShouldAcceptSpecificNetworks() throws IOException {
        assertThat(SmtpConfiguration.builder()
            .requireAuthentication()
            .withAutorizedAddresses("127.0.0.1/8")
            .build()
            .serializeAsXml())
            .isEqualTo("<?xml version=\"1.0\"?>\n" +
                "\n" +
                "<!--\n" +
                "  Licensed to the Apache Software Foundation (ASF) under one\n" +
                "  or more contributor license agreements.  See the NOTICE file\n" +
                "  distributed with this work for additional information\n" +
                "  regarding copyright ownership.  The ASF licenses this file\n" +
                "  to you under the Apache License, Version 2.0 (the\n" +
                "  \"License\"); you may not use this file except in compliance\n" +
                "  with the License.  You may obtain a copy of the License at\n" +
                "\n" +
                "    http://www.apache.org/licenses/LICENSE-2.0\n" +
                "\n" +
                "  Unless required by applicable law or agreed to in writing,\n" +
                "  software distributed under the License is distributed on an\n" +
                "  \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
                "  KIND, either express or implied.  See the License for the\n" +
                "  specific language governing permissions and limitations\n" +
                "  under the License.\n" +
                " -->\n" +
                "\n" +
                "<smtpservers>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-global</jmxName>\n" +
                "        <bind>0.0.0.0:1025</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <authRequired>true</authRequired>\n" +
                "        <authorizedAddresses>127.0.0.1/8</authorizedAddresses>\n" +
                "        <verifyIdentity>true</verifyIdentity>\n" +
                "        <maxmessagesize>0</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>true</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-TLS</jmxName>\n" +
                "        <bind>0.0.0.0:10465</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <!--\n" +
                "           Authorize only local users\n" +
                "        -->\n" +
                "        <authRequired>true</authRequired>\n" +
                "        <authorizedAddresses>127.0.0.1/8</authorizedAddresses>\n" +
                "        <!-- Trust authenticated users -->\n" +
                "        <verifyIdentity>false</verifyIdentity>\n" +
                "        <maxmessagesize>0</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>true</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-authenticated</jmxName>\n" +
                "        <bind>0.0.0.0:1587</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <!--\n" +
                "           Authorize only local users\n" +
                "        -->\n" +
                "        <authRequired>true</authRequired>\n" +
                "        <authorizedAddresses>127.0.0.1/8</authorizedAddresses>\n" +
                "        <!-- Trust authenticated users -->\n" +
                "        <verifyIdentity>false</verifyIdentity>\n" +
                "        <maxmessagesize>0</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>true</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "</smtpservers>\n\n\n");
    }

    @Test
    public void smtpConfigurationShouldAllowToNotVerifyIdentity() throws IOException {
        assertThat(SmtpConfiguration.builder()
            .doNotVerifyIdentity()
            .requireAuthentication()
            .build()
            .serializeAsXml())
            .isEqualTo("<?xml version=\"1.0\"?>\n" +
                "\n" +
                "<!--\n" +
                "  Licensed to the Apache Software Foundation (ASF) under one\n" +
                "  or more contributor license agreements.  See the NOTICE file\n" +
                "  distributed with this work for additional information\n" +
                "  regarding copyright ownership.  The ASF licenses this file\n" +
                "  to you under the Apache License, Version 2.0 (the\n" +
                "  \"License\"); you may not use this file except in compliance\n" +
                "  with the License.  You may obtain a copy of the License at\n" +
                "\n" +
                "    http://www.apache.org/licenses/LICENSE-2.0\n" +
                "\n" +
                "  Unless required by applicable law or agreed to in writing,\n" +
                "  software distributed under the License is distributed on an\n" +
                "  \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
                "  KIND, either express or implied.  See the License for the\n" +
                "  specific language governing permissions and limitations\n" +
                "  under the License.\n" +
                " -->\n" +
                "\n" +
                "<smtpservers>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-global</jmxName>\n" +
                "        <bind>0.0.0.0:1025</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <authRequired>true</authRequired>\n" +
                "        <verifyIdentity>false</verifyIdentity>\n" +
                "        <maxmessagesize>0</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>true</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-TLS</jmxName>\n" +
                "        <bind>0.0.0.0:10465</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <!--\n" +
                "           Authorize only local users\n" +
                "        -->\n" +
                "        <authRequired>true</authRequired>\n" +
                "        <!-- Trust authenticated users -->\n" +
                "        <verifyIdentity>false</verifyIdentity>\n" +
                "        <maxmessagesize>0</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>true</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "    <smtpserver enabled=\"true\">\n" +
                "        <jmxName>smtpserver-authenticated</jmxName>\n" +
                "        <bind>0.0.0.0:1587</bind>\n" +
                "        <connectionBacklog>200</connectionBacklog>\n" +
                "        <tls socketTLS=\"false\" startTLS=\"false\">\n" +
                "            <keystore>file://conf/keystore</keystore>\n" +
                "            <secret>james72laBalle</secret>\n" +
                "            <provider>org.bouncycastle.jce.provider.BouncyCastleProvider</provider>\n" +
                "            <algorithm>SunX509</algorithm>\n" +
                "        </tls>\n" +
                "        <connectiontimeout>360</connectiontimeout>\n" +
                "        <connectionLimit>0</connectionLimit>\n" +
                "        <connectionLimitPerIP>0</connectionLimitPerIP>\n" +
                "        <!--\n" +
                "           Authorize only local users\n" +
                "        -->\n" +
                "        <authRequired>true</authRequired>\n" +
                "        <!-- Trust authenticated users -->\n" +
                "        <verifyIdentity>false</verifyIdentity>\n" +
                "        <maxmessagesize>0</maxmessagesize>\n" +
                "        <addressBracketsEnforcement>true</addressBracketsEnforcement>\n" +
                "        <smtpGreeting>JAMES Linagora's SMTP awesome Server</smtpGreeting>\n" +
                "        <handlerchain>\n" +
                "            <handler class=\"org.apache.james.smtpserver.fastfail.ValidRcptHandler\"/>\n" +
                "            <handler class=\"org.apache.james.smtpserver.CoreCmdHandlerLoader\"/>\n" +
                "        </handlerchain>\n" +
                "    </smtpserver>\n" +
                "</smtpservers>\n\n\n");
    }
}
