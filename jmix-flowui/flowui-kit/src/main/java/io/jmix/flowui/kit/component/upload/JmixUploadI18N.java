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

import com.vaadin.flow.component.upload.UploadI18N;

import javax.annotation.Nullable;

/**
 * POJO class, contains i18n properties for {@link JmixUpload} and {@link JmixFileStorageUploadField}.
 */
public class JmixUploadI18N extends UploadI18N {

    public static final String FILE_NOT_SELECTED = "File is not selected";
    public static final String UPLOAD = "Upload";

    protected UploadDialog uploadDialog;

    protected String uploadText;
    protected String fileNotSelectedText;

    /**
     * @return text that should be shown in the upload button or {@code null} if not set
     */
    @Nullable
    public String getUploadText() {
        return uploadText;
    }

    /**
     * Sets the text that should be shown in the upload button.
     *
     * @param uploadText text to set
     */
    public void setUploadText(@Nullable String uploadText) {
        this.uploadText = uploadText;
    }

    /**
     * @return text that is shown when file is not uploaded or {@code null} if not set
     */
    @Nullable
    public String getFileNotSelectedText() {
        return fileNotSelectedText;
    }

    /**
     * Sets text that is shown when file is not uploaded
     *
     * @param fileNotSelectedText text to set
     */
    public void setFileNotSelectedText(@Nullable String fileNotSelectedText) {
        this.fileNotSelectedText = fileNotSelectedText;
    }

    /**
     * @return properties for the upload dialog or {@code null} if not set
     */
    @Nullable
    public UploadDialog getUploadDialog() {
        return uploadDialog;
    }

    /**
     * Sets properties for the upload dialog
     *
     * @param uploadDialog upload dialog properties to set
     */
    public void setUploadDialog(@Nullable UploadDialog uploadDialog) {
        this.uploadDialog = uploadDialog;
    }

    protected void copyNotNullPropertiesTo(JmixUploadI18N target) {
        if (uploadDialog != null) {
            target.uploadDialog = target.uploadDialog == null ? new UploadDialog() : target.uploadDialog;
            target.uploadDialog.title = uploadDialog.title == null ? target.uploadDialog.title : uploadDialog.title;
            target.uploadDialog.cancel = uploadDialog.cancel == null ? target.uploadDialog.cancel : uploadDialog.cancel;
        }
    }

    /**
     * Contains properties for the upload dialog.
     */
    public static class UploadDialog {

        protected String title;
        protected String cancel;

        /**
         * @return text that is shown in the dialog header
         */
        @Nullable
        public String getTitle() {
            return title;
        }

        /**
         * Sets text that is shown in the dialog header.
         *
         * @param title text to set
         * @return current instance
         */
        public UploadDialog setTitle(@Nullable String title) {
            this.title = title;
            return this;
        }

        /**
         * @return text from the cancel button
         */
        @Nullable
        public String getCancel() {
            return cancel;
        }

        /**
         * Sets text to the cancel button.
         *
         * @param cancel text to set
         * @return current instance
         */
        public UploadDialog setCancel(@Nullable String cancel) {
            this.cancel = cancel;
            return this;
        }
    }
}
