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

import org.apache.commons.lang3.StringUtils;

import org.apache.james.filter.api.FilterRule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public class FilterRuleConditionDTO {

    @VisibleForTesting
    static FilterRuleConditionDTO toDTO(FilterRule.Condition condition) {
        return new FilterRuleConditionDTO(
            condition.getField().asString(),
            condition.getComparator().asString(),
            condition.getValue());
    }

    private String field;
    private String comparator;
    private String value;

    @JsonCreator
    public FilterRuleConditionDTO(@JsonProperty("field") String field,
                                   @JsonProperty("comparator") String comparator,
                                   @JsonProperty("value") String value) {
        Preconditions.checkNotNull(field, "'field' is mandatory");
        Preconditions.checkArgument(StringUtils.isNotBlank(field), "field should no be empty");
        Preconditions.checkNotNull(comparator, "'comparator' is mandatory");
        Preconditions.checkArgument(StringUtils.isNotBlank(comparator), "comparator should no be empty");
        Preconditions.checkNotNull(value, "'value' is mandatory");
        Preconditions.checkArgument(StringUtils.isNotBlank(value), "value should no be empty");
        this.field = field;
        this.comparator = comparator;
        this.value = value;
    }
    
    public String getField() {
        return field;
    }

    public String getComparator() {
        return comparator;
    }

    public String getValue() {
        return value;
    }

    @JsonIgnore
    public FilterRule.Condition toFilterRuleCondition() {
        return FilterRule.Condition.of(field, comparator, value);
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof FilterRuleConditionDTO) {
            FilterRuleConditionDTO that = (FilterRuleConditionDTO) o;

            return Objects.equals(this.field, that.field)
                && Objects.equals(this.comparator, that.comparator)
                && Objects.equals(this.value, that.value);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(field, comparator, value);
    }


}
