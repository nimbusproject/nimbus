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

package org.globus.workspace.client_core.subscribe_tools.internal;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.subscribe_tools.StateChangeListener;
import org.globus.workspace.client_core.subscribe_tools.TerminationListener;
import org.globus.workspace.client_core.subscribe_tools.SubscriptionMaster;
import org.globus.workspace.client_core.subscribe_tools.StateChangeConduit;
import org.globus.workspace.client_core.subscribe_tools.TerminationConduit;
import org.globus.workspace.client_core.utils.EPRUtils;

import java.util.Enumeration;

/**
 * @see SubscriptionMaster
 */
public abstract class SubscriptionMasterImpl implements SubscriptionMaster,
                                                        StateChangeConduit,
                                                        TerminationConduit {

    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(SubscriptionMasterImpl.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final Print pr;
    
    protected final WorkspaceMap map;
    
    protected final ExecutorService executor;

    protected final Object accessLock = new Object();

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @param executorService may be null (if null, uses new CachedThreadPool)
     * @param print may not be null
     * @see SubscriptionMaster
     */
    public SubscriptionMasterImpl(ExecutorService executorService,
                                  Print print) {
        if (print == null) {
            throw new IllegalArgumentException(
                    "print may not be null, use disabled impl instead");
        }
        this.pr = print;
        this.map = new WorkspaceMap();
        if (executorService == null) {
            this.executor = Executors.newCachedThreadPool();
        } else {
            this.executor = executorService;
        }
    }

    /**
     * @see SubscriptionMaster
     * @param print may not be null
     */
    public SubscriptionMasterImpl(Print print) {
        this(null, print);
    }

    // -------------------------------------------------------------------------
    // implements SubscriptionMaster
    // -------------------------------------------------------------------------

    public boolean trackStateChanges(Workspace workspace,
                                     StateChangeListener listener) {

        if (workspace == null) {
            throw new IllegalArgumentException("workspace may not be null");
        }

        if (listener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }

        synchronized (this.accessLock) {

            final WorkspaceAndListeners wal = 
                    this.map.getWorkspace(workspace.getEpr(), this.pr);

            if (wal == null) {
                final WorkspaceAndListeners newWal =
                        new WorkspaceAndListeners(workspace);
                newWal.addStateListener(listener);
                this.map.addWorkspace(newWal, this.pr);
                return true;
            } else {
                wal.addStateListener(listener);
                return false;
            }
        }
    }

    public boolean trackTerminationChanges(Workspace workspace,
                                           TerminationListener listener) {
        
        if (workspace == null) {
            throw new IllegalArgumentException("workspace may not be null");
        }

        synchronized (this.accessLock) {

            final WorkspaceAndListeners wal =
                    this.map.getWorkspace(workspace.getEpr(), this.pr);

            if (wal == null) {
                final WorkspaceAndListeners newWal =
                        new WorkspaceAndListeners(workspace);
                newWal.addTerminationListener(listener);
                this.map.addWorkspace(newWal, this.pr);
                return true;
            } else {
                wal.addTerminationListener(listener);
                return false;
            }
        }
    }

    public boolean untrackWorkspace(Workspace workspace) {

        if (workspace == null) {
            throw new IllegalArgumentException("workspace may not be null");
        }

        synchronized (this.accessLock) {

            final WorkspaceAndListeners wal =
                    this.map.getWorkspace(workspace.getEpr(), this.pr);

            return this.map.removeWorkspace(wal, this.pr);
        }
    }


    // -------------------------------------------------------------------------
    // implements TerminationConduit
    // -------------------------------------------------------------------------

    public void terminationOccured(EndpointReferenceType epr) {

        if (epr == null) {
            throw new IllegalArgumentException("epr may not be null");
        }

        if (!EPRUtils.isInstanceEPR(epr)) {
            throw new IllegalArgumentException("epr is not an instance EPR");
        }

        final WorkspaceAndListeners wal = this.map.getWorkspace(epr, this.pr);

        this.logUnknown(wal, epr, "termination");

        if (wal == null) {
            return;  // *** EARLY RETURN ***
        }

        final Enumeration e = wal.getTerminationListeners();
        if (e != null && !e.hasMoreElements()) {
            return;  // *** EARLY RETURN ***
        } else if (e == null) {
            return;  // *** EARLY RETURN ***
        }
        
        while (e.hasMoreElements()) {
            final TerminationListener l = (TerminationListener) e.nextElement();
            final DeliverTerminationCallbackTask task =
                    new DeliverTerminationCallbackTask(this, wal.getWorkspace(), l);
            this.executor.submit(task);
        }
    }

    // -------------------------------------------------------------------------
    // implements StateChangeConduit
    // -------------------------------------------------------------------------

    public void stateChange(EndpointReferenceType epr, State newState) {

        if (epr == null) {
            throw new IllegalArgumentException("epr may not be null");
        }

        if (!EPRUtils.isInstanceEPR(epr)) {
            throw new IllegalArgumentException("epr is not an instance EPR");
        }

        if (newState == null) {
            throw new IllegalArgumentException("newState may not be null");
        }

        final WorkspaceAndListeners wal = this.map.getWorkspace(epr, this.pr);

        this.logUnknown(wal, epr, "state change");

        if (wal == null) {
            return;  // *** EARLY RETURN ***
        }

        if (newState.equals(wal.getWorkspace().getCurrentState())) {
            return;  // *** EARLY RETURN ***
        }

        final Enumeration e = wal.getStateChangeListeners();
        if (e != null && !e.hasMoreElements()) {
            return;  // *** EARLY RETURN ***
        } else if (e == null) {
            return;  // *** EARLY RETURN ***
        }

        while (e.hasMoreElements()) {
            final StateChangeListener l = (StateChangeListener) e.nextElement();
            final DeliverStateChangeCallbackTask task =
                    new DeliverStateChangeCallbackTask(this, wal.getWorkspace(), l, newState);
            this.executor.submit(task);
        }
    }


    // -------------------------------------------------------------------------
    // common
    // -------------------------------------------------------------------------

    private void logUnknown(WorkspaceAndListeners wal,
                            EndpointReferenceType epr,
                            String name) {

        if (wal == null && this.pr.enabled()) {
            final int id = EPRUtils.getIdFromEPR(epr);
            final String serviceAddress = EPRUtils.getServiceURIAsString(epr);
            final String dbg = "Received " + name + " notification for " +
                    "untracked workspace: #" + id + " @ service \"" +
                    serviceAddress + "\"";
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else {
                logger.debug(dbg);
            }
        }
    }

}
