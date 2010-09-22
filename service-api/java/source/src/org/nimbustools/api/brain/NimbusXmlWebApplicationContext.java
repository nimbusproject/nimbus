/*
 * Copyright 1999-2010 University of Chicago
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
package org.nimbustools.api.brain;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class NimbusXmlWebApplicationContext extends XmlWebApplicationContext {

    NimbusHomePathResolver nimbusHomePathResolver;

    public NimbusXmlWebApplicationContext() {
        super();
    }

    @Override
    protected Resource getResourceByPath(String path) {

        if (this.nimbusHomePathResolver == null) {
            this.nimbusHomePathResolver = new NimbusHomePathResolver();
        }

        final String resolved = this.nimbusHomePathResolver.resolvePath(path);

        if (resolved != null) {
            return new FileSystemResource(resolved);
        }
        return super.getResourceByPath(path);
    }
}
