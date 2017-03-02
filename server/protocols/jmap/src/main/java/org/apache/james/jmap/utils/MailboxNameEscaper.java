/****************************************************************
 O * Licensed to the Apache Software Foundation (ASF) under one   *
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

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.lang3.text.translate.CharSequenceTranslator;

import com.google.common.base.Preconditions;
import com.google.common.escape.CharEscaper;

public class MailboxNameEscaper {

    private static final CharEscaper CHAR_ESCAPER = new CharEscaper() {
        @Override
        protected char[] escape(char c) {
            if (c == '\\') {
                return "\\\\".toCharArray();
            }
            if (c == '/') {
                return "//".toCharArray();
            }
            if (c == '.') {
                return "\\/".toCharArray();
            }
            return null;
        }
    };

    private static final CharSequenceTranslator CHAR_UNESCAPER = new CharSequenceTranslator() {
        @Override
        public int translate(CharSequence charSequence, int postion, Writer writer) throws IOException {
            if (charSequence.charAt(postion) == '\\') {
                return translateBackSlash(charSequence, postion, writer);
            }
            if (charSequence.charAt(postion) == '/') {
                return translateSlash(charSequence, postion, writer);
            }
            writer.append(charSequence.charAt(postion));
            return 1;
        }
    };

    private static int translateBackSlash(CharSequence charSequence, int postition, Writer writer) throws IOException {
        validateSize(postition + 1, charSequence);
        if (charSequence.charAt(postition + 1) == '\\') {
            writer.append('\\');
            return 2;
        }
        if (charSequence.charAt(postition + 1) == '/') {
            writer.append('.');
            return 2;
        }
        throw new IllegalArgumentException("Unvalid unescape sequence after \\ unexpected token " + charSequence.charAt(postition + 1) + ". Expected '/' or '\\'");
    }

    private static int translateSlash(CharSequence charSequence, int postition, Writer writer) throws IOException {
        validateSize(postition + 1, charSequence);
        if (charSequence.charAt(postition + 1) == '/') {
            writer.append('/');
            return 2;
        }
        throw new IllegalArgumentException("Unvalid unescape sequence after / unexpected token " + charSequence.charAt(postition + 1) + ". Expected '/'");
    }

    private static void validateSize(int position, CharSequence charSequence) {
        if (position >= charSequence.length()) {
            throw new IllegalArgumentException("Unfinished escape at the end of String");
        }
    }

    public static String escape(String string) {
        Preconditions.checkNotNull(string);

        return CHAR_ESCAPER.escape(string);
    }

    public static String unescape(String string) {
        Preconditions.checkNotNull(string);

        return CHAR_UNESCAPER.translate(string);
    }
}
