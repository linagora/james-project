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

import static org.apache.james.jmap.utils.MailboxNameEscaper.escape;
import static org.apache.james.jmap.utils.MailboxNameEscaper.unescape;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MailboxNameEscaperTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void escapeShouldThrowOnNull() {
        expectedException.expect(NullPointerException.class);

        escape(null);
    }

    @Test
    public void unescapeShouldThrowOnNull() {
        expectedException.expect(NullPointerException.class);

        unescape(null);
    }

    @Test
    public void escapeShouldTransformEmpty() {
        assertThat(escape("")).isEqualTo("");
    }

    @Test
    public void escapeShouldTransformSingleChar() {
        assertThat(escape("a")).isEqualTo("a");
    }

    @Test
    public void escapeShouldTransformMultiChar() {
        assertThat(escape("toto")).isEqualTo("toto");
    }

    @Test
    public void escapeShouldTransformDot() {
        assertThat(escape("toto.tata")).isEqualTo("toto\\/tata");
    }

    @Test
    public void escapeShouldTransformSlash() {
        assertThat(escape("toto/tata")).isEqualTo("toto//tata");
    }

    @Test
    public void escapeShouldTransformBackSlash() {
        assertThat(escape("toto\\tata")).isEqualTo("toto\\\\tata");
    }

    @Test
    public void escapeShouldTransformMultiEscapedChar() {
        assertThat(escape("toto\\./tata")).isEqualTo("toto\\\\\\///tata");
    }

    @Test
    public void unescapeShouldTransformEmpty() {
        assertThat(unescape("")).isEqualTo("");
    }

    @Test
    public void unescapeShouldTransformSingleChar() {
        assertThat(unescape("a")).isEqualTo("a");
    }

    @Test
    public void unescapeShouldTransformMultiChar() {
        assertThat(unescape("toto")).isEqualTo("toto");
    }

    @Test
    public void unescapeShouldRecoverBackSlash() {
        assertThat(unescape("toto\\\\tata")).isEqualTo("toto\\tata");
    }

    @Test
    public void unescapeShouldRecoverSlash() {
        assertThat(unescape("toto//tata")).isEqualTo("toto/tata");
    }

    @Test
    public void unescapeShouldRecoverDot() {
        assertThat(unescape("toto\\/tata")).isEqualTo("toto.tata");
    }

    @Test
    public void unescapeShouldRecoverComplexEscapeSequence() {
        // Was failing if using String::replace for unescape
        assertThat(unescape("toto\\\\//tata")).isEqualTo("toto\\/tata");
    }

    @Test
    public void unescapeShouldThrowOnInvalidEscapeSequenceAfterBackSlash() {
        expectedException.expect(IllegalArgumentException.class);

        unescape("toto\\tata");
    }

    @Test
    public void unescapeShouldThrowOnInvalidEscapeSequenceAfterSlash() {
        expectedException.expect(IllegalArgumentException.class);

        unescape("toto/tata");
    }

    @Test
    public void unescapeSlashShouldThrowAtTheEnd() {
        expectedException.expect(IllegalArgumentException.class);

        unescape("toto/");
    }

    @Test
    public void unescapeBackSlashShouldThrowAtTheEnd() {
        expectedException.expect(IllegalArgumentException.class);

        unescape("toto\\");
    }

}
