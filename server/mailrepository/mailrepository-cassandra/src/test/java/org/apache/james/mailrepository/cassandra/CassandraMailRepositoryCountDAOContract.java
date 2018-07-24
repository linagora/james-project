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

package org.apache.james.mailrepository.cassandra;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.mailrepository.api.MailRepositoryUrl;
import org.junit.jupiter.api.Test;

public interface CassandraMailRepositoryCountDAOContract {
    MailRepositoryUrl URL = MailRepositoryUrl.from("proto://url");
    MailRepositoryUrl URL2 = MailRepositoryUrl.from("proto://url2");

    CassandraMailRepositoryCountDAO countDAO();

    @Test
    default void getCountShouldReturnZeroWhenEmpty() {
        assertThat(countDAO().getCount(URL).join())
            .isEqualTo(0L);
    }

    @Test
    default void getCountShouldReturnOneWhenIncrementedOneTime() {
        countDAO().increment(URL).join();

        assertThat(countDAO().getCount(URL).join())
            .isEqualTo(1L);
    }

    @Test
    default void incrementShouldNotAffectOtherUrls() {
        countDAO().increment(URL).join();

        assertThat(countDAO().getCount(URL2).join())
            .isEqualTo(0L);
    }

    @Test
    default void incrementCanBeAppliedSeveralTime() {
        countDAO().increment(URL).join();
        countDAO().increment(URL).join();

        assertThat(countDAO().getCount(URL).join())
            .isEqualTo(2L);
    }

    @Test
    default void decrementShouldDecreaseCount() {
        countDAO().increment(URL).join();
        countDAO().increment(URL).join();
        countDAO().increment(URL).join();

        countDAO().decrement(URL).join();

        assertThat(countDAO().getCount(URL).join())
            .isEqualTo(2L);
    }

    @Test
    default void decrementCanLeadToNegativeCount() {
        countDAO().decrement(URL).join();

        assertThat(countDAO().getCount(URL).join())
            .isEqualTo(-1L);
    }
}