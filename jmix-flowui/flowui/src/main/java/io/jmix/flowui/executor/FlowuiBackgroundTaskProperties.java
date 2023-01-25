/*
 * Copyright 2020 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.flowui.executor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jmix.flowui.background-task")
@ConstructorBinding
public class FlowuiBackgroundTaskProperties {

    /**
     * Number of background task threads.
     */
    int threadsCount;

    /**
     * Tasks that do not update their status are killed after the timeout (task's timeout plus latency timout).
     */
    int latencyTimeoutSeconds;

    /**
     * Interval for checking timeout of the {@link BackgroundTask} in ms.
     */
    long timeoutCheckInterval;

    public FlowuiBackgroundTaskProperties(
            @DefaultValue("10") int threadsCount,
            @DefaultValue("60") int latencyTimeoutSeconds,
            @DefaultValue("5000") long timeoutCheckInterval
    ) {
        this.threadsCount = threadsCount;
        this.latencyTimeoutSeconds = latencyTimeoutSeconds;
        this.timeoutCheckInterval = timeoutCheckInterval;
    }

    /**
     * @see #threadsCount
     */
    public int getThreadsCount() {
        return threadsCount;
    }

    /**
     * @see #latencyTimeoutSeconds
     */
    public int getLatencyTimeoutSeconds() {
        return latencyTimeoutSeconds;
    }

    /**
     * @see #timeoutCheckInterval
     */
    public long getTimeoutCheckInterval() {
        return timeoutCheckInterval;
    }
}
