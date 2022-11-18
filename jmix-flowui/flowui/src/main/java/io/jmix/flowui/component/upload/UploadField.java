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

import com.vaadin.flow.component.HasText;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.Messages;
import io.jmix.flowui.component.HasRequired;
import io.jmix.flowui.component.SupportsValidation;
import io.jmix.flowui.component.delegate.FieldDelegate;
import io.jmix.flowui.component.validation.Validator;
import io.jmix.flowui.data.SupportsValueSource;
import io.jmix.flowui.data.ValueSource;
import io.jmix.flowui.exception.ValidationException;
import io.jmix.flowui.kit.component.upload.JmixUploadField;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nullable;

public class UploadField extends JmixUploadField implements SupportsValueSource<byte[]>, SupportsValidation<byte[]>,
        HasRequired, ApplicationContextAware, InitializingBean {

    protected ApplicationContext applicationContext;
    protected Messages messages;

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
    }

    protected void initComponent() {
        fieldDelegate = createFieldDelegate();

        if (fileNameComponent instanceof HasText) {
            ((HasText) fileNameComponent).setText(generateFileName(null, null));
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

    @Nullable
    @Override
    protected String generateFileName(@Nullable byte[] newPresentationValue, @Nullable String uploadedFileName) {
        // Invoked from constructor, messages can be null
        if (messages != null && newPresentationValue == null) {
            return messages.getMessage("uploadField.fileNotSelected");
        }
        return super.generateFileName(newPresentationValue, uploadedFileName);
    }
}
