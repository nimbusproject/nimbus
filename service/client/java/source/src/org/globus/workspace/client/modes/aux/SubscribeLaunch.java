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

import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.utils.WSUtils;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.actions.SubscribeCurrentState_Instance;
import org.globus.workspace.client_core.actions.SubscribeTermination_Instance;
import org.globus.workspace.client_core.actions.RPQueryCurrentState;
import org.globus.workspace.client_core.subscribe_tools.StateChangeListener;
import org.globus.workspace.client_core.subscribe_tools.TerminationListener;
import org.globus.workspace.client_core.subscribe_tools.LatchUsingTerminationListener;
import org.globus.workspace.client_core.subscribe_tools.ListeningSubscriptionMaster;
import org.globus.workspace.client_core.subscribe_tools.PollingSubscriptionMaster;
import org.globus.workspace.client_core.subscribe_tools.SubscriptionMaster;
import org.globus.workspace.client_core.subscribe_tools.StateChangeConduit;
import org.globus.workspace.client_core.subscribe_tools.TerminationConduit;
import org.globus.workspace.client_core.subscribe_tools.TaskfulStateChangeListener;
import org.nimbustools.messaging.gt4_0.generated.WorkspacePortType;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.globus.workspace.client_common.CommonStrings;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis.wsrf.faults.BaseFaultType;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;

import javax.xml.rpc.Stub;
import java.util.ArrayList;
import java.util.List;

public class SubscribeLaunch {


    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(SubscribeLaunch.class.getName());
    
    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String name;
    private final Print pr;
    private final StubConfigurator stubConf;
    private final ExecutorService executor;

    private final boolean pollingMode;
    private final ListeningSubscriptionMaster listenMaster;
    private final PollingSubscriptionMaster pollMaster;
    private final SubscriptionMaster eitherMaster;
    private static final FutureTask[] EMPTY_TASK_ARRAY = new FutureTask[0];

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------


    private SubscribeLaunch(String nameToPrint,
                            Print print,
                            StubConfigurator stubconf,
                            ExecutorService executorService,
                            PollingSubscriptionMaster pollingSubscriptionMaster,
                            ListeningSubscriptionMaster listeningSubscriptionMaster) {

        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        this.pr = print;

        if (nameToPrint == null) {
            throw new IllegalArgumentException("nameToPrint may not be null");
        }
        this.name = nameToPrint;

        if (stubconf == null) {
            throw new IllegalArgumentException("stubconf may not be null");
        }
        this.stubConf = stubconf;

        this.executor = executorService;

        this.listenMaster = listeningSubscriptionMaster;
        this.pollMaster = pollingSubscriptionMaster;
        
        if (pollingSubscriptionMaster != null) {
            this.eitherMaster = pollingSubscriptionMaster;
            this.pollingMode = true;
        } else {
            this.eitherMaster = listeningSubscriptionMaster;
            this.pollingMode = false;
        }
    }

    /**
     * Poll mode.
     * 
     * @param pollingSubscriptionMaster may not be null
     * @param nameToPrint  may not be null (can be junk if print disabled)
     * @param print  may not be null
     * @param stubconf  may not be null
     * @param executorService  may be null (if null, uses new CachedThreadPool)
     */
    public SubscribeLaunch(PollingSubscriptionMaster pollingSubscriptionMaster,
                           String nameToPrint,
                           Print print,
                           StubConfigurator stubconf,
                           ExecutorService executorService) {

        this(nameToPrint, print, stubconf,
             executorService, pollingSubscriptionMaster, null);

        if (pollingSubscriptionMaster == null) {
            throw new IllegalArgumentException(
                    "pollingSubscriptionMaster may not be null");
        }
    }

    /**
     * Async mode.
     *
     * @param listeningSubscriptionMaster may not be null
     * @param nameToPrint  may not be null (can be junk if print disabled)
     * @param print  may not be null
     * @param stubconf  may not be null
     * @param executorService  may be null (if null, uses new CachedThreadPool)
     */
    public SubscribeLaunch(ListeningSubscriptionMaster listeningSubscriptionMaster,
                           String nameToPrint,
                           Print print,
                           StubConfigurator stubconf,
                           ExecutorService executorService) {

        this(nameToPrint, print, stubconf,
             executorService, null, listeningSubscriptionMaster);

        if (listeningSubscriptionMaster == null) {
            throw new IllegalArgumentException(
                    "listeningSubscriptionMaster may not be null");
        }
    }


    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    public String getName() {
        return this.name;
    }

    public void setName(String displayName) {
        this.name = displayName;
    }
    
    // -------------------------------------------------------------------------
    // SUBSCRIBE
    // -------------------------------------------------------------------------

    /**
     * @param workspaces            may not be null
     * @param exitState             may be null, causes subscribe to exit early
     *                              when the state is reached (if group, waits
     *                              for all to reach state once)
     * @param veryTerseNotifyState  may be null, causes progress meter tick at
     *                              state and destructions are noted. if not
     *                              null, terseSubscribe is not consulted.
     *                              logger (useLogging) is not used for this.
     * @param autodestroy           if true, a change to corrupted or cancelled
     *                              state will cause destroy task to be launched
     * @param queryIfListening      if true, one query is run after subscription
     *                              (this will catch races on changes after a
     *                              new deployment)
     * @param wasBestEffort         if true, triggers schedule query and print
     *                              after first state change into unpropagated
     * @throws ParameterProblem     input problem
     * @throws ExecutionProblem     problem executing
     * @throws ExitNow              exit state reached
     */
    public void subscribe(Workspace[] workspaces,
                          State exitState,
                          State veryTerseNotifyState,
                          boolean autodestroy,
                          boolean queryIfListening,
                          boolean wasBestEffort)

            throws ParameterProblem, ExecutionProblem, ExitNow {

        final SubscribeWait waiter = _subscribeImpl(workspaces,
                                                    exitState,
                                                    veryTerseNotifyState,
                                                    autodestroy,
                                                    queryIfListening,
                                                    null,
                                                    wasBestEffort);
        waiter.run(exitState);
    }

    public SubscribeWait subscribeNoWait(Workspace[] workspaces,
                                         State exitState,
                                         State veryTerseNotifyState,
                                         boolean autodestroy,
                                         boolean queryIfListening,
                                         State additionalDestroyState,
                                         boolean wasBestEffort)

            throws ParameterProblem, ExecutionProblem {

        return _subscribeImpl(workspaces,
                              exitState,
                              veryTerseNotifyState,
                              autodestroy,
                              queryIfListening,
                              additionalDestroyState,
                              wasBestEffort);
    }

    private SubscribeWait _subscribeImpl(Workspace[] workspaces,
                                         State exitState,
                                         State veryTerseNotifyState,
                                         boolean autodestroy,
                                         boolean queryIfListening,
                                         State additionalDestroyState,
                                         boolean wasBestEffort)

            throws ParameterProblem, ExecutionProblem {

        if (workspaces == null) {
            throw new IllegalArgumentException("workspaces may not be null");
        }

        if (workspaces.length == 0) {
            throw new IllegalArgumentException("workspaces may not be len 0");
        }

        final SubscribeCurrentState_Instance[] stateSubscribes;
        final SubscribeTermination_Instance[] termSubscribes;
        final WorkspacePortType[] portTypes;
        if (this.pollingMode) {
            stateSubscribes = null;
            termSubscribes = null;
            portTypes = null;
        } else {
            portTypes = this.createPortTypes(workspaces);
            stateSubscribes =
                    this.createAsynchStateSubscribes(portTypes);
            termSubscribes =
                    this.createAsynchTerminationSubscribes(portTypes);
        }

        final SubscribeWait waiter = new SubscribeWait(workspaces.length,
                                                       this.pr,
                                                       this.name,
                                                       this.executor);

        final CountDownLatch targetLatch = waiter.getTargetLatch();
        final CountDownLatch terminationLatch = waiter.getTerminationLatch();

        for (int i = 0; i < workspaces.length; i++) {
            // in poll mode, the polling master creates RP query threads via
            // the track* methods
            this.registerListeners(this.eitherMaster,
                                   workspaces[i],
                                   exitState,
                                   autodestroy,
                                   targetLatch,
                                   terminationLatch,
                                   additionalDestroyState,
                                   wasBestEffort);
        }

        if (!this.pollingMode) {

            final ExecutorService useExecutor = waiter.getExecutorService();
            if (useExecutor == null) {
                throw new IllegalStateException("no executor service?");
            }

            final FutureTask[] tasks =
                    createSubscribeTasks(stateSubscribes, termSubscribes);
            for (int i = 0; i < tasks.length; i++) {
                useExecutor.submit(tasks[i]);
            }

            for (int i = 0; i < tasks.length; i++) {
                try {
                    tasks[i].get();
                } catch (Exception e) {
                    final Throwable cause = e.getCause();
                    final String err;
                    if (cause == null) {
                        err = CommonUtil.genericExceptionMessageWrapper(e);
                    } else {
                        if (cause instanceof BaseFaultType) {
                            err = CommonStrings.
                                    faultStringOrCommonCause(
                                            (BaseFaultType)cause);
                        } else {
                            err = CommonUtil.
                                    genericExceptionMessageWrapper(cause);
                        }
                    }
                    throw new ExecutionProblem(err, e);
                }
            }
        }

        if (this.pr.enabled() && this.pr.useThis()) {

            final int code =
                    PrCodes.CREATE__SUBSCRIPTION_CREATING_PRINT_WAITING;
            final String msg = "\nWaiting for updates.\n";
            this.pr.infoln(code, msg);
            //if (this.pollingMode) {
            //    this.pr.info(code, msg);
            //} else {
            //}
        }

        // queryIfListening is unecessary in poll mode
        // (that's why it's called "if listening")
        // could go away when factory supports subscriptions @ creation
        if (queryIfListening && !this.pollingMode) {

            final ExecutorService useExecutor = waiter.getExecutorService();
            if (useExecutor == null) {
                throw new IllegalStateException("no executor service?");
            }
            if (portTypes == null) {
                throw new IllegalStateException("no portTypes?");
            }

            if (!(this.listenMaster instanceof StateChangeConduit)) {
                throw new IllegalStateException("can't do state conduit trick");
            }

            final StateChangeConduit stateConduit =
                    (StateChangeConduit)this.listenMaster;

            if (!(this.listenMaster instanceof TerminationConduit)) {
                throw new IllegalStateException("can't do termination conduit trick");
            }

            final TerminationConduit termConduit =
                    (TerminationConduit)this.listenMaster;

            // Launch one explicit query per workspace, sends state changes and
            // terminations to the same conduit that the WS listener does.  This
            // works since the listen master's implementation is keyed off the
            // EPR and since the waiting latch doesn't care what originally sets
            // off the events its waiting for.  This is a modest invasion into
            // implementation knowledge, but it's very convenient.

            final FutureTask[] queryTasks = new FutureTask[workspaces.length];
            for (int i = 0; i < workspaces.length; i++) {
                final RPQueryCurrentState action =
                        new RPQueryCurrentState(portTypes[i], this.pr);

                action.setStateConduit(stateConduit, workspaces[i].getEpr());
                action.setTerminationConduit(termConduit,
                                             workspaces[i].getEpr());
                queryTasks[i] = new FutureTask(action);
            }

            for (int i = 0; i < queryTasks.length; i++) {
                useExecutor.submit(queryTasks[i]);
            }
        }

        return waiter;
    }

    private static FutureTask[] createSubscribeTasks(
                            SubscribeCurrentState_Instance[] stateSubscribes,
                            SubscribeTermination_Instance[] termSubscribes) {

        int taskNum = 0;
        if (stateSubscribes != null) {
            taskNum += stateSubscribes.length;
        }

        if (termSubscribes != null) {
            taskNum += termSubscribes.length;
        }

        if (taskNum == 0) {
            return EMPTY_TASK_ARRAY; // *** EARLY RETURN ***
        }

        final List tasks = new ArrayList(taskNum);

        if (stateSubscribes != null) {
            for (int i = 0; i < stateSubscribes.length; i++) {
                tasks.add(new FutureTask(stateSubscribes[i]));
            }
        }

        if (termSubscribes != null) {
            for (int i = 0; i < termSubscribes.length; i++) {
                tasks.add(new FutureTask(termSubscribes[i]));
            }
        }

        return (FutureTask[]) tasks.toArray(new FutureTask[tasks.size()]);
    }

    private void registerListeners(SubscriptionMaster master,
                                   Workspace workspace,
                                   State exitState,
                                   boolean autodestroy,
                                   CountDownLatch targetLatch,
                                   CountDownLatch terminationLatch,
                                   State anotherDestroyState,
                                   boolean wasBestEffort) {

        final State getSchedAfter;
        if (wasBestEffort) {
            getSchedAfter = new State(State.STATE_Unpropagated);
        } else {
            getSchedAfter = null;
        }

        final StateChangeListener stateListener =
                new TaskfulStateChangeListener(this.pr,
                                               this.stubConf,
                                               autodestroy,
                                               anotherDestroyState,
                                               getSchedAfter,
                                               targetLatch,
                                               exitState);

        master.trackStateChanges(workspace, stateListener);

        final TerminationListener termListener =
                        new LatchUsingTerminationListener(this.pr,
                                                          terminationLatch);

        master.trackTerminationChanges(workspace, termListener);
    }

    private WorkspacePortType[] createPortTypes(Workspace[] workspaces)
                    throws ParameterProblem {

        long mstart = 0;
        if (this.pr.enabled()) {
            mstart = System.currentTimeMillis();
        }

        final WorkspacePortType[] portTypes =
                new WorkspacePortType[workspaces.length];

        try {

            for (int i = 0; i < workspaces.length; i++) {

                portTypes[i] = WSUtils.initServicePortType(
                                            workspaces[i].getEpr());

                this.stubConf.setOptions((Stub)portTypes[i]);
            }

        } catch (Exception e) {
            final String err = "Problem setting up subscription stubs: ";
            throw new ParameterProblem(err + e.getMessage(), e);
        }

        if (this.pr.enabled()) {

            final long mstop = System.currentTimeMillis();

            final String dbg = "Subscribe port type setups took " +
                    Long.toString(mstop - mstart) + "ms";

            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }

        return portTypes;
    }

    private SubscribeCurrentState_Instance[]
                createAsynchStateSubscribes(WorkspacePortType[] portTypes)
                        throws ParameterProblem {

        if (this.listenMaster == null) {
            // will not be null, leaving as safeguard/reminder
            throw new ParameterProblem("Cannot subscribe, notification " +
                    "listener was not set up correctly.");
        }

        final EndpointReferenceType consumerEPR =
                    this.listenMaster.getConsumerEPR();

        final SubscribeCurrentState_Instance[] rets =
                new SubscribeCurrentState_Instance[portTypes.length];
        
        for (int i = 0; i < portTypes.length; i++) {
            rets[i] = new SubscribeCurrentState_Instance(portTypes[i],
                                                         consumerEPR);
        }

        return rets;
    }

    private SubscribeTermination_Instance[]
                createAsynchTerminationSubscribes(WorkspacePortType[] portTypes)
                        throws ParameterProblem {

        if (this.listenMaster == null) {
            // will not be null, leaving as safeguard/reminder
            throw new ParameterProblem("Cannot subscribe, notification " +
                    "listener was not set up correctly.");
        }

        final EndpointReferenceType consumerEPR =
                    this.listenMaster.getConsumerEPR();

        final SubscribeTermination_Instance[] rets =
                new SubscribeTermination_Instance[portTypes.length];

        for (int i = 0; i < portTypes.length; i++) {
            rets[i] = new SubscribeTermination_Instance(portTypes[i],
                                                        consumerEPR);
        }

        return rets;
    }
}
