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

package org.globus.workspace.scheduler.defaults;

import java.util.BitSet;

/**
 * With certain slot managers, there can be a situation where space is
 * reserved so quickly that an asynchronous notification is consumed in
 * a thread before the workspace resources have been instatiated in the
 * service leading to a race condition when the slot manager looks up the
 * workspace ID it is hearing about.  This provides a mechanism to avoid
 * this whilst keeping the workspace DB-based lookup authoritative.
 *
 * Should be able to get rid of this with reorganization.
 */
public class CreationPending {

    private final BitSet bitSet = new BitSet(1024);

    /**
     * @param id id to set pending
     * @exception IndexOutOfBoundsException
     */
    public synchronized void pending(int id) {
        this.bitSet.set(id);
    }

    /**
     * @param ids ids to set pending
     * @exception IndexOutOfBoundsException
     */
    public synchronized void pending(int[] ids) {
        for (int i = 0; i < ids.length; i++) {
            this.bitSet.set(ids[i]);
        }
    }

    /**
     * @param id id to set not-pending
     * @exception IndexOutOfBoundsException
     */
    public synchronized void notpending(int id) {
        this.bitSet.clear(id);
    }

    /**
     * @param ids ids to set not-pending
     * @exception IndexOutOfBoundsException
     */
    public synchronized void notpending(int[] ids) {
        for (int i = 0; i < ids.length; i++) {
            this.bitSet.clear(ids[i]);
        }
    }

    /**
     * @param id id of interest
     * @exception IndexOutOfBoundsException
     * @return true if id is pending
     */
    public synchronized boolean isPending(int id) {
        return this.bitSet.get(id);
    }
}
