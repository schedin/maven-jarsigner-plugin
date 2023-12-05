/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.jarsigner;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class representing an exponential backoff strategy.
 */
public class ExponentialBackoffStrategy {
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final AtomicInteger attempts;

    /**
     * Constructs an ExponentialBackoffStrategy.
     *
     * @param initialDelay The initial delay before the first retry.
     * @param maxDelay     The maximum delay between retries.
     */
    public ExponentialBackoffStrategy(Duration initialDelay, Duration maxDelay) {
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
        this.attempts = new AtomicInteger(0);
    }

    /**
     * Calculates the delay for the next retry based on an exponential backoff formula.
     *
     * @return The delay for the next retry.
     */
    public Duration calculateNextDelay() {
        int currentAttempts = attempts.getAndIncrement();

        long delayMillis = (long) (initialDelay.toMillis() * Math.pow(2, currentAttempts));
        delayMillis = Math.min(delayMillis, maxDelay.toMillis());

        return Duration.ofMillis(delayMillis);
    }

    /**
     * Resets the attempts count to zero. Can be used after a successful attempt.
     */
    public void reset() {
        attempts.set(0);
    }
}
