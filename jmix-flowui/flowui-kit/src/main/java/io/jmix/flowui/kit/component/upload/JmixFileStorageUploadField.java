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

public class JmixFileStorageUploadField<V> extends AbstractSingleFileUploadField<V> {

    protected FileStoragePutMode fileStoragePutMode = FileStoragePutMode.IMMEDIATE;
    protected String fileStorageName;

    public JmixFileStorageUploadField() {
        super(null);
    }

    public JmixFileStorageUploadField(V defaultValue) {
        super(defaultValue);
    }

    // todo rp javDocs
    public FileStoragePutMode getFileStoragePutMode() {
        return fileStoragePutMode;
    }

    public void setFileStoragePutMode(FileStoragePutMode putMode) {
        this.fileStoragePutMode = putMode;
    }

    public String getFileStorageName() {
        return fileStorageName;
    }

    public void setFileStorageName(String fileStorageName) {
        this.fileStorageName = fileStorageName;
    }

    @Override
    protected String generateFileName() {
        // stub
        return FILE_NOT_SELECTED;
    }
}
