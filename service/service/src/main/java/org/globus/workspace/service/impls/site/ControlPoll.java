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

package org.globus.workspace.service.impls.site;

import org.globus.workspace.Lager;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.service.impls.async.ResourceMessage;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.xen.XenUtil;
import commonj.timers.TimerManager;

/**
 * todo: move to ScheduledThreadPoolExecutor or quartz
 */
public class ControlPoll extends NotificationPoll {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    public static final String STATE_WORKSPACE_PROPAGATE = "propagate";
    public static final String STATE_WORKSPACE_UNPROPAGATE = "unpropagate";
    public static final String STATE_WORKSPACE_START = "start";


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final WorkspaceHome home;
    protected final ResourceMessage resourceMessage;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public ControlPoll(TimerManager timerManager,
                       Lager lagerImpl,
                       WorkspaceHome workspaceHome,
                       ResourceMessage resourceMessageImpl,
                       long delay,
                       String verifiedPath,
                       long filepos,
                       NotificationPollCallback call) throws Exception {
        
        super(timerManager,
              lagerImpl,
              delay,
              verifiedPath,
              filepos,
              call,
              "workspace");
        
        if (workspaceHome == null) {
            throw new IllegalArgumentException("workspaceHome may not be null");
        }
        this.home = workspaceHome;

        if (resourceMessageImpl == null) {
            throw new IllegalArgumentException("resourceMessageImpl may not be null");
        }
        this.resourceMessage = resourceMessageImpl;
    }


    // sample success line from workspace control:
    // workspace-1::start::0
    //
    // sample error line from workspace control:
    // workspace-1::start::1::Some::error::message
    //
    // state values from workspace-control: with 0 or !0 error code:
    //   propagate
    //   unpropagate
    //   start

    protected boolean oneNotification(String name,
                                      String state,
                                      int code,
                                      String message) {

        // TODO: String->ID implementation should be discovered
        final int workspaceID = XenUtil.xenNameToId(name);

        if (workspaceID >= 0) {
            if (this.lager.pollLog) {
                logger.trace("workspace notification read, " +
                             Lager.id(workspaceID) +
                             ", action = " + state +
                             ", code = " + code);
            }
        }

        // TODO: site notifications should be asynchronous (and reliable)
        try {
            if (!this.home.isActiveWorkspaceID(workspaceID)) {
                logger.warn("received workspace-control notification " +
                        "about unknown id " + workspaceID);
                return false;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }

        this.resourceMessage.message(workspaceID,
                                     getIntendedWorkspaceState(state),
                                     getException(code, message));
        return true;
    }
        
    /**
     * @param code error code
     * @param message message from notification producer
     * @return exception with error string as its msg, or null if errcode = 0
     */
    private static WorkspaceException getException(int code, String message) {
        if (code == 0) {
            return null;
        } else {
            if (message == null) {
                return new WorkspaceException(
                        "(no error string in notification(?))");
            } else {
                return new WorkspaceException(message);
            }
        }
    }

    private static int getIntendedWorkspaceState(String action) {
        if (action.equalsIgnoreCase(STATE_WORKSPACE_PROPAGATE)) {
            return WorkspaceConstants.STATE_PROPAGATED;
        } else if (action.equalsIgnoreCase(STATE_WORKSPACE_UNPROPAGATE)) {
            return WorkspaceConstants.STATE_READY_FOR_TRANSPORT;
        } else if (action.equalsIgnoreCase(STATE_WORKSPACE_START)) {
            return WorkspaceConstants.STATE_STARTED;
        } else {
            return WorkspaceConstants.STATE_INVALID;
        }
    }
}
