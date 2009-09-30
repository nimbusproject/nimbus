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

package org.globus.workspace.client.modes.aux;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.utils.LatchWaiter;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_common.BaseClient;
import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;

public class SubscribeWait {

    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(SubscribeWait.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final int targetNumber;
    private final Print pr;
    private final String name;
    private final ExecutorService executor;
    private final boolean privateExecutor;

    // internally used latch
    private final CountDownLatch exitLatch;

    // latches for outside countdowns
    private final CountDownLatch targetLatch;
    private final CountDownLatch terminationLatch;

    // waiters for each outside countdown latch
    private FutureTask targetWaiter;
    private FutureTask terminationWaiter;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @param numSubscriptions number of events each latch should wait for
     * @param print print, may not be null
     * @param nameToPrint may not be null (just send junk if print is disabled)
     * @param executorService may be null (if null, uses new CachedThreadPool)
     */
    public SubscribeWait(int numSubscriptions,
                         Print print,
                         String nameToPrint,
                         ExecutorService executorService) {

        if (numSubscriptions < 1) {
            throw new IllegalArgumentException(
                    "numSubscriptions may not be less than one");
        }
        this.targetNumber = numSubscriptions;

        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        this.pr = print;

        if (nameToPrint == null) {
            throw new IllegalArgumentException("nameToPrint may not be null");
        }
        this.name = nameToPrint;

        if (executorService == null) {
            this.executor = Executors.newCachedThreadPool();
            this.privateExecutor = true;
        } else {
            this.executor = executorService;
            this.privateExecutor = false;
        }

        this.targetLatch = new CountDownLatch(this.targetNumber);
        this.terminationLatch = new CountDownLatch(this.targetNumber);
        this.exitLatch = new CountDownLatch(1);
    }


    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    CountDownLatch getTargetLatch() {
        return this.targetLatch;
    }

    CountDownLatch getTerminationLatch() {
        return this.terminationLatch;
    }

    ExecutorService getExecutorService() {
        return this.executor;
    }

    
    // -------------------------------------------------------------------------
    // ENTRY
    // -------------------------------------------------------------------------

    public void run(State exitState) throws ExecutionProblem, ExitNow {

        try {
            this._run(exitState);
        } finally {
            if (this.privateExecutor) {
                this.executor.shutdownNow();
            }
        }

    }

    private void _run(State exitState) throws ExecutionProblem, ExitNow {

        // If there is no exit state, one thread is created to wait on all
        // terminations

        // If there is an exit state, two threads are created, one to wait
        // on all terminations and one to wait on all exit states.  If EITHER
        // of those finish, this returns (and the mode is over). Any remaining
        // alive thread/s is cancelled.

        if (exitState != null) {
            this.launchStateWaiter();
        }

        this.launchTerminationWaiter();

        try {
            this.exitLatch.await();
            if (this.pr.enabled() && this.pr.useThis()) {
                this.pr.infoln(PrCodes.CREATE__EXTRALINES, "");
            }

        } catch (InterruptedException e) {
            throw new ExecutionProblem(e.getMessage(), e);
        }

        try {
            this.reapStateWaiter(exitState);
            this.reapTerminationWaiter();
        } catch (ExitNow e) {
            throw e;
        } finally {
            this.cancelTargetWaiterIfNotDone();
            this.cancelTerminationWaiterIfNotDone();
        }
    }


    // -------------------------------------------------------------------------
    // Launch threads
    // -------------------------------------------------------------------------

    private void launchStateWaiter() {
        final LatchWaiter waiter = new LatchWaiter(this.getTargetLatch(),
                                                   this.exitLatch);
        this.targetWaiter = new FutureTask(waiter);
        this.executor.submit(this.targetWaiter);
    }

    private void launchTerminationWaiter() {
        final LatchWaiter waiter = new LatchWaiter(this.getTerminationLatch(),
                                                   this.exitLatch);
        this.terminationWaiter = new FutureTask(waiter);
        this.executor.submit(this.terminationWaiter);
    }


    // -------------------------------------------------------------------------
    // Cancel waiters
    // -------------------------------------------------------------------------

    private void cancelTargetWaiterIfNotDone() {
        if (this.targetWaiter != null) {
            if (!this.targetWaiter.isDone()) {
                this.targetWaiter.cancel(true);
            }
        }
    }

    private void cancelTerminationWaiterIfNotDone() {
        if (this.terminationWaiter != null) {
            if (!this.terminationWaiter.isDone()) {
                this.terminationWaiter.cancel(true);
            }
        }
    }


    // -------------------------------------------------------------------------
    // Reap waiters
    // -------------------------------------------------------------------------

    private void reapStateWaiter(State exitState) throws ExitNow {

        if (exitState == null) {

            if (this.pr.enabled()) {
                final String dbg =
                        "Don't need to reap state target waiter, no exit state";
                if (this.pr.useThis()) {
                    this.pr.dbg(dbg);
                } else if (this.pr.useLogging()) {
                    logger.debug(dbg);
                }
            }

            return; // *** EARLY RETURN ***
        }

        if (this.targetWaiter == null) {
            throw new IllegalStateException(
                    "this.targetWaiter should not be null here");
        }

        final long numOutstanding = this.getTargetLatch().getCount();
        final long target = this.targetNumber;
        final long numDone = target - numOutstanding;

        if (numDone == target) {

            if (this.pr.enabled()) {

                final String tail = this.name +
                        "\" reached target state: " + exitState.getState();
                final String msg;
                if (target == 1) {
                    msg = "\"" + tail;
                } else {
                    msg = "All members of \"" + tail;
                }

                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.SUBSCRIPTIONS_STATECHANGE__EXTRALINES_ALLENDED, "");
                    this.pr.infoln(PrCodes.SUBSCRIPTIONS_STATECHANGE__TARGET_STATE_ALL_REACHED,
                                   msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

            return; // *** EARLY RETURN ***
        }

        // numDone != target
        // i.e., there is a problem

        if (this.pr.enabled()) {

            final String tail = this.name + "\" did NOT reach " +
                    "target state \"" + exitState.getState() + "\"";

            final String err;
            if (target == 1) {
                err = "Workspace \"" + tail;
            } else {
                if (numOutstanding == 1) {
                    err = "One member of \"" + tail + ", (" + numDone + " did).";
                } else if (numOutstanding == target) {
                    err = "All members of \"" + tail;
                } else {
                    err = "Some members of \"" + tail + ", (" + numDone + " did).";
                }
            }

            if (this.pr.useThis()) {
                this.pr.errln(PrCodes.SUBSCRIPTIONS_STATECHANGE__EXTRALINES_ALLENDED, "");
                this.pr.errln(PrCodes.SUBSCRIPTIONS_STATECHANGE__TARGET_STATE_NOT_ALL_REACHED,
                              err);
            } else if (this.pr.useLogging()) {
                logger.error(err);
            }
        }

        throw new ExitNow(BaseClient.APPLICATION_EXIT_CODE);
    }

    private void reapTerminationWaiter() {

        if (this.terminationWaiter == null) {
            throw new IllegalStateException(
                    "this.terminationWaiter should not be null here");
        }

        final long numOutstanding = this.getTerminationLatch().getCount();
        final long target = this.targetNumber;
        final long numDone = target - numOutstanding;

        if (numDone != target) {
            return; // *** EARLY RETURN ***
        }

        if (this.pr.enabled()) {

            if (target == 1) {
                // do nothing if target == 1
                // (the termination listener has a print)
            } else {
                final String msg = "All members of \"" +
                            this.name + "\" were terminated.";

                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.SUBSCRIPTIONS_TERMINATED__ALL_TERMINATED,
                                    msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }

            }

        }
    }
}
