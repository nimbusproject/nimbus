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
import org.globus.workspace.client_core.utils.StringUtils;

/**
 * <p>Left as loose container in order to use for various purposes.</p>
 *
 * <p>equals and hashCode are intentionally NOT implemented</p>
 *
 * <p>Check for nulls.</p>
 *
 * <p>If instantiated via a creation class, you can expect these fields to
 * be set for you:</p>
 *
 * <p>
 *  <ul>
 *
 *    <li>requestedInitialState</li>
 *    <li>currentState - always set to "unstaged" after creation.  This could
 *        be wrong information already, it is just the initial state of every
 *        workspace. 
 *    </li>
 *
 *    <li>requestedNetworking</li>
 *    <li>currentNetworking - whatever is returned at creation time</li>
 *
 *    <li>requestedResourceAllocation</li>
 *    <li>currentResourceAllocation - clone of requestedResourceAllocation</li>
 *
 *    <li>requestedShutdownMech</li>
 *
 *    <li>initialSchedule</li>
 *    <li>currentSchedule - clone of initialSchedule</li>
 *
 *    <li>EPR (ID is derived from this)</li>
 *    <li>uriName</li>
 *
 *    <li>Group EPR - only set if a member of a group</li>
 *    <li>Ensemble EPR - only set if a member of an ensemble</li>
 *    <li>Context EPR - only set if a member of a context</li>
 * 
 *  </ul>
 * </p>
 *
 * @see org.globus.workspace.client_core.actions.Create
 * @see org.globus.workspace.client_core.actions.Create_Group
 *
 */
public class Workspace {

    private boolean terminated;
    
    private boolean servicePrinting;

    private String uriName;

    private String givenNameOverride;

    private Integer workspaceID;
    
    private EndpointReferenceType epr;

    private String groupID;

    private EndpointReferenceType groupMemberEPR;

    private String ensembleID;

    private EndpointReferenceType ensembleMemberEPR;

    private String contextID;

    private EndpointReferenceType contextMemberEPR;

    private State requestedInitialState;

    private State currentState;

    private State targetState;

    private Schedule requestedSchedule;

    private Schedule initialSchedule;

    private Schedule currentSchedule;

    private ResourceAllocation requestedResourceAllocation;

    private ResourceAllocation currentResourceAllocation;

    private Networking requestedNetworking;

    private Networking currentNetworking;

    private ShutdownMech requestedShutdownMech;

    private String details = null;

    // future:
    // private DiskCollection diskCollection;

    public boolean isTerminated() {
        return this.terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public synchronized String getUriName() {
        return this.uriName;
    }

    public synchronized void setUriName(String uriString) {
        this.uriName = uriString;
    }

    public String getDisplayName() {

        String name;
        if (this.givenNameOverride != null) {
            name = this.givenNameOverride;
        } else if (this.uriName != null) {
            name = this.uriName;
        } else if (this.workspaceID != null) {
            name = "Workspace #" + this.workspaceID;
        } else {
            name = "[no identification]";
        }

        if (this.servicePrinting) {
            String service = StringUtils.commonAtServiceAddressSuffix(this.epr);
            if (service == null) {
                service = "[[cannot find service address]]";
            }
            name += service;
        }

        return name;
    }

    public void setDetails(String d) {
        this.details = d;
    }

    public String getDetails() {
        return this.details;
    }

    public void setDisplayName(String name) {
        this.givenNameOverride = name;
    }

    public boolean isServicePrinting() {
        return this.servicePrinting;
    }

    /**
     * @param enable  set true to make message print " @ 'service_uri' "
     */
    public void setServicePrinting(boolean enable) {
        this.servicePrinting = enable;
    }

    /**
     * @return workspace ID, will be null if EPR is null
     */
    public synchronized Integer getID() {
        return this.workspaceID;
    }

    public synchronized EndpointReferenceType getEpr() {
        return this.epr;
    }

    /**
     * @param EPR workspace instance EPR, or null
     * @throws IllegalArgumentException if !null, thrown if not a valid
     *                                  workspace instance EPR
     * @see EPRUtils#isInstanceEPR(EndpointReferenceType)
     */
    public synchronized void setEpr(EndpointReferenceType EPR)
                                            throws IllegalArgumentException {
        if (EPR == null) {
            this.epr = null;
            this.workspaceID = null;
            return;
        }

        // new EPR means new ID int cache
        // this serves as a validity check, too
        final int id = EPRUtils.getIdFromEPR(EPR);
        this.workspaceID = new Integer(id);
        this.epr = EPR;
    }

    /**
     * @return group ID, will be null if group EPR is null
     */
    public String getGroupID() {
        return this.groupID;
    }

    public EndpointReferenceType getGroupMemberEPR() {
        return this.groupMemberEPR;
    }

    /**
     * @param EPR workspace group EPR, or null
     * @throws IllegalArgumentException if !null, thrown if not a valid
     *                                  workspace group EPR
     * @see EPRUtils#isGroupEPR(EndpointReferenceType)
     */
    public void setGroupMemberEPR(EndpointReferenceType EPR) {
        if (EPR == null) {
            this.groupMemberEPR = null;
            this.groupID = null;
            return;
        }

        // new EPR means new group ID cache
        // this serves as a validity check, too
        this.groupID = EPRUtils.getGroupIdFromEPR(EPR);
        this.groupMemberEPR = EPR;
    }

    /**
     * @return ensemble ID, will be null if ensemble EPR is null
     */
    public String getEnsembleID() {
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
     * @return ensemble ID, will be null if ensemble EPR is null
     */
    public String getContextID() {
        return this.contextID;
    }

    public synchronized EndpointReferenceType getContextMemberEPR() {
        return this.contextMemberEPR;
    }

    /**
     * @param EPR workspace context EPR, or null
     * @throws IllegalArgumentException if !null, thrown if not a valid
     *                                  workspace context EPR
     * @see EPRUtils#isContextEPR(EndpointReferenceType)
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

    public synchronized State getRequestedInitialState() {
        return this.requestedInitialState;
    }

    public synchronized void setRequestedInitialState(State state) {
        this.requestedInitialState = state;
    }

    public synchronized State getCurrentState() {
        return this.currentState;
    }

    public synchronized void setCurrentState(State state) {
        this.currentState = state;
    }

    public synchronized State getTargetState() {
        return this.targetState;
    }

    public synchronized void setTargetState(State state) {
        this.targetState = state;
    }

    public synchronized Schedule getRequestedSchedule() {
        return this.requestedSchedule;
    }

    public synchronized void setRequestedSchedule(Schedule schedule) {
        this.requestedSchedule = schedule;
    }

    public synchronized Schedule getInitialSchedule() {
        return this.initialSchedule;
    }

    public synchronized void setInitialSchedule(Schedule schedule) {
        this.initialSchedule = schedule;
    }

    public synchronized Schedule getCurrentSchedule() {
        return this.currentSchedule;
    }

    public synchronized void setCurrentSchedule(Schedule schedule) {
        this.currentSchedule = schedule;
    }

    public synchronized ResourceAllocation getRequestedResourceAllocation() {
        return this.requestedResourceAllocation;
    }

    public synchronized void setRequestedResourceAllocation(
                            ResourceAllocation allocation) {
        this.requestedResourceAllocation = allocation;
    }

    public synchronized ResourceAllocation getCurrentResourceAllocation() {
        return this.currentResourceAllocation;
    }

    public synchronized void setCurrentResourceAllocation(
                            ResourceAllocation allocation) {
        this.currentResourceAllocation = allocation;
    }

    public synchronized Networking getRequestedNetworking() {
        return this.requestedNetworking;
    }

    public synchronized void setRequestedNetworking(Networking networking) {
        this.requestedNetworking = networking;
    }

    public synchronized Networking getCurrentNetworking() {
        return this.currentNetworking;
    }

    public synchronized void setCurrentNetworking(Networking networking) {
        this.currentNetworking = networking;
    }

    public ShutdownMech getRequestedShutdownMech() {
        return this.requestedShutdownMech;
    }

    public void setRequestedShutdownMech(ShutdownMech mech) {
        this.requestedShutdownMech = mech;
    }
}
