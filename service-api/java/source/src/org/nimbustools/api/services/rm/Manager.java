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

package org.nimbustools.api.services.rm;

import java.util.Calendar;

import org.nimbustools.api.NimbusModule;
import org.nimbustools.api.repr.Advertised;
import org.nimbustools.api.repr.AsyncCreateRequest;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.RequestInfo;
import org.nimbustools.api.repr.ShutdownTasks;
import org.nimbustools.api.repr.SpotCreateRequest;
import org.nimbustools.api.repr.SpotPriceEntry;
import org.nimbustools.api.repr.SpotRequestInfo;
import org.nimbustools.api.repr.Usage;
import org.nimbustools.api.repr.vm.VM;

/**
 * <p><img src="http://www.nimbusproject.org/images/sh.png" alt="[Start here] " /> 
 * The main RM interface.</p>
 *
 * <p>Better documentation/diagrams coming, the best way to learn right now is to
 * see how it is invoked in one of the message layer implementations.</p>
 *
 * <p>The main documentation task is to document the tricky areas like 'legal
 * inputs' etc.  From looking at the method names you should be able to
 * see the basic usage:</p>
 *
 * <ul>
 *  <li>create VMs</li>
 *  <li>request spot instances</li>
 *  <li>manage VMs by instance handle or group handle</li>
 *  <li>destroy VMs by instance handle or group handle</li>
 *  <li>query for up to date information</li>
 *  <li>register callbacks for state changes or terminations events</li>
 * </ul>
 *
 * <p>Click on the "Manager" link above to see the source code which has
 * a more natural grouping of the methods.</p>
 *
 * <p><b>todo</b>: document each method independently</p>
 * <p><b>todo</b>: richer exceptions/hiearchy</p>
 */
public interface Manager extends NimbusModule {

    // -------------------------------------------------------------------------
    // CONSTANTS
    // -------------------------------------------------------------------------

    public static final int INSTANCE = 0;
    public static final int GROUP = 1;
    public static final int COSCHEDULED = 2;

    
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
    public void recover_initialize() throws Exception;

    /**
     * Dying.
     */
    public void shutdownImmediately();
    

    // -------------------------------------------------------------------------
    // EVENTS CAUSED BY USER OPERATIONS - MUTATIVE
    // -------------------------------------------------------------------------

    public CreateResult create(CreateRequest req, Caller caller)
           throws AuthorizationException,
                  CoSchedulingException,
                  CreationException,
                  MetadataException,
                  ResourceRequestDeniedException,
                  SchedulingException;

    public void setDestructionTime(String id, int type, Calendar time)
            throws DoesNotExistException, ManageException;

    public void trash(String id, int type, Caller caller)
            throws DoesNotExistException, ManageException;

    public void start(String id, int type, Caller caller)
            throws DoesNotExistException, ManageException, OperationDisabledException;
    
    public void shutdown(String id, int type,
                         ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException, ManageException, OperationDisabledException;
    
    public void shutdownSave(String id, int type,
                             ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException, ManageException, OperationDisabledException;

    public void pause(String id, int type,
                      ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException, ManageException, OperationDisabledException;

    public void serialize(String id, int type,
                          ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException, ManageException, OperationDisabledException;

    public void reboot(String id, int type,
                       ShutdownTasks tasks, Caller caller)
            throws DoesNotExistException, ManageException, OperationDisabledException;

    public void coscheduleDone(String id, Caller caller)
            throws DoesNotExistException, ManageException, CoSchedulingException;
    
    
    // -------------------------------------------------------------------------
    // INFORMATION QUERIES
    // -------------------------------------------------------------------------

    public boolean exists(String id, int type);
    
    public Advertised getAdvertised();

    public Calendar getDestructionTime(String id, int type)
            throws DoesNotExistException, ManageException;

    public Caller[] getAuthorizedManagers(String id, int type)
            throws DoesNotExistException, ManageException;

    public VM getInstance(String id)
            throws DoesNotExistException, ManageException;

    public VM[] getAll(String id, int type)
            throws DoesNotExistException, ManageException;

    public VM[] getAllByCaller(Caller caller)
            throws ManageException;

    public VM[] getAllByIPAddress(String ip)
            throws ManageException;

    public VM[] getGlobalAll()
            throws ManageException;

    public Usage getCallerUsage(Caller caller)
            throws ManageException;

    public void registerStateChangeListener(String id,
                                            int type,
                                            StateChangeCallback listener)
            throws ManageException, DoesNotExistException;
    
    public void registerDestructionListener(String id,
                                            int type,
                                            DestructionCallback listener)
            throws ManageException, DoesNotExistException;

    /* Human readable VMM insight */
    public String getVMMReport();

    public String[] getResourcePools();

    // -------------------------------------------------------------------------
    // SPOT INSTANCES OPERATIONS
    // -------------------------------------------------------------------------    
    
    public SpotRequestInfo requestSpotInstances(SpotCreateRequest req, Caller caller)
            throws AuthorizationException,
                   CoSchedulingException,
                   CreationException,
                   MetadataException,
                   ResourceRequestDeniedException,
                   SchedulingException;     
    
    public SpotRequestInfo getSpotRequest(String requestID, Caller caller)
            throws DoesNotExistException, ManageException, AuthorizationException;
    
    public SpotRequestInfo[] getSpotRequests(String[] ids, Caller caller)
            throws DoesNotExistException, ManageException, AuthorizationException;    
    
    public SpotRequestInfo[] getSpotRequestsByCaller(Caller caller)
            throws ManageException;    
    
    public SpotRequestInfo[] cancelSpotInstanceRequests(String[] ids,
                                                    Caller caller)
            throws DoesNotExistException, AuthorizationException, ManageException;
    
    public Double getSpotPrice();

    public SpotPriceEntry[] getSpotPriceHistory()
            throws ManageException;
    
    public SpotPriceEntry[] getSpotPriceHistory(Calendar startDate, Calendar endDate)
            throws ManageException; 
    
    // -------------------------------------------------------------------------
    // BACKFILL OPERATIONS
    // -------------------------------------------------------------------------    
    
    public RequestInfo addBackfillRequest(AsyncCreateRequest req, Caller caller)
            throws AuthorizationException,
                   CoSchedulingException,
                   CreationException,
                   MetadataException,
                   ResourceRequestDeniedException,
                   SchedulingException;     
    
    public RequestInfo getBackfillRequest(String requestID, Caller caller)
            throws DoesNotExistException, ManageException, AuthorizationException;
    
    public RequestInfo[] getBackfillRequests(String[] ids, Caller caller)
            throws DoesNotExistException, ManageException, AuthorizationException;    
    
    public RequestInfo[] getBackfillRequestsByCaller(Caller caller)
            throws ManageException;    
    
    public RequestInfo[] cancelBackfillRequests(String[] ids,
                                                    Caller caller)
            throws DoesNotExistException, AuthorizationException, ManageException;
    
}
