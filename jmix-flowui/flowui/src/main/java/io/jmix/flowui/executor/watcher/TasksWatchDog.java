/*
 * Copyright 2019 Haulmont.
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

package io.jmix.flowui.executor.watcher;

import io.jmix.flowui.executor.FlowuiBackgroundTaskProperties;
import io.jmix.flowui.executor.impl.TaskHandlerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class TasksWatchDog extends AbstractTasksWatchDog {
    private static final Logger log = LoggerFactory.getLogger(TasksWatchDog.class);

    protected FlowuiBackgroundTaskProperties properties;

    public TasksWatchDog(FlowuiBackgroundTaskProperties properties) {
        this.properties = properties;
    }

    @Override
    protected ExecutionStatus getExecutionStatus(long actualTimeMs, TaskHandlerImpl taskHandler) {
        long timeout = taskHandler.getTimeoutMs();

        // kill tasks, which do not update status for latency milliseconds
        long latencyMs = TimeUnit.SECONDS.toMillis(properties.getLatencyTimeoutSeconds());
        if (timeout > 0 && (actualTimeMs - taskHandler.getStartTimeStamp()) > timeout + latencyMs) {
            if (log.isTraceEnabled()) {
                log.trace("Latency timeout is exceeded for task: {}", taskHandler.getTask());
            }
            return ExecutionStatus.SHOULD_BE_KILLED;
        }

        if (timeout > 0 && (actualTimeMs - taskHandler.getStartTimeStamp()) > timeout) {
            if (log.isTraceEnabled()) {
                log.trace("Timeout is exceeded for task: {}", taskHandler.getTask());
            }
            return ExecutionStatus.TIMEOUT_EXCEEDED;
        }

        return ExecutionStatus.NORMAL;
    }
}