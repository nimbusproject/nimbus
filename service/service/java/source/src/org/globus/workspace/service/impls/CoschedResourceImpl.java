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

import org.globus.workspace.service.CoschedResource;
import org.globus.workspace.service.WorkspaceCoschedHome;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.scheduler.Scheduler;
import org.globus.workspace.Lager;

import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.CoSchedulingException;
import org.nimbustools.api.services.rm.DoesNotExistException;

public class CoschedResourceImpl implements CoschedResource {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(CoschedResourceImpl.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final WorkspaceCoschedHome cohome;
    protected final WorkspaceHome home;
    protected final Scheduler scheduler;
    protected final Lager lager;

    protected String coschedid;
    protected String creatorID;
    

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public CoschedResourceImpl(WorkspaceCoschedHome cohomeImpl,
                               WorkspaceHome homeImpl,
                               Scheduler schedulerImpl,
                               Lager lagerImpl) {

        if (cohomeImpl == null) {
            throw new IllegalArgumentException("co may not be null");
        }
        this.cohome = cohomeImpl;

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
    // implements CoschedResource
    // -------------------------------------------------------------------------

    public String getID() {
        return this.coschedid;
    }

    public void setID(String coschedid) {
        this.coschedid = coschedid;
    }

    public String getCreatorID() {
        return this.creatorID;
    }

    public void setCreatorID(String creator) {
        this.creatorID = creator;
    }
    
    public void done() throws CoSchedulingException {

        if (this.coschedid == null) {
            throw new CoSchedulingException(
                    "illegal class usage, no coschedid was stored at creation");
        }

        try {
            this.scheduler.proceedCoschedule(this.coschedid);
        } catch (Exception e) {
            throw new CoSchedulingException(e.getMessage(), e);
        }
    }

    /**
     * Don't call unless you are managing the instance cache (or not using
     * one, perhaps).
     * 
     * @throws ManageException problem
     */
    public void remove() throws ManageException {

        if (this.coschedid == null) {
            throw new ManageException(
                    "illegal class usage, no coschedid was stored at creation");
        }

        try {
            _remove();
        } catch (Throwable t) {
            logger.fatal("Could not remove-all? Via cosched group '" +
                    this.coschedid + "': " + t.getMessage(), t);
            throw new ManageException("Internal service error, perhaps " +
                    "you'd like to inform the administrator of the time of " +
                    "the error and this key: '" + this.coschedid + "'");
        }
    }

    protected void _remove() throws ManageException {

        if (this.lager.eventLog) {
            logger.info(
                    Lager.ensembleev(this.coschedid) + "destroy begins");
        } else if (this.lager.traceLog) {
            logger.trace(
                    Lager.ensembleid(this.coschedid) + " destroy begins");
        }

        final String errors = do_remove();
        if (errors != null) {
            throw new ManageException(errors);
        }

        if (lager.eventLog) {
            logger.info(Lager.ensembleev(this.coschedid) + "destroyed");
        } else if (lager.traceLog) {
            logger.trace(Lager.ensembleid(this.coschedid) + " destroyed");
        }
    }

    protected String do_remove() {

        final int[] ids;
        try {
            ids = this.cohome.findMemberIDs(this.coschedid);
        } catch (ManageException e) {
            return e.getMessage();
        } catch (DoesNotExistException e) {
            return e.getMessage();
        }

        if (ids == null || ids.length == 0) {
            logger.warn("Removing, but no workspaces found in coscheduling " +
                "group '" + this.coschedid + "' (not necessarily a problem)");
            return null;
        }

        final String sourceStr = "via cosched-remove, cosched-group " +
                                          "id = '" + this.coschedid + "'";
        return this.home.destroyMultiple(ids, sourceStr);
    }
}
