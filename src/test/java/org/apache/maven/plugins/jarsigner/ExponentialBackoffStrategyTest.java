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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExponentialBackoffStrategyTest {

    @Test
    public void calculateNextDelay_initialDelay500ms_maxDelay30s() {
        ExponentialBackoffStrategy backoffStrategy =
                new ExponentialBackoffStrategy(Duration.ofMillis(500), Duration.ofSeconds(30));
        assertEquals(Duration.ofMillis(500), backoffStrategy.calculateNextDelay());
        assertEquals(Duration.ofMillis(1000), backoffStrategy.calculateNextDelay());
        assertEquals(Duration.ofMillis(2000), backoffStrategy.calculateNextDelay());
    }

    @Test
    public void reset_attemptsShouldBeZeroAfterReset() {
        ExponentialBackoffStrategy backoffStrategy =
                new ExponentialBackoffStrategy(Duration.ofMillis(500), Duration.ofSeconds(30));

        // Perform attempts
        backoffStrategy.calculateNextDelay();
        backoffStrategy.calculateNextDelay();
        backoffStrategy.calculateNextDelay();

        // Reset
        backoffStrategy.reset();

        // After reset, the delay should be the same as the initial delay (500ms)
        assertEquals(Duration.ofMillis(500), backoffStrategy.calculateNextDelay());
    }

    @Test(timeout = 5000)
    public void calculateNextDelay_doesNotExceedMaxDelay() {
        ExponentialBackoffStrategy backoffStrategy =
                new ExponentialBackoffStrategy(Duration.ofMillis(500), Duration.ofSeconds(30));

        // Perform attempts until maxDelay is reached or timeout occurs
        while (backoffStrategy.calculateNextDelay().compareTo(Duration.ofSeconds(30)) < 0) {
            // Continue
        }

        // After reaching maxDelay, further attempts should still result in maxDelay
        assertEquals(Duration.ofSeconds(30), backoffStrategy.calculateNextDelay());

        // Make sure it can handle lots of more attempts without crashing
        for (int i = 0; i < 10000; i++) {
            assertEquals(Duration.ofSeconds(30), backoffStrategy.calculateNextDelay());
        }
    }
}
