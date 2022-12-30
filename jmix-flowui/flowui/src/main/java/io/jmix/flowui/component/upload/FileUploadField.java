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

package io.jmix.flowui.component.upload;

import com.google.common.base.Strings;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.Messages;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.HasRequired;
import io.jmix.flowui.component.SupportsStatusChangeHandler;
import io.jmix.flowui.component.SupportsValidation;
import io.jmix.flowui.component.delegate.FieldDelegate;
import io.jmix.flowui.component.validation.Validator;
import io.jmix.flowui.data.SupportsValueSource;
import io.jmix.flowui.data.ValueSource;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.exception.ValidationException;
import io.jmix.flowui.kit.component.upload.JmixFileUploadField;
import io.jmix.flowui.kit.component.upload.JmixUploadI18N;
import io.jmix.flowui.kit.component.upload.event.FileUploadFileRejectedEvent;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class FileUploadField extends JmixFileUploadField<FileUploadField> implements SupportsValueSource<byte[]>,
        SupportsValidation<byte[]>, SupportsStatusChangeHandler<FileUploadField>, HasRequired, ApplicationContextAware, InitializingBean {

    protected ApplicationContext applicationContext;
    protected Messages messages;
    protected Downloader downloader;
    protected Notifications notifications;

    protected FieldDelegate<FileUploadField, byte[], byte[]> fieldDelegate;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        autowireDependencies();
        initComponent();
    }

    protected void autowireDependencies() {
        messages = applicationContext.getBean(Messages.class);
        downloader = applicationContext.getBean(Downloader.class);
        notifications = applicationContext.getBean(Notifications.class);
    }

    protected void initComponent() {
        fieldDelegate = createFieldDelegate();

        setComponentText(fileNameComponent, generateFileName());
        setComponentText(upload.getUploadButton(), getDefaultUploadText());

        setComponentClickListener(fileNameComponent, this::onFileNameClick);

        applyI18nDefaults();

        attachValueChangeListener(this::onValueChange);

        attachUploadEvents(upload);
    }

    protected FieldDelegate<FileUploadField, byte[], byte[]> createFieldDelegate() {
        return applicationContext.getBean(FieldDelegate.class, this);
    }

    @Nullable
    @Override
    public ValueSource<byte[]> getValueSource() {
        return fieldDelegate.getValueSource();
    }

    @Override
    public void setValueSource(@Nullable ValueSource<byte[]> valueSource) {
        fieldDelegate.setValueSource(valueSource);
    }

    @Override
    public boolean isInvalid() {
        return fieldDelegate.isInvalid();
    }

    @Nullable
    @Override
    public String getRequiredMessage() {
        return fieldDelegate.getRequiredMessage();
    }

    @Override
    public void setRequiredMessage(@Nullable String requiredMessage) {
        fieldDelegate.setRequiredMessage(requiredMessage);
    }

    @Override
    public Registration addValidator(Validator<? super byte[]> validator) {
        return fieldDelegate.addValidator(validator);
    }

    @Override
    public void executeValidators() throws ValidationException {
        fieldDelegate.executeValidators();
    }

    @Override
    public void setInvalid(boolean invalid) {
        fieldDelegate.setInvalid(invalid);
    }

    @Override
    public void setStatusChangeHandler(@Nullable Consumer<StatusContext<FileUploadField>> handler) {
        fieldDelegate.setStatusChangeHandler(handler);
    }

    protected void onFileNameClick(ClickEvent<?> clickEvent) {
        if (!isEnabled()) {
            return;
        }

        byte[] value = getValue();
        if (value != null) {
            downloader.download(value, generateFileName());
        }
    }

    @Override
    protected String generateFileName() {
        // Invoked from constructor, messages can be null
        if (messages != null
                && getValue() == null
                && Strings.isNullOrEmpty(getFileNotSelectedText())) {
            return messages.getMessage("fileUploadField.fileNotSelected");
        }
        return super.generateFileName();
    }

    @Override
    protected String getDefaultUploadText() {
        return messages != null
                ? messages.getMessage("fileUploadField.upload.text")
                : super.getDefaultUploadText();
    }

    @Override
    protected String convertValueToFileName(byte[] value) {
        return messages.formatMessage("", "fileUploadField.noFileName",
                FileUtils.byteCountToDisplaySize(value.length));
    }

    protected void attachValueChangeListener(
            ValueChangeListener<ComponentValueChangeEvent<FileUploadField, byte[]>> listener) {
        addValueChangeListener(listener);
    }

    protected void onValueChange(ComponentValueChangeEvent<FileUploadField, byte[]> event) {
        isInvalid();
    }

    @Override
    protected void onFileRejectedEvent(FileRejectedEvent event) {
        if (!getEventBus().hasListener(FileUploadFileRejectedEvent.class)) {
            notifications.create(event.getErrorMessage())
                    .withType(Notifications.Type.WARNING)
                    .show();
        }
        super.onFileRejectedEvent(event);
    }

    protected void applyI18nDefaults() {
        JmixUploadI18N i18nDefaults = applicationContext.getBean(UploadFieldI18NSupport.class).getI18nUploadField();
        setI18n(i18nDefaults);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        isInvalid();
    }
}
