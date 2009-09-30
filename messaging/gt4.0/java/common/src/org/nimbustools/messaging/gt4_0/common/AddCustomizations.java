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

package org.nimbustools.messaging.gt4_0.common;

import org.nimbustools.api.repr.CustomizationRequest;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.services.metadata.MetadataServer;
import org.nimbustools.api._repr._CustomizationRequest;
import org.nimbustools.api._repr._CreateRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AddCustomizations {

    protected static final Log logger =
            LogFactory.getLog(AddCustomizations.class.getName());

    public static void addAll(_CreateRequest creq,
                              ReprFactory reprFactory,
                              MetadataServer mdServer) {

        if (creq == null) {
            return;
        }
        
        final CustomizationRequest custReq =
                    AddCustomizations.metadataServerURL(reprFactory,
                                                        mdServer);

        CustomizationRequest[] newreqs = null;
        if (custReq != null) {
            CustomizationRequest[] reqs = creq.getCustomizationRequests();
            if (reqs == null) {
                newreqs = new CustomizationRequest[1];
                newreqs[0] = custReq;
            } else {
                newreqs = new CustomizationRequest[reqs.length + 1];
                System.arraycopy(reqs, 0, newreqs, 0, reqs.length);
                newreqs[reqs.length] = custReq;
            }
        }

        if (newreqs != null) {
            logger.debug("adding extra customization request " +
                    "(metadata server url)");
            creq.setCustomizationRequests(newreqs);
        }
    }

    public static CustomizationRequest metadataServerURL(ReprFactory reprFactory,
                                                         MetadataServer mdServer) {
        if (reprFactory == null || mdServer == null) {
            return null;
        }

        if (mdServer.isEnabled()) {

            final String path = mdServer.getCustomizationPath();
            final String url = mdServer.getContactURL();

            // all conditions are met, cause a new customization task to
            // be added:
            if (path != null && url != null) {
                final _CustomizationRequest req =
                        reprFactory._newCustomizationRequest();
                req.setContent(url);
                req.setPathOnVM(path);
                return req;
            }
        }

        return null;
    }
}
