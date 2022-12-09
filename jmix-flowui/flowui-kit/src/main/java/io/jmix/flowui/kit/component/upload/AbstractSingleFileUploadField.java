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

package io.jmix.flowui.kit.component.upload;

import com.google.common.base.Strings;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.upload.*;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.shared.Registration;

import javax.annotation.Nullable;

/**
 * @param <V> value type
 */
@Tag("jmix-upload-field")
@JsModule("./src/uploadfield/jmix-upload-field.js")
public abstract class AbstractSingleFileUploadField<V> extends AbstractField<AbstractSingleFileUploadField<V>, V>
        implements HasLabel, HasHelper, HasSize, HasStyle {

    protected static final String INPUT_CONTAINER_CLASS_NAME = "jmix-upload-field-input-container";
    protected static final String FILE_NAME_COMPONENT_CLASS_NAME = "jmix-upload-field-file-name";
    protected static final String FILE_NAME_COMPONENT_EMPTY_CLASS_NAME = "empty";
    protected static final String CLEAR_COMPONENT_CLASS_NAME = "jmix-upload-field-clear";

    protected static final String FILE_NOT_SELECTED = "File is not selected";

    protected JmixUpload upload;
    protected HasComponents content;

    protected Component fileNameComponent;
    protected Component clearComponent;

    protected V internalValue;

    public AbstractSingleFileUploadField(V defaultValue) {
        super(defaultValue);

        content = createContentComponent();
        initContentComponent(content);

        upload = createUploadComponent();
        initUploadComponent(upload);

        fileNameComponent = createFileNameComponent();
        initFileNameComponent(fileNameComponent);

        clearComponent = createClearComponent();
        initClearComponent(clearComponent);

        attachContent(content);
    }

    protected JmixUpload createUploadComponent() {
        return new JmixUpload();
    }

    protected HasComponents createContentComponent() {
        return new Div();
    }

    protected Component createFileNameComponent() {
        return new Button();
    }

    protected void initFileNameComponent(Component fileNameComponent) {
        addClassNames(fileNameComponent, FILE_NAME_COMPONENT_CLASS_NAME, FILE_NAME_COMPONENT_EMPTY_CLASS_NAME);

        if (fileNameComponent instanceof HasText) {
            String fileName = Strings.nullToEmpty(generateFileName());
            ((HasText) fileNameComponent).setText(fileName);
        }

        if (fileNameComponent instanceof Button) {
            ((Button) fileNameComponent).addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        }

        setComponentEnabled(fileNameComponent, false);
    }

    protected Component createClearComponent() {
        return new NativeButton();
    }

    protected void initClearComponent(Component clearComponent) {
        addClassNames(clearComponent, CLEAR_COMPONENT_CLASS_NAME);

        if (clearComponent instanceof ClickNotifier) {
            ((ClickNotifier<?>) clearComponent).addClickListener(this::onClearButtonClick);
        }
    }

    protected void onClearButtonClick(ClickEvent<?> clickEvent) {
        if (!isEnabled() || isReadOnly()) {
            return;
        }
        upload.clearFileList();
        setInternalValue(getEmptyValue());
    }

    protected void attachContent(HasComponents content) {
        content.add(upload, fileNameComponent);

        content.getElement().setAttribute("slot", "input");
        getElement().appendChild(content.getElement());
    }

    protected void initUploadComponent(JmixUpload upload) {
        upload.setDropAllowed(false);
        upload.setReceiver(createUploadReceiver());
    }

    protected void initContentComponent(HasComponents component) {
        addClassNames(component, INPUT_CONTAINER_CLASS_NAME);

        if (component instanceof HasSize) {
            ((HasSize) component).setWidthFull();
        }
    }

    protected <T extends HasComponents> T getContent() {
        return (T) content;
    }

    protected void attachSucceededListener(ComponentEventListener<SucceededEvent> listener) {
        upload.addSucceededListener(listener);
    }

    protected Receiver createUploadReceiver() {
        return new MemoryBuffer();
    }

    @Override
    public void setValue(@Nullable V value) {
        setInternalValue(value);
    }

    @Nullable
    @Override
    public V getValue() {
        return internalValue;
    }

    @Override
    public V getEmptyValue() {
        return null;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);

        updateReadOnly();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        upload.setEnabled(enabled);
    }

    /**
     * Add a progress listener that is informed on upload progress.
     *
     * @param listener progress listener to add
     * @return registration for removal of listener
     */
    public Registration addProgressListener(ComponentEventListener<ProgressUpdateEvent> listener) {
        return upload.addProgressListener(listener);
    }

    // todo rp javaDocs
    public Registration addFailedListener(ComponentEventListener<FailedEvent> listener) {
        return upload.addFailedListener(listener);
    }

    public Registration addFinishedListener(ComponentEventListener<FinishedEvent> listener) {
        return upload.addFinishedListener(listener);
    }

    /**
     * Add a succeeded listener that is informed on upload start.
     *
     * @param listener registration for removal of listener
     * @return registration for removal of listener
     */
    public Registration addStartedListener(ComponentEventListener<StartedEvent> listener) {
        return upload.addStartedListener(listener);
    }

    public Registration addSucceededListener(ComponentEventListener<SucceededEvent> listener) {
        return upload.addSucceededListener(listener);
    }

    public Registration addFileRejectedListener(ComponentEventListener<FileRejectedEvent> listener) {
        return upload.addFileRejectedListener(listener);
    }

    public void setMaxFileSize(int maxFileSize) {
        upload.setMaxFileSize(maxFileSize);
    }

    public void setAcceptedFileTypes(String... acceptedFileTypes) {
        upload.setAcceptedFileTypes(acceptedFileTypes);
    }

    public void setUploadButtonText(String text) {
        // todo rp?
    }

    @Override
    public Registration addValueChangeListener(ValueChangeListener<? super ComponentValueChangeEvent<AbstractSingleFileUploadField<V>, V>> listener) {
        @SuppressWarnings("rawtypes")
        ComponentEventListener componentListener = event -> {
            ComponentValueChangeEvent<AbstractSingleFileUploadField<V>, V> valueChangeEvent =
                    (ComponentValueChangeEvent<AbstractSingleFileUploadField<V>, V>) event;
            listener.valueChanged(valueChangeEvent);
        };

        return ComponentUtil.addListener(this, ComponentValueChangeEvent.class, componentListener);
    }

    protected void setInternalValue(@Nullable V value) {
        setInternalValue(value, false);
    }

    protected void setInternalValue(@Nullable V value, boolean fromClient) {
        if (valueEquals(internalValue, value)) {
            return;
        }

        V oldValue = internalValue;
        internalValue = value;

        // update presentation
        setPresentationValue(value);

        ComponentValueChangeEvent<AbstractSingleFileUploadField<V>, V> event =
                new ComponentValueChangeEvent<>(this, this, oldValue, fromClient);
        fireEvent(event);
    }

    @Override
    protected void setPresentationValue(@Nullable V newPresentationValue) {
        getContent().remove(fileNameComponent, clearComponent);
        removeClassNames(fileNameComponent, FILE_NAME_COMPONENT_EMPTY_CLASS_NAME);

        String uploadedFileName = generateFileName();

        setComponentText(fileNameComponent, uploadedFileName);

        getContent().add(fileNameComponent);
        setComponentEnabled(fileNameComponent, newPresentationValue != null);

        if (newPresentationValue == null) {
            addClassNames(fileNameComponent, FILE_NAME_COMPONENT_EMPTY_CLASS_NAME);
        } else {
            getContent().add(clearComponent);
        }
    }

    protected abstract String generateFileName();

    protected void addClassNames(HasElement component, String... classNames) {
        if (component instanceof HasStyle) {
            ((HasStyle) component).addClassNames(classNames);
        }
    }

    protected void removeClassNames(HasElement component, String... classNames) {
        if (component instanceof HasStyle) {
            ((HasStyle) component).removeClassNames(classNames);
        }
    }

    protected void setComponentEnabled(Component component, boolean enabled) {
        if (component instanceof HasEnabled) {
            ((HasEnabled) component).setEnabled(enabled);
        }
    }

    protected void setComponentText(Component component, String text) {
        if (component instanceof HasText) {
            ((HasText) component).setText(Strings.nullToEmpty(text));
        }
    }

    protected void updateReadOnly() {
        upload.setReadOnly(isReadOnly());
        clearComponent.setVisible(!isReadOnly());
    }
}
