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

package org.apache.james.jmap.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.mail.Flags;

import org.apache.james.jmap.methods.ValidationResult;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

@JsonDeserialize(builder = UpdateMessagePatch.Builder.class)
public class UpdateMessagePatch {

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Optional<List<String>> mailboxIds = Optional.empty();
        private Optional<Boolean> isFlagged = Optional.empty();
        private Optional<Boolean> isUnread = Optional.empty();
        private Optional<Boolean> isAnswered = Optional.empty();
        private Optional<Map<String, Boolean>> keywords = Optional.empty();
        private Set<ValidationResult> validationResult = Sets.newHashSet();

        public Builder mailboxIds(List<String> mailboxIds) {
            this.mailboxIds = Optional.of(ImmutableList.copyOf(mailboxIds));
            return this;
        }

        public Builder keywords(Map<String, Boolean> keywords) {
            this.keywords = Optional.of(ImmutableMap.copyOf(keywords));
            return this;
        }

        public Builder isFlagged(Boolean isFlagged) {
            this.isFlagged = Optional.of(isFlagged);
            return this;
        }

        public Builder isUnread(Boolean isUnread) {
            this.isUnread = Optional.of(isUnread);
            return this;
        }

        public Builder isAnswered(Boolean isAnswered) {
            this.isAnswered = Optional.of(isAnswered);
            return this;
        }

        public Builder validationResult(Set<ValidationResult> validationResult) {
            this.validationResult.addAll(validationResult);
            return this;
        }

        public UpdateMessagePatch build() {
            if (mailboxIds.isPresent() && mailboxIds.get().isEmpty()) {
                validationResult(ImmutableSet.of(ValidationResult.builder()
                    .property("mailboxIds")
                    .message("mailboxIds property is not supposed to be empty")
                    .build()));
            }
            UpdateMessagePatch updateMessagePatch = new UpdateMessagePatch(mailboxIds, isUnread, isFlagged, isAnswered,
                    Keyword.buildKeywords(keywords), ImmutableList.copyOf(validationResult));
            if (updateMessagePatch.isBothKeywordsAndIsFlagProperties()) {
                validationResult(ImmutableSet.of(ValidationResult.builder()
                    .property("keywords")
                    .message("Does not support both is* and keywords")
                    .build()));
            }
            return updateMessagePatch;
        }


    }

    private final Optional<List<String>> mailboxIds;
    private final Optional<Boolean> isUnread;
    private final Optional<Boolean> isFlagged;
    private final Optional<Boolean> isAnswered;
    private final Optional<ImmutableSet<Keyword>> keywords;

    private final ImmutableList<ValidationResult> validationErrors;

    @VisibleForTesting
    UpdateMessagePatch(Optional<List<String>> mailboxIds,
                       Optional<Boolean> isUnread,
                       Optional<Boolean> isFlagged,
                       Optional<Boolean> isAnswered,
                       Optional<ImmutableSet<Keyword>> keywords,
                       ImmutableList<ValidationResult> validationResults) {

        this.mailboxIds = mailboxIds;
        this.isUnread = isUnread;
        this.isFlagged = isFlagged;
        this.isAnswered = isAnswered;
        this.keywords = keywords;
        this.validationErrors = validationResults;
    }

    public Optional<List<String>> getMailboxIds() {
        return mailboxIds;
    }

    public Optional<Boolean> isUnread() {
        return isUnread;
    }

    public Optional<Boolean> isFlagged() {
        return isFlagged;
    }

    public Optional<Boolean> isAnswered() {
        return isAnswered;
    }

    public Optional<ImmutableSet<Keyword>> getKeywords() {
        return keywords;
    }

    public boolean isFlagsIdentity() {
        Preconditions.checkArgument(!isBothKeywordsAndIsFlagProperties(),
            "Does not support both is* and keywords");
        return !isIsFlagProperties()
            && !isKeywordsProperty();
    }

    public ImmutableList<ValidationResult> getValidationErrors() {
        return validationErrors;
    }

    public boolean isValid() {
        return getValidationErrors().isEmpty();
    }

    @VisibleForTesting
    protected boolean containKeyword(Keyword keyword) {
        return keywords.isPresent() && keywords.get().contains(keyword);
    }

    public Flags applyToState(Flags currentFlags) {
        if (keywords.isPresent()) {
            return getFlagsFromKeywords(currentFlags);
        }

        return getFlagsFromIsFlag(currentFlags);
    }

    private Flags getFlagsFromIsFlag(Flags currentFlags) {
        Flags newStateFlags = new Flags();
        if (isFlagged().orElse(currentFlags.contains(Flags.Flag.FLAGGED))) {
            newStateFlags.add(Flags.Flag.FLAGGED);
        }
        if (isAnswered().orElse(currentFlags.contains(Flags.Flag.ANSWERED))) {
            newStateFlags.add(Flags.Flag.ANSWERED);
        }
        boolean shouldMessageBeMarkSeen = isUnread().map(b -> !b).orElse(currentFlags.contains(Flags.Flag.SEEN));
        if (shouldMessageBeMarkSeen) {
            newStateFlags.add(Flags.Flag.SEEN);
        }
        return newStateFlags;
    }

    public Flags getFlagsFromKeywords(Flags currentFlags) {
        Preconditions.checkArgument(notContainUnsupportedKeywords(), "Does not allow to update 'Deleted' or 'Recent' flag");

        if (isAddDraft(currentFlags) || isRemoveDraft(currentFlags)) {
            throw new IllegalArgumentException("Cannot add or remove draft flag");
        }

        return Keyword.fromKeywords(Stream.concat(
                Keyword.fromSystemFlags(currentFlags)
                    .stream()
                    .filter(keyword -> Keyword.UNSUPPORTED_KEYWORDS.contains(keyword)),
                keywords.get().stream())
            .collect(Guavate.toImmutableSet()));
    }

    private boolean notContainUnsupportedKeywords() {
        return Keyword.UNSUPPORTED_KEYWORDS
            .stream()
            .noneMatch(keyword -> keywords.get().contains(keyword));
    }


    public boolean isBothKeywordsAndIsFlagProperties() {
        return isKeywordsProperty() && isIsFlagProperties();
    }

    private boolean isKeywordsProperty() {
        return keywords.isPresent();
    }

    private boolean isIsFlagProperties() {
        return isAnswered.isPresent() || isFlagged.isPresent() || isUnread.isPresent();
    }

    private boolean isRemoveDraft(Flags currentFlags) {
        return !containKeyword(Keyword.DRAFT) && currentFlags.contains(Flags.Flag.DRAFT);
    }

    private boolean isAddDraft(Flags currentFlags) {
        return containKeyword(Keyword.DRAFT) && !currentFlags.contains(Flags.Flag.DRAFT);
    }

}
