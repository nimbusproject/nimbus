/*
 * Copyright 1999-2009 University of Chicago
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

package org.nimbustools.gateway.ec2.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.api.repr.Advertised;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.ShutdownTasks;
import org.nimbustools.api.repr.Usage;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.rm.CoSchedulingException;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.DestructionCallback;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.MetadataException;
import org.nimbustools.api.services.rm.OperationDisabledException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.SchedulingException;
import org.nimbustools.api.services.rm.StateChangeCallback;
import org.nimbustools.gateway.ec2.creation.Creation;

import java.util.Calendar;

public class EC2GatewayManager implements Manager {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(EC2GatewayManager.class.getName());

    protected static final VM[] EMPTY_VM_ARRAY = new VM[0];
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final Creation creation;

    public EC2GatewayManager(Creation creation) {
        if (creation == null) {
            throw new IllegalArgumentException("creation may not be null");
        }
        this.creation = creation;
    }


    // -------------------------------------------------------------------------
    // implements NimbusModule
    // -------------------------------------------------------------------------

    public String report() {
        final StringBuffer buf = new StringBuffer("Class: ");
        buf.append(this.getClass().getName())
           .append("\n")
           .append("Workspace Service Manager");
        return buf.toString();
    }


    // -------------------------------------------------------------------------
    // implements Manager (several following sections)
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // LIFECYCLE
    // -------------------------------------------------------------------------

    /**
     * <p>This informs the implementation that its container (or whatever is
     * instantiating it) has just recovered from a failure or restart.</p>
     *
     * <p>If it is left up to normal initialization mechanisms, then there is
     * no chance for change/destruction listeners to be registered <i>before</i>
     * something happens.</p>
     *
     * @throws Exception problem, cannot continue
     */
    public void recover_initialize() throws Exception {
        logger.info("recover_initialize");
    }

    public void shutdownImmediately() {
        logger.info("shutdownImmediately");
    }



    // -------------------------------------------------------------------------
    // EVENTS CAUSED BY USER OPERATIONS - MUTATIVE
    // -------------------------------------------------------------------------

    public CreateResult create(CreateRequest req, Caller caller)
           throws CoSchedulingException,
                  CreationException,
                  MetadataException,
                  ResourceRequestDeniedException,
                  SchedulingException {

        logger.info("create");
        return null;
    }

    public void setDestructionTime(String id, int type, Calendar time)
            throws DoesNotExistException, ManageException {
        logger.info("setDestructionTime");
    }

    public void trash(String id, int type, Caller caller)
            throws DoesNotExistException, ManageException {
        logger.info("trash");
    }

    public void start(String id, int type, Caller caller)
            throws DoesNotExistException, ManageException,
                   OperationDisabledException {
        logger.info("start");
    }

    public void shutdown(String id, int type,
                         ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {
        logger.info("shutdown");
    }

    public void shutdownSave(String id, int type,
                             ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {
        logger.info("shutdownSave");
    }

    public void pause(String id, int type,
                      ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {
        logger.info("pause");
    }

    public void serialize(String id, int type,
                          ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {
        logger.info("serialize");
    }

    public void reboot(String id, int type,
                       ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException,
                   ManageException,
                   OperationDisabledException {
        logger.info("reboot");
    }

    public void coscheduleDone(String id, Caller caller)

            throws DoesNotExistException,
                   ManageException,
                   CoSchedulingException {
        logger.info("coscheduleDone");
    }


    // -------------------------------------------------------------------------
    // INFORMATION QUERIES
    // -------------------------------------------------------------------------

    public boolean exists(String id, int type) {
        return true;
    }

    public Calendar getDestructionTime(String id, int type)
            throws DoesNotExistException, ManageException {
        logger.info("getDestructionTime");
        return null;
    }

    public Caller[] getAuthorizedManagers(String id, int type)
            throws DoesNotExistException, ManageException {
        logger.info("getAuthorizedManagers");
        return null;
    }

    public VM getInstance(String id)
            throws DoesNotExistException, ManageException {
        logger.info("getInstance");
        return null;
    }

    public VM[] getAll(String id, int type)
            throws DoesNotExistException, ManageException {
        logger.info("getInstance");
        return null;
    }

    public VM[] getAllByCaller(Caller caller)
            throws ManageException {
        logger.info("getAllByCaller");
        return null;
    }

    public VM[] getAllByIPAddress(String ip) throws ManageException {
        logger.info("getAllByIPAddress");
        return null;
    }

    public VM[] getGlobalAll() throws ManageException {
        logger.info("getGlobalAll");
        return null;
    }

    public Usage getCallerUsage(Caller caller) throws ManageException {
        logger.info("getCallerUsage");
        return null;
    }

    public Advertised getAdvertised() {
        logger.info("getAdvertised");
        return null;
    }

    public void registerStateChangeListener(String id,
                                            int type,
                                            StateChangeCallback listener)

            throws ManageException, DoesNotExistException {
        logger.info("registerStateChangeListener");
    }

    public void registerDestructionListener(String id,
                                            int type,
                                            DestructionCallback listener)

            throws ManageException, DoesNotExistException {
        logger.info("registerDestructionListener");
    }
}
