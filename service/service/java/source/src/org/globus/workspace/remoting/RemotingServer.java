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
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class RemotingServer {

    private static final Log logger =
            LogFactory.getLog(RemotingServer.class.getName());

    private File socketDirectory;
    private Map<String, Remote> bindings;

    public void initialize() throws RemoteException, AlreadyBoundException {
        if (this.socketDirectory == null) {
            throw new IllegalStateException("socketDirectory must be specified");
        }
        if (!this.socketDirectory.isDirectory()) {
            throw new IllegalStateException("socketDirectory must be an existing directory");
        }

        if (this.bindings == null || this.bindings.isEmpty()) {
            throw new IllegalStateException("at least one binding must be specified");
        }

        final AFUNIXNaming naming = AFUNIXNaming.getInstance(this.socketDirectory);
        logger.debug("Socket directory: " + naming.getSocketFactory().getSocketDir());

        final Registry registry = naming.createRegistry();

        for (final String bindingName : bindings.keySet()) {
            final Remote obj = bindings.get(bindingName);
            if (obj == null) {
                throw new IllegalStateException("Binding object "+ bindingName + "' is null");
            }

            logger.debug("Binding " + obj.toString() + " to name '"+ bindingName + "'");

            final Remote remote = UnicastRemoteObject.exportObject(obj, 0,
                naming.getSocketFactory(), naming.getSocketFactory());
            registry.bind(bindingName, remote);
        }
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

    public Map<String, Remote> getBindings() {
        return bindings;
    }

    public void setBindings(Map<String, Remote> bindings) {
        this.bindings = bindings;
    }
}
