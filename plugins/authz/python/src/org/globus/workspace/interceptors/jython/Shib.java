/*
 * Copyright 1999-2006 University of Chicago
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

package org.globus.workspace.interceptors.jython;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.wsrf.impl.security.authorization.attributes.SAMLAttribute;
import org.globus.wsrf.impl.security.authorization.attributes.SAMLAttributeInformation;
import org.python.util.PythonInterpreter;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class Shib {

    private static Log logger = LogFactory.getLog(Shib.class.getName());

    /**
     * best to call this from VomsUtil which does class check
     * @param credSet
     * @param interp
     */
    public static void handleShib(Set credSet, PythonInterpreter interp) {

        Iterator creds = credSet.iterator();
        while (creds.hasNext()) {
            Object o = creds.next();
            if (o instanceof SAMLAttributeInformation) {
                handle((SAMLAttributeInformation)o, interp);
                break;
            }
        }
    }

    private static void handle(SAMLAttributeInformation samlinfo,
                               PythonInterpreter interp) {
        logger.debug("adding SAML attributes");

        Vector attrVec = samlinfo.getAttrs();
        if (attrVec.isEmpty()) {
            logger.debug("SAML attribute information present, but empty");
        } else {
            logger.debug("found " + attrVec.size() + " attribute(s)");
            interp.exec(WorkspacePythonAuthorization.NEW_SHIB);

            Iterator iter = attrVec.iterator();
            while (iter.hasNext()) {
                interp.exec(WorkspacePythonAuthorization.NEW_SAMLATTR);
                SAMLAttribute attr = (SAMLAttribute)iter.next();

                WorkspacePythonAuthorization.setString(
                        "attr.name", attr.getName(), interp);
                WorkspacePythonAuthorization.setString(
                        "attr.namespace", attr.getNamespace(), interp);

                String[] values = attr.getValue();
                for (int i = 0; i < values.length; i++) {
                    interp.exec(
                         "attr.values.append('" + values[i] + "')");
                }

                interp.exec("shib.attributes.append(attr)");
            }
        }

    }

}
