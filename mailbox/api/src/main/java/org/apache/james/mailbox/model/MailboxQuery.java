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

package org.apache.james.mailbox.model;

import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.james.mailbox.MailboxSession;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;


/**
 * Expresses select criteria for mailboxes.
 */
public final class MailboxQuery {

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(MailboxSession session) {
        return builder().pathDelimiter(session.getPathDelimiter()).username(session.getUser().getUserName());
    }

    interface PathExpressionCriteria {
        String asString();
    }

    public static class BasicPathExpressionCriteria implements PathExpressionCriteria {
        private final String expression;

        public BasicPathExpressionCriteria(String expression) {
            this.expression = expression;
        }

        @Override
        public String asString() {
            return expression;
        }
    }

    public enum BuiltinPathExpressionCriteria implements PathExpressionCriteria {
        MatchAll {
            @Override
            public String asString() {
                return String.valueOf(FREEWILDCARD);
            }
        }
    }

    interface SearchBaseCriteria {
        String asStringPath();
    }

    public static class BasePathCriteria implements SearchBaseCriteria {
        private final String path;

        public BasePathCriteria(String path) {
            this.path = path;
        }

        @Override
        public String asStringPath() {
            return path;
        }
    }

    public enum BuiltinSearchBaseCriteria implements SearchBaseCriteria {
        ROOT {
            @Override
            public String asStringPath() {
                return "";
            }
        };
    }

    interface UserCriteria {
        String asString();
    }

    public static class BasicUserCriteria implements UserCriteria {
        private final String username;

        public BasicUserCriteria(String username) {
            this.username = username;
        }

        @Override
        public String asString() {
            return username;
        }
    }

    public enum BuiltinUserCriteria implements UserCriteria {
        AllUsers {
            @Override
            public String asString() {
                return "";
            }
        };
    }

    public static class Builder {
        @VisibleForTesting Optional<PathExpressionCriteria> expression;
        @VisibleForTesting Optional<String> namespace;
        @VisibleForTesting Optional<Character> pathDelimiter;
        @VisibleForTesting Optional<SearchBaseCriteria> searchBase;
        @VisibleForTesting Optional<UserCriteria> username;

        private Builder() {
            this.expression = Optional.empty();
            this.namespace = Optional.empty();
            this.pathDelimiter = Optional.empty();
            this.searchBase = Optional.empty();
            this.username = Optional.empty();
        }
        
        public Builder expression(String expression) {
            this.expression = Optional.of(new BasicPathExpressionCriteria(expression));
            return this;
        }

        public Builder matchesAll() {
            this.expression = Optional.of(BuiltinPathExpressionCriteria.MatchAll);
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = Optional.of(namespace);
            return this;
        }

        public Builder pathDelimiter(char pathDelimiter) {
            this.pathDelimiter = Optional.of(pathDelimiter);
            return this;
        }

        public Builder searchBase(String base) {
            this.searchBase = Optional.ofNullable(base).map(BasePathCriteria::new);
            return this;
        }

        public Builder searchBase(MailboxPath base) {
            searchBase(base.getName());
            username(base.getUser());
            namespace(base.getNamespace());
            return this;
        }

        public Builder searchFromRoot() {
            this.searchBase = Optional.of(BuiltinSearchBaseCriteria.ROOT);
            return this;
        }

        public Builder username(String username) {
            this.username = Optional.ofNullable(username).map(BasicUserCriteria::new);
            return this;
        }

        public MailboxQuery build() {
            Preconditions.checkState(pathDelimiter.isPresent());
            return
                new MailboxQuery(
                    expression.orElse(BuiltinPathExpressionCriteria.MatchAll),
                    namespace.orElse(MailboxConstants.USER_NAMESPACE),
                    pathDelimiter.get(),
                    searchBase.orElse(BuiltinSearchBaseCriteria.ROOT),
                    username.orElse(BuiltinUserCriteria.AllUsers));
        }
    }


    /**
     * Use this wildcard to match every char including the hierarchy delimiter
     */
    public final static char FREEWILDCARD = '*';

    /**
     * Use this wildcard to match every char except the hierarchy delimiter
     */
    public final static char LOCALWILDCARD = '%';

    private final char pathDelimiter;
    private final SearchBaseCriteria searchBaseCriteria;
    private final UserCriteria userCriteria;
    private final PathExpressionCriteria pathExpressionCriteria;
    private final Pattern pattern;
    private final String namespace;


    public MailboxQuery(PathExpressionCriteria pathExpressionCriteria, String namespace, Character pathDelimiter, SearchBaseCriteria searchBaseCriteria, UserCriteria userCriteria) {
        super();
        this.pathExpressionCriteria = pathExpressionCriteria;
        this.namespace = namespace;
        this.pathDelimiter = pathDelimiter;
        this.searchBaseCriteria = searchBaseCriteria;
        this.userCriteria = userCriteria;
        pattern = constructEscapedRegex();
    }

    /**
     * Gets the searchBase reference for the search.
     * 
     * @return the searchBase
     */
    public final MailboxPath getBase() {
        return base;
    }

    /**
     * Gets the name search expression. This may contain wildcards.
     * 
     * @return the expression
     */
    public final String getExpression() {
        return expression;
    }

    /**
     * Gets wildcard character that matches any series of characters.
     * 
     * @return the freeWildcard
     */
    public final char getFreeWildcard() {
        return FREEWILDCARD;
    }

    /**
     * Gets wildcard character that matches any series of characters excluding
     * hierarchy delimiters. Effectively, this means that it matches any
     * sequence within a name part.
     * 
     * @return the localWildcard
     */
    public final char getLocalWildcard() {
        return LOCALWILDCARD;
    }

    /**
     * Is the given name a match for {@link #getExpression()}?
     * 
     * @param name
     *            name to be matched
     * @return true if the given name matches this expression, false otherwise
     */
    public final boolean isExpressionMatch(String name) {
        final boolean result;
        if (isWild()) {
            if (name == null) {
                result = false;
            } else {
                result = pattern.matcher(name).matches();
            }
        } else {
            result = expression.equals(name);
        }
        return result;
    }
  
    /**
     * Get combined name formed by adding the expression to the searchBase using the
     * given hierarchy delimiter. Note that the wildcards are retained in the
     * combined name.
     * 
     * @return {@link #getBase()} combined with {@link #getExpression()},
     *         notnull
     */
    public String getCombinedName() {
        final String result;
        if (base != null && base.getName() != null && base.getName().length() > 0) {
            final int baseLength = base.getName().length();
            if (base.getName().charAt(baseLength - 1) == pathDelimiter) {
                if (expression != null && expression.length() > 0) {
                    if (expression.charAt(0) == pathDelimiter) {
                        result = base.getName() + expression.substring(1);
                    } else {
                        result = base.getName() + expression;
                    }
                } else {
                    result = base.getName();
                }
            } else {
                if (expression != null && expression.length() > 0) {
                    if (expression.charAt(0) == pathDelimiter) {
                        result = base.getName() + expression;
                    } else {
                        result = base.getName() + pathDelimiter + expression;
                    }
                } else {
                    result = base.getName();
                }
            }
        } else {
            result = expression;
        }
        return result;
    }

    /**
     * Is this expression wild?
     * 
     * @return true if wildcard contained, false otherwise
     */
    public boolean isWild() {
        return expression != null && (expression.indexOf(getFreeWildcard()) >= 0 || expression.indexOf(getLocalWildcard()) >= 0);
    }

    /**
     * Renders a string suitable for logging.
     * 
     * @return a <code>String</code> representation of this object.
     */
    public String toString() {
        final String TAB = " ";
        return "MailboxExpression [ " + "searchBase = " + this.base + TAB + "expression = " + this.expression + TAB + "freeWildcard = " + this.getFreeWildcard() + TAB + "localWildcard = " + this.getLocalWildcard() + TAB + " ]";
    }


    private Pattern constructEscapedRegex() {
        StringBuilder stringBuilder = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(expression, "*%", true);
        while (tokenizer.hasMoreTokens()) {
            stringBuilder.append(getRegexPartAssociatedWithToken(tokenizer));
        }
        return Pattern.compile(stringBuilder.toString());
    }

    private String getRegexPartAssociatedWithToken(StringTokenizer tokenizer) {
        String token = tokenizer.nextToken();
        if (token.equals("*")) {
            return ".*";
        } else if (token.equals("%")) {
            return "[^" + Pattern.quote(String.valueOf(pathDelimiter)) + "]*";
        } else {
            return Pattern.quote(token);
        }
    }

}
