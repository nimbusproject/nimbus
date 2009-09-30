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

package org.globus.workspace.scheduler;

import java.util.Calendar;

public class Reservation {

    private final int[] ids;
    private final String[] nodes;
    private final int[] durationList;
    private Calendar startTime = null;
    private Calendar stopTime = null;

    public Reservation(int[] workspaceIDs) {
        this(workspaceIDs, null, null);
    }

    public Reservation(int[] workspaceIDs,
                       String[] hostnames) {
        this(workspaceIDs, hostnames, null);
    }

    public Reservation(int[] workspaceIDs,
                       String[] hostnames,
                       int[] durations) {

        if (workspaceIDs == null || workspaceIDs.length == 0) {
            throw new IllegalArgumentException(
                        "workspaceIDs is null or length==0");
        }

        this.ids = workspaceIDs;

        if (hostnames == null) {
            this.nodes = null;
        } else {

            if (workspaceIDs.length != hostnames.length) {
                throw new IllegalArgumentException(
                        "workspaceIDs.length != hostnames.length");
            }

            for (int i = 0; i < workspaceIDs.length; i++) {
                
                if (workspaceIDs[i] < 1) {
                    throw new IllegalArgumentException(
                            "workspaceIDs[" + i + "] less than one");
                }

                // mix of best-effort and non-best effort is NOT supported,
                // have your slot manager act in best effort mode and report
                // the decided nodes semi-immediately (scheduler can handle
                // a race on those, see DefaultSchedulerAdapter#fetchResource()
                if (hostnames[i] == null ||
                        hostnames[i].trim().length() == 0) {
                    throw new IllegalArgumentException(
                            "hostnames[" + i + "] is null or empty");
                }
            }

            this.nodes = hostnames;
            
        }

        if (durations == null) {
            this.durationList = null;
        } else {
            if (workspaceIDs.length != durations.length) {
                throw new IllegalArgumentException(
                        "workspaceIDs.length != durations.length");
            }
            this.durationList = durations;
        }
    }

    public int[] getIds() {
        return this.ids;
    }

    public int getResponseLength() {
        if (this.nodes == null) {
            return 0;
        }
        return this.nodes.length;
    }

    /**
     * Concrete currently means:
     *    - if true, getIdHostnamePair will return at least one tuple
     *    - if true, getStartTime and getStopTime will be populated
     *
     * @return true if "concrete" (see definition)
     */
    public boolean isConcrete() {
        return this.startTime != null &&
               this.stopTime != null &&
               this.nodes != null &&
               this.nodes.length > 0;
    }

    /**
     * @param index must be >= 0, < getResponseLength()
     * @return IdHostnameTuple or null if there are none
     * @throws IndexOutOfBoundsException
     *         if getResponseLength() == 0
     *         or if index < 0, >= getResponseLength()
     */
    public IdHostnameTuple getIdHostnamePair(int index) {

        if (this.ids == null || this.nodes == null) {
            throw new IndexOutOfBoundsException("response length is zero");
        }

        if (index < 0 || index >= this.ids.length) {
            throw new IndexOutOfBoundsException();
        }
        // ...
        return new IdHostnameTuple(this.ids[index],
                                   this.nodes[index]);
    }

    public Calendar getStartTime() {
        return this.startTime;
    }

    public void setStartTime(Calendar startTime) {
        this.startTime = startTime;
    }

    public Calendar getStopTime() {
        return this.stopTime;
    }

    public void setStopTime(Calendar stopTime) {
        this.stopTime = stopTime;
    }



    public boolean hasDurationList() {
        return this.durationList != null;
    }

    /**
     * TODO: this should all be generalized, this is now a dual-mode object
     * and that is not good.
     * 
     * @param index must be >= 0, < getResponseLength()
     * @return duration
     * @throws IndexOutOfBoundsException
     *         if getResponseLength() == 0
     *         or if index < 0, >= getResponseLength()
     */
    public int getDurationByIndex(int index) {

        if (!this.hasDurationList()) {
            throw new IndexOutOfBoundsException("there are no durations");
        }

        if (index < 0 || index >= this.durationList.length) {
            throw new IndexOutOfBoundsException();
        }

        return this.durationList[index];
    }
}
