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

package org.globus.workspace.service.impls.async;

import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.Lager;
import org.globus.workspace.WorkspaceUtil;
import org.globus.workspace.persistence.DataConvert;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.LockAcquisitionFailure;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ResourceMessage implements WorkspaceConstants {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(ResourceMessage.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final WorkspaceHome home;
    protected final DataConvert dataConvert;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public ResourceMessage(WorkspaceHome workspaceHome,
                           DataConvert dataConvertImpl) {
        if (workspaceHome == null) {
            throw new IllegalArgumentException("workspaceHome may not be null");
        }
        this.home = workspaceHome;

        if (dataConvertImpl == null) {
            throw new IllegalArgumentException("dataConvertImpl may not be null");
        }
        this.dataConvert = dataConvertImpl;
    }


    // -------------------------------------------------------------------------
    // MESSAGES
    // -------------------------------------------------------------------------

    /**
     * Entry point for asynchronous callbacks.  Conforming WorkspaceRequest
     * tasks are expected to send the value of notify property in the
     * WorkspaceRequestContext unless it is STATE_INVALID.
     *
     * Deciding to move to corrupted etc. should be decided in the
     * service implementation, task implementations should not ever
     * call anything in the service that mutates
     *
     * Right now a simple pass through to StatefulResourceImpl, attempting
     * to keep async package somewhat encapsulated...
     *
     * @param id workspid
     * @param notify new state (or intended state if err)
     * @param err if not null, means failure
     */
    public void message(int id, int notify, Exception err) {
        
        // do not throw IllegalArgumentException, just log and return
        if (WorkspaceUtil.isInvalidState(notify)) {
            final String msg = "received invalid state " + notify
                                + ", id = " + id + " exception = " + err;
            logger.error(msg, err);
            return;
        }

        boolean corrupted = false;
        if (err != null) {
            logger.error("Problem moving " + Lager.id(id) + " to state '" +
                        this.dataConvert.stateName(notify) + "': " + err.getMessage());
            corrupted = true;
        }

        final InstanceResource resource;
        try {
            resource = this.home.find(id);
        } catch (ManageException e) {
            logger.error("",e);
            return;
        } catch (DoesNotExistException e) {
            // error already logged above
            final String errStr ="received notification concerning resource " +
                    "that does not exist anymore.  " + Lager.id(id) +
                    ", state = " + this.dataConvert.stateName(notify);
            logger.error(errStr);
            return;
        }

        try {

            // the corrupted states can be used to track the target state
            // at the time of corruption (notify() of success and error send
            // the same state, the target state that task was supposed to
            // take the resource to)

            if (corrupted) {
                if (notify == STATE_DESTROYING) {
                    resource.setState(STATE_DESTROY_FAILED, err);
                    resource.setTargetState(STATE_DESTROY_FAILED);
                } else if (notify >= STATE_FIRST_LEGAL && notify < STATE_DESTROYING) {
                    resource.setState(notify + STATE_CORRUPTED, err);
                } else {
                    logger.error("erroneous error notification, target " +
                            "state not legal, val=" + notify);
                    resource.setState(STATE_CORRUPTED_GENERIC, err);
                }

            } else {
                if (notify == STATE_DESTROYING) {
                    resource.setState(STATE_DESTROY_SUCCEEDED, null);
                } else {
                    resource.setState(notify, null);
                }
            }

        } catch (LockAcquisitionFailure e) {
            // notify() is one way only
            logger.fatal("notify->setState failed to " +
                    "acquire lock: " + e.getMessage(), e);
        } catch (ManageException e) {
            logger.fatal("Failed to record " + Lager.id(id) +
                                 " as DestroyFailed: " + e.getMessage());
        }
    }
}
