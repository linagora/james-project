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

package org.apache.james.imap.processor.base;

import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.james.mailbox.PathDelimiter;

import com.github.steveash.guavate.Guavate;

public class PrefixedRegex implements MailboxNameExpression {

    private final String prefix;
    private final String regex;
    private final Pattern pattern;
    private final PathDelimiter pathDelimiter;

    public PrefixedRegex(String prefix, String regex, PathDelimiter pathDelimiter) {
        this.prefix = Optional.ofNullable(prefix).orElse("");
        this.regex = Optional.ofNullable(regex).orElse("");
        this.pathDelimiter = pathDelimiter;
        this.pattern = constructEscapedRegex(this.regex);
    }

    @Override
    public boolean isExpressionMatch(String name) {
        return name.startsWith(prefix)
            && regexMatching(name.substring(prefix.length()));
    }

    private boolean regexMatching(String name) {
        if (isWild()) {
            return name != null
                && pattern.matcher(name).matches();
        } else {
            return regex.equals(name);
        }
    }

    @Override
    public String getCombinedName() {
        String sanitizedPrefix = pathDelimiter.removeTrailingSeparatorAtTheEnd(prefix);
        String sanitizedRegex = pathDelimiter.removeTrailingSeparatorAtTheBeginning(regex);
        return pathDelimiter.join(
            Stream.of(sanitizedPrefix, sanitizedRegex)
                .filter(s -> !s.isEmpty())
                .collect(Guavate.toImmutableList()));
    }

    @Override
    public boolean isWild() {
        return regex != null
            && (
            regex.indexOf(FREEWILDCARD) >= 0
                || regex.indexOf(LOCALWILDCARD) >= 0);
    }

    private Pattern constructEscapedRegex(String regex) {
        StringBuilder stringBuilder = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(regex, "*%", true);
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
            return "[^" + Pattern.quote(String.valueOf(pathDelimiter.getPathDelimiter())) + "]*";
        } else {
            return Pattern.quote(token);
        }
    }
}
