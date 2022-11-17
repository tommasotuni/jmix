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

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.upload.*;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.shared.Registration;

import javax.annotation.Nullable;

/**
 * @param <V> value type
 */
@Tag("jmix-upload-field")
@JsModule("./src/upload/jmix-upload-field.js")
public abstract class UploadFieldBase<V> extends AbstractField<UploadFieldBase<V>, V> implements HasLabel, HasHelper {

    protected static final String INPUT_CONTAINER_CLASS_NAME = "jmix-upload-field-input-container";
    protected static final String VALUE_CONTAINER_CLASS_NAME = "jmix-upload-field-value-input-container";

    protected JmixUpload upload;
    protected HasComponents content;
    protected HasComponents valueContainer;

    protected V internalValue;

    public UploadFieldBase(V defaultValue) {
        super(defaultValue);

        content = createContentComponent();
        initContentComponent(content);

        valueContainer = createValueContainer();
        initValueContainer(valueContainer);

        upload = createUploadComponent();
        initUploadComponent(upload);

        attachContent(content);
    }

    protected JmixUpload createUploadComponent() {
        return new JmixUpload();
    }

    protected HasComponents createValueContainer() {
        return new Div();
    }

    protected HasComponents createContentComponent() {
        return new Div();
    }

    protected Component createFileNameComponent(String fileName) {
        Span span = new Span();
        span.setText(fileName);
        return span;
    }

    protected Component createClearButton() {
        Icon icon = new Icon("lumo", "cross");
        icon.addClickListener(this::onClearButtonClick);
        return icon;
    }

    protected void onClearButtonClick(ClickEvent<Icon> clickEvent) {
        upload.clearFileList();
        setInternalValue(getEmptyValue());
    }

    protected void attachContent(HasComponents content) {
        content.add(upload, (Component) valueContainer);

        content.getElement().setAttribute("slot", "input");
        getElement().appendChild(content.getElement());
    }

    protected void initUploadComponent(JmixUpload upload) {
        upload.setDropAllowed(false);
        upload.setReceiver(createUploadReceiver());
    }

    protected void initValueContainer(HasComponents component) {
        if (component instanceof HasStyle) {
            ((HasStyle) component).addClassName(VALUE_CONTAINER_CLASS_NAME);
        }
    }

    protected void initContentComponent(HasComponents component) {
        if (component instanceof HasStyle) {
            ((HasStyle) component).addClassName(INPUT_CONTAINER_CLASS_NAME);
        }
        if (component instanceof HasSize) {
            ((HasSize) component).setWidthFull();
        }
    }

    protected <T> T getValueContainer() {
        return (T) valueContainer;
    }

    protected void attachSucceededListener(ComponentEventListener<SucceededEvent> listener) {
        upload.addSucceededListener(listener);
    }

    protected Receiver createUploadReceiver() {
        return new MemoryBuffer();
    }

    @Override
    public void setValue(V value) {
        setInternalValue(value);
    }

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

        upload.setReadOnly(readOnly);
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

    @Override
    public Registration addValueChangeListener(ValueChangeListener<? super ComponentValueChangeEvent<UploadFieldBase<V>, V>> listener) {
        @SuppressWarnings("rawtypes")
        ComponentEventListener componentListener = event -> {
            ComponentValueChangeEvent<UploadFieldBase<V>, V> valueChangeEvent =
                    (ComponentValueChangeEvent<UploadFieldBase<V>, V>) event;
            listener.valueChanged(valueChangeEvent);
        };

        return ComponentUtil.addListener(this, ComponentValueChangeEvent.class, componentListener);
    }

    protected void setInternalValue(@Nullable V value) {
        setInternalValue(value, null, false);
    }

    protected void setInternalValue(@Nullable V value, @Nullable String uploadedFileName, boolean fromClient) {
        if (valueEquals(internalValue, value)) {
            return;
        }

        V oldValue = internalValue;
        internalValue = value;

        // update file name
        setPresentationValue(value, uploadedFileName);

        ComponentValueChangeEvent<UploadFieldBase<V>, V> event =
                new ComponentValueChangeEvent<>(this, this, oldValue, fromClient);
        fireEvent(event);
    }

    @Override
    protected void setPresentationValue(V newPresentationValue) {
        // do nothing
    }

    protected abstract void setPresentationValue(@Nullable V newPresentationValue, @Nullable String uploadedFileName);
}
