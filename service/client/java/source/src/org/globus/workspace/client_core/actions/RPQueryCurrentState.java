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

package org.globus.workspace.client_core.actions;

import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.WSAction_Instance;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.subscribe_tools.TerminationConduit;
import org.globus.workspace.client_core.subscribe_tools.StateChangeConduit;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.repr.StateOrTerminated;
import org.globus.workspace.client_core.utils.StateUtils;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.nimbustools.messaging.gt4_0.generated.WorkspacePortType;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.message.MessageElement;
import org.oasis.wsrf.properties.GetResourcePropertyResponse;
import org.oasis.wsrf.properties.ResourceUnknownFaultType;

public class RPQueryCurrentState extends WSAction_Instance implements Runnable {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final String addr;
    private final int id;

    private StateChangeConduit stateChangeConduit;
    private TerminationConduit termConduit;

    // epr is not derived easily from portType if in that mode
    private EndpointReferenceType stateEPR;
    private EndpointReferenceType termEPR;

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Instance
     */
    public RPQueryCurrentState(EndpointReferenceType epr,
                               StubConfigurator stubConf,
                               Print debug) {
        
        super(epr, stubConf, debug);
        this.id = EPRUtils.getIdFromEPR(epr);
        this.addr = EPRUtils.getServiceURIAsString(epr);
    }

    /**
     * @see WSAction_Instance
     */
    public RPQueryCurrentState(WorkspacePortType instancePortType,
                               Print debug) {
        super(instancePortType, debug);
        this.id = -1;
        this.addr = null;
    }

    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    public StateChangeConduit getStateChangeConduit() {
        return this.stateChangeConduit;
    }

    /**
     * For subscription mode, provide conduit and EPR to notify about.
     *
     * @param stateConduit  may be null
     * @param stateEpr      must NOT be null if stateConduit is not null
     */
    public void setStateConduit(StateChangeConduit stateConduit,
                                EndpointReferenceType stateEpr) {

        if (stateConduit != null && stateEpr == null) {
            throw new IllegalArgumentException("if stateConduit is not null, " +
                    "stateEpr must be not null");
        }

        this.stateChangeConduit = stateConduit;
        this.stateEPR = stateEpr;
    }

    public TerminationConduit getTerminationOccuredConduit() {
        return this.termConduit;
    }


    /**
     * For subscription mode, provide conduit and EPR to notify about.
     * 
     * @param terminationConduit  may be null
     * @param termEpr             must NOT be null if stateConduit is not null
     */
    public void setTerminationConduit(TerminationConduit terminationConduit,
                                      EndpointReferenceType termEpr) {

        if (terminationConduit != null && termEpr == null) {
            throw new IllegalArgumentException("if terminationConduit is " +
                    "not null, termEpr must be not null");
        }
        
        this.termConduit = terminationConduit;
        this.termEPR = termEpr;
    }


    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    /**
     * Necessary to implement Runnable because of quirk when running repeatedly
     * via scheduleWithFixedDelay
     * 
     * @see org.globus.workspace.client_core.subscribe_tools.internal.PollingSubscriptionMasterImpl#trackCommon(org.globus.workspace.client_core.repr.Workspace, org.globus.workspace.client_core.subscribe_tools.StateChangeListener, org.globus.workspace.client_core.subscribe_tools.TerminationListener)
     */
    public void run() {
        try {
            this.call();
        } catch (Exception e) {
            // call() already logged exception
        }
    }

    protected Object action() throws Exception {
        return this.queryOnce();
    }

    /**
     * The action
     *
     * @return StateOrTerminated never null
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem problem running
     */
    public StateOrTerminated queryOnce() throws ParameterProblem,
                                                ExecutionProblem {
        this.validateAll();

        final StateOrTerminated stateOrTerm = this.execWrap();
        if (stateOrTerm.getState() != null &&
                this.stateChangeConduit != null) {
            this.stateChangeConduit.stateChange(this.stateEPR,
                                                stateOrTerm.getState());
        }

        if (stateOrTerm.isTerminated() &&
                this.termConduit != null) {
            this.termConduit.terminationOccured(this.termEPR);
        }

        return stateOrTerm;
    }

    private StateOrTerminated execWrap() throws ExecutionProblem {

        try {

            final GetResourcePropertyResponse resp =
                    ((WorkspacePortType) this.portType).getResourceProperty(
                                Constants_GT4_0.RP_CURRENT_STATE);

            final MessageElement[] me;
            if (resp == null) {
                me = null;
            } else {
                me = resp.get_any();
            }

            final String workspaceID = Integer.toString(this.id);
            
            final State state =
                    StateUtils.fromWireHelper(workspaceID, this.addr, me);

            return new StateOrTerminated(state);
            
        } catch (ResourceUnknownFaultType unknown) {
            return new StateOrTerminated(null);
        } catch (Exception e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }
    }

}
