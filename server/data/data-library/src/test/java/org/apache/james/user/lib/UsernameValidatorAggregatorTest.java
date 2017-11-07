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

package org.apache.james.user.lib;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.james.user.api.UsersRepositoryException;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class UsernameValidatorAggregatorTest {

    @Test
    public void validateShouldNotThrowWhenNoValidator() throws UsersRepositoryException {
        new UsernameValidatorAggregator(ImmutableSet.of())
            .validate("any");
    }

    @Test
    public void validateShouldNotThrowWhenAllValidatorsPass() throws UsersRepositoryException {
        new UsernameValidatorAggregator(ImmutableSet.of(
            username -> {}))
            .validate("any");
    }

    @Test
    public void validateShouldThrowWhenAllValidatorsFail() throws UsersRepositoryException {
        assertThatThrownBy(() ->
            new UsernameValidatorAggregator(ImmutableSet.of(
                username -> {throw new UsersRepositoryException("expected");}))
                .validate("any"))
            .isInstanceOf(UsersRepositoryException.class);
    }

    @Test
    public void validateShouldThrowWhenAtLeastOneValidatorFail() throws UsersRepositoryException {
        assertThatThrownBy(() ->
            new UsernameValidatorAggregator(ImmutableSet.of(
                username -> {},
                username -> {throw new UsersRepositoryException("expected");}))
                .validate("any"))
            .isInstanceOf(UsersRepositoryException.class);
    }

}