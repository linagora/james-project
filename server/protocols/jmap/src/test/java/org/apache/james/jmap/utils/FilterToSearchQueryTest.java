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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;

import javax.mail.Flags.Flag;

import org.apache.commons.lang.NotImplementedException;
import org.apache.james.jmap.model.Filter;
import org.apache.james.jmap.model.FilterCondition;
import org.apache.james.jmap.model.FilterOperator;
import org.apache.james.jmap.model.Operator;
import org.apache.james.mailbox.model.SearchQuery;
import org.apache.james.mailbox.model.SearchQuery.AddressOperator;
import org.apache.james.mailbox.model.SearchQuery.AddressType;
import org.apache.james.mailbox.model.SearchQuery.BooleanOperator;
import org.apache.james.mailbox.model.SearchQuery.Conjunction;
import org.apache.james.mailbox.model.SearchQuery.ConjunctionCriterion;
import org.apache.james.mailbox.model.SearchQuery.ContainsOperator;
import org.apache.james.mailbox.model.SearchQuery.DateComparator;
import org.apache.james.mailbox.model.SearchQuery.DateOperator;
import org.apache.james.mailbox.model.SearchQuery.DateResolution;
import org.apache.james.mailbox.model.SearchQuery.ExistsOperator;
import org.apache.james.mailbox.model.SearchQuery.FlagCriterion;
import org.apache.james.mailbox.model.SearchQuery.HeaderCriterion;
import org.apache.james.mailbox.model.SearchQuery.InternalDateCriterion;
import org.apache.james.mailbox.model.SearchQuery.NumericComparator;
import org.apache.james.mailbox.model.SearchQuery.NumericOperator;
import org.apache.james.mailbox.model.SearchQuery.Scope;
import org.apache.james.mailbox.model.SearchQuery.SizeCriterion;
import org.apache.james.mailbox.model.SearchQuery.TextCriterion;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class FilterToSearchQueryTest {

    @Test
    public void filterConditionShouldMapWhenFrom() {
        String from = "sender@james.org";
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .from(from)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(HeaderCriterion.class);
        HeaderCriterion criterion = (HeaderCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getHeaderName()).isEqualTo(AddressType.From.name());
        assertThat(criterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(from));
    }

    @Test
    public void filterConditionShouldMapWhenTo() {
        String to = "recipient@james.org";
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .to(to)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(HeaderCriterion.class);
        HeaderCriterion criterion = (HeaderCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getHeaderName()).isEqualTo(AddressType.To.name());
        assertThat(criterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(to));
    }

    @Test
    public void filterConditionShouldMapWhenCc() {
        String cc = "copy@james.org";
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .cc(cc)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(HeaderCriterion.class);
        HeaderCriterion criterion = (HeaderCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getHeaderName()).isEqualTo(AddressType.Cc.name());
        assertThat(criterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(cc));
    }

    @Test
    public void filterConditionShouldMapWhenBcc() {
        String bcc = "blindcopy@james.org";
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .bcc(bcc)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(HeaderCriterion.class);
        HeaderCriterion criterion = (HeaderCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getHeaderName()).isEqualTo(AddressType.Bcc.name());
        assertThat(criterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(bcc));
    }

    @Test
    public void filterConditionShouldMapWhenSubject() {
        String subject = "subject";
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .subject(subject)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(HeaderCriterion.class);
        HeaderCriterion criterion = (HeaderCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getHeaderName()).isEqualTo("Subject");
        assertThat(criterion.getOperator()).isInstanceOf(ContainsOperator.class).isEqualTo(new ContainsOperator(subject));
    }

    @Test
    public void filterConditionShouldMapWhenBody() {
        String body = "body";
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .body(body)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(TextCriterion.class);
        TextCriterion criterion = (TextCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getType()).isEqualTo(Scope.BODY);
        assertThat(criterion.getOperator()).isInstanceOf(ContainsOperator.class).isEqualTo(new ContainsOperator(body));
    }

    @Test
    public void filterConditionShouldMapWhenText() {
        String text = "text";

        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .text(text)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(ConjunctionCriterion.class);

        // From
        ConjunctionCriterion firstLevel = (ConjunctionCriterion) searchQuery.getCriterias().get(0);
        assertThat(firstLevel.getType()).isEqualTo(Conjunction.OR);
        assertThat(firstLevel.getCriteria()).hasSize(2);
        assertThat(firstLevel.getCriteria().get(0)).isInstanceOf(HeaderCriterion.class);
        HeaderCriterion fromCriterion = (HeaderCriterion) firstLevel.getCriteria().get(0);
        assertThat(fromCriterion.getHeaderName()).isEqualTo(AddressType.From.name());
        assertThat(fromCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(text));

        // To
        ConjunctionCriterion secondLevel = (ConjunctionCriterion) firstLevel.getCriteria().get(1);
        assertThat(secondLevel.getType()).isEqualTo(Conjunction.OR);
        assertThat(secondLevel.getCriteria()).hasSize(2);
        assertThat(secondLevel.getCriteria().get(0)).isInstanceOf(HeaderCriterion.class);
        HeaderCriterion toCriterion = (HeaderCriterion) secondLevel.getCriteria().get(0);
        assertThat(toCriterion.getHeaderName()).isEqualTo(AddressType.To.name());
        assertThat(toCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(text));

        // Cc
        ConjunctionCriterion thirdLevel = (ConjunctionCriterion) secondLevel.getCriteria().get(1);
        assertThat(thirdLevel.getType()).isEqualTo(Conjunction.OR);
        assertThat(thirdLevel.getCriteria()).hasSize(2);
        assertThat(thirdLevel.getCriteria().get(0)).isInstanceOf(HeaderCriterion.class);
        HeaderCriterion ccCriterion = (HeaderCriterion) thirdLevel.getCriteria().get(0);
        assertThat(ccCriterion.getHeaderName()).isEqualTo(AddressType.Cc.name());
        assertThat(ccCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(text));

        // Bcc
        ConjunctionCriterion fourthLevel = (ConjunctionCriterion) thirdLevel.getCriteria().get(1);
        assertThat(fourthLevel.getType()).isEqualTo(Conjunction.OR);
        assertThat(fourthLevel.getCriteria()).hasSize(2);
        assertThat(fourthLevel.getCriteria().get(0)).isInstanceOf(HeaderCriterion.class);
        HeaderCriterion bccCriterion = (HeaderCriterion) fourthLevel.getCriteria().get(0);
        assertThat(bccCriterion.getHeaderName()).isEqualTo(AddressType.Bcc.name());
        assertThat(bccCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(text));

        // Subject
        ConjunctionCriterion fifthLevel = (ConjunctionCriterion) fourthLevel.getCriteria().get(1);
        assertThat(fifthLevel.getType()).isEqualTo(Conjunction.OR);
        assertThat(fifthLevel.getCriteria()).hasSize(2);
        assertThat(fifthLevel.getCriteria().get(0)).isInstanceOf(HeaderCriterion.class);
        HeaderCriterion subjectCriterion = (HeaderCriterion) fifthLevel.getCriteria().get(0);
        assertThat(subjectCriterion.getHeaderName()).isEqualTo("Subject");
        assertThat(subjectCriterion.getOperator()).isInstanceOf(ContainsOperator.class).isEqualTo(new ContainsOperator(text));

        // Body
        TextCriterion bodyCriterion = (TextCriterion) fifthLevel.getCriteria().get(1);
        assertThat(bodyCriterion.getType()).isEqualTo(Scope.BODY);
        assertThat(bodyCriterion.getOperator()).isInstanceOf(ContainsOperator.class).isEqualTo(new ContainsOperator(text));
    }

    @Test
    public void filterConditionShouldMapWhenAfter() {
        Date after = new Date();
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .after(after)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(InternalDateCriterion.class);
        InternalDateCriterion criterion = (InternalDateCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getOperator()).isEqualTo(new DateOperator(DateComparator.AFTER, after, DateResolution.Second));
    }

    @Test
    public void filterConditionShouldMapWhenBefore() {
        Date before = new Date();
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .before(before)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(InternalDateCriterion.class);
        InternalDateCriterion criterion = (InternalDateCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getOperator()).isEqualTo(new DateOperator(DateComparator.BEFORE, before, DateResolution.Second));
    }

    @Test
    public void filterConditionShouldThrowWhenHasAttachment() {
        assertThatThrownBy(() -> new FilterToSearchQuery().map(FilterCondition.builder()
                .hasAttachment(true)
                .build()))
            .isInstanceOf(NotImplementedException.class);
    }

    @Test
    public void filterConditionShouldMapWhenHeaderWithOneElement() {
        String headerName = "name";
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .header(ImmutableList.of(headerName))
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(HeaderCriterion.class);
        HeaderCriterion criterion = (HeaderCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getHeaderName()).isEqualTo(headerName);
        assertThat(criterion.getOperator()).isEqualTo(ExistsOperator.exists());
    }

    @Test
    public void filterConditionShouldMapWhenIsAnswered() {
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .isAnswered(true)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(FlagCriterion.class);
        FlagCriterion criterion = (FlagCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getFlag()).isEqualTo(Flag.ANSWERED);
        assertThat(criterion.getOperator()).isEqualTo(BooleanOperator.set());
    }

    @Test
    public void filterConditionShouldMapWhenIsDraft() {
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .isDraft(true)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(FlagCriterion.class);
        FlagCriterion criterion = (FlagCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getFlag()).isEqualTo(Flag.DRAFT);
        assertThat(criterion.getOperator()).isEqualTo(BooleanOperator.set());
    }

    @Test
    public void filterConditionShouldMapWhenIsFlagged() {
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .isFlagged(true)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(FlagCriterion.class);
        FlagCriterion criterion = (FlagCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getFlag()).isEqualTo(Flag.FLAGGED);
        assertThat(criterion.getOperator()).isEqualTo(BooleanOperator.set());
    }

    @Test
    public void filterConditionShouldMapWhenIsUnread() {
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .isUnread(true)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(FlagCriterion.class);
        FlagCriterion criterion = (FlagCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getFlag()).isEqualTo(Flag.SEEN);
        assertThat(criterion.getOperator()).isEqualTo(BooleanOperator.unset());
    }

    @Test
    public void filterConditionShouldMapWhenMaxSize() {
        int maxSize = 123;
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .maxSize(maxSize)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(SizeCriterion.class);
        SizeCriterion criterion = (SizeCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getOperator()).isEqualTo(new NumericOperator(maxSize, NumericComparator.LESS_THAN));
    }

    @Test
    public void filterConditionShouldMapWhenMinSize() {
        int minSize = 4;
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .minSize(minSize)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(SizeCriterion.class);
        SizeCriterion criterion = (SizeCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getOperator()).isEqualTo(new NumericOperator(minSize, NumericComparator.GREATER_THAN));
    }

    @Test
    public void filterConditionShouldMapWhenHeaderWithTwoElements() {
        String headerName = "name";
        String headerValue = "value";
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .header(ImmutableList.of(headerName, headerValue))
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(1);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(HeaderCriterion.class);
        HeaderCriterion criterion = (HeaderCriterion) searchQuery.getCriterias().get(0);
        assertThat(criterion.getHeaderName()).isEqualTo(headerName);
        assertThat(criterion.getOperator()).isEqualTo(new ContainsOperator(headerValue));
    }

    @Test
    public void filterConditionShouldMapTwoConditions() {
        String from = "sender@james.org";
        String to = "recipient@james.org";
        SearchQuery searchQuery = new FilterToSearchQuery().map(FilterCondition.builder()
                .from(from)
                .to(to)
                .build());

        assertThat(searchQuery.getCriterias()).hasSize(2);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(HeaderCriterion.class);
        // From
        HeaderCriterion fromCriterion = (HeaderCriterion) searchQuery.getCriterias().get(0);
        assertThat(fromCriterion.getHeaderName()).isEqualTo(AddressType.From.name());
        assertThat(fromCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(from));
        // To
        HeaderCriterion toCriterion = (HeaderCriterion) searchQuery.getCriterias().get(1);
        assertThat(toCriterion.getHeaderName()).isEqualTo(AddressType.To.name());
        assertThat(toCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(to));
    }

    @Test
    public void filterConditionShouldMapWhenAndOperator() {
        String from = "sender@james.org";
        String to = "recipient@james.org";
        String subject = "subject";
        Filter complexFilter = FilterOperator.builder()
                .operator(Operator.AND)
                .conditions(ImmutableList.of(
                        FilterCondition.builder()
                            .from(from)
                            .build(),
                        FilterCondition.builder()
                            .to(to)
                            .build(),
                        FilterCondition.builder()
                            .subject(subject)
                            .build()))
                .build();

        SearchQuery searchQuery = new FilterToSearchQuery().map(complexFilter);

        assertThat(searchQuery.getCriterias()).hasSize(3);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(ConjunctionCriterion.class);
        // From
        ConjunctionCriterion fromConjuction = (ConjunctionCriterion) searchQuery.getCriterias().get(0);
        assertThat(fromConjuction.getCriteria()).hasSize(1);
        assertThat(fromConjuction.getType()).isEqualTo(Conjunction.AND);
        HeaderCriterion fromCriterion = (HeaderCriterion) fromConjuction.getCriteria().get(0);
        assertThat(fromCriterion.getHeaderName()).isEqualTo(AddressType.From.name());
        assertThat(fromCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(from));
        // To
        ConjunctionCriterion toConjuction = (ConjunctionCriterion) searchQuery.getCriterias().get(1);
        assertThat(toConjuction.getCriteria()).hasSize(1);
        assertThat(toConjuction.getType()).isEqualTo(Conjunction.AND);
        HeaderCriterion toCriterion = (HeaderCriterion) toConjuction.getCriteria().get(0);
        assertThat(toCriterion.getHeaderName()).isEqualTo(AddressType.To.name());
        assertThat(toCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(to));
        // Subject
        ConjunctionCriterion subjectConjuction = (ConjunctionCriterion) searchQuery.getCriterias().get(2);
        assertThat(subjectConjuction.getCriteria()).hasSize(1);
        assertThat(subjectConjuction.getType()).isEqualTo(Conjunction.AND);
        HeaderCriterion subjectCriterion = (HeaderCriterion) subjectConjuction.getCriteria().get(0);
        assertThat(subjectCriterion.getHeaderName()).isEqualTo("Subject");
        assertThat(subjectCriterion.getOperator()).isInstanceOf(ContainsOperator.class).isEqualTo(new ContainsOperator(subject));
    }

    @Test
    public void filterConditionShouldMapWhenOrOperator() {
        String from = "sender@james.org";
        String to = "recipient@james.org";
        String subject = "subject";
        Filter complexFilter = FilterOperator.builder()
                .operator(Operator.OR)
                .conditions(ImmutableList.of(
                        FilterCondition.builder()
                            .from(from)
                            .build(),
                        FilterCondition.builder()
                            .to(to)
                            .build(),
                        FilterCondition.builder()
                            .subject(subject)
                            .build()))
                .build();

        SearchQuery searchQuery = new FilterToSearchQuery().map(complexFilter);

        assertThat(searchQuery.getCriterias()).hasSize(3);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(ConjunctionCriterion.class);
        // From
        ConjunctionCriterion fromConjuction = (ConjunctionCriterion) searchQuery.getCriterias().get(0);
        assertThat(fromConjuction.getCriteria()).hasSize(1);
        assertThat(fromConjuction.getType()).isEqualTo(Conjunction.OR);
        HeaderCriterion fromCriterion = (HeaderCriterion) fromConjuction.getCriteria().get(0);
        assertThat(fromCriterion.getHeaderName()).isEqualTo(AddressType.From.name());
        assertThat(fromCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(from));
        // To
        ConjunctionCriterion toConjuction = (ConjunctionCriterion) searchQuery.getCriterias().get(1);
        assertThat(toConjuction.getCriteria()).hasSize(1);
        assertThat(toConjuction.getType()).isEqualTo(Conjunction.OR);
        HeaderCriterion toCriterion = (HeaderCriterion) toConjuction.getCriteria().get(0);
        assertThat(toCriterion.getHeaderName()).isEqualTo(AddressType.To.name());
        assertThat(toCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(to));
        // Subject
        ConjunctionCriterion subjectConjuction = (ConjunctionCriterion) searchQuery.getCriterias().get(2);
        assertThat(subjectConjuction.getCriteria()).hasSize(1);
        assertThat(subjectConjuction.getType()).isEqualTo(Conjunction.OR);
        HeaderCriterion subjectCriterion = (HeaderCriterion) subjectConjuction.getCriteria().get(0);
        assertThat(subjectCriterion.getHeaderName()).isEqualTo("Subject");
        assertThat(subjectCriterion.getOperator()).isInstanceOf(ContainsOperator.class).isEqualTo(new ContainsOperator(subject));
    }

    @Test
    public void filterConditionShouldMapWhenNotOperator() {
        String from = "sender@james.org";
        String to = "recipient@james.org";
        String subject = "subject";
        Filter complexFilter = FilterOperator.builder()
                .operator(Operator.NOT)
                .conditions(ImmutableList.of(
                        FilterCondition.builder()
                            .from(from)
                            .build(),
                        FilterCondition.builder()
                            .to(to)
                            .build(),
                        FilterCondition.builder()
                            .subject(subject)
                            .build()))
                .build();

        SearchQuery searchQuery = new FilterToSearchQuery().map(complexFilter);

        assertThat(searchQuery.getCriterias()).hasSize(3);
        assertThat(searchQuery.getCriterias().get(0)).isInstanceOf(ConjunctionCriterion.class);
        // From
        ConjunctionCriterion fromConjuction = (ConjunctionCriterion) searchQuery.getCriterias().get(0);
        assertThat(fromConjuction.getCriteria()).hasSize(1);
        assertThat(fromConjuction.getType()).isEqualTo(Conjunction.NOR);
        HeaderCriterion fromCriterion = (HeaderCriterion) fromConjuction.getCriteria().get(0);
        assertThat(fromCriterion.getHeaderName()).isEqualTo(AddressType.From.name());
        assertThat(fromCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(from));
        // To
        ConjunctionCriterion toConjuction = (ConjunctionCriterion) searchQuery.getCriterias().get(1);
        assertThat(toConjuction.getCriteria()).hasSize(1);
        assertThat(toConjuction.getType()).isEqualTo(Conjunction.NOR);
        HeaderCriterion toCriterion = (HeaderCriterion) toConjuction.getCriteria().get(0);
        assertThat(toCriterion.getHeaderName()).isEqualTo(AddressType.To.name());
        assertThat(toCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(to));
        // Subject
        ConjunctionCriterion subjectConjuction = (ConjunctionCriterion) searchQuery.getCriterias().get(2);
        assertThat(subjectConjuction.getCriteria()).hasSize(1);
        assertThat(subjectConjuction.getType()).isEqualTo(Conjunction.NOR);
        HeaderCriterion subjectCriterion = (HeaderCriterion) subjectConjuction.getCriteria().get(0);
        assertThat(subjectCriterion.getHeaderName()).isEqualTo("Subject");
        assertThat(subjectCriterion.getOperator()).isInstanceOf(ContainsOperator.class).isEqualTo(new ContainsOperator(subject));
    }

    @Test
    public void filterConditionShouldMapWhenComplexFilterTree() {
        String from = "sender@james.org";
        String to = "recipient@james.org";
        String cc = "copy@james.org";
        Filter complexFilter = FilterOperator.builder()
                .operator(Operator.AND)
                .conditions(ImmutableList.of(
                        FilterCondition.builder()
                            .from(from)
                            .build(),
                        FilterOperator.builder()
                            .operator(Operator.OR)
                            .conditions(ImmutableList.of(
                                    FilterOperator.builder()
                                        .operator(Operator.NOT)
                                        .conditions(ImmutableList.of(
                                                FilterCondition.builder()
                                                    .to(to)
                                                    .build()))
                                        .build(),
                                    FilterCondition.builder()
                                        .cc(cc)
                                        .build()
                                )).build()))
                .build();

        SearchQuery searchQuery = new FilterToSearchQuery().map(complexFilter);

        assertThat(searchQuery.getCriterias()).hasSize(2);
        // From
        ConjunctionCriterion andConjuction = (ConjunctionCriterion) searchQuery.getCriterias().get(0);
        assertThat(andConjuction.getCriteria()).hasSize(1);
        assertThat(andConjuction.getType()).isEqualTo(Conjunction.AND);
        HeaderCriterion fromCriterion = (HeaderCriterion) andConjuction.getCriteria().get(0);
        assertThat(fromCriterion.getHeaderName()).isEqualTo(AddressType.From.name());
        assertThat(fromCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(from));
        // To
        ConjunctionCriterion orConjuction = (ConjunctionCriterion) searchQuery.getCriterias().get(1);
        assertThat(orConjuction.getCriteria()).hasSize(2);
        assertThat(orConjuction.getType()).isEqualTo(Conjunction.AND);
        ConjunctionCriterion notConjuction = (ConjunctionCriterion) orConjuction.getCriteria().get(0);
        assertThat(notConjuction.getCriteria()).hasSize(1);
        assertThat(notConjuction.getType()).isEqualTo(Conjunction.OR);
        ConjunctionCriterion toConjuction = (ConjunctionCriterion) notConjuction.getCriteria().get(0);
        assertThat(toConjuction.getCriteria()).hasSize(1);
        assertThat(toConjuction.getType()).isEqualTo(Conjunction.NOR);
        HeaderCriterion toCriterion = (HeaderCriterion) toConjuction.getCriteria().get(0);
        assertThat(toCriterion.getHeaderName()).isEqualTo(AddressType.To.name());
        assertThat(toCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(to));
        // Cc
        ConjunctionCriterion ccConjuction = (ConjunctionCriterion) orConjuction.getCriteria().get(1);
        assertThat(ccConjuction.getCriteria()).hasSize(1);
        assertThat(ccConjuction.getType()).isEqualTo(Conjunction.OR);
        HeaderCriterion ccCriterion = (HeaderCriterion) ccConjuction.getCriteria().get(0);
        assertThat(ccCriterion.getHeaderName()).isEqualTo(AddressType.Cc.name());
        assertThat(ccCriterion.getOperator()).isInstanceOf(AddressOperator.class).isEqualTo(new AddressOperator(cc));
    }
}
