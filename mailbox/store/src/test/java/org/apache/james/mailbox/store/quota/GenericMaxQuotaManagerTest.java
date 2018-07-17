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

    MaxQuotaManager testee();

    @Test
    default void getMaxMessageShouldReturnEmptyWhenNoGlobalValue() throws Exception {
        assertThat(testee().getMaxMessage(QUOTA_ROOT)).isEmpty();
    }

    @Test
    default void getMaxStorageShouldReturnEmptyWhenNoGlobalValue() throws Exception {
        assertThat(testee().getMaxStorage(QUOTA_ROOT)).isEmpty();
    }

    @Test
    default void getMaxMessageShouldReturnDomainWhenNoValue() throws Exception {
        testee().setGlobalMaxMessage(QuotaCount.count(36));
        testee().setDomainMaxMessage(DOMAIN, QuotaCount.count(23));
        assertThat(testee().getMaxMessage(QUOTA_ROOT)).contains(QuotaCount.count(23));
    }

    @Test
    default void getMaxMessageShouldReturnGlobalWhenNoValue() throws Exception {
        testee().setGlobalMaxMessage(QuotaCount.count(36));
        assertThat(testee().getMaxMessage(QUOTA_ROOT)).contains(QuotaCount.count(36));
    }

    @Test
    default void getMaxStorageShouldReturnGlobalWhenNoValue() throws Exception {
        testee().setGlobalMaxStorage(QuotaSize.size(36));
        assertThat(testee().getMaxStorage(QUOTA_ROOT)).contains(QuotaSize.size(36));
    }

    @Test
    default void getMaxStorageShouldReturnDomainWhenNoValue() throws Exception {
        testee().setGlobalMaxStorage(QuotaSize.size(234));
        testee().setDomainMaxStorage(DOMAIN, QuotaSize.size(111));
        assertThat(testee().getMaxStorage(QUOTA_ROOT)).contains(QuotaSize.size(111));
    }

    @Test
    default void getMaxMessageShouldReturnProvidedValue() throws Exception {
        testee().setMaxMessage(QUOTA_ROOT, QuotaCount.count(36));
        assertThat(testee().getMaxMessage(QUOTA_ROOT)).contains(QuotaCount.count(36));
    }

    @Test
    default void getMaxStorageShouldReturnProvidedValue() throws Exception {
        testee().setMaxStorage(QUOTA_ROOT, QuotaSize.size(36));
        assertThat(testee().getMaxStorage(QUOTA_ROOT)).contains(QuotaSize.size(36));
    }

    @Test
    default void deleteMaxStorageShouldRemoveCurrentValue() throws Exception {
        testee().setMaxStorage(QUOTA_ROOT, QuotaSize.size(36));
        testee().removeMaxStorage(QUOTA_ROOT);
        assertThat(testee().getMaxStorage(QUOTA_ROOT)).isEmpty();
    }

    @Test
    default void deleteMaxMessageShouldRemoveCurrentValue() throws Exception {
        testee().setMaxMessage(QUOTA_ROOT, QuotaCount.count(36));
        testee().removeMaxMessage(QUOTA_ROOT);
        assertThat(testee().getMaxMessage(QUOTA_ROOT)).isEmpty();
    }

    @Test
    default void deleteGlobalMaxStorageShouldRemoveCurrentValue() throws Exception {
        testee().setGlobalMaxStorage(QuotaSize.size(36));
        testee().removeGlobalMaxStorage();
        assertThat(testee().getGlobalMaxStorage()).isEmpty();
    }

    @Test
    default void deleteGlobalMaxMessageShouldRemoveCurrentValue() throws Exception {
        testee().setGlobalMaxMessage(QuotaCount.count(36));
        testee().removeGlobalMaxMessage();
        assertThat(testee().getGlobalMaxMessage()).isEmpty();
    }

    @Test
    default void listMaxMessagesDetailsShouldReturnEmptyWhenNoQuotaDefined() {
        assertThat(testee().listMaxMessagesDetails(QUOTA_ROOT)).isEmpty();
    }

    @Test
    default void listMaxStorageDetailsShouldReturnEmptyWhenNoQuotaDefined() {
        assertThat(testee().listMaxStorageDetails(QUOTA_ROOT)).isEmpty();
    }

    @Test
    default void listMaxMessagesDetailsShouldReturnGlobalValueWhenDefined() throws Exception {
        testee().setGlobalMaxMessage(QuotaCount.count(123));
        assertThat(testee().listMaxMessagesDetails(QUOTA_ROOT))
            .hasSize(1)
            .containsEntry(Quota.Scope.Global, QuotaCount.count(123));
    }

    @Test
    default void listMaxMessagesDetailsShouldReturnDomainValueWhenDefined() throws Exception {
        testee().setDomainMaxMessage(DOMAIN, QuotaCount.count(123));
        assertThat(testee().listMaxMessagesDetails(QUOTA_ROOT))
            .hasSize(1)
            .containsEntry(Quota.Scope.Domain, QuotaCount.count(123));
    }

    @Test
    default void listMaxMessagesDetailsShouldReturnUserValueWhenDefined() throws Exception {
        testee().setMaxMessage(QUOTA_ROOT, QuotaCount.count(123));
        assertThat(testee().listMaxMessagesDetails(QUOTA_ROOT))
            .hasSize(1)
            .containsEntry(Quota.Scope.User, QuotaCount.count(123));
    }

    @Test
    default void listMaxMessagesDetailsShouldReturnBothValuesWhenGlobalAndUserDefined() throws Exception {
        testee().setGlobalMaxMessage(QuotaCount.count(1234));
        testee().setMaxMessage(QUOTA_ROOT, QuotaCount.count(123));
        assertThat(testee().listMaxMessagesDetails(QUOTA_ROOT))
            .hasSize(2)
            .containsEntry(Quota.Scope.Global, QuotaCount.count(1234))
            .containsEntry(Quota.Scope.User, QuotaCount.count(123));
    }

    @Test
    default void listMaxMessagesDetailsShouldReturnAllValuesWhenDefined() throws Exception {
        testee().setGlobalMaxMessage(QuotaCount.count(1234));
        testee().setDomainMaxMessage(DOMAIN, QuotaCount.count(333));
        testee().setMaxMessage(QUOTA_ROOT, QuotaCount.count(123));
        assertThat(testee().listMaxMessagesDetails(QUOTA_ROOT))
            .hasSize(3)
            .containsEntry(Quota.Scope.Global, QuotaCount.count(1234))
            .containsEntry(Quota.Scope.Domain, QuotaCount.count(333))
            .containsEntry(Quota.Scope.User, QuotaCount.count(123));
    }

    @Test
    default void listMaxStorageDetailsShouldReturnGlobalValueWhenDefined() throws Exception {
        testee().setGlobalMaxStorage(QuotaSize.size(1111));
        assertThat(testee().listMaxStorageDetails(QUOTA_ROOT))
            .hasSize(1)
            .containsEntry(Quota.Scope.Global, QuotaSize.size(1111));
    }

    @Test
    default void listMaxStorageDetailsShouldReturnDomainValueWhenDefined() throws Exception {
        testee().setDomainMaxStorage(DOMAIN, QuotaSize.size(1111));
        assertThat(testee().listMaxStorageDetails(QUOTA_ROOT))
            .hasSize(1)
            .containsEntry(Quota.Scope.Domain, QuotaSize.size(1111));
    }

    @Test
    default void listMaxStorageDetailsShouldReturnUserValueWhenDefined() throws Exception {
        testee().setMaxStorage(QUOTA_ROOT, QuotaSize.size(2222));
        assertThat(testee().listMaxStorageDetails(QUOTA_ROOT))
            .hasSize(1)
            .containsEntry(Quota.Scope.User, QuotaSize.size(2222));
    }

    @Test
    default void listMaxStorageDetailsShouldReturnBothValuesWhenDefined() throws Exception {
        testee().setGlobalMaxStorage(QuotaSize.size(3333));
        testee().setMaxStorage(QUOTA_ROOT, QuotaSize.size(4444));
        assertThat(testee().listMaxStorageDetails(QUOTA_ROOT))
            .hasSize(2)
            .containsEntry(Quota.Scope.Global, QuotaSize.size(3333))
            .containsEntry(Quota.Scope.User, QuotaSize.size(4444));
    }

    @Test
    default void listMaxStorageDetailsShouldReturnAllValuesWhenDefined() throws Exception {
        testee().setGlobalMaxStorage(QuotaSize.size(3333));
        testee().setDomainMaxStorage(DOMAIN, QuotaSize.size(2222));
        testee().setMaxStorage(QUOTA_ROOT, QuotaSize.size(4444));
        assertThat(testee().listMaxStorageDetails(QUOTA_ROOT))
            .hasSize(3)
            .containsEntry(Quota.Scope.Global, QuotaSize.size(3333))
            .containsEntry(Quota.Scope.Domain, QuotaSize.size(2222))
            .containsEntry(Quota.Scope.User, QuotaSize.size(4444));
    }

    @Test
    default void getDomainMaxMessageShouldReturnEmptyWhenNoGlobalValue() {
        assertThat(testee().getDomainMaxMessage(DOMAIN)).isEmpty();
    }

    @Test
    default void getDomainMaxStorageShouldReturnEmptyWhenNoGlobalValue() {
        assertThat(testee().getDomainMaxStorage(DOMAIN)).isEmpty();
    }

    @Test
    default void getDomainMaxMessageShouldReturnProvidedValue() throws Exception {
        testee().setDomainMaxMessage(DOMAIN, QuotaCount.count(36));
        assertThat(testee().getDomainMaxMessage(DOMAIN)).contains(QuotaCount.count(36));
    }

    @Test
    default void getDomainMaxStorageShouldReturnProvidedValue() throws Exception {
        testee().setDomainMaxStorage(DOMAIN, QuotaSize.size(36));
        assertThat(testee().getDomainMaxStorage(DOMAIN)).contains(QuotaSize.size(36));
    }

    @Test
    default void deleteDomainMaxStorageShouldRemoveCurrentValue() throws Exception {
        testee().setDomainMaxStorage(DOMAIN, QuotaSize.size(36));
        testee().removeDomainMaxStorage(DOMAIN);
        assertThat(testee().getDomainMaxStorage(DOMAIN)).isEmpty();
    }

    @Test
    default void deleteDomainMaxMessageShouldRemoveCurrentValue() throws Exception {
        testee().setDomainMaxMessage(DOMAIN, QuotaCount.count(36));
        testee().removeDomainMaxMessage(DOMAIN);
        assertThat(testee().getDomainMaxMessage(DOMAIN)).isEmpty();
    }

    @Test
    default void deleteDomainMaxMessageShouldNotBeCaseSensitive() throws Exception {
        testee().setDomainMaxMessage(DOMAIN, QuotaCount.count(36));

        testee().removeDomainMaxMessage(DOMAIN_CASE_VARIATION);

        assertThat(testee().getDomainMaxMessage(DOMAIN)).isEmpty();
    }

    @Test
    default void deleteDomainMaxStorageShouldNotBeCaseSensitive() throws Exception {
        testee().setDomainMaxStorage(DOMAIN, QuotaSize.size(36));

        testee().removeDomainMaxStorage(DOMAIN_CASE_VARIATION);

        assertThat(testee().getDomainMaxStorage(DOMAIN)).isEmpty();
    }

    @Test
    default void setDomainMaxMessageShouldNotBeCaseSensitive() throws Exception {
        testee().setDomainMaxMessage(DOMAIN_CASE_VARIATION, QuotaCount.count(36));


        assertThat(testee().getDomainMaxMessage(DOMAIN))
            .contains(QuotaCount.count(36));
    }

    @Test
    default void setDomainMaxStorageShouldNotBeCaseSensitive() throws Exception {
        testee().setDomainMaxStorage(DOMAIN_CASE_VARIATION, QuotaSize.size(36));

        assertThat(testee().getDomainMaxStorage(DOMAIN))
            .contains(QuotaSize.size(36));
    }

    @Test
    default void getDomainMaxMessageShouldNotBeCaseSensitive() throws Exception {
        testee().setDomainMaxMessage(DOMAIN, QuotaCount.count(36));


        assertThat(testee().getDomainMaxMessage(DOMAIN_CASE_VARIATION))
            .contains(QuotaCount.count(36));
    }

    @Test
    default void getDomainMaxStorageShouldNotBeCaseSensitive() throws Exception {
        testee().setDomainMaxStorage(DOMAIN, QuotaSize.size(36));

        assertThat(testee().getDomainMaxStorage(DOMAIN_CASE_VARIATION))
            .contains(QuotaSize.size(36));
    }

}
