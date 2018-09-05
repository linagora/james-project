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

package org.apache.james.modules.objectstorage;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.objectstorage.ContainerName;
import org.apache.james.blob.objectstorage.ObjectStorageBlobsDAO;
import org.apache.james.utils.PropertiesProvider;

import com.google.common.base.Preconditions;

public class ObjectStorageBlobsDAOProvider implements Provider<ObjectStorageBlobsDAO> {
    private static final String OBJECTSTORAGE_CONFIGURATION_NAME = "objectstorage";

    private static final String OBJECTSTORAGE_NAMESPACE = "objectstorage.namespace";
    private static final String OBJECTSTORAGE_PROVIDER = "objectstorage.provider";
    private static final String OBJECTSTORAGE_SWIFT_AUTH_API = "objectstorage.swift.authapi";

    private final Configuration configuration;
    private final BlobId.Factory blobIdFactory;
    private final HashMap<String, Function<ContainerName, ObjectStorageBlobsDAO>> providersByName;
    private final HashMap<String, Function<ContainerName, ObjectStorageBlobsDAO>> swiftAuthApiByName;

    @Inject
    public ObjectStorageBlobsDAOProvider(PropertiesProvider propertiesProvider,
                                         BlobId.Factory blobIdFactory) throws ConfigurationException {
        providersByName = new HashMap<>();
        providersByName.put("swift", this::getSwiftObjectStorageBlobsDao);
        swiftAuthApiByName = new HashMap<>();
        providersByName.put("tempauth", this::getTempAuthBlobsDao);
        providersByName.put("keystone2", this::getKeystone2BlobsDao);
        providersByName.put("keystone3", this::getKeystone3Configuration);

        this.blobIdFactory = blobIdFactory;
        try {
            this.configuration = propertiesProvider.getConfiguration(OBJECTSTORAGE_CONFIGURATION_NAME);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException(OBJECTSTORAGE_CONFIGURATION_NAME + "configuration was not found");
        }
    }

    @Override
    public ObjectStorageBlobsDAO get() {
        String provider = configuration.getString(OBJECTSTORAGE_PROVIDER, null);
        String namespace = configuration.getString(OBJECTSTORAGE_NAMESPACE, null);
        Preconditions.checkArgument(provider != null,
            "Mandatory configuration value " + OBJECTSTORAGE_PROVIDER + " is missing from " + OBJECTSTORAGE_CONFIGURATION_NAME + " configuration");
        Preconditions.checkArgument(namespace != null,
            "Mandatory configuration value " + OBJECTSTORAGE_NAMESPACE + " is missing from " + OBJECTSTORAGE_CONFIGURATION_NAME + " configuration");

        return providersByName.get(provider).apply(ContainerName.of(namespace));
    }

    private ObjectStorageBlobsDAO getSwiftObjectStorageBlobsDao(ContainerName containerName) {
        String authApi = configuration.getString(OBJECTSTORAGE_SWIFT_AUTH_API, null);
        Preconditions.checkArgument(authApi != null,
            "Mandatory configuration value " + OBJECTSTORAGE_PROVIDER + " is missing from " + OBJECTSTORAGE_CONFIGURATION_NAME + " configuration");
        return swiftAuthApiByName.get(authApi).apply(containerName);
    }

    private ObjectStorageBlobsDAO getTempAuthBlobsDao(ContainerName containerName) {
        return ObjectStorageBlobsDAO.builder(SwiftTmpAuthConfigurationReader.readSwiftConfiguration(configuration))
            .blobIdFactory(blobIdFactory)
            .container(containerName)
            .build();

    }

    private ObjectStorageBlobsDAO getKeystone2BlobsDao(ContainerName containerName) {
        return ObjectStorageBlobsDAO.builder(SwiftKeystone2ConfigurationReader.readSwiftConfiguration(configuration))
            .blobIdFactory(blobIdFactory)
            .container(containerName)
            .build();
    }

    private ObjectStorageBlobsDAO getKeystone3Configuration(ContainerName containerName) {
        return ObjectStorageBlobsDAO.builder(SwiftKeystone3ConfigurationReader.readSwiftConfiguration(configuration))
            .blobIdFactory(blobIdFactory)
            .container(containerName)
            .build();
    }
}
