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

package org.globus.workspace.service.impls;

import org.globus.workspace.persistence.impls.VMPersistence;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.DataConvert;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.BindNetwork;
import org.globus.workspace.service.binding.BindingAdapter;
import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.Lager;
import org.globus.workspace.LockManager;
import org.globus.workspace.scheduler.Scheduler;
import commonj.timers.TimerManager;

public class OneXenVM extends StatefulResourceImpl
                      implements Xen, VMPersistence {

    public OneXenVM(PersistenceAdapter persistenceImpl,
                    BindingAdapter bindingImpl,
                    GlobalPolicies globalsImpl,
                    DataConvert dataConvertImpl,
                    Lager lagerImpl,
                    BindNetwork bindNetworkImpl,
                    Scheduler schedulerImpl,
                    LockManager lockMgrImpl,
                    StateTransition transitionImpl,
                    TimerManager timerManagerImpl) {
        
        super(persistenceImpl, bindingImpl, globalsImpl, dataConvertImpl,
              lagerImpl, bindNetworkImpl, schedulerImpl, lockMgrImpl, transitionImpl,
              timerManagerImpl);
    }

    public void setWorkspace(VirtualMachine virtualMachine) {
        this.vm = virtualMachine;
    }
}
