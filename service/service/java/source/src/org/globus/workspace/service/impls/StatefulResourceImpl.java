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

package org.globus.workspace.service.impls;

import commonj.timers.TimerManager;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.*;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.DataConvert;
import org.globus.workspace.scheduler.Scheduler;
import org.globus.workspace.service.binding.BindNetwork;
import org.globus.workspace.service.binding.BindingAdapter;
import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.globus.workspace.service.impls.async.TaskNotImplementedException;
import org.globus.workspace.xen.XenUtil;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.StateChangeCallback;
import org.nimbustools.api.repr.vm.State;

public abstract class StatefulResourceImpl extends InstanceResourceImpl
                                           implements WorkspaceConstants {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
        LogFactory.getLog(StatefulResourceImpl.class.getName());


    // When these states are reached, a notification will be sent to
    // any subscribers of the current deployment state.
    // also sent when state is >= STATE_CORRUPTED_GENERIC
    // todo: should be external policy driven
    public static final int[] SEND_NOTIFICATION =
                                           {STATE_UNPROPAGATED,
                                            STATE_PROPAGATED,
                                            STATE_STARTED,
                                            STATE_SERIALIZED,
                                            STATE_PAUSED,
                                            STATE_READY_FOR_TRANSPORT,
                                            STATE_STAGED_OUT,
                                            STATE_CANCELLING_AT_VMM,
                                            STATE_DESTROYING};


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final Scheduler scheduler;
    protected final LockManager lockMgr;
    protected final StateTransition stateTransition;
    protected final TimerManager timerManager;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    protected StatefulResourceImpl(PersistenceAdapter persistenceImpl,
                                   BindingAdapter bindingImpl,
                                   GlobalPolicies globalsImpl,
                                   DataConvert dataConvertImpl,
                                   Lager lagerImpl,
                                   BindNetwork bindNetworkImpl,
                                   Scheduler schedulerImpl,
                                   LockManager lockMgrImpl,
                                   StateTransition transitionImpl,
                                   TimerManager timerManagerImpl) {

        super(persistenceImpl, bindingImpl, globalsImpl,
              dataConvertImpl, lagerImpl,bindNetworkImpl);

        if (schedulerImpl == null) {
            throw new IllegalArgumentException("schedulerImpl may not be null");
        }
        this.scheduler = schedulerImpl;

        if (lockMgrImpl == null) {
            throw new IllegalArgumentException("lockMgrImpl may not be null");
        }
        this.lockMgr = lockMgrImpl;

        if (transitionImpl == null) {
            throw new IllegalArgumentException("transitionImpl may not be null");
        }
        this.stateTransition = transitionImpl;

        if (timerManagerImpl == null) {
            throw new IllegalArgumentException("timerManagerImpl may not be null");
        }
        this.timerManager = timerManagerImpl;
    }

    // -------------------------------------------------------------------------
    // ACCESSORS
    // -------------------------------------------------------------------------

    public LockManager getLockManager() {
        return this.lockMgr;
    }

    // -------------------------------------------------------------------------
    // ACTIVATION
    // -------------------------------------------------------------------------

    public synchronized void activate() throws ManageException {
        try {
            this.stateTransition.run(this);
        } catch (ManageException e) {
            logger.error("problem activating " + Lager.id(this.id), e);
            throw e;
        }
    }

    public synchronized void activateOverride(int targetState)
                                                throws ManageException {
        try {
            setTargetState(targetState, true);
        } catch (ManageException e) {
            logger.error("problem activating " + Lager.id(this.id), e);
            throw e;
        }
    }


    // -------------------------------------------------------------------------
    // CURRENT STATE
    // -------------------------------------------------------------------------

    public int getState() {
        return this.state;
    }

    public void setInitialState(int state, Throwable t) {
        this.state = state;
        this.throwableForState = t;
    }

    public Throwable getStateThrowable() {
        return this.throwableForState;
    }

    public void setState(int newstate,
                         Throwable t) throws LockAcquisitionFailure {
        setState(newstate, t, true);
    }

    void setStateUnderLock(int newstate,
                           Throwable t) throws LockAcquisitionFailure {
        setState(newstate, t, false);
    }

    // evaluate flag allows setState calls from within StateTransition to
    // bypass running StateTransition again
    private void setState(int newstate, Throwable t, boolean evaluate)
                                    throws LockAcquisitionFailure {

        Lock lock = null;
        Lock destroy_lock = null;
        if (evaluate) {
            destroy_lock = lockMgr.getLock("destroy_" + this.id);
            lock = lockMgr.getLock(this.id);

            try {
                destroy_lock.lockInterruptibly();
            } catch (InterruptedException e) {
                throw new LockAcquisitionFailure(e);
            }

            try {
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                destroy_lock.unlock();
                throw new LockAcquisitionFailure(e);
            }

            if (lager.traceLog) {
                logger.trace(Lager.id(this.id) + ": acquired lock");
            }
        }

        try {
            setStateImpl(newstate, t, evaluate);
        } finally {
            if (lock != null) {
                if (lager.traceLog) {
                    logger.trace(Lager.id(this.id) + ": releasing lock");
                }
                lock.unlock();
                destroy_lock.unlock();
            }
        }
    }

    private void updateCumulus()
    {
        VirtualMachine vm = this.getVM();

        try
        {
            RepoFileSystemAdaptor nsTrans = XenUtil.getNsTrans();

            VirtualMachinePartition[] parts = vm.getPartitions();

            for(int i = 0; i < parts.length; i++) {
                if (parts[i].isRootdisk()) {
                    String img = parts[i].getImage();
                    if(parts[i].getAlternateUnpropTarget() != null)
                    {
                        img = parts[i].getAlternateUnpropTarget();
                    }

                    if(nsTrans != null) {
                        nsTrans.unpropagationFinished(img, this.getCreatorID(), vm);
                    }
                    break;
                }
            }
        }
        catch(WorkspaceException wex)
        {
            logger.fatal("\nUnable to update the cumulus database about details on the unpropagated file.  The information in cumulus will be wrong!\n"
                    + wex.getMessage() + "\n" + wex.toString());            
        }
    }


    private void setStateImpl(int newstate,
                              Throwable t,
                              boolean evaluate) {

        if (lager.stateLog || lager.traceLog) {

            String msg = "setState(): " + Lager.id(this.id)
                    + ", old state = " + this.dataConvert.stateName(this.state)
                    + ", new state = " + this.dataConvert.stateName(newstate);
            if (!evaluate) {
                msg = msg + " (evaluate off)";
            }

            if (lager.stateLog) {
                logger.trace("\n\n   ***** " + msg + "\n");
            } else if (evaluate) {
                logger.trace(msg);
            }
        }

        if (WorkspaceUtil.isInvalidState(newstate)) {
            logger.fatal("\nprogrammer error, invalid newstate sent to"
                    + " setState: " + Lager.id(this.id)
                    + "\nold state = " + this.dataConvert.stateName(this.state)
                    + "\nnew state = " + this.dataConvert.stateName(newstate)
                    + "\nevaluate = " + evaluate);
            return;
        }

        // no turning back once this state is reached
        if (newstate >= STATE_READYING_FOR_TRANSPORT) {
            // it is not expensive to call this when it is already disabled
            this.setOpsEnabled(false);
        }

        if (newstate == STATE_READY_FOR_TRANSPORT)
        {
            updateCumulus();
        }

        // Some notifications of completed tasks are ignored if we are in
        // the process of destroying.  When the resource's target state is
        // set to destroying, a series of cancellation actions may be
        // taken.
        //
        // Those cancellation actions will not produce a notification to
        // notify().  But those cancellation actions themselves take
        // time, there is a race condition where the pending action at
        // destruction time completes before the cancellation occurs
        // (also, the cancellation action may just fail).  The logic
        // branch below addresses this.
        //
        // When target state is destroying, this is the only time that two
        // WorkspaceRequest objects concerning the same resource should
        // be executing at the same time and/or on a queue waiting to
        // execute at the same time.
        //
        // In the future, we will allow more complicated workflows where
        // a state change may only occur after a number of related actions
        // are complete, but right now there is a serial nature to the
        // tasks + state transitions (except at destroy time as just
        // explained above).  When the serial nature is removed, logic
        // should be written above this layer to handle other workflows.

        if (this.targetState == STATE_DESTROYING
                        && newstate != STATE_DESTROYING) {

            // These states signal where the cancellation related race
            // condition could potentially leave things in an inconsistent
            // state on the managed image repository or hypervisor node's
            // filesystem(s).
            //
            // Run through StateTransition again to run the remove handler
            // again.
            //
            // If a cancellation task is not implemented or fails, this
            // situation would not be rare except for the fact that
            // the remove handler has removed the workspace and
            // WorkspaceHome.find() would not have found it.

            if (newstate == STATE_UNPROPAGATED ||
                newstate == STATE_PROPAGATED ||
                newstate == STATE_STARTED ||
                newstate == STATE_PAUSED ||
                newstate == STATE_READYING_FOR_TRANSPORT ||
                newstate == STATE_READY_FOR_TRANSPORT ||
                newstate == STATE_DESTROY_SUCCEEDED ||
                newstate == STATE_DESTROY_FAILED ||
                newstate == STATE_STAGING_OUT) {

                logger.debug("More termination work may be necessary, " +
                   "not ignoring setState during targetState==Destroying");

            } else {
                return;
            }
        }

        final int oldState = this.state;

        if (newstate == oldState) {
            // got an erroneous message
            logger.debug(Lager.id(this.id) + ": equal states, done");

            if (newstate == STATE_DESTROYING) {
                // this should be an un-reachable situation
                // candidate for admin log/trigger of severe issues
                logger.error("programmer error: " + Lager.id(this.id) +
                        ", current=destroying and target=destroying");
                do_remove();
            }
            return;
        }

        if (newstate == STATE_STARTED) {
            if (this.getVM() == null) {
                logger.fatal("no vm?");
                // falls through to doStateChange
                newstate = STATE_CORRUPTED + oldState;
            }
        }

        if (!doStateChange(newstate, t)) {
            return;
        }

        if (lager.traceLog) {
            logger.trace("doStateChange succeeded, newstate = " + newstate +
                         "  = " + this.dataConvert.stateName(newstate));
        }

        if (newstate == STATE_DESTROY_SUCCEEDED) {
            do_remove();
            return;
        }

        // Corner case where an attempt to bring it out of corrupted has
        // failed and so it was set to corrupted by _setState instead of
        // target state.
        if (oldState >= STATE_CORRUPTED_GENERIC &&
                this.state == STATE_CORRUPTED_GENERIC) {
            return;
        }

        try {
            if (isClientVisibleState(newstate)) {
                this.sendNotifications(newstate, t);
            }
        } catch(Exception e) {
            logger.error("",e);
        }
        
        if (evaluate) {
            // re-evaluate what to do now that the state has changed
            try {
                this.stateTransition.run(this);
            } catch (ManageException e) {
                logger.error(
                        "problem transitioning " + Lager.id(this.id), e);
                // setting to corrupted and/or destroying is dependent on
                // the situation and has already happened at this point
            }
        }

    }

    private void sendNotifications(int newstate,
                                   Throwable throwable) throws Exception {

        if (this.lager.traceLog) {
            final String faultStr;
            if (throwable == null) {
                faultStr = " (throwable not present)";
            } else {
                faultStr = " [[[ throwable is present: '" + 
                        ErrorUtil.recurseForSomeString(throwable) + "']]]";
            }
            logger.trace(Lager.id(this.id) + ": notifications, new state: " +
                               this.dataConvert.stateName(newstate) + faultStr);
        }

        // todo: assuming listeners will quickly return from the notification,
        // should not assume that about all future messaging layers, so in the
        // future launch a separate thread to then make the callbacks (there
        // are many articles about this to consult)
        synchronized(this.stateListeners) {
            
            for (int i = 0; i < this.stateListeners.size(); i++) {

                try {

                    final StateChangeCallback scc =
                            (StateChangeCallback) this.stateListeners.get(i);
                    
                    if (scc != null) {
                        final State stateRepr = this.dataConvert.getState(this);
                        scc.newState(stateRepr);
                    }

                } catch (Throwable t) {
                    final String err = "Problem with asynchronous state " +
                            "change notification: " + t.getMessage();
                    logger.error(err, t);
                }
                
            }
        }
    }

    private boolean doStateChange(int state, Throwable t) {

        if (lager.traceLog) {
            logger.trace(Lager.id(this.id) + "doStateChange(): state = " +
                    state);
        }

        try {
            this.persistence.setState(this.id, state, t);
        } catch (ManageException e) {

            // since we probably cannot also effectively set workspace to
            // corrupted, we are in an inconsistent state now

            // candidate for admin log/trigger of severe issues
            logger.fatal(Lager.id(this.id) + ": exception with " +
                                 "persistence, not notifying", e);
            return false;
        }

        this.state = state;
        this.throwableForState = t;

        try {
            this.scheduler.stateNotification(this.id, state);
        } catch (ManageException e) {
            // this would be very rare
            final String errMsg = Lager.id(this.id) + ": exception with " +
                    "scheduling plugin, setting state to CORRUPTED: " +
                    e.getMessage();
            logger.fatal(errMsg, e);

            this.state = STATE_CORRUPTED_GENERIC;

            try {
                this.persistence.setState(this.id,
                                          STATE_CORRUPTED_GENERIC,
                                          t);
            } catch (ManageException e2) {

                // candidate for admin log/trigger of severe issues
                logger.fatal(Lager.id(this.id) + ": exception with " +
                                "persistence plugin, not notifying",e2);
                return false;
            }
        }

        return true;
    }

    
    // -------------------------------------------------------------------------
    // TARGET STATE
    // -------------------------------------------------------------------------

    public int getTargetState() {
        return this.targetState;
    }

    public void setInitialTargetState(int state) {
        this.targetState = state;
    }

    public synchronized void setTargetState(int targetState)
                                            throws ManageException {
        setTargetState(targetState, false);
    }

    // see activateOverride(), the only valid use of force==true
    private void setTargetState(int targetState, boolean force)
                                            throws ManageException {

        final Lock lock = this.lockMgr.getLock(this.id);

        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new LockAcquisitionFailure(Lager.id(this.id), e);
        }

        if (lager.traceLog) {
            logger.trace(Lager.id(this.id) + ": acquired lock");
        }

        try {
            setTargetStateImpl(targetState, force, true);
        } catch (ManageException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw e;
        } finally {
            if (lager.traceLog) {
                logger.trace(Lager.id(this.id) + ": releasing lock");
            }
            lock.unlock();
        }
    }

    private void setTargetStateImpl(int target,
                                    boolean force,
                                    boolean evaluate)
                                            throws ManageException {

        if (lager.traceLog) {
            final String msg = "setTargetState(): " + Lager.id(this.id) +
                    ", current state = " + this.dataConvert.stateName(this.state) +
                    ", old target = " + this.dataConvert.stateName(this.targetState) +
                    ", target = " + this.dataConvert.stateName(target);

            if (lager.stateLog) {
                logger.trace("\n\n   ***** " + msg + "\n");
            } else {
                logger.trace(msg);
            }
        }

        String msg = isValidTargetState(target);
        if (msg != null) {
            msg = Lager.id(this.id) + " " + msg;
            logger.error(msg);
            throw new ManageException(msg);
        }

        if (!force) {
            if (target == this.targetState) {
                if (lager.traceLog) {
                    logger.trace(Lager.id(this.id)
                                        + ": equal target states, done");
                }
                return;
            }
        }

        try {
            this.persistence.setTargetState(this.id, target);
        } catch (ManageException e) {
            logger.fatal("problem setting target state for "
                                                    + Lager.id(this.id), e);
            return;
        }

        final int oldTargetState = this.targetState;
        this.targetState = target;

        if (evaluate) {
            try {
                // re-evaluate what to do now that the target state has changed
                this.stateTransition.run(this);
            } catch (TaskNotImplementedException e) {
                // set the target state back in this case, these errors are
                // not cause for corruption (which StateTransition handles)
                // don't re-evaluate "new" target state
                this.targetState = oldTargetState;
                try {
                    this.persistence.setTargetState(this.id, target);
                } catch (ManageException e2) {
                    logger.fatal("problem setting target state for "
                                                    + Lager.id(this.id), e2);
                }
                throw e;
            } catch (ManageException e) {
                logger.error("problem transitioning " + Lager.id(this.id), e);
                // setting to corrupted and/or destroying is dependent on the
                // situation and has already happened at this point
                throw e;
            }
        }
    }

    void setTargetStateUnderLock(int newstate) throws ManageException {
        setTargetStateImpl(newstate, false, false);
    }

    public void setTargetStateUnderLockEvaluate(int newstate) throws ManageException {
        setTargetStateImpl(newstate, false, true);
    }

    /**
     * @param targetState state int
     * @return null for success or failure msg for helpful logging
     */
    private String isValidTargetState(int targetState) {

        if (targetState >= STATE_CORRUPTED
                && targetState <= STATE_CORRUPTED+STATE_DESTROYING) {
            return null;
        }

        String msg = null;
        switch(targetState) {
            // may never be an "-ing" transitionary state except for destroying
            case STATE_STAGING_IN:
            case STATE_PROPAGATING:
            case STATE_PROPAGATING_TO_START:
            case STATE_PROPAGATING_TO_PAUSE:
            case STATE_STARTING:
            case STATE_SERIALIZING:
            case STATE_PAUSING:
            case STATE_READYING_FOR_TRANSPORT:
            case STATE_STAGING_OUT:
            case STATE_CANCELLING_STAGING_IN:
            case STATE_CANCELLING_UNPROPAGATED:
            case STATE_CANCELLING_PROPAGATING:
            case STATE_CANCELLING_PROPAGATING_TO_START:
            case STATE_CANCELLING_PROPAGATING_TO_PAUSE:
            case STATE_CANCELLING_AT_VMM:
            case STATE_CANCELLING_READYING_FOR_TRANSPORT:
            case STATE_CANCELLING_READY_FOR_TRANSPORT:
            case STATE_CANCELLING_STAGING_OUT:
                msg = "illegal use of transitionary state '"
                        + this.dataConvert.stateName(targetState)
                        + "' as target state";
                break;
            // legal target states
            case STATE_UNSTAGED:
            case STATE_UNPROPAGATED:
            case STATE_PROPAGATED:
            case STATE_STARTED:
            case STATE_SERIALIZED:
            case STATE_PAUSED:
            case STATE_REBOOT:
            case STATE_READY_FOR_TRANSPORT:
            case STATE_STAGED_OUT:
            case STATE_DESTROYING:
            case STATE_DESTROY_FAILED:
            case STATE_CORRUPTED_GENERIC:
                break;
            default:
                msg = "illegal target state, unknown int " + targetState;
        }

        return msg;
    }

    private static boolean isClientVisibleState(int newstate) {

        for (int i = 0; i < SEND_NOTIFICATION.length; i++) {
            if (newstate == SEND_NOTIFICATION[i]) {
                return true;
            }
        }

        return newstate >= STATE_CORRUPTED_GENERIC
                && newstate <= STATE_LAST_LEGAL;
    }

    public boolean isZombie() {
        return this.state == STATE_DESTROY_FAILED;
    }
}
