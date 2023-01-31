/*
 * Copyright 2022 Haulmont.
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

package io.jmix.flowui.testassist.support;

import io.jmix.core.JmixOrder;
import io.jmix.flowui.exception.UiExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(JmixOrder.LOWEST_PRECEDENCE - 50)
@Component("test_assist_TestAssistExceptionHandler")
public class TestAssistExceptionHandler implements UiExceptionHandler {

    @Override
    public boolean handle(Throwable exception) {
        // Any exception thrown from application are handled by UiExceptionHandlers bean.
        // However, the test in which exception was thrown considered as passed. So
        // throw exception to fail the test.
        throw new IllegalStateException("Exception occurred during the test", exception);
    }
}
