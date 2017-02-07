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
package org.apache.james.user.ldap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.james.util.streams.SwarmGenericContainer;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.shaded.com.github.dockerjava.api.command.InspectContainerResponse;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

public class LdapGenericContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapGenericContainer.class);
    public static final int DEFAULT_LDAP_PORT = 389;

    public static Builder builder(TemporaryFolder temporaryFolder) {
        return new Builder(temporaryFolder);
    }

    public static class Builder {

        private String domain;
        private String password;
        private TemporaryFolder temporaryFolder;

        private Builder(TemporaryFolder temporaryFolder) {
            this.temporaryFolder = temporaryFolder;
        }

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public LdapGenericContainer build() {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(domain), "'domain' is mandatory");
            Preconditions.checkArgument(!Strings.isNullOrEmpty(password), "'password' is mandatory");
            return new LdapGenericContainer(createContainer());
        }

        private SwarmGenericContainer createContainer() {
            try {
                File ldifFolder = temporaryFolder.newFolder("ldif-files");
                FileOutputStream outputStream = new FileOutputStream(new File(ldifFolder, "populate.ldif"));
                IOUtils.copy(ClassLoader.getSystemResourceAsStream("ldif-files/populate.ldif"), outputStream);
                return new SwarmGenericContainer("dinkel/openldap:latest")
                        .withAffinityToContainer()
                        .withEnv("SLAPD_DOMAIN", domain)
                        .withEnv("SLAPD_PASSWORD", password)
                        .withEnv("SLAPD_CONFIG_PASSWORD", password)
//                    .withClasspathResourceMapping("ldif-files", "/etc/ldap.dist/prepopulate", BindMode.READ_ONLY)
                        .withFileSystemBind(ldifFolder.getAbsolutePath(), "/etc/ldap.dist/prepopulate", BindMode.READ_ONLY)
                        .withExposedPorts(DEFAULT_LDAP_PORT);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private final SwarmGenericContainer container;

    private LdapGenericContainer(SwarmGenericContainer container) {
        this.container = container;
    }

    public void start() {
        container.start();
    }

    public void stop() {
        container.stop();
    }

    public String getLdapHost() {
        return "ldap://" +
                container.getContainerIpAddress() +
                ":" + 
                container.getMappedPort(LdapGenericContainer.DEFAULT_LDAP_PORT);
    }

    public String getLdapHostOnContainer() {
        return "ldap://" +
                container.getIp() +
                ":" + 
                LdapGenericContainer.DEFAULT_LDAP_PORT;
    }

    public String getContainerIpAddress() {
        return container.getContainerIpAddress();
    }

    public String getIp() {
        return container.getIp();
    }

    public void logConfiguration() {
        LOGGER.debug("Container configuration");
        InspectContainerResponse response = container.getDockerClient().inspectContainerCmd(container.getContainerId()).exec();
        LOGGER.debug(response.toString());
    }
}
