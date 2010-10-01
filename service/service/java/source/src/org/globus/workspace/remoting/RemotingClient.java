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
package org.globus.workspace.remoting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.newsclub.net.unix.rmi.AFUNIXNaming;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

public class RemotingClient {

    private static final Log logger =
            LogFactory.getLog(RemotingClient.class.getName());

    private File socketDirectory;
    private Registry rmiRegistry;


    public void initialize() throws Exception {
        if (this.socketDirectory == null) {
            throw new IllegalStateException("socketDirectory must be specified");
        }
        if (!this.socketDirectory.isDirectory()) {
            throw new IllegalStateException("socketDirectory must be an existing directory");
        }

        AFUNIXNaming naming = AFUNIXNaming.getInstance(this.socketDirectory);
        logger.debug("Socket directory: " + naming.getSocketFactory().getSocketDir());

        this.rmiRegistry = naming.getRegistry();
    }

    public Remote lookup(String name) throws RemoteException, NotBoundException {
        if (name == null) {
            throw new IllegalArgumentException("name may not be null");
        }

        if (this.rmiRegistry == null) {
            throw new IllegalStateException("Remoting client is uninitialized");
        }

        return this.rmiRegistry.lookup(name);
    }

    public File getSocketDirectory() {
        return socketDirectory;
    }

    public void setSocketDirectory(File socketDirectory) {
        this.socketDirectory = socketDirectory;
    }

    public void setSocketResource(Resource socketResource) throws IOException {
        this.socketDirectory = socketResource.getFile();
    }
}
