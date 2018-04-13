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
package org.apache.james.rrt.lib;

import java.util.Map;

import javax.inject.Inject;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.james.core.Domain;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.api.RecipientRewriteTableManagementMBean;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

/**
 * Management for RecipientRewriteTables
 */
public class RecipientRewriteTableManagement extends StandardMBean implements RecipientRewriteTableManagementMBean {

    private final RecipientRewriteTable rrt;

    @Inject
    protected RecipientRewriteTableManagement(RecipientRewriteTable rrt) throws NotCompliantMBeanException {
        super(RecipientRewriteTableManagementMBean.class);
        this.rrt = rrt;
    }

    @Override
    public void addRegexMapping(String user, String domain, String regex) {
        try {
            rrt.addRegexMapping(user, Domain.of(domain), regex);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void removeRegexMapping(String user, String domain, String regex) {
        try {
            rrt.removeRegexMapping(user, Domain.of(domain), regex);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void addAddressMapping(String user, String domain, String address) {
        try {
            rrt.addAddressMapping(user, Domain.of(domain), address);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void removeAddressMapping(String user, String domain, String address) {
        try {
            rrt.removeAddressMapping(user, Domain.of(domain), address);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void addErrorMapping(String user, String domain, String error) {
        try {
            rrt.addErrorMapping(user, Domain.of(domain), error);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void removeErrorMapping(String user, String domain, String error) {
        try {
            rrt.removeErrorMapping(user, Domain.of(domain), error);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void addDomainMapping(String domain, String targetDomain) {
        try {
            rrt.addAliasDomainMapping(Domain.of(domain), Domain.of(targetDomain));
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void removeDomainMapping(String domain, String targetDomain) {
        try {
            rrt.removeAliasDomainMapping(Domain.of(domain), Domain.of(targetDomain));
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Mappings getUserDomainMappings(String user, String domain) {
        try {
            return rrt.getUserDomainMappings(user, Domain.of(domain));
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void addMapping(String user, String domain, String mapping) {
        try {
            rrt.addMapping(user, Domain.of(domain), MappingImpl.of(mapping));
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void removeMapping(String user, String domain, String mapping) {
        try {
            rrt.removeMapping(user, Domain.of(domain), MappingImpl.of(mapping));
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Map<String, Mappings> getAllMappings() {
        try {
            return ImmutableMap.copyOf(rrt.getAllMappings());
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void addForwardMapping(String user, String domain, String address) {
        try {
            rrt.addForwardMapping(user, Domain.of(domain), address);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void removeForwardMapping(String user, String domain, String address) {
        try {
            rrt.removeForwardMapping(user, Domain.of(domain), address);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

}
