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

package io.jmix.flowui.xml.layout.loader.component;

import io.jmix.flowui.data.SupportsValueSource;
import io.jmix.flowui.kit.component.upload.AbstractSingleUploadField;
import io.jmix.flowui.xml.layout.loader.AbstractComponentLoader;
import io.jmix.flowui.xml.layout.support.DataLoaderSupport;

public abstract class AbstractUploadFieldLoader<C extends AbstractSingleUploadField & SupportsValueSource> extends AbstractComponentLoader<C> {

    protected DataLoaderSupport dataLoaderSupport;

    @Override
    public void loadComponent() {
        getDataLoaderSupport().loadData(resultComponent, element);

        componentLoader().loadSizeAttributes(resultComponent, element);
        componentLoader().loadLabel(resultComponent, element);
        componentLoader().loadEnabled(resultComponent, element);
        componentLoader().loadClassNames(resultComponent, element);

        componentLoader().loadHelperText(resultComponent, element);
        componentLoader().loadValueAndElementAttributes(resultComponent, element);

        getLoaderSupport().loadInteger(element, "maxFileSize", resultComponent::setMaxFileSize);

        getLoaderSupport().loadResourceString(element, "uploadButtonText",
                getContext().getMessageGroup(), resultComponent::setUploadButtonText);
        getLoaderSupport().loadStringVarargs(element, "acceptedFileTypes",
                resultComponent::setAcceptedFileTypes);
    }

    protected DataLoaderSupport getDataLoaderSupport() {
        if (dataLoaderSupport == null) {
            dataLoaderSupport = applicationContext.getBean(DataLoaderSupport.class, context);
        }
        return dataLoaderSupport;
    }
}
