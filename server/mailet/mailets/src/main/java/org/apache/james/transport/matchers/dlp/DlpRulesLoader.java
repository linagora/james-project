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

package org.apache.james.transport.matchers.dlp;

import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.james.core.Domain;
import org.apache.james.dlp.api.DLPConfigurationItem;
import org.apache.james.dlp.api.DLPConfigurationStore;
import org.apache.james.util.StreamUtils;

import com.github.steveash.guavate.Guavate;

public interface DlpRulesLoader {

    DlpDomainRules load(Domain domain);

    class Impl implements DlpRulesLoader {

        private final DLPConfigurationStore configurationStore;
        private final DlpDomainRule.Factory factory;

        @Inject
        public Impl(DLPConfigurationStore configurationStore) {
            this.configurationStore = configurationStore;
            this.factory = DlpDomainRule.factory();
        }

        @Override
        public DlpDomainRules load(Domain domain) {
          return toRules(configurationStore.list(domain));
        }

        private DlpDomainRules toRules(Stream<DLPConfigurationItem> items) {
            return DlpDomainRules.of(items.flatMap(this::toRules)
                .collect(Guavate.toImmutableList()));
        }

        private Stream<DlpDomainRule> toRules(DLPConfigurationItem item) {
            return StreamUtils.flatten(
                toRecipientsRule(item),
                toSenderRule(item),
                toContentRule(item));
        }

        private Stream<DlpDomainRule> toRecipientsRule(DLPConfigurationItem item) {
            return toRule(item, factory::recipientRule);
        }

        private Stream<DlpDomainRule> toSenderRule(DLPConfigurationItem item) {
            return toRule(item, factory::senderRule);
        }

        private Stream<DlpDomainRule> toContentRule(DLPConfigurationItem item) {
            return toRule(item, factory::contentRule);
        }

        private Stream<DlpDomainRule> toRule(DLPConfigurationItem item,
                                                   BiFunction<DLPConfigurationItem.Id, Pattern, DlpDomainRule> toRule) {
            if (item.getTargets().isRecipientTargeted()) {
                return Stream.of(toRule.apply(item.getId(), Pattern.compile(item.getRegexp())));
            }
            return Stream.of();
        }

    }
}
