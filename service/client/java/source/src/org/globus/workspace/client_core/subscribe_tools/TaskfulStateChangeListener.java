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

package org.globus.workspace.client_core.subscribe_tools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.repr.Schedule;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.utils.ScheduleUtils;
import org.globus.workspace.client_core.actions.RPQuerySchedule;
import org.globus.workspace.client_core.actions.Destroy_Instance;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;

public class TaskfulStateChangeListener extends GenericStateChangeListener {


    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final Log logger =
            LogFactory.getLog(TaskfulStateChangeListener.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final StubConfigurator stubconf;

    private final boolean destroyOnBadStates;
    private final State additionalDestroyState;

    private final Object destroyLaunchLock = new Object();
    private boolean launchedDestroy;

    
    private final State getScheduleAfterState;
    private boolean launchedScheduleQuery;

    private final CountDownLatch targetLatch;
    private final State stateTarget;
    private boolean okToCountDown = true;
    

    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    /**
     * 
     * @param print may be null (will be disabled)
     * @param stubconfig necessary for any WS task (query, destroy..)
     * @param destroyOnBadStates if true, a change to corrupted/cancelled will
     *        cause destroy to be sent
     * @param additionalDestroyState may be null, if this state is reached,
     *        destroy will be called (in addition to corrupted/cancelled if
     *        destroyOnBadStates is true)
     * @param getScheduleAfterState  may be null, if this state (or one "after"
     *        it) is reached, schedule will be queried, happens only once
     * @param stateTarget may be null, if this state is reached the target
     *        latch will be called, happens only once
     * @param targetLatch may not be null if stateTarget is not null
     */
    public TaskfulStateChangeListener(Print print,
                                      StubConfigurator stubconfig,
                                      boolean destroyOnBadStates,
                                      State additionalDestroyState,
                                      State getScheduleAfterState,
                                      CountDownLatch targetLatch,
                                      State stateTarget) {

        super(print);

        if (destroyOnBadStates && stubconfig == null) {
            throw new IllegalArgumentException("destroyOnBadStates " +
                    "requires StubConfigurator");
        }

        if (additionalDestroyState != null && stubconfig == null) {
            throw new IllegalArgumentException("additionalDestroyState " +
                    "presence requires StubConfigurator");
        }

        if (getScheduleAfterState != null && stubconfig == null) {
            throw new IllegalArgumentException("getScheduleAfterState " +
                    "presence requires StubConfigurator");
        }

        if (stateTarget != null && targetLatch == null) {
            throw new IllegalArgumentException("stateTarget " +
                    "presence requires target latch");
        }

        this.stubconf = stubconfig;
        this.destroyOnBadStates = destroyOnBadStates;
        this.additionalDestroyState = additionalDestroyState;
        this.getScheduleAfterState = getScheduleAfterState;
        this.targetLatch = targetLatch;
        this.stateTarget = stateTarget;
    }


    // -------------------------------------------------------------------------
    // overrides GenericStateChangeListener
    // -------------------------------------------------------------------------

    protected void theNewState(Workspace workspace,
                               State oldState,
                               State newState) {

        this.launchDestroyTaskPossibly(workspace, oldState, newState);
        this.launchScheduleQueryPossibly(workspace, oldState, newState);
        this.countDownPossibly(workspace, oldState, newState);
    }

    // -------------------------------------------------------------------------
    // count down the target latch, possibly.
    // -------------------------------------------------------------------------

    /**
     * @param workspace never null, not used by this impl, here for overriders
     * @param oldState might be null, not used by this impl, here for overriders
     * @param newState never null
     */
    protected void countDownPossibly(Workspace workspace,
                                     State oldState,
                                     State newState) {

        if (this.targetLatch == null) {
            // not ever needed
            return; // *** EARLY RETURN ***
        }

        // only notify of one change or else count could be inaccurate

        synchronized (this.targetLatch) {
            if (this.okToCountDown) {
                if (newState.equals(this.stateTarget)) {
                    this.targetLatch.countDown();
                    this.okToCountDown = false;
                }
            }
        }
    }

    
    // -------------------------------------------------------------------------
    // launch destroy task, possibly.
    // -------------------------------------------------------------------------

    /**
     * @param workspace never null
     * @param oldState might be null, not used by this impl, here for overriders
     * @param newState never null
     * @return true if destroy task was needed and launched (either in this
     *              call or in a previous call)
     */
    protected boolean launchDestroyTaskPossibly(Workspace workspace,
                                                State oldState,
                                                State newState) {

        if (!this.destroyOnBadStates && this.additionalDestroyState == null) {
            // not ever needed
            return false; // *** EARLY RETURN ***
        }
        
        synchronized (this.destroyLaunchLock) {

            if (this.launchedDestroy) {
                return true; // *** EARLY RETURN ***
            }

            if (this.destroyOnBadStates
                    && newState.isProblemState()) {

                if (this.pr.enabled()) {
                    final String dbg = "destroying because not-OK state '" +
                            newState.toString() + "' was reached";
                    if (this.pr.useThis()) {
                        this.pr.debugln(dbg);
                    } else if (this.pr.useLogging()) {
                        logger.debug(dbg);
                    }
                }
                destroy(workspace, newState, this.pr, this.stubconf);
                this.launchedDestroy = true;
                return true;

            } else if (this.additionalDestroyState != null
                    && newState.equals(this.additionalDestroyState)) {

                if (this.pr.enabled()) {
                    final String dbg = "destroying because additional destroy " +
                            "state '" + newState.toString() + "' was reached";
                    if (this.pr.useThis()) {
                        this.pr.debugln(dbg);
                    } else if (this.pr.useLogging()) {
                        logger.debug(dbg);
                    }
                }
                destroy(workspace, newState, this.pr, this.stubconf);
                this.launchedDestroy = true;
                return true;

            } else {

                // not needed now
                return false;
            }
        }
    }
    
    

    // -------------------------------------------------------------------------
    // Destroy impl.
    // -------------------------------------------------------------------------

    static void destroy(Workspace workspace,
                        State newState,
                        Print print,
                        StubConfigurator stubconf) {

        if (print.enabled()) {
            final String err = "\"" + workspace.getDisplayName() + "\": " +
                    newState.getState() + ", calling destroy for you.";
            if (print.useThis()) {

				// Uncleanly injecting cloud-client message here.
				// And also uncleanly consulting the 'PrintOpts' internals.  Bravo.
				if (print.getOpts().printThis(PrCodes.LISTENER_AUTODESTROY_CLOUD_UNPROPAGATE)) {
					if (newState.getState().equals(State.STATE_TransportReady)) {
						print.errln(PrCodes.LISTENER_AUTODESTROY_CLOUD_UNPROPAGATE,
									"The image has successfully been transferred to your " +
											"repository directory.\n\nFinalizing the " +
											"deployment now (terminating the resource " +
											"lease).\n");
					}
				} else {
                	print.errln(PrCodes.LISTENER_AUTODESTROY, err);
				}
            } else if (print.useLogging()) {
                logger.error(err);
            }
        }

        final Destroy_Instance destroy =
                new Destroy_Instance(workspace.getEpr(),
                                     stubconf,
                                     print);


        // nothing to do but log errors if they occur
        try {
            destroy.destroy();
        } catch (Throwable t) {
            if (print.enabled()) {
                final String err = "\"" + workspace.getDisplayName() + "\" " +
                        "auto-destruction did not succeed: " +
                        CommonUtil.genericExceptionMessageWrapper(t);
                if (print.useThis()) {
                    print.errln(PrCodes.LISTENER_AUTODESTROY__ERRORS,
                                  err);
                } else if (print.useLogging()) {
                    logger.error(err);
                }
            }
        }
    }
    
    // -------------------------------------------------------------------------
    // launch Schedule query, possibly.
    // -------------------------------------------------------------------------

    /**
     * @param workspace never null
     * @param oldState might be null, not used by this impl, here for overriders
     * @param newState never null
     */
    protected void launchScheduleQueryPossibly(Workspace workspace,
                                               State oldState,
                                               State newState) {

        if (this.getScheduleAfterState == null) {
            // not needed
            return; // *** EARLY RETURN ***
        }

        synchronized(this.getScheduleAfterState) {

            if (this.launchedScheduleQuery) {
                return; // *** EARLY RETURN ***
            }

            if (newState.isOKAndEqualOrAfter(this.getScheduleAfterState)) {

                final RPQuerySchedule query =
                        new RPQuerySchedule(workspace.getEpr(),
                                            this.stubconf,
                                            this.pr);

                // nothing to do but log errors if they occur
                try {
                    final Schedule schedule = query.queryOnce();
                    final Workspace w = new Workspace();
                    w.setCurrentSchedule(schedule);
                    ScheduleUtils.instanceCreateResultSchedulePrint(this.pr, w);
                } catch (Throwable t) {
                    if (this.pr.enabled()) {
                        final String err = "\"" + workspace.getDisplayName() +
                                "\" schedule query did not succeed: " +
                                CommonUtil.genericExceptionMessageWrapper(t);
                        if (this.pr.useThis()) {
                            this.pr.errln(PrCodes.LISTENER_LOGISTICSQUERY__ERRORS,
                                          err);
                        } else if (this.pr.useLogging()) {
                            logger.error(err);
                        }
                    }
                }

                this.launchedScheduleQuery = true;
            }
        }
    }
}
