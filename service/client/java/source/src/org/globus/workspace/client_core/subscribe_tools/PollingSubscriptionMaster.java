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

package org.globus.workspace.client_core.subscribe_tools;

public interface PollingSubscriptionMaster extends SubscriptionMaster {

    public static final int DEFAULT_MAX_POOL_SIZE = 128;

    /**
     * @param maxThreads maxThreads running at any one time, must be > 0
     */
    public void setMaxThreads(int maxThreads);

    /**
     * Forcibly stop running all poll tasks (those in progress may be past the
     * point of cancellation).
     */
    public void stopPolling();


    /**
     * @return milliseconds between polls for each task
     */
    public long getPollingMs();

}
