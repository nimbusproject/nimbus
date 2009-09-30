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

package org.globus.workspace.client_core.repr;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.workspace.client_core.utils.EPRUtils;

/**
 * A convenience class for dealing with groups of workspaces
 */
public class Group {

    private static final Workspace[] NONE = new Workspace[0];

    private Workspace[] workspaceArray = NONE;

    private String groupID;

    private EndpointReferenceType groupEPR;

    private String ensembleID;

    private EndpointReferenceType ensembleMemberEPR;

    private String contextID;

    private EndpointReferenceType contextMemberEPR;

    /* ****** */
    /* FIELDS */
    /* ****** */

    /**
     * @return Workspace[]: workspaces in group,
     *                      never null but array length might be zero
     */
    public synchronized Workspace[] getWorkspaces() {
        return this.workspaceArray;
    }

    /**
     * @param workspaces if null, will set internal field to zero length array
     */
    public synchronized void setWorkspaces(Workspace[] workspaces) {
        if (workspaces == null) {
            this.workspaceArray = NONE;
        } else {
            this.workspaceArray = workspaces;
        }
    }

    /**
     * @return String group ID, will be null if group EPR is null
     */
    public synchronized String getGroupID() {
        return this.groupID;
    }

    public synchronized EndpointReferenceType getGroupEPR() {
        return this.groupEPR;
    }

    /**
     * @param EPR workspace group EPR, or null
     * @throws IllegalArgumentException if !null, thrown if not a valid
     *                                  workspace group EPR
     * @see EPRUtils#isGroupEPR(EndpointReferenceType)
     */
    public synchronized void setGroupEPR(EndpointReferenceType EPR) {
        if (EPR == null) {
            this.groupEPR = null;
            this.groupID = null;
            return;
        }

        // new EPR means new group ID cache
        // this serves as a validity check, too
        this.groupID = EPRUtils.getGroupIdFromEPR(EPR);
        this.groupEPR = EPR;
    }

    /**
     * @return String ensemble ID, will be null if ensemble EPR is null
     */
    public synchronized String getEnsembleID() {
        return this.ensembleID;
    }

    public synchronized EndpointReferenceType getEnsembleMemberEPR() {
        return this.ensembleMemberEPR;
    }

    /**
     * @param EPR workspace ensemble EPR, or null
     * @throws IllegalArgumentException if !null, thrown if not a valid
     *                                  workspace ensemble EPR
     * @see EPRUtils#isEnsembleEPR(EndpointReferenceType)
     */
    public synchronized void setEnsembleMemberEPR(EndpointReferenceType EPR) {
        if (EPR == null) {
            this.ensembleMemberEPR = null;
            this.ensembleID = null;
            return;
        }

        // new EPR means new ensemble ID cache
        // this serves as a validity check, too
        this.ensembleID = EPRUtils.getEnsembleIdFromEPR(EPR);
        this.ensembleMemberEPR = EPR;
    }

    /**
     * @return String context ID, will be null if context EPR is null
     */
    public synchronized String getContextID() {
        return this.contextID;
    }

    public synchronized EndpointReferenceType getContextMemberEPR() {
        return this.contextMemberEPR;
    }

    /**
     * @param EPR workspace context EPR, or null
     * @throws IllegalArgumentException if !null, thrown if not a valid
     *                                  workspace context broker EPR
     * @see EPRUtils#isEnsembleEPR(EndpointReferenceType)
     */
    public synchronized void setContextMemberEPR(EndpointReferenceType EPR) {
        if (EPR == null) {
            this.contextMemberEPR = null;
            this.contextID = null;
            return;
        }

        // new EPR means new context ID cache
        // this serves as a validity check, too
        this.contextID = EPRUtils.getContextIdFromEPR(EPR);
        this.contextMemberEPR = EPR;
    }

    /* ******* */
    /* HELPERS */
    /* ******* */

    /**
     * @return int: the number of workspaces in this group
     */
    public synchronized int size() {
        return this.workspaceArray.length;
    }

    /**
     * <p>Returns true if all workspaces in the group are currently in the
     * state of interest.</p>
     *
     * @see Workspace#getCurrentState()
     * @see State#equals(Object)
     *
     * <p>If some thread has a reference to one of the workspaces and changes
     * the state, there will be a check-then-act problem. This is just a helper
     * method that loops over the stored workspace array.  Coordinate your
     * threads appropriately.</p>
     *
     * <p>If you don't, you could still use this method and get correct
     * information if you ensure you're only querying for <i>terminal</i>
     * states, states your threads wouldn't ever move workspaces out of...</p>
     *
     * @param state state of interest, may not be null
     * @return true if all workspaces in the group are currently in given state
     * 
     */
    public synchronized boolean membersAllInState(State state) {

        if (state == null) {
            throw new IllegalArgumentException("state may not be null");
        }

        for (int i = 0; i < this.workspaceArray.length; i++) {
            final State aState = this.workspaceArray[i].getCurrentState();
            if (aState == null) {
                return false;
            }
            if (!aState.equals(state)) {
                return false;
            }
        }
        
        return true;
    }
    
}
