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
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.upload.*;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.*;
import io.jmix.flowui.Notifications;
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
import io.jmix.flowui.kit.component.upload.JmixUploadI18N;
import io.jmix.flowui.kit.component.upload.event.FileUploadFileRejectedEvent;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.upload.TemporaryStorage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class FileStorageUploadField extends JmixFileStorageUploadField<FileStorageUploadField, FileRef>
        implements SupportsValueSource<FileRef>, SupportsValidation<FileRef>, HasRequired, ApplicationContextAware,
        InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(FileStorageUploadField.class);

    protected ApplicationContext applicationContext;
    protected TemporaryStorage temporaryStorage;
    protected FileStorageLocator fileStorageLocator;
    protected Downloader downloader;
    protected Notifications notifications;
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
        messages = applicationContext.getBean(Messages.class);
        notifications = applicationContext.getBean(Notifications.class);
    }

    protected void initComponent() {
        fieldDelegate = createFieldDelegate();

        upload.setReceiver(applicationContext.getBean(TemporaryStorageReceiver.class));

        setComponentText(fileNameComponent, generateFileName());
        setComponentText(upload.getUploadButton(), getDefaultUploadText());

        setComponentClickListener(fileNameComponent, this::onFileNameClick);

        applyI18nDefaults();

        attachValueChangeListener(this::onValueChange);

        attachUploadEvents(upload);
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

    /**
     * Add a succeeded listener that is informed on upload succeeded.
     * <p>
     * For instance, if component has {@link FileStoragePutMode#MANUAL},
     * we can handle the uploading, like the following:
     * <pre>
     *     manuallyControlledField.addFileUploadSucceededListener(event -&gt; {
     *          TemporaryStorageReceiver receiver = event.getReceiver();
     *          File file = temporaryStorage.getFile(receiver.getFileInfo().getId());
     *          if (file != null) {
     *              notifications.create("File is uploaded to temporary storage at " + file.getAbsolutePath())
     *                      .show();
     *          }
     *
     *          FileRef fileRef = temporaryStorage.putFileIntoStorage(receiver.getFileInfo().getId(), event.getFileName());
     *          manuallyControlledField.setValue(fileRef);
     *
     *          notifications.create("Uploaded file: " + event.getFileName())
     *                  .show();
     *      });
     * </pre>
     *
     * @param listener listener to add
     * @return registration for removal of listener
     * @see TemporaryStorageReceiver
     */
    @Override
    public Registration addFileUploadSucceededListener(
            ComponentEventListener<FileUploadSucceededEvent<FileStorageUploadField>> listener) {
        return super.addFileUploadSucceededListener(listener);
    }

    @Override
    protected void onSucceededEvent(SucceededEvent event) {
        Upload upload = event.getUpload();
        Receiver receiver = upload.getReceiver();

        if (receiver instanceof TemporaryStorageReceiver) {
            TemporaryStorageReceiver storageReceiver = (TemporaryStorageReceiver) receiver;

            if (getFileStoragePutMode() == FileStoragePutMode.IMMEDIATE) {
                checkFileStorageInitialized();

                FileRef fileRef = temporaryStorage.putFileIntoStorage(
                        storageReceiver.getFileInfo().getId(),
                        storageReceiver.getFileName(),
                        fileStorage);

                setInternalValue(fileRef, true);
            } else {
                // clear previous value silently
                internalValue = null;
                setPresentationValue(null);
                // update file name explicitly
                setComponentText(fileNameComponent, storageReceiver.getFileName());
            }
        } else {
            throw new IllegalStateException("Unsupported receiver: " + receiver.getClass().getName());
        }
        super.onSucceededEvent(event);
    }

    @Override
    protected String generateFileName() {
        if (getValue() == null) {
            // Invoked from constructor, messages can be null
            return messages != null && Strings.isNullOrEmpty(getFileNotSelectedText())
                    ? messages.getMessage("fileStorageUploadField.fileNotSelected")
                    : super.generateFileName();
        }
        return getValue().getFileName();
    }

    @Override
    protected String getDefaultUploadText() {
        return messages != null
                ? messages.getMessage("fileStorageUploadField.upload.text")
                : super.getDefaultUploadText();
    }

    protected void checkFileStorageInitialized() {
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

    protected void attachValueChangeListener(
            ValueChangeListener<ComponentValueChangeEvent<FileStorageUploadField, FileRef>> listener) {
        addValueChangeListener(listener);
    }

    protected void onValueChange(ComponentValueChangeEvent<FileStorageUploadField, FileRef> event) {
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

    @Override
    protected void onFailedEvent(FailedEvent event) {
        Receiver receiver = upload.getReceiver();
        if (receiver instanceof TemporaryStorageReceiver) {
            UUID tempFileId = ((TemporaryStorageReceiver) receiver).getFileInfo().getId();
            try {
                temporaryStorage.deleteFile(tempFileId);
            } catch (Exception e) {
                if (e instanceof FileStorageException) {
                    FileStorageException fse = (FileStorageException) e;
                    if (fse.getType() != FileStorageException.Type.FILE_NOT_FOUND) {
                        log.warn(String.format("Could not remove temp file %s after broken uploading", tempFileId));
                    }
                }
                log.warn(String.format("Error while delete temp file %s", tempFileId));
            }
        }
        super.onFailedEvent(event);
    }

    protected void applyI18nDefaults() {
        JmixUploadI18N i18nDefaults = applicationContext.getBean(UploadFieldI18NSupport.class)
                .getI18nFileStorageUploadField();
        setI18n(i18nDefaults);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        isInvalid();
    }
}
