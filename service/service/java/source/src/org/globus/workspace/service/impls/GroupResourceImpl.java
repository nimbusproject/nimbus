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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.globus.workspace.service.GroupResource;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.service.WorkspaceGroupHome;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.scheduler.Scheduler;
import org.globus.workspace.Lager;

import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.OperationDisabledException;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.repr.ShutdownTasks;

public class GroupResourceImpl implements GroupResource {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(GroupResourceImpl.class.getName());

    public final static String prefix = "[*] ";
    public final static String badprefix = "[X] ";
    public final static String suffix = "\n";

    protected static final int t_START = 0;
    protected static final int t_SHUTDOWN = 1;
    protected static final int t_SHUTDOWN_SAVE = 2;
    protected static final int t_PAUSE = 3;
    protected static final int t_SERIALIZE = 4;
    protected static final int t_REBOOT = 5;


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final WorkspaceGroupHome ghome;
    protected final WorkspaceHome home;
    protected final Scheduler scheduler;
    protected final Lager lager;

    protected String groupid;
    protected String creatorID;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public GroupResourceImpl(WorkspaceGroupHome groupHomeImpl,
                             WorkspaceHome homeImpl,
                             Scheduler schedulerImpl,
                             Lager lagerImpl) {
        
        if (groupHomeImpl == null) {
            throw new IllegalArgumentException("groupHomeImpl may not be null");
        }
        this.ghome = groupHomeImpl;

        if (homeImpl == null) {
            throw new IllegalArgumentException("homeImpl may not be null");
        }
        this.home = homeImpl;

        if (schedulerImpl == null) {
            throw new IllegalArgumentException("schedulerImpl may not be null");
        }
        this.scheduler = schedulerImpl;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;
    }

    
    // -------------------------------------------------------------------------
    // implements GroupResource
    // -------------------------------------------------------------------------

    public String getID() {
        return this.groupid;
    }

    public void setID(String groupid) {
        this.groupid = groupid;
    }

    public String getCreatorID() {
        return this.creatorID;
    }

    public void setCreatorID(String creator) {
        this.creatorID = creator;
    }


    // -------------------------------------------------------------------------
    // REMOVAL
    // -------------------------------------------------------------------------

    /**
     * Don't call unless you are managing the instance cache (or not using
     * one, perhaps).
     *
     * @throws ManageException
     * @throws DoesNotExistException
     */
    public void remove() throws ManageException, DoesNotExistException {

        if (this.groupid == null) {
            throw new ManageException(
                    "illegal class usage, no group ID was stored at creation");
        }

        try {
            _remove();
        } catch (Throwable t) {
            logger.fatal("Could not remove-all? Via group '" +
                    this.groupid + "': " + t.getMessage(), t);
            throw new ManageException("Internal service error, perhaps " +
                    "you'd like to inform the administrator of the time of " +
                    "the error and this key: '" + this.groupid + "'");
        }
    }

    protected void _remove() throws ManageException {

        if (this.lager.eventLog) {
            logger.info(
                    Lager.groupev(this.groupid) + "destroy begins");
        } else if (this.lager.traceLog) {
            logger.trace(
                    Lager.groupid(this.groupid) + " destroy begins");
        }

        final String errors = do_remove();
        if (errors != null) {
            throw new ManageException(errors);
        }

        if (lager.eventLog) {
            logger.info(Lager.groupev(this.groupid) + "destroyed");
        } else if (lager.traceLog) {
            logger.trace(Lager.groupid(this.groupid) + " destroyed");
        }
    }

    protected String do_remove() {

        final int[] ids;
        try {
            ids = this.ghome.findMemberIDs(this.groupid);
        } catch (ManageException e) {
            return e.getMessage();
        } catch (DoesNotExistException e) {
            return e.getMessage();
        }

        if (ids == null || ids.length == 0) {
            logger.warn("No workspaces found in group '" + this.groupid +
                    "' (not necessarily a problem)");
            return null;
        }

        final String sourceStr = "via group-remove, group " +
                                          "id = '" + this.groupid + "'";
        return this.home.destroyMultiple(ids, sourceStr);
    }


    // -------------------------------------------------------------------------
    // GROUP OPERATIONS
    // -------------------------------------------------------------------------
    
    public void start()
            throws ManageException, DoesNotExistException,
                   OperationDisabledException {
        this.anyOperation(t_START, null);
    }

    public void shutdown(ShutdownTasks tasks)
            throws ManageException, DoesNotExistException,
                   OperationDisabledException {
        this.anyOperation(t_SHUTDOWN, tasks);
    }

    public void shutdownSave(ShutdownTasks tasks)
            throws ManageException, DoesNotExistException,
                   OperationDisabledException {
        this.anyOperation(t_SHUTDOWN_SAVE, tasks);
    }

    public void pause(ShutdownTasks tasks)
            throws ManageException, DoesNotExistException,
                   OperationDisabledException {
        this.anyOperation(t_PAUSE, tasks);
    }

    public void serialize(ShutdownTasks tasks)
            throws ManageException, DoesNotExistException,
                   OperationDisabledException {
        this.anyOperation(t_SERIALIZE, tasks);
    }

    public void reboot(ShutdownTasks tasks)
            throws ManageException, DoesNotExistException,
                   OperationDisabledException {
        this.anyOperation(t_REBOOT, tasks);
    }

    protected void anyOperation(int type, ShutdownTasks tasks)
            throws ManageException, DoesNotExistException,
                   OperationDisabledException {

        final String verb = trTypeVerb(type);
        final String cVerb = trTypeVerbCaps(type);
        final String gerund = trTypeGerund(type);

        final InstanceResource[] resources =
                    this.ghome.findMembers(this.groupid);

        if (resources == null || resources.length == 0) {
            throw new DoesNotExistException("Cannot " + verb + ", there are " +
                    "no workspaces in group '" + this.groupid + "'");
        }

        boolean aProblem = false;
        final StringBuffer returnMsg = new StringBuffer();

        for (int i = 0; i < resources.length; i++) {

            final InstanceResource resource = resources[i];
            
            // in the future, the VMM controller(s) will be able to support
            // getting bulk requests
            resource.setLastInGroup(false);
            resource.setPartOfGroupRequest(false);

            try {

                switch (type) {
                    case t_START:
                        resource.start();
                        break;
                    case t_SHUTDOWN:
                        resource.shutdown(tasks);
                        break;
                    case t_SHUTDOWN_SAVE:
                        resource.shutdownSave(tasks); 
                        break;
                    case t_PAUSE:
                        resource.pause(tasks); 
                        break;
                    case t_SERIALIZE:
                        resource.serialize(tasks); 
                        break;
                    case t_REBOOT:
                        resource.reboot(tasks); 
                        break;
                    default: throw new ManageException("Unknown request");
                }

            } catch (Throwable t) {
                returnMsg.append(badprefix)
                         .append("Problem ")
                         .append(gerund)
                         .append(" workspace #")
                         .append(resource.getID())
                         .append(": ")
                         .append(t.getMessage())
                         .append(suffix);
                aProblem = true;
                continue;
            }

            // we say "sent to" because the task is asynchronous
            returnMsg.append(prefix)
                     .append(cVerb)
                     .append(" sent to workspace #")
                     .append(resource.getID())
                     .append(suffix);
        }

        final String ret = returnMsg.toString();

        // in future return information about what happened to client
        if (this.lager.eventLog) {
            logger.info(Lager.groupev(this.groupid) + "\n" + ret);
        } else if (this.lager.traceLog) {
            logger.trace(Lager.groupid(this.groupid) + "\n" + ret);
        }

        if (aProblem) {
            throw new ManageException(ret);
        }
    }

    protected static String trTypeVerb(int type) {
        switch (type) {
            case t_START: return "start";
            case t_SHUTDOWN: return "shutdown";
            case t_SHUTDOWN_SAVE: return "shutdown-save";
            case t_PAUSE: return "pause";
            case t_SERIALIZE: return "serialize";
            case t_REBOOT: return "reboot";
            default: return "UNKNOWN: " + type;
        }
    }

    protected static String trTypeVerbCaps(int type) {
        switch (type) {
            case t_START: return "Start";
            case t_SHUTDOWN: return "Shutdown";
            case t_SHUTDOWN_SAVE: return "Shutdown-save";
            case t_PAUSE: return "Pause";
            case t_SERIALIZE: return "Serialize";
            case t_REBOOT: return "Reboot";
            default: return "UNKNOWN: " + type;
        }
    }

    protected static String trTypeGerund(int type) {
        switch (type) {
            case t_START: return "starting";
            case t_SHUTDOWN: return "shutting down";
            case t_SHUTDOWN_SAVE: return "running shutdown-save on";
            case t_PAUSE: return "pausing";
            case t_SERIALIZE: return "serializing";
            case t_REBOOT: return "rebooting";
            default: return "UNKNOWN: " + type;
        }
    }

}
