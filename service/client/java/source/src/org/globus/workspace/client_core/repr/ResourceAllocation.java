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

package org.globus.workspace.client_core.repr;

import org.nimbustools.messaging.gt4_0.generated.negotiable.ResourceAllocation_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.Storage_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.Entry;
import org.ggf.jsdl.RangeValue_Type;

import java.util.ArrayList;

/**
 * Like the current service itself, this does not cover bandwidth request yet.
 */
public class ResourceAllocation {

    private static final DiskSpace[] NO_DISK_SPACE = new DiskSpace[0];

    private int memoryMB;
    private DiskSpace[] diskSpaceRequests = NO_DISK_SPACE;

    public ResourceAllocation() {
        // empty
    }

    public ResourceAllocation(ResourceAllocation_Type xmlRA) {

        if (xmlRA == null) {
            throw new IllegalArgumentException("xmlRA may not be null");
        }

        final RangeValue_Type rvt = xmlRA.getIndividualPhysicalMemory();
        if (rvt == null) {
            throw new IllegalArgumentException(
                    "memory specification is not present");
        }
        final GenericIntRange exact = new GenericIntRange(rvt);
        if (exact.getMin() != exact.getMax()) {
            throw new IllegalArgumentException(
                    "memory range requests aren't supported right now");
        }
        this.memoryMB = exact.getMax();

        final Storage_Type storage = xmlRA.getStorage();
        if (storage != null) {
            final Entry[] entries = storage.getEntry();
            if (entries != null) {
                final ArrayList dsList = new ArrayList(entries.length);
                for (int i = 0; i < entries.length; i++) {
                    final DiskSpace ds = getDiskSpace(entries[i]);
                    if (ds != null) {
                        dsList.add(ds);
                    }
                }
                this.diskSpaceRequests = (DiskSpace[])
                        dsList.toArray(new DiskSpace[dsList.size()]);
            }
        }

    }

    private static DiskSpace getDiskSpace(Entry entry) {
        if (entry == null) {
            return null;
        }

        final RangeValue_Type rvt = entry.getIndividualDiskSpace();
        if (rvt != null) {
            final GenericIntRange range = new GenericIntRange(rvt);

            if (range.getMin() != range.getMax()) {
                throw new IllegalArgumentException(
                    "disk space range requests aren't supported right now");
            }

            return new DiskSpace(range.getMax(), entry.getPartitionName());
        }
        
        return null;
    }

    public int getMemoryMB() {
        return this.memoryMB;
    }

    public void setMemoryMB(int megabytes) {
        this.memoryMB = megabytes;
    }

    /**
     * @return the requests, never null but array length may be zero
     */
    public DiskSpace[] getDiskSpaceRequests() {
        return this.diskSpaceRequests;
    }

    /**
     * @param requests if null, will set internal field to zero length array
     */
    public void setDiskSpaceRequests(DiskSpace[] requests) {
        if (requests == null) {
            this.diskSpaceRequests = NO_DISK_SPACE;
        } else {
            this.diskSpaceRequests = requests;
        }
    }
}
