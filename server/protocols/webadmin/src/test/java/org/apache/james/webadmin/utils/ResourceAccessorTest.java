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

package org.apache.james.webadmin.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

public class ResourceAccessorTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private String operationAppliedOn;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        operationAppliedOn = null;
    }

    @Test
    public void operationShouldNotBeAppliedWithNullPath() throws Exception {
        when(request.getPathInfo()).thenReturn(null);

        ResourceAccessor.applyOnResource(request, response, resource -> operationAppliedOn = resource);

        assertThat(operationAppliedOn).isNull();
    }

    @Test
    public void operationShouldNotBeAppliedWithEmptyPath() throws Exception {
        when(request.getPathInfo()).thenReturn("");

        ResourceAccessor.applyOnResource(request, response, resource -> operationAppliedOn = resource);

        assertThat(operationAppliedOn).isNull();
    }

    @Test
    public void operationShouldNotBeAppliedWithUnderSizedPath() throws Exception {
        when(request.getPathInfo()).thenReturn("a");

        ResourceAccessor.applyOnResource(request, response, resource -> operationAppliedOn = resource);

        assertThat(operationAppliedOn).isNull();
    }

    @Test
    public void operationShouldBeWellAppliedOnValidPath() throws Exception {
        when(request.getPathInfo()).thenReturn("/resource");

        ResourceAccessor.applyOnResource(request, response, resource -> operationAppliedOn = resource);

        assertThat(operationAppliedOn).isEqualTo("resource");
    }

    @Test
    public void applyOnResourceShouldSetNotFoundOnNullPath() throws Exception {
        when(request.getPathInfo()).thenReturn(null);

        ResourceAccessor.applyOnResource(request, response, resource -> operationAppliedOn = resource);

        verify(response).setStatus(400);
    }

    @Test
    public void applyOnResourceShouldSetNotFoundOnEmptyPath() throws Exception {
        when(request.getPathInfo()).thenReturn("");

        ResourceAccessor.applyOnResource(request, response, resource -> operationAppliedOn = resource);

        verify(response).setStatus(400);
    }

    @Test
    public void applyOnResourceShouldSetNotFoundOnUnderSizedPath() throws Exception {
        when(request.getPathInfo()).thenReturn("a");

        ResourceAccessor.applyOnResource(request, response, resource -> operationAppliedOn = resource);

        verify(response).setStatus(400);
    }

    @Test
    public void applyOnResourceShouldSetOkOnValidPath() throws Exception {
        when(request.getPathInfo()).thenReturn("/resource");

        ResourceAccessor.applyOnResource(request, response, resource -> operationAppliedOn = resource);

        verify(response).setStatus(200);
    }

}
