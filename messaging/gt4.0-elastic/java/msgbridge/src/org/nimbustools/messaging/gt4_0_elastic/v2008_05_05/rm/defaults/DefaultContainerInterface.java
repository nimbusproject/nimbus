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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.defaults;

import org.globus.wsrf.security.SecurityManager;
import org.nimbustools.api._repr._Caller;
import org.nimbustools.api.brain.ModuleLocator;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.ContainerInterface;

import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;

public class DefaultContainerInterface implements ContainerInterface {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final ReprFactory repr;
    

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultContainerInterface(ModuleLocator locator) throws Exception {
        if (locator == null) {
            throw new IllegalArgumentException("locator may not be null");
        }
        this.repr = locator.getReprFactory();
    }


    // -------------------------------------------------------------------------
    // implement ContainerInterface
    // -------------------------------------------------------------------------

    public Caller getCaller() throws RemoteException {

        final String callerDN = SecurityManager.getManager().getCaller();
        if (callerDN == null) {
            throw new RemoteException("Cannot determine caller");
        }
        
        final _Caller caller = this.repr._newCaller();
        caller.setIdentity(callerDN);
        return caller;
    }

    public String getOwnerID(Caller caller) throws CannotTranslateException {

        if (caller == null) {
            throw new CannotTranslateException("missing caller");
        }

        final String id = caller.getIdentity();
        if (id == null) {
            throw new CannotTranslateException("missing caller identity");
        }

        if (!HashUtil.isInitialized()) {
            throw new CannotTranslateException("hashing algorithm failed to initialize");
        }

        try {
            return HashUtil.hashDN(id);
        } catch (NoSuchAlgorithmException e) {
            throw new CannotTranslateException(e.getMessage(), e);
        }
    }
}
