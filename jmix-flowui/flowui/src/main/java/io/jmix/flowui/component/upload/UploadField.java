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
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.Messages;
import io.jmix.flowui.component.HasRequired;
import io.jmix.flowui.component.SupportsValidation;
import io.jmix.flowui.component.delegate.FieldDelegate;
import io.jmix.flowui.component.validation.Validator;
import io.jmix.flowui.data.SupportsValueSource;
import io.jmix.flowui.data.ValueSource;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.exception.ValidationException;
import io.jmix.flowui.kit.component.upload.JmixUploadField;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nullable;

// todo rp move interfaces to abstract class ?
public class UploadField extends JmixUploadField implements SupportsValueSource<byte[]>, SupportsValidation<byte[]>,
        HasRequired, ApplicationContextAware, InitializingBean {

    protected ApplicationContext applicationContext;
    protected Messages messages;
    protected Downloader downloader;

    protected FieldDelegate<UploadField, byte[], byte[]> fieldDelegate;

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
    }

    protected void initComponent() {
        fieldDelegate = createFieldDelegate();

        // Takes the file name from Messages
        if (fileNameComponent instanceof HasText) {
            ((HasText) fileNameComponent).setText(generateFileName());
        }
        if (fileNameComponent instanceof ClickNotifier) {
            ((ClickNotifier<?>) fileNameComponent).addClickListener(this::onFileNameClick);
        }

        attachSucceededListener(this::onSucceededEvent);
    }

    protected FieldDelegate<UploadField, byte[], byte[]> createFieldDelegate() {
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

    protected void onFileNameClick(ClickEvent<?> clickEvent) {
        if (!isEnabled()) {
            return;
        }

        byte[] value = getValue();
        if (value == null) {
            return;
        }

        String fileName = generateFileName();
        if (Strings.isNullOrEmpty(fileName)) {
            fileName = convertValueToFileName(value);
        }

        downloader.download(value, fileName);
    }

    @Override
    protected String generateFileName() {
        // Invoked from constructor, messages can be null
        if (messages != null && getValue() == null) {
            return messages.getMessage("uploadField.fileNotSelected");
        }
        return super.generateFileName();
    }

    @Override
    protected String convertValueToFileName(byte[] value) {
        return messages.formatMessage("", "uploadField.noFileName",
                FileUtils.byteCountToDisplaySize(value.length));
    }
}
