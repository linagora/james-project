/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.james.sieverepository.file;

import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.sieverepository.api.ScriptSummary;
import org.apache.james.sieverepository.api.SieveRepository;
import org.apache.james.sieverepository.api.exception.DuplicateException;
import org.apache.james.sieverepository.api.exception.DuplicateUserException;
import org.apache.james.sieverepository.api.exception.IsActiveException;
import org.apache.james.sieverepository.api.exception.QuotaExceededException;
import org.apache.james.sieverepository.api.exception.QuotaNotFoundException;
import org.apache.james.sieverepository.api.exception.ScriptNotFoundException;
import org.apache.james.sieverepository.api.exception.StorageException;
import org.apache.james.sieverepository.api.exception.UserNotFoundException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * <code>SieveFileRepository</code> manages sieve scripts stored on the file system.
 * <p>The sieve root directory is a sub-directory of the application base directory named "sieve".
 * Scripts are stored in sub-directories of the sieve root directory, each with the name of the
 * associated user.
 */
public class SieveDefaultRepository implements SieveRepository {
    private FileSystem fileSystem = null;

    @Override
    public void haveSpace(String user, String name, long size) throws UserNotFoundException, QuotaExceededException, StorageException {
        throw new NotImplementedException();
    }

    @Override
    public void putScript(String user, String name, String content) throws UserNotFoundException, StorageException, QuotaExceededException {
        throw new NotImplementedException();
    }

    @Override
    public List<ScriptSummary> listScripts(String user) throws UserNotFoundException, StorageException {
        throw new NotImplementedException();
    }

    @Override
    public InputStream getActive(String user) throws UserNotFoundException, ScriptNotFoundException, StorageException {
        // RFC 5228 permits extensions: .siv .sieve
        String sieveFilePrefix = FileSystem.FILE_PROTOCOL + "sieve/" + user + ".";
        try {
            return new FileInputStream(fileSystem.getFile(sieveFilePrefix + "sieve"));
        } catch (FileNotFoundException ex) {
            try {
                return new FileInputStream(fileSystem.getFile(sieveFilePrefix + "siv"));
            } catch (FileNotFoundException e) {
                throw new ScriptNotFoundException();
            }
        }
    }

    @Override
    public void setActive(String user, String name) throws UserNotFoundException, ScriptNotFoundException, StorageException {
        throw new NotImplementedException();
    }

    @Override
    public InputStream getScript(String user, String name) throws UserNotFoundException, ScriptNotFoundException, StorageException {
        throw new NotImplementedException();
    }

    @Override
    public void deleteScript(String user, String name) throws UserNotFoundException, ScriptNotFoundException, IsActiveException, StorageException {
        throw new NotImplementedException();
    }

    @Override
    public void renameScript(String user, String oldName, String newName) throws UserNotFoundException, ScriptNotFoundException, DuplicateException, StorageException {
        throw new NotImplementedException();
    }

    @Override
    public boolean hasUser(String user) throws StorageException {
        throw new NotImplementedException();
    }

    @Override
    public void addUser(String user) throws DuplicateUserException, StorageException {
        throw new NotImplementedException();
    }

    @Override
    public void removeUser(String user) throws UserNotFoundException, StorageException {
        throw new NotImplementedException();
    }

    @Override
    public boolean hasQuota() throws StorageException {
        throw new NotImplementedException();
    }

    @Override
    public long getQuota() throws QuotaNotFoundException, StorageException {
        throw new NotImplementedException();
    }

    @Override
    public void setQuota(long quota) throws StorageException {
        throw new NotImplementedException();
    }

    @Override
    public void removeQuota() throws QuotaNotFoundException, StorageException {
        throw new NotImplementedException();
    }

    @Override
    public boolean hasQuota(String user) throws UserNotFoundException, StorageException {
        throw new NotImplementedException();
    }

    @Override
    public long getQuota(String user) throws UserNotFoundException, QuotaNotFoundException, StorageException {
        throw new NotImplementedException();
    }

    @Override
    public void setQuota(String user, long quota) throws UserNotFoundException, StorageException {
        throw new NotImplementedException();
    }

    @Override
    public void removeQuota(String user) throws UserNotFoundException, QuotaNotFoundException, StorageException {
        throw new NotImplementedException();
    }
}
