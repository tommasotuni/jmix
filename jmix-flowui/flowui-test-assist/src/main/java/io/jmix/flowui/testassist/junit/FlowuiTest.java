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

package io.jmix.flowui.testassist.junit;

import io.jmix.core.security.SystemAuthenticator;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.*;

/**
 * The annotation is used for testing Flow UI views on JUnit.
 * <p>
 * Base example:
 * <pre>
 * &#64;FlowuiTest(authenticator = CustomTestAuthenticator.class)
 * &#64;ExtendWith({SpringExtension.class, FlowuiTestExtension.class})
 * &#64;SpringBootTest
 * public class FlowJunitTest {
 *     &#64;Autowired
 *     private ViewNavigators viewNavigators;
 *
 *     &#64;Test
 *     public void testOrderView() {
 *         viewNavigators.view(OrderListView.class)
 *                 .navigate();
 *     }
 * }
 * </pre>
 * @see FlowuiTestExtension
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({SpringExtension.class, FlowuiTestExtension.class})
@Documented
@Inherited
public @interface FlowuiTest {

    /**
     * Views under these packages will be available in test.
     * <p>
     * Note that depending on the test's configuration all application views may be available.
     *
     * @return array of view packages should be registered
     */
    String[] viewBasePackages() default {};

    /**
     * Class providing authentication management in tests.
     * <p>
     * By default, for authentication is used {@link SystemAuthenticator}.
     *
     * @return class that implement {@link TestAuthenticator}
     */
    Class<? extends TestAuthenticator> authenticator() default NoopAuthenticator.class;

    /**
     * Dummy class.
     */
    abstract class NoopAuthenticator implements TestAuthenticator {
    }
}
