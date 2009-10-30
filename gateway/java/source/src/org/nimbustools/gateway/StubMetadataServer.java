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

package org.nimbustools.gateway;

import org.nimbustools.api.services.metadata.MetadataServer;
import org.nimbustools.api.services.metadata.MetadataServerException;
import org.nimbustools.api.services.metadata.MetadataServerUnauthorizedException;

public class StubMetadataServer implements MetadataServer {
    public String getResponse(String target, String remoteAddress)
            throws MetadataServerException, MetadataServerUnauthorizedException {
        return null;
    }

    public String getCustomizationPath() {
        return null;
    }

    public String getContactURL() {
        return null;
    }

    public boolean isEnabled() {
        return false;
    }
}
