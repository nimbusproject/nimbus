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

// TODO: move to executor
public class WorkspaceThread extends Thread {

    private static final Log logger =
                    LogFactory.getLog(WorkspaceThread.class.getName());

    private final WorkspaceRequestQueue queue;
    private final WorkspaceThreadPool threadPool;

    public WorkspaceThread(WorkspaceRequestQueue queue,
                           WorkspaceThreadPool pool) {
        this.queue = queue;
        this.threadPool = pool;
    }

    public void run() {
        while (true) {
            try {
                WorkspaceRequest request = this.queue.dequeue();
                if (request == null) {
                    this.threadPool.removeThread(this);
                    break;
                }

                try {
                    request.execute();
                } catch (Throwable t) {
                    logger.error("runtime exception from task " +
                                 "implementation: " + t.getMessage(), t);
                }

            } catch (Throwable e) {
                logger.error("runtime exception from async task " +
                        "infrastructure", e);
            }
        }
    }
}
