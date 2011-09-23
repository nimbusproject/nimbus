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

package org.nimbustools.api.defaults.services.rm;

import org.nimbustools.api.brain.Logging;
import org.nimbustools.api.repr.Advertised;
import org.nimbustools.api.repr.AsyncCreateRequest;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.RequestInfo;
import org.nimbustools.api.repr.SpotCreateRequest;
import org.nimbustools.api.repr.SpotPriceEntry;
import org.nimbustools.api.repr.SpotRequestInfo;
import org.nimbustools.api.repr.ShutdownTasks;
import org.nimbustools.api.repr.Usage;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.BasicLegality;
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

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;

/**
 * This does nothing but log invocations.
 */
public class DefaultManager implements Manager {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final ReprFactory repr;
    private final BasicLegality legals;
    private final DateFormat localFormat = DateFormat.getDateTimeInstance();

    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DefaultManager(ReprFactory reprFactory, BasicLegality legals) {
        if (reprFactory == null) {
            throw new IllegalArgumentException("reprFactory may not be null");
        }
        if (legals == null) {
            throw new IllegalArgumentException("legals may not be null");
        }
        this.repr = reprFactory;
        this.legals = legals;
    }

    
    // -------------------------------------------------------------------------
    // implements NimbusModule
    // -------------------------------------------------------------------------

    public String report() {
        final StringBuffer buf = new StringBuffer("Class: ");
        buf.append(this.getClass().getName())
           .append("\n")
           .append("WARNING: This does nothing but log things");
        return buf.toString();
    }


    // -------------------------------------------------------------------------
    // implements Manager: LIFECYCLE
    // -------------------------------------------------------------------------

    /**
     * <p>This informs the implementation that its container (or whatever is
     * instantiating it) has just recovered from a failure or restart.</p>
     *
     * <p>If it is left up to normal initialization mechanisms, then there is no
     * chance for change/destruction listeners to be registered <i>before</i>
     * something happens.</p>
     *
     * @throws Exception problem, cannot continue
     */
    public void recover_initialize() throws Exception {
        Logging.debug("Manager.recover_initialize()");
    }

    public void shutdownImmediately() {
        Logging.debug("Manager.shutdownImmediately()");
    }

    // -------------------------------------------------------------------------
    // implements Manager: EVENTS CAUSED BY USER OPERATIONS - MUTATIVE
    // -------------------------------------------------------------------------

    public CreateResult create(CreateRequest req, Caller caller)

            throws CoSchedulingException,
                   CreationException,
                   MetadataException,
                   ResourceRequestDeniedException,
                   SchedulingException {

        Logging.debug("Manager.create() -- caller '" + caller + "', " +
                                       "request:" + req);
        return null;
    }
    
    public void setDestructionTime(String id, int type, Calendar time)
            throws DoesNotExistException, ManageException {
        Logging.debug("Manager.setDestructionTime() -- id '" + id +
                      "', type '" + type +
                      "', time '" + this.localFormat.format(time) + "'");
    }

    public void trash(String id, int type, Caller caller)
            throws DoesNotExistException, ManageException {
        Logging.debug("Manager.trash() -- id '" + id +
                            "', type '" + type + "'");
    }

    public void start(String id, int type, Caller caller) throws
                                                             DoesNotExistException,
                                                             ManageException,
                                                             OperationDisabledException {
        Logging.debug("Manager.start() -- id '" + id +
                            "', type '" + type + "', caller '" + caller + "'");
    }

    public void shutdown(String id, int type, ShutdownTasks tasks,
                         Caller caller) throws DoesNotExistException,
                                               ManageException,
                                               OperationDisabledException {
        Logging.debug("Manager.shutdown() -- id '" + id +
                        "', type '" + type + "'" + ", tasks '" + tasks +
                        "', caller '" + caller + "'");
    }

    public void shutdownSave(String id, int type, ShutdownTasks tasks,
                             Caller caller) throws DoesNotExistException,
                                                   ManageException,
                                                   OperationDisabledException {
        Logging.debug("Manager.shutdownSave() -- id '" + id +
                        "', type '" + type + "'" + ", tasks '" + tasks +
                        "', caller '" + caller + "'");
    }

    public void pause(String id, int type, ShutdownTasks tasks,
                      Caller caller) throws DoesNotExistException,
                                            ManageException,
                                            OperationDisabledException {
        Logging.debug("Manager.pause() -- id '" + id +
                        "', type '" + type + "'" + ", tasks '" + tasks +
                        "', caller '" + caller + "'");
    }

    public void serialize(String id, int type, ShutdownTasks tasks,
                          Caller caller) throws DoesNotExistException,
                                                ManageException,
                                                OperationDisabledException {
        Logging.debug("Manager.serialize() -- id '" + id +
                        "', type '" + type + "'" + ", tasks '" + tasks +
                        "', caller '" + caller + "'");
    }

    public void reboot(String id, int type, ShutdownTasks tasks,
                       Caller caller) throws DoesNotExistException,
                                             ManageException,
                                             OperationDisabledException {
        Logging.debug("Manager.reboot() -- id '" + id +
                        "', type '" + type + "'" + ", tasks '" + tasks +
                        "', caller '" + caller + "'");
    }

    public void coscheduleDone(String id, Caller caller)
            throws DoesNotExistException, ManageException {
        Logging.debug("Manager.coscheduleDone() -- id '" + id +
                        "', caller '" + caller + "'");
    }

    // -------------------------------------------------------------------------
    // implements Manager: INFORMATION QUERIES
    // -------------------------------------------------------------------------

    public boolean exists(String id, int type) {
        Logging.debug("Manager.exists() -- id '" + id +
                        "', type '" + type + "'");
        return false;
    }

    public Advertised getAdvertised() {
        Logging.debug("Manager.getAdvertised()");
        return null;
    }

    public Calendar getDestructionTime(String id, int type)
            throws DoesNotExistException, ManageException {
        Logging.debug("Manager.getDestructionTime() -- id '" + id +
                        "', type '" + type + "'");
        return null;
    }

    public Caller[] getAuthorizedManagers(String id, int type)
            throws DoesNotExistException, ManageException {
        Logging.debug("Manager.getAuthorizedManagers() -- id '" + id +
                        "', type '" + type + "'");
        return new Caller[0];
    }

    public VM getInstance(String id)
            throws DoesNotExistException, ManageException {
        Logging.debug("Manager.getInstance() -- id '" + id + "'");
        return null;
    }


    public VM[] getAll(String id, int type)
            throws DoesNotExistException, ManageException {
        Logging.debug("Manager.getAll() -- id '" + id +
                        "', type '" + type + "'");
        return new VM[0];
    }

    public VM[] getAllByCaller(Caller caller) throws ManageException {
        Logging.debug("Manager.getAllByCaller() -- caller '" + caller + "'");
        return new VM[0];
    }

    public VM[] getAllByIPAddress(String ip) throws ManageException {
        Logging.debug("Manager.getAllByIPAddress() -- ip '" + ip + "'");
        return new VM[0];
    }

    public VM[] getGlobalAll() throws ManageException {
        Logging.debug("Manager.getGlobalAll()");
        return new VM[0];
    }

    public Usage getCallerUsage(Caller caller) throws ManageException {
        Logging.debug("Manager.getCallerUsage() -- caller '" + caller + "'");
        return null;
    }

    public void registerStateChangeListener(String id, int type,
                                            StateChangeCallback listener) {
        Logging.debug("Manager.registerStateChangeListener() -- id '" + id +
                        "', type '" + type + "', listener '" + listener + "'");
    }

    public void registerDestructionListener(String id, int type,
                                            DestructionCallback listener) {
        Logging.debug("Manager.registerDestructionListener() -- id '" + id +
                "', type '" + type + "', listener '" + listener + "'");
    }

    public String getVMMReport() {
        return "No report";
    }

    public String[] getResourcePools() {
        return new String[0];
    }

    // -------------------------------------------------------------------------
    // SPOT INSTANCES OPERATIONS
    // -------------------------------------------------------------------------        
    
    public SpotRequestInfo requestSpotInstances(SpotCreateRequest req, Caller caller) {

        Logging.debug("Manager.requestSpotInstances() -- caller '" + caller + "', " +
                "request:" + req);
        
        return null;
    }        
    
    public Double getSpotPrice() {

        Logging.debug("Manager.getSpotPrice()'");        
        
        return null;
    }


    public SpotRequestInfo getSpotRequest(String id, Caller caller) throws DoesNotExistException,
            ManageException {
        Logging.debug("Manager.getSpotRequest() -- caller '" + caller + "', " +
                "id:" + id);
        
        return null;
    }


    public SpotRequestInfo[] getSpotRequestsByCaller(Caller caller)
            throws ManageException {
        Logging.debug("Manager.getSpotRequestsByCaller() -- caller '" + caller + "'");
        
        return null;
    }


    public SpotRequestInfo[] cancelSpotInstanceRequests(String[] ids, Caller caller) {
        Logging.debug("Manager.cancelSpotInstanceRequests() -- caller '" + caller + "', " +
                "ids:" + Arrays.toString(ids));
        
        return null;
    }


    public SpotRequestInfo[] getSpotRequests(String[] ids, Caller caller)
            throws DoesNotExistException, ManageException,
            AuthorizationException {
        Logging.debug("Manager.getSpotRequest() -- caller '" + caller + "', " +
                "ids:" + Arrays.toString(ids));
        
        return null;
    }

    public SpotPriceEntry[] getSpotPriceHistory() throws ManageException {
        Logging.debug("Manager.getSpotPriceHistory()'");        
        
        return null;
    }

    public SpotPriceEntry[] getSpotPriceHistory(Calendar startDate,
            Calendar endDate) throws ManageException {
        Logging.debug("Manager.getSpotPriceHistory() startDate: " + startDate + ". endDate: " + endDate);
        return null;
    }

    public RequestInfo addBackfillRequest(AsyncCreateRequest req, Caller caller)
            throws AuthorizationException, CoSchedulingException,
            CreationException, MetadataException,
            ResourceRequestDeniedException, SchedulingException {
        Logging.debug("Manager.addBackfillRequest() -- caller '" + caller + "', " +
                "request:" + req);
        
        return null;
    }

    public RequestInfo[] cancelBackfillRequests(String[] ids, Caller caller)
            throws DoesNotExistException, AuthorizationException,
            ManageException {
        Logging.debug("Manager.cancelBackfillRequests() -- caller '" + caller + "', " +
                "ids:" + Arrays.toString(ids));
        
        return null;
    }

    public RequestInfo getBackfillRequest(String requestID, Caller caller)
            throws DoesNotExistException, ManageException,
            AuthorizationException {
        Logging.debug("Manager.getBackfillRequest() -- caller '" + caller + "', " +
                "id:" + requestID);
        
        return null;
    }

    public RequestInfo[] getBackfillRequestsByCaller(Caller caller)
            throws ManageException {
        Logging.debug("Manager.getBackfillRequestsByCaller() -- caller '" + caller + "'");
        
        return null;
    }


    public RequestInfo[] getBackfillRequests(String[] ids, Caller caller)
            throws DoesNotExistException, ManageException,
            AuthorizationException {

        Logging.debug("Manager.getBackfillRequests() -- caller '" + caller + "', " +
                "ids:" + Arrays.toString(ids));        
        
        return null;
    }

}
