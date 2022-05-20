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

package io.jmix.securityflowui.access;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.ViewAccessChecker;
import io.jmix.core.AccessManager;
import io.jmix.core.security.SecurityContextHelper;
import io.jmix.flowui.accesscontext.FlowuiShowScreenContext;
import io.jmix.flowui.screen.Screen;
import io.jmix.flowui.screen.UiController;
import io.jmix.flowui.sys.UiDescriptorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

public class FlowuiScreenAccessChecker implements BeforeEnterListener {

    private static final Logger log = LoggerFactory.getLogger(FlowuiScreenAccessChecker.class);

    protected final AccessManager accessManager;
    protected final AccessAnnotationChecker accessAnnotationChecker;

    protected boolean enabled;

    protected Class<? extends Component> loginScreen;

    public FlowuiScreenAccessChecker(AccessManager accessManager) {
        this(true, accessManager);
    }

    public FlowuiScreenAccessChecker(boolean enabled, AccessManager accessManager) {
        this.enabled = enabled;
        this.accessManager = accessManager;
        this.accessAnnotationChecker = new AccessAnnotationChecker();
    }

    /**
     * Enables the access checker.
     * <p>
     * This must be called for the access checker to perform any checks.
     * By default, the access checker is disabled.
     */
    public void enable() {
        this.enabled = true;
    }

    public void setLoginScreen(Class<? extends Component> loginScreen) {
        throwIfLoginScreenSet();
        this.loginScreen = loginScreen;
    }

    protected void throwIfLoginScreenSet() {
        if (this.loginScreen != null) {
            throw new IllegalStateException("Already using "
                    + this.loginScreen.getName() + " as the login screen");
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (!enabled) {
            return;
        }
        Class<?> targetView = beforeEnterEvent.getNavigationTarget();

        VaadinServletRequest vaadinServletRequest = VaadinServletRequest.getCurrent();
        if (vaadinServletRequest == null) {
            // This is in a background thread and we cannot access the request
            // to check access
            log.warn("Preventing navigation to " + targetView.getName()
                    + " because no HTTP request is available for checking access.");
            beforeEnterEvent.rerouteToError(NotFoundException.class);
            return;
        }

        log.debug("Checking access for view {}", targetView.getName());
        if (loginScreen != null && targetView == loginScreen) {
            log.debug("Allowing access for login view {}", targetView.getName());
            return;
        }

        HttpServletRequest httpServletRequest = vaadinServletRequest.getHttpServletRequest();
        boolean hasAccess = isHasAccess(targetView, httpServletRequest);

        if (!hasAccess && isSupportedScreen(targetView)) {
            hasAccess = isHasSecurityPermission(targetView);
        }

        if (hasAccess) {
            log.debug("Allowed access to view {}", targetView.getName());
            return;
        }

        log.debug("Denied access to view {}", targetView.getName());
        if (isAnonymousAuthentication()) {
            httpServletRequest.getSession()
                    // Use constant from Vaadin class to avoid
                    // VaadinSavedRequestAwareAuthenticationSuccessHandler extension
                    .setAttribute(ViewAccessChecker.SESSION_STORED_REDIRECT, beforeEnterEvent
                            .getLocation().getPathWithQueryParameters());
            if (loginScreen != null) {
                beforeEnterEvent.forwardTo(loginScreen);
            } else {
                // Prevent the view from being created
                // TODO: gg, throw an exception?
                beforeEnterEvent.rerouteToError(NotFoundException.class);
            }
        } else if (isProductionMode(beforeEnterEvent)) {
            // Intentionally does not reveal if the route exists
            // TODO: gg, throw an exception?
            beforeEnterEvent.rerouteToError(NotFoundException.class);
        } else {
            // TODO: gg, throw an exception?
            beforeEnterEvent.rerouteToError(NotFoundException.class, "Access denied");
        }
    }

    protected boolean isHasAccess(Class<?> targetView, HttpServletRequest httpServletRequest) {
        return accessAnnotationChecker.hasAccess(targetView, httpServletRequest);
    }

    protected boolean isHasSecurityPermission(Class<?> targetView) {
        String screenId = UiDescriptorUtils.getInferredScreenId(targetView);
        FlowuiShowScreenContext context = new FlowuiShowScreenContext(screenId);
        accessManager.applyRegisteredConstraints(context);
        return context.isPermitted();
    }

    protected boolean isSupportedScreen(Class<?> targetView) {
        return Screen.class.isAssignableFrom(targetView)
                && targetView.getAnnotation(UiController.class) != null;
    }

    protected boolean isProductionMode(BeforeEnterEvent beforeEnterEvent) {
        return beforeEnterEvent.getUI().getSession().getConfiguration()
                .isProductionMode();
    }

    protected boolean isAnonymousAuthentication() {
        Authentication authentication = SecurityContextHelper.getAuthentication();
        return authentication == null ||
                authentication instanceof AnonymousAuthenticationToken;
    }
}
