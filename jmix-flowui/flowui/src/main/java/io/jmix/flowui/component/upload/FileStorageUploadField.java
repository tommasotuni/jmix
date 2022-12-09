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

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageLocator;
import io.jmix.core.Messages;
import io.jmix.flowui.component.HasRequired;
import io.jmix.flowui.component.SupportsValidation;
import io.jmix.flowui.component.delegate.FieldDelegate;
import io.jmix.flowui.component.upload.receiver.TemporaryStorageReceiver;
import io.jmix.flowui.component.validation.Validator;
import io.jmix.flowui.data.SupportsValueSource;
import io.jmix.flowui.data.ValueSource;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.exception.ValidationException;
import io.jmix.flowui.kit.component.upload.FileStoragePutMode;
import io.jmix.flowui.kit.component.upload.JmixFileStorageUploadField;
import io.jmix.flowui.upload.TemporaryStorage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nullable;
import java.util.Objects;

public class FileStorageUploadField extends JmixFileStorageUploadField<FileRef>
        implements SupportsValueSource<FileRef>, SupportsValidation<FileRef>, HasRequired, ApplicationContextAware,
        InitializingBean {

    protected ApplicationContext applicationContext;
    protected TemporaryStorage temporaryStorage;
    protected FileStorageLocator fileStorageLocator;
    protected Downloader downloader;
    protected Messages messages;

    protected FieldDelegate<FileStorageUploadField, FileRef, FileRef> fieldDelegate;

    protected FileStorage fileStorage;

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
        temporaryStorage = applicationContext.getBean(TemporaryStorage.class);
        fileStorageLocator = applicationContext.getBean(FileStorageLocator.class);
        downloader = applicationContext.getBean(Downloader.class);
    }

    protected void initComponent() {
        fieldDelegate = createFieldDelegate();

        upload.setReceiver(applicationContext.getBean(TemporaryStorageReceiver.class));

        if (fileNameComponent instanceof ClickNotifier) {
            ((ClickNotifier<?>) fileNameComponent).addClickListener(this::onFileNameClick);
        }

        attachSucceededListener(this::onSucceedEvent);
    }

    protected FieldDelegate<FileStorageUploadField, FileRef, FileRef> createFieldDelegate() {
        return applicationContext.getBean(FieldDelegate.class, this);
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
    public Registration addValidator(Validator<? super FileRef> validator) {
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

    @Nullable
    @Override
    public ValueSource<FileRef> getValueSource() {
        return fieldDelegate.getValueSource();
    }

    @Override
    public void setValueSource(@Nullable ValueSource<FileRef> valueSource) {
        fieldDelegate.setValueSource(valueSource);
    }

    protected void onSucceedEvent(SucceededEvent event) {
        Upload upload = event.getUpload();
        Receiver receiver = upload.getReceiver();

        if (receiver instanceof TemporaryStorageReceiver) {
            TemporaryStorageReceiver storageReceiver = (TemporaryStorageReceiver) receiver;

            if (getFileStoragePutMode() == FileStoragePutMode.IMMEDIATE) {
                checkFilStorageInitialized();

                FileRef fileRef = temporaryStorage.putFileIntoStorage(
                        storageReceiver.getFileInfo().getId(),
                        storageReceiver.getFileName(),
                        fileStorage);

                setInternalValue(fileRef, true);
            } else {
                internalValue = null;
                setPresentationValue(null);
                // update file name explicitly
                setComponentText(fileNameComponent, storageReceiver.getFileName());
            }
            return;
        }

        throw new IllegalStateException("Unsupported receiver: " + receiver.getClass().getName());
    }

    @Override
    protected String generateFileName() {
        if (getValue() == null) {
            // Invoked from constructor, messages can be null
            return messages != null
                    ? messages.getMessage("fileStorageUploadField.fileNotSelected")
                    : FILE_NOT_SELECTED;
        }
        return getValue().getFileName();
    }

    protected void checkFilStorageInitialized() {
        if (fileStorage == null) {
            if (StringUtils.isNotEmpty(fileStorageName)) {
                fileStorage = fileStorageLocator.getByName(fileStorageName);
            } else {
                fileStorage = fileStorageLocator.getDefault();
            }
        }
    }

    protected void onFileNameClick(ClickEvent<?> clickEvent) {
        if (!isEnabled()) {
            return;
        }

        FileRef value = getValue();
        if (value != null) {
            downloader.download(value);
        }
    }

    @Override
    protected boolean valueEquals(FileRef value1, FileRef value2) {
        return Objects.equals(value1, value2);
    }
}
