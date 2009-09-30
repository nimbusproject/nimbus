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
import org.globus.voms.VomsCredentialInformation;
import org.python.util.PythonInterpreter;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class Voms {

    private static Log logger = LogFactory.getLog(Voms.class.getName());

    /**
     * best to call this from VomsUtil which does class check
     * @param credSet
     * @param interp
     */
    public static void handleVoms(Set credSet, PythonInterpreter interp) {

        Iterator creds = credSet.iterator();
        while (creds.hasNext()) {
            Object o = creds.next();
            if (o instanceof VomsCredentialInformation) {
                handle((VomsCredentialInformation)o, interp);
                break;
            }
        }
    }

    private static void handle(VomsCredentialInformation vomsinfo,
                               PythonInterpreter interp) {
        logger.debug("adding VOMS attributes");

        Vector attrVec = vomsinfo.getAttrs();

        //note: if there are no roles (e.g., when a regular proxy is used),
        //      the vector object will be present, but have size 0
        if (attrVec == null) {
            logger.error("cannot retrieve roles from credential" +
                    "information");
            return;
        }


        if (attrVec.isEmpty()) {
            logger.debug("VOMS attribute information present, but empty");
        } else {
            logger.debug("found " + attrVec.size() + " attribute(s)");
            interp.exec(WorkspacePythonAuthorization.NEW_VOMS);

            Iterator iter = attrVec.iterator();
            while (iter.hasNext()) {
                String fqan = (String) iter.next();
                if (fqan != null) {
                    interp.exec(
                         "voms.attributes.append('" + fqan + "')");
                }
            }
        }
        WorkspacePythonAuthorization.setString(
                  "voms.VO", vomsinfo.getVO(), interp);
        WorkspacePythonAuthorization.setString(
                  "voms.hostport", vomsinfo.getHostport(), interp);

    }
}
