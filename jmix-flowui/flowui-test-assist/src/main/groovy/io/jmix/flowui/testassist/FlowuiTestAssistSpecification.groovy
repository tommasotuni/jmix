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

package io.jmix.flowui.testassist

import com.google.common.base.Strings
import com.vaadin.flow.component.UI
import com.vaadin.flow.internal.CurrentInstance
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteConfiguration
import com.vaadin.flow.server.InitParameters
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.VaadinService
import com.vaadin.flow.server.VaadinSession
import com.vaadin.flow.spring.SpringServlet
import com.vaadin.flow.spring.VaadinServletContextInitializer
import io.jmix.core.impl.scanning.AnnotationScanMetadataReaderFactory
import io.jmix.flowui.ViewNavigators
import io.jmix.flowui.sys.ViewControllersConfiguration
import io.jmix.flowui.testassist.support.TestServletContext
import io.jmix.flowui.testassist.support.TestSpringServlet
import io.jmix.flowui.testassist.support.TestVaadinRequest
import io.jmix.flowui.testassist.support.TestVaadinSession
import io.jmix.flowui.view.View
import io.jmix.flowui.view.ViewRegistry
import org.apache.commons.lang3.ArrayUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.mock.web.MockServletConfig
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import spock.lang.Specification

import javax.servlet.ServletException

import static org.apache.commons.lang3.reflect.FieldUtils.getDeclaredField
import static org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE

abstract class FlowuiTestAssistSpecification extends Specification {

    private static final String APP_ID = "testFlowuiAppId"

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ViewNavigators viewNavigators

    @Autowired
    ViewRegistry viewRegistry

    // saving session and UI to avoid it be GC'ed
    protected VaadinSession vaadinSession
    protected UI ui

    void setup() {
        setupAuthentication()
        setupVaadinUi()
    }

    void cleanup() {
        removeAuthentication()
        resetViewRegistry()
    }

    /**
     * Implement to set up authentication before each test.
     * For example, use {@link io.jmix.core.security.SystemAuthenticator#begin()}.
     */
    protected abstract void setupAuthentication();

    /**
     * Implement to set up authentication before each test.
     * For example, use {@link io.jmix.core.security.SystemAuthenticator#end()}.
     */
    protected abstract void removeAuthentication();

    protected void setupVaadinUi() {
        SpringServlet springServlet = new TestSpringServlet(applicationContext, true)

        VaadinServletContextInitializer contextInitializer =
                applicationContext.getBean(VaadinServletContextInitializer.class, applicationContext)
        try {
            TestServletContext servletContext = new TestServletContext()
            servletContext.setAttribute(ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext)
            servletContext.setInitParameter(InitParameters.SERVLET_PARAMETER_ENABLE_DEV_SERVER, "false")

            MockServletConfig mockServletConfig = new MockServletConfig(servletContext)

            // fill servlet context with listeners from VaadinServletContextInitializer
            contextInitializer.onStartup(mockServletConfig.getServletContext())

            servletContext.fireServletContextInitialized()

            springServlet.init(mockServletConfig)
        } catch (ServletException e) {
            throw new IllegalStateException(String.format("Cannot init %s", TestSpringServlet.class.getName()), e)
        }

        VaadinService.setCurrent(springServlet.getService())

        vaadinSession = new TestVaadinSession(springServlet.getService())
        VaadinSession.setCurrent(vaadinSession)

        def request = new TestVaadinRequest(springServlet.getService())
        CurrentInstance.set(VaadinRequest, request)

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))

        ui = new UI()
        ui.getInternals().setSession(vaadinSession)
        ui.doInit(request, 1, APP_ID)
        UI.setCurrent(ui)
    }

    protected void registerViewBasePackages(String[] viewBasePackages) {
        if (ArrayUtils.isEmpty(viewBasePackages)) {
            return
        }

        def metadataReaderFactory = applicationContext.getBean(AnnotationScanMetadataReaderFactory.class)

        def configuration = new ViewControllersConfiguration(applicationContext, metadataReaderFactory)

        def injector = applicationContext.getAutowireCapableBeanFactory()
        injector.autowireBean(configuration)

        configuration.setBasePackages(Arrays.asList(viewBasePackages))

        try {
            def configurationsField = getDeclaredField(ViewRegistry.class,
                    "configurations", true)
            //noinspection unchecked
            def configurations = (Collection<ViewControllersConfiguration>) configurationsField.get(viewRegistry)

            def modifiedConfigurations = new ArrayList<>(configurations)
            modifiedConfigurations.add(configuration)

            configurationsField.set(viewRegistry, modifiedConfigurations)

            getDeclaredField(ViewRegistry.class, "initialized", true)
                    .set(viewRegistry, false)
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot register view base packages", e)
        }

        registerViewRoutes(viewBasePackages)
    }

    protected void registerViewRoutes(String[] viewBasePackages) {
        if (ArrayUtils.isEmpty(viewBasePackages)) {
            return
        }

        def views = viewRegistry.getViewInfos()
                .findAll({ isClassInPackages(it.getControllerClass().getPackageName(), viewBasePackages)})
        views.forEach({
            Class<? extends View> controllerClass = it.getControllerClass()
            Route route = controllerClass.getAnnotation(Route.class)
            if (route == null) {
                return
            }

            RouteConfiguration routeConfiguration = RouteConfiguration.forSessionScope()
            if (Strings.isNullOrEmpty(route.value())
                    || routeConfiguration.isPathAvailable(route.value())) {
                return
            }

            if (route.layout() == UI.class) {
                routeConfiguration.setRoute(route.value(), controllerClass)
            } else {
                routeConfiguration.setRoute(route.value(), controllerClass, route.layout())
            }
        })
    }

    protected boolean isClassInPackages(String classPackage, String[] viewBasePackages) {
        return viewBasePackages.findAll {classPackage.startsWith(it)}.size() > 0
    }

    protected void resetViewRegistry() {
        viewRegistry.configurations = []
        viewRegistry.initialized = false
    }
}
