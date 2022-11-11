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
import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

@Tag("jmix-upload-field")
@JsModule("./src/upload/jmix-upload-field.js")
public abstract class UploadFieldBase<V> extends AbstractField<UploadFieldBase<V>, V> implements HasLabel, HasHelper {

    protected JmixUpload upload;

    public UploadFieldBase(V defaultValue) {
        super(defaultValue);

        upload = createUploadComponent();
        initUploadComponent(upload);

        attachUploadComponent(upload);
    }

    protected JmixUpload createUploadComponent() {
        return new JmixUpload();
    }

    protected void attachUploadComponent(JmixUpload upload) {
        upload.getElement().setAttribute("slot", "input");
        getElement().appendChild(upload.getElement());
    }

    protected void initUploadComponent(JmixUpload upload) {
        upload.setAutoUpload(false);
        upload.setDropAllowed(false);
        upload.setReceiver(createUploadReceiver());
    }

    protected Receiver createUploadReceiver() {
        return new MemoryBuffer();
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
}
