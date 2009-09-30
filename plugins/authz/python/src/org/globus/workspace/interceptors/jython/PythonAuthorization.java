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
import org.globus.jython.JythonLoader;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.globus.workspace.service.binding.authorization.CreationAuthorizationCallout;
import org.globus.workspace.service.binding.authorization.Restrictions;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.persistence.DataConvert;

import javax.security.auth.Subject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class PythonAuthorization implements CreationAuthorizationCallout {

    public boolean isEnabled() {
        return true;
    }

    private static Log logger =
        LogFactory.getLog(PythonAuthorization.class.getName());

    private final DataConvert dataConvert;

    public PythonAuthorization(DataConvert dataConvert) {
        if (dataConvert == null) {
            throw new IllegalArgumentException("dataConvert may not be null");
        }
        this.dataConvert = dataConvert;
    }

    private String scriptLocation;

    public void setScriptLocation(String scriptLocation) {
        this.scriptLocation = scriptLocation;
    }

    public void initializeCallout() throws Exception {

        if (this.scriptLocation == null) {
            throw new Exception("scriptLocation parameter missing");
        }

        this.scriptLocation = this.scriptLocation.trim();

        final File script = new File(this.scriptLocation);

        if (!script.isAbsolute()) {
            throw new Exception("Cannot handle relative paths " +
                    "for authz script '" + this.scriptLocation + "'");
        }

        if (!script.canRead()) {
            throw new Exception("File cannot be read: "
                                                    + this.scriptLocation);
        }

        // TODO: should make sure file is not world writeable

        try {
            JythonLoader.loadJython();
        } catch (ClassNotFoundException e) {
            throw new Exception("problem loading Jython," +
                    " jython.jar is probably not in the classpath", e);
        }

        // for pre-compiling, otherwise it would have been
        // interp.execfile(new FileInputStream(codefile))

        BufferedReader in = null;
        String code = null;
        try {
            in = new BufferedReader(new FileReader(script));

            final StringBuffer buf = new StringBuffer();
            String line = in.readLine();
            while (line != null) {
                buf.append(line);
                buf.append("\n");
                line = in.readLine();
            }
            code = buf.toString();
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception e) {logger.error("",e);} }
        }

        if (code != null) {
            WorkspacePythonAuthorization.setScript(code);
        } else {
            throw new Exception("no code");
        }
    }

    // ResourceRequestDeniedException is for Deny + message for client
    public Integer isPermitted(VirtualMachine[] bindings,
                               String callerDN,
                               Subject subject,
                               Long elapsedMins,
                               Long reservedMins,
                               int numWorkspaces)

            throws AuthorizationException,
                   ResourceRequestDeniedException {

        final Restrictions restr = new Restrictions(); // unused currently
        return WorkspacePythonAuthorization.isPermitted(
                                   bindings, subject, callerDN, restr,
                                   elapsedMins, reservedMins, numWorkspaces,
                                   this.dataConvert);
    }
}
