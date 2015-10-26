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

package org.apache.james.transport.mailets;

import org.apache.james.sieverepository.api.SieveRepository;
import org.apache.james.sieverepository.api.exception.ScriptNotFoundException;
import org.apache.james.sieverepository.api.exception.StorageException;
import org.apache.james.sieverepository.api.exception.UserNotFoundException;
import org.apache.james.sieverepository.file.SieveDefaultRepository;
import org.apache.jsieve.mailet.ResourceLocator;

import java.io.IOException;
import java.io.InputStream;

/**
 * To maintain backwards compatibility with existing installations, this uses
 * the old file based scheme.
 * <p> The scripts are stored in the <code>sieve</code> sub directory of the application
 * installation directory.
 */
public class ResourceLocatorImpl implements ResourceLocator {

    private final boolean virtualHosting;
    
    private SieveRepository sieveRepository = null;

    public ResourceLocatorImpl(boolean virtualHosting) {
        this.virtualHosting = virtualHosting;
        this.sieveRepository = new SieveDefaultRepository();
    }

    public InputStream get(String uri) throws IOException {
        // Use the complete email address for finding the sieve file
        uri = uri.substring(2);

        String username;
        if (virtualHosting) {
            username = uri.substring(0, uri.indexOf("/"));
        } else {
            username = uri.substring(0, uri.indexOf("@"));
        }

        try {
            return sieveRepository.getActive(username);
        } catch (UserNotFoundException e) {
            throw new IOException();
        } catch (ScriptNotFoundException e) {
            throw new IOException();
        } catch (StorageException e) {
            throw new IOException();
        }
    }
}
