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

package org.apache.james.jmap.utils;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.mail.Flags.Flag;

import org.apache.commons.lang.NotImplementedException;
import org.apache.james.jmap.model.Filter;
import org.apache.james.jmap.model.FilterCondition;
import org.apache.james.jmap.model.FilterOperator;
import org.apache.james.mailbox.model.SearchQuery;
import org.apache.james.mailbox.model.SearchQuery.AddressType;
import org.apache.james.mailbox.model.SearchQuery.Criterion;
import org.apache.james.mailbox.model.SearchQuery.DateResolution;

import com.google.common.collect.Iterables;

public class FilterToSearchQuery {

    public SearchQuery map(Filter filter) {
        if (filter instanceof FilterCondition) {
            return mapCondition((FilterCondition) filter);
        }
        if (filter instanceof FilterOperator) {
            return mapOperator((FilterOperator) filter);
        }
        throw new RuntimeException("Unknown filter");
    }

    private SearchQuery mapCondition(FilterCondition filter) {
        SearchQuery searchQuery = new SearchQuery();
        if (filter.getText().isPresent()) {
            String text = filter.getText().get();
            searchQuery.andCriteria(
                    SearchQuery.or(SearchQuery.address(AddressType.From, text),
                            SearchQuery.or(SearchQuery.address(AddressType.To, text),
                                    SearchQuery.or(SearchQuery.address(AddressType.Cc, text),
                                            SearchQuery.or(SearchQuery.address(AddressType.Bcc, text),
                                                    SearchQuery.or(SearchQuery.headerContains("Subject", text),
                                                            SearchQuery.bodyContains(text))
                                                    )
                                            )
                                    )
                            )
                    );
        }
        if (filter.getFrom().isPresent()) {
            searchQuery.andCriteria(SearchQuery.address(AddressType.From, filter.getFrom().get()));
        }
        if (filter.getTo().isPresent()) {
            searchQuery.andCriteria(SearchQuery.address(AddressType.To, filter.getTo().get()));
        }
        if (filter.getCc().isPresent()) {
            searchQuery.andCriteria(SearchQuery.address(AddressType.Cc, filter.getCc().get()));
        }
        if (filter.getBcc().isPresent()) {
            searchQuery.andCriteria(SearchQuery.address(AddressType.Bcc, filter.getBcc().get()));
        }
        if (filter.getSubject().isPresent()) {
            searchQuery.andCriteria(SearchQuery.headerContains("Subject", filter.getSubject().get()));
        }
        if (filter.getBody().isPresent()) {
            searchQuery.andCriteria(SearchQuery.bodyContains(filter.getBody().get()));
        }
        if (filter.getAfter().isPresent()) {
            searchQuery.andCriteria(SearchQuery.internalDateAfter(filter.getAfter().get(), DateResolution.Second));
        }
        if (filter.getBefore().isPresent()) {
            searchQuery.andCriteria(SearchQuery.internalDateBefore(filter.getBefore().get(), DateResolution.Second));
        }
        if (filter.getHasAttachment().isPresent()) {
            throw new NotImplementedException();
        }
        if (filter.getHeader().isPresent()) {
            List<String> header = filter.getHeader().get();
            searchQuery.andCriteria(SearchQuery.headerContains(header.get(0), Iterables.get(header, 1, null)));
        }
        if (filter.getIsAnswered().isPresent()) {
            searchQuery.andCriteria(SearchQuery.flagIsSet(Flag.ANSWERED));
        }
        if (filter.getIsDraft().isPresent()) {
            searchQuery.andCriteria(SearchQuery.flagIsSet(Flag.DRAFT));
        }
        if (filter.getIsFlagged().isPresent()) {
            searchQuery.andCriteria(SearchQuery.flagIsSet(Flag.FLAGGED));
        }
        if (filter.getIsUnread().isPresent()) {
            searchQuery.andCriteria(SearchQuery.flagIsUnSet(Flag.SEEN));
        }
        if (filter.getMaxSize().isPresent()) {
            searchQuery.andCriteria(SearchQuery.sizeLessThan(filter.getMaxSize().get()));
        }
        if (filter.getMinSize().isPresent()) {
            searchQuery.andCriteria(SearchQuery.sizeGreaterThan(filter.getMinSize().get()));
        }
        return searchQuery;
    }

    private SearchQuery mapOperator(FilterOperator filter) {
        SearchQuery searchQuery = new SearchQuery();
        Consumer<Criterion> andCriteria = f -> searchQuery.andCriteria(f);
        Stream<List<Criterion>> mappedConditions = filter.getConditions().stream()
            .map(this::map)
            .map(q -> q.getCriterias());
        switch (filter.getOperator()) {
        case AND:
            mappedConditions
                .map(SearchQuery::and)
                .forEach(andCriteria);
            return searchQuery;
   
        case OR:
            mappedConditions
                .map(SearchQuery::or)
                .forEach(andCriteria);
            return searchQuery;
   
        case NOT:
            mappedConditions
                .map(SearchQuery::not)
                .forEach(andCriteria);
            return searchQuery;
        }
        throw new RuntimeException("Unknown operator");
    }
}
