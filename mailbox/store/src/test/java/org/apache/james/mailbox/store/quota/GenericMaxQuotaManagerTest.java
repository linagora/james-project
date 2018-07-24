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

package org.apache.james.mailbox.store.quota;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.apache.james.core.Domain;
import org.apache.james.core.quota.QuotaCount;
import org.apache.james.core.quota.QuotaSize;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.quota.MaxQuotaManager;
import org.junit.jupiter.api.Test;

public interface GenericMaxQuotaManagerTest {
    Domain DOMAIN = Domain.of("domain");
    Domain DOMAIN_CASE_VARIATION = Domain.of("doMain");
    QuotaRoot QUOTA_ROOT = QuotaRoot.quotaRoot("benwa@domain", Optional.of(DOMAIN));

    MaxQuotaManager maxQuotaManager();

    @Test
    default void getMaxMessageShouldReturnEmptyWhenNoGlobalValue() throws Exception {
        assertThat(maxQuotaManager().getMaxMessage(QUOTA_ROOT)).isEmpty();
    }

    @Test
    default void getMaxStorageShouldReturnEmptyWhenNoGlobalValue() throws Exception {
        assertThat(maxQuotaManager().getMaxStorage(QUOTA_ROOT)).isEmpty();
    }

    @Test
    default void getMaxMessageShouldReturnDomainWhenNoValue() throws Exception {
        maxQuotaManager().setGlobalMaxMessage(QuotaCount.count(36));
        maxQuotaManager().setDomainMaxMessage(DOMAIN, QuotaCount.count(23));
        assertThat(maxQuotaManager().getMaxMessage(QUOTA_ROOT)).contains(QuotaCount.count(23));
    }

    @Test
    default void getMaxMessageShouldReturnGlobalWhenNoValue() throws Exception {
        maxQuotaManager().setGlobalMaxMessage(QuotaCount.count(36));
        assertThat(maxQuotaManager().getMaxMessage(QUOTA_ROOT)).contains(QuotaCount.count(36));
    }

    @Test
    default void getMaxStorageShouldReturnGlobalWhenNoValue() throws Exception {
        maxQuotaManager().setGlobalMaxStorage(QuotaSize.size(36));
        assertThat(maxQuotaManager().getMaxStorage(QUOTA_ROOT)).contains(QuotaSize.size(36));
    }

    @Test
    default void getMaxStorageShouldReturnDomainWhenNoValue() throws Exception {
        maxQuotaManager().setGlobalMaxStorage(QuotaSize.size(234));
        maxQuotaManager().setDomainMaxStorage(DOMAIN, QuotaSize.size(111));
        assertThat(maxQuotaManager().getMaxStorage(QUOTA_ROOT)).contains(QuotaSize.size(111));
    }

    @Test
    default void getMaxMessageShouldReturnProvidedValue() throws Exception {
        maxQuotaManager().setMaxMessage(QUOTA_ROOT, QuotaCount.count(36));
        assertThat(maxQuotaManager().getMaxMessage(QUOTA_ROOT)).contains(QuotaCount.count(36));
    }

    @Test
    default void getMaxStorageShouldReturnProvidedValue() throws Exception {
        maxQuotaManager().setMaxStorage(QUOTA_ROOT, QuotaSize.size(36));
        assertThat(maxQuotaManager().getMaxStorage(QUOTA_ROOT)).contains(QuotaSize.size(36));
    }

    @Test
    default void deleteMaxStorageShouldRemoveCurrentValue() throws Exception {
        maxQuotaManager().setMaxStorage(QUOTA_ROOT, QuotaSize.size(36));
        maxQuotaManager().removeMaxStorage(QUOTA_ROOT);
        assertThat(maxQuotaManager().getMaxStorage(QUOTA_ROOT)).isEmpty();
    }

    @Test
    default void deleteMaxMessageShouldRemoveCurrentValue() throws Exception {
        maxQuotaManager().setMaxMessage(QUOTA_ROOT, QuotaCount.count(36));
        maxQuotaManager().removeMaxMessage(QUOTA_ROOT);
        assertThat(maxQuotaManager().getMaxMessage(QUOTA_ROOT)).isEmpty();
    }

    @Test
    default void deleteGlobalMaxStorageShouldRemoveCurrentValue() throws Exception {
        maxQuotaManager().setGlobalMaxStorage(QuotaSize.size(36));
        maxQuotaManager().removeGlobalMaxStorage();
        assertThat(maxQuotaManager().getGlobalMaxStorage()).isEmpty();
    }

    @Test
    default void deleteGlobalMaxMessageShouldRemoveCurrentValue() throws Exception {
        maxQuotaManager().setGlobalMaxMessage(QuotaCount.count(36));
        maxQuotaManager().removeGlobalMaxMessage();
        assertThat(maxQuotaManager().getGlobalMaxMessage()).isEmpty();
    }

    @Test
    default void listMaxMessagesDetailsShouldReturnEmptyWhenNoQuotaDefined() {
        assertThat(maxQuotaManager().listMaxMessagesDetails(QUOTA_ROOT)).isEmpty();
    }

    @Test
    default void listMaxStorageDetailsShouldReturnEmptyWhenNoQuotaDefined() {
        assertThat(maxQuotaManager().listMaxStorageDetails(QUOTA_ROOT)).isEmpty();
    }

    @Test
    default void listMaxMessagesDetailsShouldReturnGlobalValueWhenDefined() throws Exception {
        maxQuotaManager().setGlobalMaxMessage(QuotaCount.count(123));
        assertThat(maxQuotaManager().listMaxMessagesDetails(QUOTA_ROOT))
            .hasSize(1)
            .containsEntry(Quota.Scope.Global, QuotaCount.count(123));
    }

    @Test
    default void listMaxMessagesDetailsShouldReturnDomainValueWhenDefined() throws Exception {
        maxQuotaManager().setDomainMaxMessage(DOMAIN, QuotaCount.count(123));
        assertThat(maxQuotaManager().listMaxMessagesDetails(QUOTA_ROOT))
            .hasSize(1)
            .containsEntry(Quota.Scope.Domain, QuotaCount.count(123));
    }

    @Test
    default void listMaxMessagesDetailsShouldReturnUserValueWhenDefined() throws Exception {
        maxQuotaManager().setMaxMessage(QUOTA_ROOT, QuotaCount.count(123));
        assertThat(maxQuotaManager().listMaxMessagesDetails(QUOTA_ROOT))
            .hasSize(1)
            .containsEntry(Quota.Scope.User, QuotaCount.count(123));
    }

    @Test
    default void listMaxMessagesDetailsShouldReturnBothValuesWhenGlobalAndUserDefined() throws Exception {
        maxQuotaManager().setGlobalMaxMessage(QuotaCount.count(1234));
        maxQuotaManager().setMaxMessage(QUOTA_ROOT, QuotaCount.count(123));
        assertThat(maxQuotaManager().listMaxMessagesDetails(QUOTA_ROOT))
            .hasSize(2)
            .containsEntry(Quota.Scope.Global, QuotaCount.count(1234))
            .containsEntry(Quota.Scope.User, QuotaCount.count(123));
    }

    @Test
    default void listMaxMessagesDetailsShouldReturnAllValuesWhenDefined() throws Exception {
        maxQuotaManager().setGlobalMaxMessage(QuotaCount.count(1234));
        maxQuotaManager().setDomainMaxMessage(DOMAIN, QuotaCount.count(333));
        maxQuotaManager().setMaxMessage(QUOTA_ROOT, QuotaCount.count(123));
        assertThat(maxQuotaManager().listMaxMessagesDetails(QUOTA_ROOT))
            .hasSize(3)
            .containsEntry(Quota.Scope.Global, QuotaCount.count(1234))
            .containsEntry(Quota.Scope.Domain, QuotaCount.count(333))
            .containsEntry(Quota.Scope.User, QuotaCount.count(123));
    }

    @Test
    default void listMaxStorageDetailsShouldReturnGlobalValueWhenDefined() throws Exception {
        maxQuotaManager().setGlobalMaxStorage(QuotaSize.size(1111));
        assertThat(maxQuotaManager().listMaxStorageDetails(QUOTA_ROOT))
            .hasSize(1)
            .containsEntry(Quota.Scope.Global, QuotaSize.size(1111));
    }

    @Test
    default void listMaxStorageDetailsShouldReturnDomainValueWhenDefined() throws Exception {
        maxQuotaManager().setDomainMaxStorage(DOMAIN, QuotaSize.size(1111));
        assertThat(maxQuotaManager().listMaxStorageDetails(QUOTA_ROOT))
            .hasSize(1)
            .containsEntry(Quota.Scope.Domain, QuotaSize.size(1111));
    }

    @Test
    default void listMaxStorageDetailsShouldReturnUserValueWhenDefined() throws Exception {
        maxQuotaManager().setMaxStorage(QUOTA_ROOT, QuotaSize.size(2222));
        assertThat(maxQuotaManager().listMaxStorageDetails(QUOTA_ROOT))
            .hasSize(1)
            .containsEntry(Quota.Scope.User, QuotaSize.size(2222));
    }

    @Test
    default void listMaxStorageDetailsShouldReturnBothValuesWhenDefined() throws Exception {
        maxQuotaManager().setGlobalMaxStorage(QuotaSize.size(3333));
        maxQuotaManager().setMaxStorage(QUOTA_ROOT, QuotaSize.size(4444));
        assertThat(maxQuotaManager().listMaxStorageDetails(QUOTA_ROOT))
            .hasSize(2)
            .containsEntry(Quota.Scope.Global, QuotaSize.size(3333))
            .containsEntry(Quota.Scope.User, QuotaSize.size(4444));
    }

    @Test
    default void listMaxStorageDetailsShouldReturnAllValuesWhenDefined() throws Exception {
        maxQuotaManager().setGlobalMaxStorage(QuotaSize.size(3333));
        maxQuotaManager().setDomainMaxStorage(DOMAIN, QuotaSize.size(2222));
        maxQuotaManager().setMaxStorage(QUOTA_ROOT, QuotaSize.size(4444));
        assertThat(maxQuotaManager().listMaxStorageDetails(QUOTA_ROOT))
            .hasSize(3)
            .containsEntry(Quota.Scope.Global, QuotaSize.size(3333))
            .containsEntry(Quota.Scope.Domain, QuotaSize.size(2222))
            .containsEntry(Quota.Scope.User, QuotaSize.size(4444));
    }

    @Test
    default void getDomainMaxMessageShouldReturnEmptyWhenNoGlobalValue() {
        assertThat(maxQuotaManager().getDomainMaxMessage(DOMAIN)).isEmpty();
    }

    @Test
    default void getDomainMaxStorageShouldReturnEmptyWhenNoGlobalValue() {
        assertThat(maxQuotaManager().getDomainMaxStorage(DOMAIN)).isEmpty();
    }

    @Test
    default void getDomainMaxMessageShouldReturnProvidedValue() throws Exception {
        maxQuotaManager().setDomainMaxMessage(DOMAIN, QuotaCount.count(36));
        assertThat(maxQuotaManager().getDomainMaxMessage(DOMAIN)).contains(QuotaCount.count(36));
    }

    @Test
    default void getDomainMaxStorageShouldReturnProvidedValue() throws Exception {
        maxQuotaManager().setDomainMaxStorage(DOMAIN, QuotaSize.size(36));
        assertThat(maxQuotaManager().getDomainMaxStorage(DOMAIN)).contains(QuotaSize.size(36));
    }

    @Test
    default void deleteDomainMaxStorageShouldRemoveCurrentValue() throws Exception {
        maxQuotaManager().setDomainMaxStorage(DOMAIN, QuotaSize.size(36));
        maxQuotaManager().removeDomainMaxStorage(DOMAIN);
        assertThat(maxQuotaManager().getDomainMaxStorage(DOMAIN)).isEmpty();
    }

    @Test
    default void deleteDomainMaxMessageShouldRemoveCurrentValue() throws Exception {
        maxQuotaManager().setDomainMaxMessage(DOMAIN, QuotaCount.count(36));
        maxQuotaManager().removeDomainMaxMessage(DOMAIN);
        assertThat(maxQuotaManager().getDomainMaxMessage(DOMAIN)).isEmpty();
    }

    @Test
    default void deleteDomainMaxMessageShouldNotBeCaseSensitive() throws Exception {
        maxQuotaManager().setDomainMaxMessage(DOMAIN, QuotaCount.count(36));

        maxQuotaManager().removeDomainMaxMessage(DOMAIN_CASE_VARIATION);

        assertThat(maxQuotaManager().getDomainMaxMessage(DOMAIN)).isEmpty();
    }

    @Test
    default void deleteDomainMaxStorageShouldNotBeCaseSensitive() throws Exception {
        maxQuotaManager().setDomainMaxStorage(DOMAIN, QuotaSize.size(36));

        maxQuotaManager().removeDomainMaxStorage(DOMAIN_CASE_VARIATION);

        assertThat(maxQuotaManager().getDomainMaxStorage(DOMAIN)).isEmpty();
    }

    @Test
    default void setDomainMaxMessageShouldNotBeCaseSensitive() throws Exception {
        maxQuotaManager().setDomainMaxMessage(DOMAIN_CASE_VARIATION, QuotaCount.count(36));


        assertThat(maxQuotaManager().getDomainMaxMessage(DOMAIN))
            .contains(QuotaCount.count(36));
    }

    @Test
    default void setDomainMaxStorageShouldNotBeCaseSensitive() throws Exception {
        maxQuotaManager().setDomainMaxStorage(DOMAIN_CASE_VARIATION, QuotaSize.size(36));

        assertThat(maxQuotaManager().getDomainMaxStorage(DOMAIN))
            .contains(QuotaSize.size(36));
    }

    @Test
    default void getDomainMaxMessageShouldNotBeCaseSensitive() throws Exception {
        maxQuotaManager().setDomainMaxMessage(DOMAIN, QuotaCount.count(36));


        assertThat(maxQuotaManager().getDomainMaxMessage(DOMAIN_CASE_VARIATION))
            .contains(QuotaCount.count(36));
    }

    @Test
    default void getDomainMaxStorageShouldNotBeCaseSensitive() throws Exception {
        maxQuotaManager().setDomainMaxStorage(DOMAIN, QuotaSize.size(36));

        assertThat(maxQuotaManager().getDomainMaxStorage(DOMAIN_CASE_VARIATION))
            .contains(QuotaSize.size(36));
    }

}
