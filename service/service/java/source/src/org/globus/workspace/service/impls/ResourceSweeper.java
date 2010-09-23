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

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.service.Sweepable;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.Lager;

import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Used to sweep for terminated instances.
 *
 * Functionality will improve and be pushed to scheduler module in the future
 */
public class ResourceSweeper implements Runnable {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(ResourceSweeper.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final ExecutorService executor;
    protected final WorkspaceHome home;
    protected final Lager lager;
    

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public ResourceSweeper(ExecutorService executorService,
                           WorkspaceHome whome,
                           Lager lagerImpl) {

        if (executorService == null) {
            throw new IllegalArgumentException("executorService may not be null");
        }
        this.executor = executorService;

        if (whome == null) {
            throw new IllegalArgumentException("whome may not be null");
        }
        this.home = whome;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;
    }

    
    // -------------------------------------------------------------------------
    // implements Runnable
    // -------------------------------------------------------------------------

    public void run() {
        try {
            this.findAndDestroy();
        } catch (Throwable t) {
            logger.error("Problem sweeping resources: " + t.getMessage(), t);
        }
    }


    // -------------------------------------------------------------------------
    // IMPL
    // -------------------------------------------------------------------------

    protected void findAndDestroy() {

        if (this.lager.pollLog) {
            logger.trace("findAndDestroy - sweeping");
        }
        
        // this doesn't need to be synchronized anymore
        final Sweepable[] toSweep = this.home.currentSweeps();

        if (toSweep == null || toSweep.length == 0) {
            return; // *** EARLY RETURN ***
        }

        // find things to destroy
        final List killList = this.getDestroyTasks(toSweep);

        if (killList.isEmpty()) {
            return; // *** EARLY RETURN ***
        }

        // destroy things
        final Iterator iter = killList.iterator();
        while (iter.hasNext()) {
            final Runnable task = (Runnable) iter.next();
            this.executor.submit(task);
        }

        // Log any unexpected errors.  Wait two minutes (normal destroy time
        // should be a matter of seconds even if there is high congestion).

        // todo: make timeout configurable when this class comes via IoC

        final Iterator iter2 = killList.iterator();
        while (iter2.hasNext()) {
            final FutureTask task = (FutureTask) iter2.next();
            try {
                final String msg = (String) task.get(120L, TimeUnit.SECONDS);
                if (msg != null) {
                    logger.debug(msg);
                }
            } catch (Exception e) {
                final String err = "Error while destroying sweeped " +
                                            "instance: " + e.getMessage();
                if (logger.isDebugEnabled()) {
                    logger.error(err, e);
                } else {
                    logger.error(err);
                }
            }
        }
    }

    protected List getDestroyTasks(Sweepable[] toSweep) {

        final Calendar currentTime = Calendar.getInstance();
        final LinkedList killList = new LinkedList();

        for (int i = 0; i < toSweep.length; i++) {

            final Sweepable sw = toSweep[i];

            if (sw != null) {

                final boolean expired = isExpired(sw.getTerminationTime(),
                                                  currentTime);

                if (expired) {
                    final DestroyFutureTask task =
                            new DestroyFutureTask(sw.getID(), this.home);

                    killList.add(new FutureTask(task));
                }
            }
        }

        return killList;
    }

    public static boolean isExpired(Calendar terminationTime,
                                    Calendar currentTime) {
        return terminationTime != null && terminationTime.before(currentTime);
    }
}
