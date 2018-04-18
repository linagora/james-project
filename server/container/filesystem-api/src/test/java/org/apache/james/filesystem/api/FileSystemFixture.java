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

package org.apache.james.filesystem.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.NotImplementedException;

public class FileSystemFixture {

    public static final FileSystem THROWING_FILE_SYSTEM = new FileSystem() {
        @Override
        public InputStream getResource(String url) {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public File getFile(String fileURL) throws FileNotFoundException {
            throw new FileNotFoundException();
        }

        @Override
        public File getBasedir() {
            throw new NotImplementedException("Not implemented");
        }
    };

    public static final Function<String, FileSystem> FIXED_CLASSPATH_FILE_SYSTEM = fixedDir -> new FileSystem() {
        @Override
        public InputStream getResource(String url) {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public File getFile(String fileURL) throws FileNotFoundException {
            return Optional.ofNullable(ClassLoader.getSystemResource(fixedDir))
                .map(URL::getFile)
                .map(File::new)
                .orElseThrow(() -> new FileNotFoundException(fileURL));
        }

        @Override
        public File getBasedir() {
            throw new NotImplementedException("Not implemented");
        }
    };

    public static final FileSystem CLASSPATH_FILE_SYSTEM = new FileSystem() {
        @Override
        public InputStream getResource(String url) {
            return ClassLoader.getSystemResourceAsStream(url);
        }

        @Override
        public File getFile(String fileURL) throws FileNotFoundException {
            return Optional.ofNullable(ClassLoader.getSystemResource(fileURL))
                .map(URL::getFile)
                .map(File::new)
                .orElseThrow(() -> new FileNotFoundException(fileURL));
        }

        @Override
        public File getBasedir() {
            throw new NotImplementedException("Not implemented");
        }
    };

    public static final Function<String, FileSystem> FILE_SYSTEM_WITH_BASE_DIR = baseDir -> new FileSystem() {
        @Override
        public File getBasedir() {
            return new File(baseDir);
        }

        @Override
        public InputStream getResource(String url) throws IOException {
            return new FileInputStream(getFile(url));
        }

        @Override
        public File getFile(String fileURL) {
            return new File(getBasedir(), fileURL.substring(FileSystem.FILE_PROTOCOL.length()));
        }
    };

    public static final Supplier<FileSystem> TEMP_FILE_SYSTEM = () ->
        FILE_SYSTEM_WITH_BASE_DIR.apply(System.getProperty("java.io.tmpdir"));
}
