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

package org.apache.james.webadmin.service;

import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.apache.james.mailrepository.api.MailKey;
import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.mailrepository.api.MailRepositoryPath;
import org.apache.james.mailrepository.api.MailRepositoryStore;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.util.OptionalUtils;
import org.apache.james.util.streams.Iterators;
import org.apache.mailet.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;

public class ReprocessingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReprocessingService.class);

    public static class MissingKeyException extends RuntimeException {
        MissingKeyException(MailKey key) {
            super(key.asString() + " can not be found");
        }
    }

    interface MissingMailPolicy {
        Optional<Mail> validate(Optional<Mail> mail, MailKey key);
    }

    static class Performer {
        private final MissingMailPolicy missingMailPolicy;
        private final MailQueue mailQueue;
        private final Optional<String> targetProcessor;

        Performer(MissingMailPolicy missingMailPolicy, MailQueue mailQueue, Optional<String> targetProcessor) {
            this.missingMailPolicy = missingMailPolicy;
            this.mailQueue = mailQueue;
            this.targetProcessor = targetProcessor;
        }

        private void reprocess(MailRepository repository, MailKey key) {
            try {
                missingMailPolicy.validate(Optional.ofNullable(repository.retrieve(key)), key)
                    .ifPresent(Throwing.<Mail>consumer(
                        mail -> {
                            targetProcessor.ifPresent(mail::setState);
                            mailQueue.enQueue(mail);
                            repository.remove(key);
                        })
                        .sneakyThrow());
            } catch (MessagingException e) {
                throw new RuntimeException("Error encountered while reprocessing mail " + key.asString(), e);
            }
        }
    }

    private static final MissingMailPolicy STRICT_MISSING_MAIL_POLICY = (mail, key) -> OptionalUtils.executeIfEmpty(mail,
        () -> {
            throw new MissingKeyException(key);
        });

    private static final MissingMailPolicy LENIENT_MISSING_MAIL_POLICY = (mail, key) -> OptionalUtils.executeIfEmpty(mail,
        () -> LOGGER.warn("Missing key {} during reprocessing. This might be caused by a concurrent remove.", key));

    private final MailQueueFactory<?> mailQueueFactory;
    private final MailRepositoryStoreService mailRepositoryStoreService;

    @Inject
    public ReprocessingService(MailQueueFactory<?> mailQueueFactory,
                               MailRepositoryStoreService mailRepositoryStoreService) {
        this.mailQueueFactory = mailQueueFactory;
        this.mailRepositoryStoreService = mailRepositoryStoreService;
    }

    public void reprocessAll(MailRepositoryPath path, Optional<String> targetProcessor, String targetQueue, Consumer<MailKey> keyListener) throws MailRepositoryStore.MailRepositoryStoreException, MessagingException {
        Performer performer = new Performer(LENIENT_MISSING_MAIL_POLICY, getMailQueue(targetQueue), targetProcessor);

        mailRepositoryStoreService
            .getRepositories(path)
            .forEach(Throwing.consumer((MailRepository repository) ->
                Iterators.toStream(repository.list())
                    .peek(keyListener)
                    .forEach(key -> performer.reprocess(repository, key))));
    }

    public void reprocess(MailRepositoryPath path, MailKey key, Optional<String> targetProcessor, String targetQueue) throws MailRepositoryStore.MailRepositoryStoreException, MessagingException {
        Performer performer = new Performer(STRICT_MISSING_MAIL_POLICY, getMailQueue(targetQueue), targetProcessor);

        mailRepositoryStoreService
            .getRepositories(path)
            .forEach(repository -> performer.reprocess(repository, key));
    }

    private MailQueue getMailQueue(String targetQueue) {
        return mailQueueFactory.getQueue(targetQueue)
            .orElseThrow(() -> new RuntimeException("Can not find queue " + targetQueue));
    }
}
