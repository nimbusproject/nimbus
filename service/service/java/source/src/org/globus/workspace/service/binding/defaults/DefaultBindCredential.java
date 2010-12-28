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

package org.globus.workspace.service.binding.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.PathConfigs;
import org.globus.workspace.service.binding.BindCredential;
import org.globus.workspace.service.binding.vm.FileCopyNeed;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.services.rm.CreationException;
import org.safehaus.uuid.UUIDGenerator;

import java.io.File;
import java.io.FileOutputStream;

public class DefaultBindCredential implements BindCredential {

    private static final Log logger =
            LogFactory.getLog(DefaultBindCredential.class.getName());

    protected final PathConfigs paths;
    protected final UUIDGenerator uuidGen = UUIDGenerator.getInstance();

    public DefaultBindCredential(PathConfigs paths) {
        if (paths == null) {
            throw new IllegalArgumentException("paths may not be null");
        }
        this.paths = paths;
    }

    public void consume(VirtualMachine vm, final String credential)
            throws CreationException {

        if (vm == null) {
            throw new IllegalArgumentException("vm may not be null");
        }
        else if (credential == null) {
            // return early and leave credential null
            return;
        }

        final String localTempDirectory = this.paths.getLocalTempDirPath();
        final String credentialName = this.uuidGen.generateRandomBasedUUID().toString();
        final String localPath = localTempDirectory + "/" + credentialName;


        try {
            FileOutputStream out = new FileOutputStream(localPath);
            out.write(credential.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            throw new CreationException("Couldn't save credential to " + localTempDirectory
                                         + ". " + e.getMessage());
        }

        final FileCopyNeed need;
        try {
            // FileCopyNeed expects a file in nimbus's tmp, not a full path
            need = new FileCopyNeed(credentialName);
            vm.addFileCopyNeed(need);
        } catch (Exception e) {
            final String err = "problem setting up file copy for credential: " +
                    credentialName + " : " + e.getMessage();
            throw new CreationException(err);
        }

        vm.setCredentialName(credentialName);
    }
}
