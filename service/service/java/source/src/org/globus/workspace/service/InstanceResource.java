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

package org.globus.workspace.service;

import org.globus.workspace.LockAcquisitionFailure;
import org.globus.workspace.service.binding.vm.FileCopyNeed;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.nimbustools.api.services.rm.OperationDisabledException;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.StateChangeCallback;
import org.nimbustools.api.services.rm.DestructionCallback;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.repr.ShutdownTasks;

import java.util.Calendar;

/**
 * More organization (and docs) coming.  Classes implementing this interface
 * are at the heart of how the service works.
 */
public interface InstanceResource extends Sweepable {

    // -------------------------------------------------------------------------
    // GENERAL INFORMATION
    // -------------------------------------------------------------------------

    public int getID(); // also in Sweepable

    public void setID(int id);

    public String getName();

    public void setName(String name);

    public String getEnsembleId();
    
    public void setEnsembleId(String name);

    public String getGroupId();

    public void setGroupId(String name);

    public int getGroupSize();

    public void setGroupSize(int groupsize);

    public boolean isLastInGroup();

    public void setLastInGroup(boolean lastInGroup);

    public boolean isPartOfGroupRequest();

    public void setPartOfGroupRequest(boolean isInGroupRequest);

    public int getLaunchIndex();

    public void setLaunchIndex(int launchIndex);

    public VirtualMachine getVM();

    public String getCreatorID();

    public void setCreatorID(String ID);

    public Calendar getStartTime();

    public Calendar getTerminationTime(); // also in Sweepable

    public void setTerminationTime(Calendar termTime);

    public void setStartTime(Calendar startTime);


    // -------------------------------------------------------------------------
    // ACTIVATION
    // -------------------------------------------------------------------------

    /**
     * Scheduler must call either this or activateOverride(int) once, when
     * it initially releases the workspace.
     *
     * The rest of the scheduler's calls should be made to setTargetState
     * ONLY.  The reason the first call is not to setTargetState is so
     * we don't have to duplicate each workspace's preferred initial
     * target state (once it gets going) over in the scheduler.
     *
     * However, if the scheduler wants to override the client's requested
     * target state, it can use activateOverride(targetState) below to
     * activate the workspace with the scheduler's desired targetState
     * *instead* of calling activate()
     *
     * @throws ManageException exc
     */
    public void activate()

            throws ManageException;


    /**
     * See activate() documentation
     *
     * @param targetState internal state integer
     * @throws ManageException exc
     */
    public void activateOverride(int targetState)

            throws ManageException;


    // -------------------------------------------------------------------------
    // ACTIONS
    // -------------------------------------------------------------------------
    
    public void start()

            throws ManageException, OperationDisabledException;

    public void shutdown(ShutdownTasks tasks)

            throws ManageException, OperationDisabledException;

    public void shutdownSave(ShutdownTasks tasks)

            throws ManageException, OperationDisabledException;

    public void pause(ShutdownTasks tasks)

            throws ManageException, OperationDisabledException;

    public void serialize(ShutdownTasks tasks)

            throws ManageException, OperationDisabledException;

    public void reboot(ShutdownTasks tasks)

            throws ManageException, OperationDisabledException;


    // -------------------------------------------------------------------------
    // INFORMATION / ADJUSTMENTS
    // -------------------------------------------------------------------------

    public void newStartTime(Calendar startTime);

    public void newStopTime(Calendar stopTime);

    public void newNetwork(String network);

    public void newFileCopyNeed(FileCopyNeed need);

    /**
     * Don't call unless you are managing the instance cache (or not using
     * one, perhaps).
     *
     * @throws ManageException problem
     * @throws DoesNotExistException missing
     */
    public void remove()

            throws ManageException, DoesNotExistException;

    /**
     * Called when a new state is actually achieved.  Depending on the
     * target state, this can cause task requests to be issued from
     * StateTransition.
     *
     * The ONLY way to change a resource state (besides special method
     * setInitialState).
     *
     * @param state internal state integer
     * @param throwable problem accompanying state, may be null
     * @throws LockAcquisitionFailure exc
     */
    public void setState(int state, Throwable throwable)

            throws LockAcquisitionFailure;

    public int getState();

    public Throwable getStateThrowable();


    /**
     * Called to set a target state.  Depending on the current state,
     * this can cause task requests to be issued from StateTransition.
     *
     * The ONLY way to change a target state (besides special methods
     * setInitialTargetState and activateOverride).
     *
     * @param state internal state integer
     * @throws ManageException exc
     */
    public void setTargetState(int state)

            throws ManageException;

    public int getTargetState();

    /**
     * @return true if client is permitted to invoke WS operations
     *         other than destroy
     */
    public boolean isOpsEnabled();

    public void setOpsEnabled(boolean opsEnabled);


    /**
     * Propagation is required if the deployment request included images
     * with URL schemes matching the need for propagation.
     *
     * If propagate is required and there is no propagate implementation
     * configured (or the implementation cannot handle the request or
     * fails to), then we move to corrupted state.
     *
     * Set only once, at resource creation.
     *
     * @return true if required
     */
    public boolean isPropagateRequired();


    /**
     * Returns true if propagation was required unless there is a situation
     * like the client's default shutdown-state request is shutdown-trash.
     *
     * We currently don't have an option for that in the WSDL (hoping we
     * will), but best to separate this internally from the start.
     *
     * Set only once, at resource creation.
     * @ see #isPropagateRequired
     *
     * @return true if required
     */
    public boolean isUnPropagateRequired();

    /**
     * Propagation + start is a common optimization, allowing the remote
     * VMM to pull files down to its node and start the VM without needing
     * an additional notification + response round.  Some per-resource
     * situations may require disabling this, allowing work to be done
     * between propagate and start (start and start-paused are treated
     * the same here).
     *
     * Set only once, at resource creation.
     *
     * @return true if required
     */
    public boolean isPropagateStartOK();


    /**
     * In rare cases, a slot may have been evicted and the service did not do
     * any of the necessary clean up work (perhaps the service was down and
     * couldn't act in time).  In those cases during cancellation/destruction,
     * it is not OK to send a workspace-trash signal to workspace-control.
     * It also means actions such as transfers are not legal.
     *
     * @return false if we are in a restricted environment and the chance
     *         to alter an image on the VMM has passed.
     */
    public boolean isVMMaccessOK();

    public void setVMMaccessOK(boolean vmmAccessOK);

    public void newHostname(String hostname);


    // -------------------------------------------------------------------------
    // LISTENERS
    // -------------------------------------------------------------------------

    public void registerStateChangeListener(StateChangeCallback listener);

    public void registerDestructionListener(DestructionCallback listener);


    // -------------------------------------------------------------------------
    // INITIAL POPULATION
    // -------------------------------------------------------------------------

    /**
     * Only called when WorkspaceHome creates this resource.
     *
     * WorkspaceResource interface
     *
     * @param id id#
     * @param binding instantiation details
     * @param startTime start, can be null
     * @param termTime resource destruction time, can be null which mean either
     *                 "never" or "no setting" (while using best-effort sched)
     * @param node assigned node, can be null
     * @throws CreationException problem
     */
    public void populate(int id,
                         VirtualMachine binding,
                         Calendar startTime,
                         Calendar termTime,
                         String node)

            throws CreationException;

    /**
     * Don't call directly unless you are managing the cache.
     *
     * @param key id
     * @throws ManageException problem
     * @throws DoesNotExistException missing
     */
    public void load(String key)

            throws ManageException, DoesNotExistException;

    /**
     * For creating the object in the first place; setInitialOpsEnabled()
     * is expected to persist changes - but entry for this workspace would
     * not exist in persistence yet when setInitialOpsEnabled() is needed.
     *
     * Calling this at the incorrect time will break assumptions.
     *
     * @param opsEnabled opsEnabled status
     */
    public void setInitialOpsEnabled(boolean opsEnabled);

    /**
     * For creating the object in the first place; setState() is expected
     * to persist changes - but entry for this workspace would not exist
     * in persistence yet when setInitialState() is needed.
     *
     * Calling this at the incorrect time will break assumptions.
     *
     * @param state internal state integer
     * @param throwable problem, may be null
     */
    public void setInitialState(int state, Throwable throwable);


    /**
     * For creating the object in the first place; setTargetState() is
     * expected to persist changes - but entry for this workspace would
     * not exist in persistence yet when setInitialTargetState() is needed.
     *
     * Calling this at the incorrect time will break many assumptions
     *
     * @param state internal state integer
     */
    public void setInitialTargetState(int state);

    /**
     * For creating the object in the first place; setVMMaccessOK()
     * is expected to persist changes - but entry for this workspace would
     * not exist in persistence yet when setVMMaccessOK() is needed.
     *
     * Calling this at the incorrect time will break assumptions.
     *
     * @param trashOK vmmAccessOK status
     */
    public void setInitialVMMaccessOK(boolean trashOK);
}
