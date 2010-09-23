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

import org.globus.workspace.client_core.subscribe_tools.PollingSubscriptionMaster;
import org.globus.workspace.client_core.subscribe_tools.StateChangeListener;
import org.globus.workspace.client_core.subscribe_tools.TerminationListener;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.actions.RPQueryCurrentState;
import org.globus.workspace.client_core.StubConfigurator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis.message.addressing.EndpointReferenceType;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledFuture;

import java.util.Hashtable;

public class PollingSubscriptionMasterImpl extends SubscriptionMasterImpl
                                           implements PollingSubscriptionMaster {
    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(ListeningSubscriptionMasterImpl.class.getName());

    public static final long DEFAULT_POLL_MS = 2000;
    public static final int DEFAULT_POOL_SIZE = 4;


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final ScheduledThreadPoolExecutor scheduled;
    private final long pollDelayMs;
    private final StubConfigurator stubConf;
    
    // key: AddressIDPair
    // value: FutureTask
    private final Hashtable currentTasks;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public PollingSubscriptionMasterImpl(long pollDelayMilliseconds,
                                         int maxThreads,
                                         StubConfigurator stubConfigurator,
                                         ExecutorService executorService,
                                         Print print) {
        super(executorService, print);
        this.pollDelayMs = pollDelayMilliseconds;
        this.stubConf = stubConfigurator;

        // This is different than executorService, which is what to use for
        // callback task threads.
        // This is for running the poll tasks themselves.
        this.scheduled = new ScheduledThreadPoolExecutor(DEFAULT_POOL_SIZE);

        this.scheduled.setRemoveOnCancelPolicy(true);

        this.scheduled.setMaximumPoolSize(maxThreads);

        this.currentTasks = new Hashtable(8);
    }

    public PollingSubscriptionMasterImpl(long pollDelayMilliseconds,
                                         StubConfigurator stubConfigurator,
                                         ExecutorService executorService,
                                         Print print) {
        this(pollDelayMilliseconds,
             DEFAULT_MAX_POOL_SIZE,
             stubConfigurator,
             executorService,
             print);
    }

    public PollingSubscriptionMasterImpl(StubConfigurator stubConfigurator,
                                         Print print) {
        this(DEFAULT_POLL_MS,
             DEFAULT_MAX_POOL_SIZE,
             stubConfigurator,
             null,
             print);
    }

    
    // -------------------------------------------------------------------------
    // implements PollingSubscriptionMaster
    // -------------------------------------------------------------------------

    public void stopPolling() {
        this.scheduled.shutdownNow();
    }

    public long getPollingMs() {
        return this.pollDelayMs;
    }

    public void setMaxThreads(int maxThreads) {
        if (maxThreads < 1) {
            throw new IllegalArgumentException(
                    "maxThreads may not be less than 1, value: " + maxThreads);
        }
        this.scheduled.setMaximumPoolSize(maxThreads);
    }

    // -------------------------------------------------------------------------
    // overrides SubscriptionMasterImpl
    // -------------------------------------------------------------------------

    public boolean trackStateChanges(Workspace workspace,
                                     StateChangeListener listener) {
        return this.trackCommon(workspace, listener, null);
    }

    public boolean trackTerminationChanges(Workspace workspace,
                                           TerminationListener listener) {
        return this.trackCommon(workspace, null, listener);
    }

    // state changes and termination changes are both tracked via same poll
    private boolean trackCommon(Workspace workspace,
                                StateChangeListener stateListener,
                                TerminationListener terminationListener) {
        
        if (workspace == null) {
            throw new IllegalArgumentException("workspace may not be null");
        }

        if (terminationListener == null && stateListener == null) {
            throw new IllegalArgumentException(
                    "illegal to call with all listeners null");
        }

        if (terminationListener != null && stateListener != null) {
            throw new IllegalArgumentException(
                    "illegal to call with more than one listener non-null");
        }

        synchronized (this.accessLock) {

            final EndpointReferenceType epr = workspace.getEpr();

            // look in the map to see if anything is being tracked already
            final WorkspaceAndListeners wal =
                    this.map.getWorkspace(epr, this.pr);

            // if neither is being tracked yet, start a polling thread
            if (wal == null) {

                final AddressIDPair addrID =
                    WorkspaceMap.chooseAddrID(epr, this.pr);

                final RPQueryCurrentState action =
                        new RPQueryCurrentState(epr, this.stubConf, this.pr);
                action.setStateConduit(this, epr);
                action.setTerminationConduit(this, epr);

                // Can't use FutureTask because of the way scheduleWithFixedDelay
                // will wrap the object.  Results in just one call instead of
                // repeating (thread state gets put into "RAN" in the inner
                // callable.  Instead, made RPQueryCurrentState also implement
                // Runnable interface and so now is native parameter to
                // scheduleWithFixedDelay method instead of wrapped in FutureTask.
                //NOPE: final FutureTask task = new FutureTask(action);

                final ScheduledFuture scheduledFuture =
                        this.scheduled.scheduleWithFixedDelay(
                                action, this.pollDelayMs, this.pollDelayMs,
                                TimeUnit.MILLISECONDS);

                this.currentTasks.put(addrID, scheduledFuture);
            }

            if (terminationListener != null) {
                return super.trackTerminationChanges(workspace,
                                                     terminationListener);
            } else {
                return super.trackStateChanges(workspace,
                                               stateListener);
            }
        }
    }

    public boolean untrackWorkspace(Workspace workspace) {
        
        if (workspace == null) {
            throw new IllegalArgumentException("workspace may not be null");
        }

        synchronized (this.accessLock) {

            final EndpointReferenceType epr = workspace.getEpr();

            final AddressIDPair addrID =
                    WorkspaceMap.chooseAddrID(epr, this.pr);

            if (addrID == null) {
                
                // *** EARLY RETURN ***
                return super.untrackWorkspace(workspace);
            }

            final ScheduledFuture task =
                    (ScheduledFuture) this.currentTasks.get(addrID);
            
            if (task == null) {
                if (this.pr.enabled()) {
                    final String err = "Unexpected: parent tracking this " +
                            "but not the poll subscription manager? " +
                            addrID.toString();
                    if (this.pr.useThis()) {
                        this.pr.errln(err);
                    } else if (this.pr.useLogging()) {
                        logger.error(err);
                    }
                }
                // *** EARLY RETURN ***
                return super.untrackWorkspace(workspace);
            }

            task.cancel(true);
            
            this.currentTasks.remove(addrID);

            return super.untrackWorkspace(workspace);
        }
    }
}
