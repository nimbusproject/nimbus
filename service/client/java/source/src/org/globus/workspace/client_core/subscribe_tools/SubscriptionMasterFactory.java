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

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.subscribe_tools.internal.ListeningSubscriptionMasterImpl;
import org.globus.workspace.client_core.subscribe_tools.internal.PollingSubscriptionMasterImpl;
import org.globus.workspace.client_core.StubConfigurator;

public class SubscriptionMasterFactory {

    /**
     * Optionally pass in an ExecutorService instance for the callback tasks.
     *
     * @param executorService may be null
     * @param print may not be null
     * @return ListeningSubscriptionMaster
     * @see SubscriptionMaster
     * @see ListeningSubscriptionMaster
     */
    public static ListeningSubscriptionMaster newListeningMaster(
                                                ExecutorService executorService,
                                                Print print) {
        return new ListeningSubscriptionMasterImpl(executorService, print);
    }

    /**
     * Use all defaults
     *
     * @param print may not be null
     * @param stubConfigurator may not be null
     * @return PollingSubscriptionMaster
     * @see SubscriptionMaster
     * @see PollingSubscriptionMaster
     */
    public static PollingSubscriptionMaster newPollingMaster(
                                            StubConfigurator stubConfigurator,
                                            Print print) {

        return new PollingSubscriptionMasterImpl(stubConfigurator, print);
    }


    /**
     * Use some defaults
     *
     * (you can still pass in null for ExecutorService to get default
     *  ExecutorService for the callback tasks)
     *
     * @param pollDelayMs must be at least 1ms (recommend something far longer
     *                    than that, like > 1 second)
     * @param stubConfigurator may not be null
     * @param executorService may be null
     * @param print may not be null
     * @return PollingSubscriptionMaster
     * @see SubscriptionMaster
     * @see PollingSubscriptionMaster
     */
    public static PollingSubscriptionMaster newPollingMaster(
                                              long pollDelayMs,
                                              StubConfigurator stubConfigurator,
                                              ExecutorService executorService,
                                              Print print) {

        return new PollingSubscriptionMasterImpl(pollDelayMs,
                                                 stubConfigurator,
                                                 executorService,
                                                 print);
    }

    /**
     * Use some defaults
     *
     * (you can still pass in null for ExecutorService to get default
     *  ExecutorService for the callback tasks)
     *
     * @param pollDelayMs must be at least 1ms (recommend something far longer
     *                    than that, like > 1 second)
     * @param maxThreads maxThreads running at any one time, must be >0
     * @param stubConfigurator may not be null
     * @param executorService may be null
     * @param print may not be null
     * @return PollingSubscriptionMaster
     * @see SubscriptionMaster
     * @see PollingSubscriptionMaster
     */
    public static PollingSubscriptionMaster newPollingMaster(
                                              long pollDelayMs,
                                              int maxThreads,
                                              StubConfigurator stubConfigurator,
                                              ExecutorService executorService,
                                              Print print) {
        
        return new PollingSubscriptionMasterImpl(pollDelayMs,
                                                 maxThreads,
                                                 stubConfigurator,
                                                 executorService,
                                                 print);
    }
}
