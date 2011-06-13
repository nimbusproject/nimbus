package org.globus.workspace.remoting.admin.defaults;

import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.admin.RemoteAdminToolsManagement;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.Manager;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;

/**
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
 *
 * User: rrusnak
 */
public class DefaultRemoteAdminToolsMgmt implements RemoteAdminToolsManagement {

    protected Manager manager;
    protected ReprFactory reprFactory;


    public void initialize() throws Exception {
    }

    public Hashtable getAllRunningVMs() throws RemoteException {
        try {
            VM[] allRunningVms = manager.getGlobalAll();
            int vmSize = allRunningVms.length;
            Hashtable ht = new Hashtable(vmSize);

            for(int i = 0; i < vmSize; i++) {
                ArrayList al = new ArrayList();
                String id = allRunningVms[i].getID();
                Caller caller = allRunningVms[i].getCreator();

                al.add(allRunningVms[i].getGroupID());
                al.add(allRunningVms[i].getCreator().getIdentity());
                al.add(allRunningVms[i].getState().getState());
                ht.put(id, al);
            }
            return ht;
        }
        catch (ManageException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void createVM() {
        //final Caller caller = this.populator().getCaller();
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public void setReprFactory(ReprFactory reprFactory) {
        this.reprFactory = reprFactory;
    }


}
