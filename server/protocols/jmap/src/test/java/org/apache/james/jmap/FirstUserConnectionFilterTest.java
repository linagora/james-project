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
package org.apache.james.jmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.lib.mock.InMemoryUsersRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.Times;
import org.slf4j.Logger;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;

public class FirstUserConnectionFilterTest {

    private FirstUserConnectionFilter sut;
    private InMemoryUsersRepository usersRepository;
    private MailboxManager mailboxManager;

    @Before
    public void setup() {
        usersRepository = new InMemoryUsersRepository();
        mailboxManager = mock(MailboxManager.class);
        sut = new FirstUserConnectionFilter(usersRepository, mailboxManager);
    }

    @Test
    public void filterShouldDoNothingOnNullSession() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        sut.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertThat(usersRepository.list()).isEmpty();
    }

    @Test
    public void filterShouldNotThrowOnConcurrentUserCreation() throws Exception {
        String userName = "userName";
        MailboxSession.User user = mock(MailboxSession.User.class);
        when(user.getUserName())
            .thenReturn(userName);
        MailboxSession mailboxSession = mock(MailboxSession.class);
        when(mailboxSession.getUser())
            .thenReturn(user);
        when(mailboxSession.getPersonalSpace())
            .thenReturn("#private");

        when(mailboxManager.createSystemSession(any(String.class), any(Logger.class)))
            .thenReturn(mailboxSession);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(AuthenticationFilter.MAILBOX_SESSION))
            .thenReturn(mailboxSession);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        SleepingFilter sleepingFilter = new SleepingFilter(usersRepository, mailboxManager);
        Stopwatch stopwatch = Stopwatch.createStarted();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(new Runnable() {
            
            @Override
            public void run() {
                try {
                    sleepingFilter.doFilter(request, response, chain);
                } catch (IOException | ServletException e) {
                    Throwables.propagate(e);
                }
            }
        });
        executorService.execute(new Runnable() {
            
            @Override
            public void run() {
                try {
                    sleepingFilter.doFilter(request, response, chain);
                } catch (IOException | ServletException e) {
                    Throwables.propagate(e);
                }
            }
        });

        executorService.awaitTermination(3, TimeUnit.SECONDS);
        executorService.shutdown();
        verify(chain, new Times(2)).doFilter(request, response);

        assertThat(stopwatch.elapsed(TimeUnit.SECONDS)).isGreaterThanOrEqualTo(2);
        assertThat(usersRepository.list()).containsExactly(userName);
    }

    private static class SleepingFilter extends FirstUserConnectionFilter {

        public SleepingFilter(UsersRepository usersRepository, MailboxManager mailboxManager) {
            super(usersRepository, mailboxManager);
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            try {
                super.doFilter(request, response, chain);
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Throwables.propagate(e);
            }
        }
    }
}

