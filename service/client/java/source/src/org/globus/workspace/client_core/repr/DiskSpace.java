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

/**
 * Ranges not supported yet.
 */
public class DiskSpace {

    private final int space;
    private final String partition;

    /**
     * Range requests not supported yet
     *
     * TODO: When ranges are supported, provide a GenericIntRange based constructor
     * 
     * @param diskSpace megabytes in request
     * @param partitionName name, may not be null
     */
    public DiskSpace(int diskSpace, String partitionName) {
        this.space = diskSpace;
        if (partitionName == null) {
            throw new IllegalArgumentException("partitionName may not be null");
        }
        this.partition = partitionName;
    }

    public int getDiskSpaceMegabytes() {
        return this.space;
    }

    public String getPartitionName() {
        return this.partition;
    }
}
