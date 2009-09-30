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

package org.globus.workspace.service.impls.async;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RequestDispatch {

    private static final Log logger =
                        LogFactory.getLog(RequestDispatch.class.getName());

    protected static WorkspaceRequestQueue queue;
    private static Options opts;
    private static WorkspaceThreadPool threadPool;
    private static Semaphore semaphore = new Semaphore();

    static class Options {
        int numThreads;
        final int maxThreads;
        final int highWaterMark;
        boolean stopped;
        int addedThreads;

        Options(int numThreads, int maxThreads, int highWaterMark) {
            this.numThreads = numThreads;
            this.maxThreads = maxThreads;
            this.highWaterMark = highWaterMark;
        }
    }

    public synchronized static void setOptions(int numThreads,
                                               int maxThreads,
                                               int highWaterMark) {
        
        logger.debug("RequestDispatch options: numThreads = " +
                     numThreads + ", maxThreads = " + maxThreads +
                     ", highWaterMark = " + highWaterMark);
        if (opts != null) {
            logger.warn("Attempt to call setOptions more than once?  " +
                    "RequestDispatch is JVM or classloader wide.");
        } else {
            opts = new Options(numThreads, maxThreads, highWaterMark);
        }
    }

    private static void initialize() {
        logger.debug("intializing RequestDispatch");
        queue = new WorkspaceRequestQueue();
        if (opts == null) {
            logger.warn("Options were not initialized already (?).");
            opts = new Options(5,50,20);
        }
        threadPool = new WorkspaceThreadPool(queue);
    }

    /**
     * @param req request
     * @param id workspid
     */
    public synchronized static void addRequest(WorkspaceRequest req, int id) {
        // the first request activates this
        if (queue == null) {
            initialize();
            start();
        }

        if (isStopped()) {
            logger.error("cannot add request to queue, " +
                    "RequestDispatch is disabled");
            return;
        }

        int waitingThreads = queue.enqueue(req);

        if (waitingThreads == 0 && threadPool.getThreads() < opts.maxThreads) {
            opts.addedThreads += addThread();
        } else if (waitingThreads > opts.highWaterMark) {
            if (opts.addedThreads > 0) {
                removeThread();
                opts.addedThreads --;
            }
        }
    }

    public synchronized static void start() {
        logger.debug("starting up request handler with "
                                            + opts.numThreads + " threads");

        threadPool.startThreads(opts.numThreads);
        semaphore.sendSignal();
    }

    public void waitForInit() throws InterruptedException {
        semaphore.waitForSignal();
    }

    public void waitForStop() throws InterruptedException {
        threadPool.waitForThreads();
    }

    private static synchronized int addThread() {
        if (opts == null || opts.stopped) {
            return 0;
        } else {
            threadPool.startThreads(1);
            return 1;
        }
    }

    private static void removeThread() {
        threadPool.stopThreads(1);
    }

    public synchronized static void stop() {
        if (opts == null || opts.stopped) {
            return;
        }

        logger.debug("Disabling dispatcher");

        opts.stopped = true;

        if (threadPool != null) {
            // request threads to stop
            threadPool.stopThreads();
            // wait util they actually stop or 2 min time out
            try {
                threadPool.waitForThreads(1000 * 60 * 2);
            } catch (InterruptedException e) {
                // we can ignore it
            }
        }

        logger.debug("Disabled dispatcher");
    }

    public synchronized static boolean isStopped() {
        if (opts == null) {
            return true;
        }
        return opts.stopped;
    }

}
