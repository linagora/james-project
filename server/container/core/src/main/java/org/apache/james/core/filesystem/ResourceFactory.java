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
package org.apache.james.core.filesystem;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.filesystem.api.JamesDirectoriesProvider;

public class ResourceFactory {

    private JamesDirectoriesProvider directoryProvider;

    public ResourceFactory(JamesDirectoriesProvider directoryProvider) {
        this.directoryProvider = directoryProvider;
    }
    
    public Resource getResource(String fileURL) {
        if (fileURL.startsWith(FileSystem.CLASSPATH_PROTOCOL)) {
            return handleClasspathProtocol(fileURL);
        } else if (fileURL.startsWith(FileSystem.FILE_PROTOCOL)) {
            return handleFileProtocol(fileURL);
        } else {
            try {
                // Try to parse the location as a URL...
                return handleUrlResource(fileURL);
            } catch (MalformedURLException ex) {
                // No URL -> resolve as resource path.
                return new ClassPathResource(fileURL);
            }
        }
    }

    private Resource handleUrlResource(String fileURL) throws MalformedURLException {
        URL url = new URL(fileURL);
        return new UrlResource(url);
    }

    private Resource handleClasspathProtocol(String fileURL) {
        String resourceName = fileURL.substring(FileSystem.CLASSPATH_PROTOCOL.length());
        return new ClassPathResource(resourceName);
    }
    
    private Resource handleFileProtocol(String fileURL) {
        File file;
        if (fileURL.startsWith(FileSystem.FILE_PROTOCOL_AND_CONF)) {
            file = new File(directoryProvider.getConfDirectory() + "/" + fileURL.substring(FileSystem.FILE_PROTOCOL_AND_CONF.length()));
        } else if (fileURL.startsWith(FileSystem.FILE_PROTOCOL_AND_VAR)) {
            file = new File(directoryProvider.getVarDirectory() + "/" + fileURL.substring(FileSystem.FILE_PROTOCOL_AND_VAR.length()));
        } else if (fileURL.startsWith(FileSystem.FILE_PROTOCOL_ABSOLUTE)) {
            file = new File(directoryProvider.getAbsoluteDirectory() + fileURL.substring(FileSystem.FILE_PROTOCOL_ABSOLUTE.length()));
        } else {
            // move to the root folder of the spring deployment
            file = new File(directoryProvider.getRootDirectory() + "/" + fileURL.substring(FileSystem.FILE_PROTOCOL.length()));
        }
        return new FileSystemResource(file);
    }
}