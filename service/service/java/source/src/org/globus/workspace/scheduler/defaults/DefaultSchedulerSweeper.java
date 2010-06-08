/*
 * Copyright 1999-2010 University of Chicago
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

package org.globus.workspace.scheduler.defaults;

import commonj.timers.Timer;
import commonj.timers.TimerListener;
import commonj.timers.TimerManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.globus.workspace.Lager;
import org.globus.workspace.WorkspaceConstants;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.service.InstanceResource;

public class DefaultSchedulerSweeper implements TimerListener,
                                                WorkspaceConstants,
                                                DefaultSchedulerConstants {

    private static final Log logger =
        LogFactory.getLog(DefaultSchedulerSweeper.class.getName());


    private final WorkspaceHome home;
    private final TimerManager timerManager;
    private final long delay;
    private final DefaultSchedulerAdapter scheduler;
    private final Lager lager;

    private Timer timerInstance;

    public DefaultSchedulerSweeper(TimerManager mgr,
                                   WorkspaceHome homeImpl,
                                   long delayMs,
                                   Lager lagerImpl,
                                   DefaultSchedulerAdapter adapter) {

        if (homeImpl == null || mgr == null ||
                adapter == null || lagerImpl == null) {
            throw new IllegalArgumentException();
        }
        
        this.delay = delayMs;
        this.scheduler = adapter;
        this.timerManager = mgr;
        this.home = homeImpl;
        this.lager = lagerImpl;
    }

    public void timerExpired(Timer timer) {
        if (lager.schedLog) {
            logger.trace("timerExpired()");
        }

        int left = 0;
        try {
            left = this.scheduler.anyLeft();
        } catch (WorkspaceDatabaseException e) {
            logger.error("", e);
        }

        int killed = 0;

        if (left > 0) {
            // right now this class only invokes shutdown
            killed = sweep();
            if (lager.schedLog) {
                logger.trace("shutdown " + killed + " workspaces");
            }
        }

        resetSweeper();

        if (left > killed) {
            scheduleSweeper();
        } else {
            if (lager.schedLog) {
                logger.trace("no workspaces are left that are unpropagated," +
                        " propagated, running, or paused: not re-scheduling" +
                        " DefaultSchedulerSweeper");
            }
        }
    }

    synchronized void scheduleSweeper() {
        if (lager.schedLog) {
            logger.trace("scheduleSweeper()");
        }

        if (this.timerInstance == null) {
            this.timerInstance = this.timerManager.schedule(this, this.delay);
            if (lager.schedLog) {
                logger.trace("scheduled sweeper");
            }
        }
    }

    private synchronized void resetSweeper() {
        if (lager.schedLog) {
            logger.trace("resetSweeper()");
        }

        this.timerInstance = null;
    }


    /**
     * TODO: sweeper should poll less often and start timed events for anything
     *       set to happen before the next sweep (using TimerListener from
     *       core).  It will have to track which events have been instantiated
     *       that way in case container crashes in between starting the timed
     *       listener and the event's timer going off and completing.
     *
     * @return int # of workspaces shutdown
     */
    private int sweep() {
        if (lager.schedLog) {
            logger.trace("sweep()");
        }

        int[] ids = null;
        try {
            ids = this.scheduler.findWorkspacesToShutdown();
        } catch (WorkspaceDatabaseException e) {
            logger.error("",e);
        }

        if ((ids == null) || (ids.length == 0)) {
            if (lager.schedLog) {
                logger.trace("sweep() is done");
            }
            return 0;
        }

        for (int i = 0; i < ids.length; i++) {
            InstanceResource resource;
            try {
                resource = this.home.find(ids[i]);
            } catch (Exception e) {
                logger.error(Lager.id(ids[i]), e);
                try {
                    this.scheduler.stateNotification(ids[i], STATE_DESTROYING);
                } catch (ManageException e2) {
                    logger.error("", e2);
                }
                continue;
            }

            resource.setOpsEnabled(false);
            try {

                // serialize request is invalid right now
                if (resource.getVM().getRequestedShutdownMechanism()
                        == DEFAULT_SHUTDOWN_TRASH) {

                    if (lager.eventLog) {

                        logger.info(Lager.ev(ids[i]) + "Running time has" +
                                " expired, client requested default" +
                                " shutdown mechanism of Trash," +
                                " destroying resource");
                    }

                    this.home.destroy(ids[i]);

                } else if (resource.getState() > STATE_STAGED_OUT) {

                    if (lager.eventLog) {

                        logger.info(Lager.ev(ids[i]) + "Running time has" +
                            " expired, client requested default shutdown" +
                            " mechanism of Normal, but the resource is" +
                            " either being destroyed or has been corrupted," +
                            " nothing to do");
                    }

                } else {

                    if (lager.eventLog) {
                        logger.info(Lager.ev(ids[i]) + "Running time has" +
                            " expired, client requested default shutdown" +
                            " mechanism of Normal, setting target state" +
                            " to StagedOut");
                    }

                    resource.setTargetState(STATE_STAGED_OUT);
                }

                this.scheduler.markShutdown(ids[i]);
                
            } catch (ManageException e) {
                logger.error(e.getMessage(), e);
            } catch (DoesNotExistException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return ids.length;
    }
}
