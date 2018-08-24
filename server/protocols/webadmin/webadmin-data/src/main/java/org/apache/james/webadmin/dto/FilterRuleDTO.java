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

package org.apache.james.webadmin.dto;

import java.util.Objects;

import org.apache.james.filter.api.FilterRule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public class FilterRuleDTO {

    @VisibleForTesting
    static FilterRuleDTO toDTO(FilterRule rule) {
        return new FilterRuleDTO(
            rule.getId().asString(),
            rule.getName(),
            FilterRuleConditionDTO.toDTO(rule.getCondition()),
            FilterRuleActionDTO.toDTO(rule.getAction()));
    }

    private String id;
    private String name;
    private FilterRuleConditionDTO condition;
    private FilterRuleActionDTO action;

    @JsonCreator
    public FilterRuleDTO(@JsonProperty("id") String id,
                                   @JsonProperty("name") String name,
                                   @JsonProperty("condition") FilterRuleConditionDTO condition,
                                   @JsonProperty("action") FilterRuleActionDTO action) {
        Preconditions.checkNotNull(id, "'id' is mandatory");
        Preconditions.checkNotNull(name, "'name' is mandatory");
        Preconditions.checkNotNull(condition, "'condition' is mandatory");
        Preconditions.checkNotNull(action, "'action' is mandatory");
        this.id = id;
        this.name = name;
        this.condition = condition;
        this.action = action;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
    public FilterRuleConditionDTO getCondition() {
        return condition;
    }
    
    public FilterRuleActionDTO getAction() {
        return action;
    }

    @JsonIgnore
    public FilterRule toFilterRule() {
        return FilterRule.builder()
            .id(FilterRule.Id.of(id))
            .name(name)
            .condition(condition.toFilterRuleCondition())
            .action(action.toFilterRuleAction())
            .build();
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof FilterRuleDTO) {
            FilterRuleDTO that = (FilterRuleDTO) o;

            return Objects.equals(this.id, that.id)
                && Objects.equals(this.name, that.name)
                && Objects.equals(this.condition, that.condition)
                && Objects.equals(this.action, that.action);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id, name, condition, action);
    }
}
