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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    // instance ID : attempt count
    protected final Map<Integer,Integer> zombieBackoffs = new Hashtable<Integer,Integer>();
    

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

        // Log any unexpected errors.  Wait 30s (normal destroy time
        // should be a matter of seconds even if there is high congestion).

        // todo: make timeout configurable when this class comes via IoC

        final Iterator iter2 = killList.iterator();
        while (iter2.hasNext()) {
            final FutureTask task = (FutureTask) iter2.next();
            try {
                final String msg = (String) task.get(30L, TimeUnit.SECONDS);
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

    protected synchronized List getDestroyTasks(Sweepable[] toSweep) {

        final Calendar currentTime = Calendar.getInstance();
        final LinkedList killList = new LinkedList();

        for (int i = 0; i < toSweep.length; i++) {

            final Sweepable sw = toSweep[i];

            if (sw != null) {

                final boolean expired = isExpired(sw.getTerminationTime(),
                                                  currentTime);

                if (expired) {
                    logger.debug("Sweep found that " + Lager.id(sw.getID()) + " is expired.");
                }

                if (sw.isZombie()) {
                    
                    // Only attempt on the following attempt # of sweeper runs:
                    // 1st, 2nd, 3rd, 6th, 10th, 15th, 25th, and then on modulo 20's
                    
                    final Integer exists = this.zombieBackoffs.get(sw.getID());

                    final Integer attemptCount;
                    if (exists == null) {
                        attemptCount = 1;
                    } else {
                        attemptCount = exists + 1;
                    }
                    this.zombieBackoffs.put(sw.getID(), attemptCount);

                    int actualRetryNumber = attemptCount;
                    if (attemptCount < 40) {
                        switch (attemptCount) {
                            case 1:
                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                            case 6:
                                actualRetryNumber = 4;
                                break;
                            case 10:
                                actualRetryNumber = 5;
                                break;
                            case 15:
                                actualRetryNumber = 6;
                                break;
                            case 25:
                                actualRetryNumber = 7;
                                break;
                            default:
                                continue;
                        }
                    } else {
                        if (attemptCount % 20 != 0) {
                            continue;
                        } else {
                            actualRetryNumber = 6 + attemptCount / 20;
                        }
                    }

                    logger.warn(Lager.ev(sw.getID()) + "Node that could not be destroyed " +
                                "previously, attempting again.  Retry #" + actualRetryNumber);
                }

                if (expired || sw.isZombie()) {
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
