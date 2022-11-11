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
import {Upload} from '@vaadin/upload/src/vaadin-upload.js';

class JmixUpload extends Upload {

    static get is() {
        return 'jmix-upload';
    }

    static get properties() {
        return {
            readOnly: {
                type: Boolean,
                value: false,
            },
            enabled: {
                type: Boolean,
                value: true,
            }
        };
    }

    ready() {
        super.ready();
        this.$.fileList.hidden = true;
    }

    static get observers() {
        return [
            '_onEnabledChanged(enabled)',
        ]
    }

    /**
     * @param e
     * @private
     * @override
     */
    _onAddFilesTouchEnd(e) {
        // Don't open add file dialog if component is readOnly or disabled
        if (this.readOnly || !this.enabled) {
            e.stopPropagation();
            e.preventDefault();
            return
        }
        super._onAddFilesTouchEnd(e)
    }

    /**
     * @param e
     * @private
     * @override
     */
    _onAddFilesClick(e) {
        // Don't open add file dialog if component is readOnly or disabled
        if (this.readOnly || !this.enabled) {
            e.stopPropagation();
            e.preventDefault();
            return
        }
        super._onAddFilesClick(e)
    }

    _onEnabledChanged(enabled) {
        // disable upload component
        const uploadComponent = this.shadowRoot.querySelector('slot').children[0];
        if (uploadComponent) {
            if (enabled) {
                uploadComponent.removeAttribute("disabled")
            } else {
                uploadComponent.setAttribute("disabled", "");
            }
        }
    }
}

customElements.define(JmixUpload.is, JmixUpload);

export {JmixUpload};