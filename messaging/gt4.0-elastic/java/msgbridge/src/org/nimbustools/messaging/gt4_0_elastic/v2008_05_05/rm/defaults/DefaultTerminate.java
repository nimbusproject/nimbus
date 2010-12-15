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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.vm.State;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.InstanceIdSetType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.InstanceIdType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.InstanceStateChangeSetType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.InstanceStateChangeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.TerminateInstancesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.TerminateInstancesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.InstanceStateType;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.IDMappings;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.Terminate;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general.StateMap;

import java.util.LinkedList;
import java.util.List;
import java.rmi.RemoteException;

public class DefaultTerminate implements Terminate {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultTerminate.class.getName());
    
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final IDMappings ids;
    

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultTerminate(IDMappings idsImpl) {

        if (idsImpl == null) {
            throw new IllegalArgumentException("idsImpl may not be null");
        }
        this.ids = idsImpl;
    }


    // -------------------------------------------------------------------------
    // BACKOUT
    // -------------------------------------------------------------------------

    public void backOutCreateResult(CreateResult result,
                                    Caller caller,
                                    Manager manager) {

        if (manager == null) {
            throw new IllegalArgumentException("manager may not be null");
        }
        
        if (result != null) {
            final VM[] vms = result.getVMs();
            if (vms != null && vms.length > 0) {
                for (int i = 0; i < vms.length; i++) {
                    final VM vm = vms[i];
                    if (vm != null) {
                        _backout(vm, manager, caller);
                    }
                }
            }
        }
    }

    private static void _backout(VM vm, Manager manager, Caller caller) {
        try {
            manager.trash(vm.getID(), Manager.INSTANCE, caller);
        } catch (Throwable t) {
            final String msg = "Problem backing out id-" + vm.getID() + ": ";
            if (logger.isDebugEnabled()) {
                logger.error(msg + t.getMessage(), t);
            } else {
                logger.error(msg + t.getMessage());
            }
        }
    }


    // -------------------------------------------------------------------------
    // TERMINATE OPERATION
    // -------------------------------------------------------------------------
    
    public TerminateInstancesResponseType terminate(TerminateInstancesType req,
                                                    Caller caller,
                                                    Manager manager)
            throws RemoteException {

        if (manager == null) {
            throw new IllegalArgumentException("manager may not be null");
        }
        if (caller == null) {
            throw new IllegalArgumentException("caller may not be null");
        }
        if (req == null) {
            throw new IllegalArgumentException("req may not be null");
        }

        final String[] elasticInstIDs = this.getElasticIDs(req);
        if (elasticInstIDs.length == 0) {
            throw new RemoteException("No instance IDs in termination request");
        }

        final String[] managerInstances =
                            this.translateIDs(elasticInstIDs);

        // currentStates array needs to be same length as managerInstances and
        // elasticInstanceIDs, used to correlate later

        final InstanceStateType[] currentStates =
                        new InstanceStateType[managerInstances.length];

        for (int i = 0; i < managerInstances.length; i++) {

            final String mgrInstanceID = managerInstances[i];

            try {
                final VM vm = manager.getInstance(mgrInstanceID);

                if (vm != null) {
                    final State state = vm.getState();
                    if (state != null) {
                        final String mgrState = state.getState();
                        final InstanceStateType ist = new InstanceStateType();
                        ist.setName(StateMap.managerStringToElasticString(mgrState));
                        ist.setCode(StateMap.managerStringToElasticInt(mgrState));
                        currentStates[i] = ist;
                    }
                }

            } catch (DoesNotExistException e) {
                currentStates[i] = null;
            } catch (ManageException e) {
                currentStates[i] = null;
                logger.error(e.getMessage());
            }
        }

        for (int i = 0; i < managerInstances.length; i++) {

            if (currentStates[i] == null) {
                continue;
            }

            final String mgrID = managerInstances[i];
            try {
                manager.trash(mgrID, Manager.INSTANCE, caller);
            } catch (DoesNotExistException e) {
                // do nothing, already accomplished
            } catch (ManageException e) {
                if (logger.isDebugEnabled()) {
                    logger.error(e.getMessage(), e);
                } else {
                    logger.error(e.getMessage());
                }
            }
        }


        final InstanceStateType terminated = new InstanceStateType();
        terminated.setCode(StateMap.STATE_TERMINATED.intValue());
        terminated.setName(StateMap.STATE_TERMINATED_STR);

        final InstanceStateType[] newStates =
                        new InstanceStateType[managerInstances.length];

        for (int i = 0; i < managerInstances.length; i++) {

            if (currentStates[i] == null) {
                continue;
            }

            final String mgrInstanceID = managerInstances[i];

            try {
                final VM vm = manager.getInstance(mgrInstanceID);

                if (vm != null) {
                    final State state = vm.getState();
                    if (state != null) {
                        final String mgrState = state.getState();
                        final InstanceStateType ist = new InstanceStateType();
                        ist.setName(StateMap.managerStringToElasticString(mgrState));
                        ist.setCode(StateMap.managerStringToElasticInt(mgrState));
                        newStates[i] = ist;
                    }
                }

            } catch (DoesNotExistException e) {
                newStates[i] = terminated;
            } catch (ManageException e) {
                newStates[i] = terminated;
                logger.error(e.getMessage());
            }
        }

        final List<InstanceStateChangeType> retList = 
                            new LinkedList<InstanceStateChangeType>();

        for (int i = 0; i < newStates.length; i++) {

            if (currentStates[i] == null) {
                continue;
            }
            
            retList.add(
                    new InstanceStateChangeType(currentStates[i], 
                                                elasticInstIDs[i], 
                                                newStates[i]));
        }


        final InstanceStateChangeType[] tirits =
                retList.toArray(
                            new InstanceStateChangeType[retList.size()]);

        final InstanceStateChangeSetType tirtSet =
                                new InstanceStateChangeSetType();
        tirtSet.setItem(tirits);
        final TerminateInstancesResponseType tirt =
                                new TerminateInstancesResponseType();
        tirt.setInstancesSet(tirtSet);
        return tirt;
    }

    
    /**
     * @param req from the wire
     * @return list of IDs in the request -- could be length zero, never null
     */
    protected String[] getElasticIDs(TerminateInstancesType req) {
        
        if (req == null) {
            throw new IllegalArgumentException("req may not be null");
        }

        final List<String> elasticIDs = new LinkedList<String>();

        final InstanceIdSetType tiitSet = req.getInstancesSet();
        if (tiitSet != null) {

            final InstanceIdType[] tiits = tiitSet.getItem();

            if (tiits == null || tiits.length == 0) {
                return EMPTY_STRING_ARRAY; // *** EARLY RETURN ***
            }

            for (int i = 0; i < tiits.length; i++) {
                final InstanceIdType tiit = tiits[i];
                if (tiit != null) {
                    final String idUntrimmed = tiit.getInstanceId();
                    if (idUntrimmed != null) {
                        final String id = idUntrimmed.trim();
                        if (id != null && id.length() > 0) {
                            elasticIDs.add(id);
                        }
                    }
                }
            }
        }

        if (elasticIDs.isEmpty()) {
            return EMPTY_STRING_ARRAY;
        } else {
            return (String[]) elasticIDs.toArray(new String[elasticIDs.size()]);
        }
    }

    /**
     * Turn given elastic IDs into instance IDs the manager understands.
     * Nothing is returned for unknown elastic IDs (return array may be
     * different length).
     *
     * @param elasticIDs elastic IDs
     * @return array of manager instance IDs -- always same length as input
     */
    protected String[] translateIDs(String[] elasticIDs) {

        if (elasticIDs == null || elasticIDs.length == 0) {
            return EMPTY_STRING_ARRAY; // *** EARLY RETURN ***
        }

        final String[] ret = new String[elasticIDs.length];

        for (int i = 0; i < elasticIDs.length; i++) {
            final String elastic = elasticIDs[i];
            final String mgrID = this.ids.instanceToManager(elastic);
            ret[i] = mgrID;
            if (mgrID == null) {
                logger.warn("Request to terminate unknown " +
                                        "elastic ID '" + elastic + "'");
            }
        }

        return ret;
    }
}
