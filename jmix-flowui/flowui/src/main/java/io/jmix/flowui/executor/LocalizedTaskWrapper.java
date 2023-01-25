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

import io.jmix.core.Messages;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

// todo rp do we need this?
public class LocalizedTaskWrapper<T, V> extends BackgroundTask<T, V> {

    private static final Logger log = LoggerFactory.getLogger(BackgroundWorker.class);

    protected BackgroundTask<T, V> wrappedTask;
    protected View<?> view;
    protected Messages messages;

    public LocalizedTaskWrapper(BackgroundTask<T, V> wrappedTask, View<?> view, Messages messages) {
        super(wrappedTask.getTimeoutSeconds(), view);
        this.wrappedTask = wrappedTask;
        this.view = view;
        this.messages = messages;
    }

    @Override
    public Map<String, Object> getParams() {
        return wrappedTask.getParams();
    }

    @Override
    public V run(TaskLifeCycle<T> lifeCycle) throws Exception {
        return wrappedTask.run(lifeCycle);
    }

    @Override
    public boolean handleException(Exception ex) {
        boolean handled = wrappedTask.handleException(ex);

        if (handled || wrappedTask.getOwnerView() == null) {
            // todo rp remove from parent or close somehow?
//            Screens screens = getScreenContext().getScreens();
//            screens.remove(view);
        } else {
            // todo rp remove from parent or close somehow?
//            Screens screens = getScreenContext().getScreens();
//            screens.remove(view);

            showExecutionError(ex);

            log.error("Exception occurred in background task", ex);

            handled = true;
        }
        return handled;
    }

    @Override
    public boolean handleTimeoutException() {
        boolean handled = wrappedTask.handleTimeoutException();
        if (handled || wrappedTask.getOwnerView() == null) {
            // todo rp remove from parent or close somehow?
//            Screens screens = getScreenContext().getScreens();
//            screens.remove(view);
        } else {
            // todo rp remove from parent or close somehow?
//            Screens screens = getScreenContext().getScreens();
//            screens.remove(view);

            // todo rp get Spring context? Or get somehow notifications
            /*Notifications notifications = getScreenContext().getNotifications();

            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(messages.getMessage(LocalizedTaskWrapper.class, "backgroundWorkProgress.timeout"))
                    .withDescription(messages.getMessage(LocalizedTaskWrapper.class, "backgroundWorkProgress.timeoutMessage"))
                    .show();*/

            handled = true;
        }
        return handled;
    }

    @Override
    public void done(V result) {
        // todo rp remove from parent or close somehow?
//        Screens screens = getScreenContext().getScreens();
//        screens.remove(view);

        try {
            wrappedTask.done(result);
        } catch (Exception ex) {
            // we should show exception messages immediately
            showExecutionError(ex);
        }
    }

    @Override
    public void canceled() {
        try {
            wrappedTask.canceled();
        } catch (Exception ex) {
            // we should show exception messages immediately
            showExecutionError(ex);
        }
    }

    @Override
    public void progress(List<T> changes) {
        wrappedTask.progress(changes);
    }

    protected void showExecutionError(Exception ex) {
        View<?> ownerFrame = wrappedTask.getOwnerView();
        if (ownerFrame != null) {
            // todo rp implement
            /*Dialogs dialogs = getScreenContext().getDialogs();

            dialogs.createExceptionDialog()
                    .withThrowable(ex)
                    .withCaption(messages.getMessage(LocalizedTaskWrapper.class, "backgroundWorkProgress.executionError"))
                    .withMessage(ex.getLocalizedMessage())
                    .show();*/
        }
    }
}