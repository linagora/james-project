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

package org.apache.james.mailets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.apache.james.core.JamesServerResourceLoader;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.filesystem.api.JamesDirectoriesProvider;
import org.apache.james.modules.CommonServicesModule;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class TemporaryFilesystemModule extends AbstractModule {
    
    private final Supplier<File> workingDirectory;

    private static File rootDir(TemporaryFolder temporaryFolder) {
        return temporaryFolder.getRoot();
    }

    public TemporaryFilesystemModule(TemporaryFolder temporaryFolder) {
        this(() -> TemporaryFilesystemModule.rootDir(temporaryFolder));
    }

    public TemporaryFilesystemModule(Supplier<File> workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    protected void configure() {
        try {
            String resourcesFolder = workingDirectory.get().getAbsolutePath() + File.separator + "conf";
            bind(JamesDirectoriesProvider.class).toInstance(new JamesServerResourceLoader(workingDirectory.get().getAbsolutePath()));
            copyResources(resourcesFolder);
            bindConstant().annotatedWith(Names.named(CommonServicesModule.CONFIGURATION_PATH)).to(FileSystem.FILE_PROTOCOL_AND_CONF);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    private void copyResources(String resourcesFolder) throws FileNotFoundException, IOException {
        copyResource(resourcesFolder, "dnsservice.xml");
        copyResource(resourcesFolder, "domainlist.xml");
        copyResource(resourcesFolder, "imapserver.xml");
        copyResource(resourcesFolder, "keystore");
        copyResource(resourcesFolder, "lmtpserver.xml");
        copyResource(resourcesFolder, "mailrepositorystore.xml");
        copyResource(resourcesFolder, "managesieveserver.xml");
        copyResource(resourcesFolder, "pop3server.xml");
        copyResource(resourcesFolder, "recipientrewritetable.xml");
        copyResource(resourcesFolder, "smtpserver.xml");
    }

    private void copyResource(String resourcesFolder, String resourceName) throws FileNotFoundException, IOException {
        URL resource = ClassLoader.getSystemClassLoader().getResource(resourceName);
        IOUtils.copy(resource.openStream(), new FileOutputStream(resourcesFolder + File.separator + resourceName));
    }
}
