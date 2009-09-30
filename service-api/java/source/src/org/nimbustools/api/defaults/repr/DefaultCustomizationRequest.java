/*
 * Copyright 1999-2008 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.nimbustools.api.defaults.repr;

import org.nimbustools.api._repr._CustomizationRequest;

public class DefaultCustomizationRequest implements _CustomizationRequest {

    private String pathOnVM;
    private String content;

    public String getPathOnVM() {
        return this.pathOnVM;
    }

    public void setPathOnVM(String pathOnVM) {
        this.pathOnVM = pathOnVM;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String toString() {
        final String x;
        if (this.content != null) {
            x = "[content-is-present]";
        } else {
            x = "[content-is-not-present]";
        }
        return "DefaultCustomizationRequest{" +
                "pathOnVM='" + this.pathOnVM + '\'' +
                ", content=" + x +
                '}';
    }
}
