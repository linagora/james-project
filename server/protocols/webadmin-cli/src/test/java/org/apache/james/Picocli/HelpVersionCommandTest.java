/******************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one     *
 * or more contributor license agreements.  See the NOTICE file   *
 * distributed with this work for additional information          *
 * regarding copyright ownership.  The ASF licenses this file     *
 * to you under the Apache License, Version 2.0 (the              *
 * "License"); you may not use this file except in compliance     *
 * with the License.  You may obtain a copy of the License at     *
 *                                                                *
 * http://www.apache.org/licenses/LICENSE-2.0                     *
 *                                                                *
 * Unless required by applicable law or agreed to in writing,     *
 * software distributed under the License is distributed on an    *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY         *
 * KIND, either express or implied.  See the License for the      *
 * specific language governing permissions and limitations        *
 * under the License.                                             *
 ******************************************************************/

package org.apache.james.Picocli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.*;

public class HelpVersionCommandTest {

    private static final String successHelpMessage = "Usage: ./james-cli [-hV]\n" +
            "James Webadmin CLI\n" +
            "  -h, --help      Show this help message and exit.\n" +
            "  -V, --version   Print version information and exit.";

    private static final String rightVersion = "1.0";

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    void setup() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown() {
        System.setOut(standardOut);
    }

    @Test
    void longHelpCommandShouldWork() {
        new CommandLine(new Picocli()).execute("--help");
        assertThat(outputStreamCaptor.toString().trim()).isEqualTo(successHelpMessage);
    }

    @Test
    void shortHelpCommandShouldWork() {
        new CommandLine(new Picocli()).execute("-h");
        assertThat(outputStreamCaptor.toString().trim()).isEqualTo(successHelpMessage);
    }

    @Test
    void longVersionCommandShouldWork() {
        new CommandLine(new Picocli()).execute("--version");
        assertThat(outputStreamCaptor.toString().trim()).isEqualTo(rightVersion);
    }

    @Test
    void shortVersionCommandShouldWork() {
        new CommandLine(new Picocli()).execute("-V");
        assertThat(outputStreamCaptor.toString().trim()).isEqualTo(rightVersion);
    }

}
