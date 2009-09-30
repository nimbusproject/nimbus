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

package org.globus.workspace.client_core.subscribe_tools.internal;

/**
 * EPR (over)simplification for comparisons.
 * EndpointReferenceType has default equals which is no good for key object
 */
class AddressIDPair {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final Integer id;
    private final String address;

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @param workspaceID id
     * @param serviceAddress may not be null
     */
    AddressIDPair(int workspaceID, String serviceAddress) {
        this.id = new Integer(workspaceID);
        if (serviceAddress == null) {
            throw new IllegalArgumentException(
                                "serviceAddress may not be null");
        }
        this.address = serviceAddress;
    }

    
    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    /**
     * @return id, never null
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * @return address, never null
     */
    public String getAddress() {
        return this.address;
    }

    
    // -------------------------------------------------------------------------
    // overrides Object
    // -------------------------------------------------------------------------

    public boolean equals(Object o) {
        
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AddressIDPair that = (AddressIDPair) o;

        if (!this.address.equals(that.getAddress())) {
            return false;
        }

        return this.id.equals(that.getId());
    }

    public int hashCode() {
        int result;
        result = this.id.hashCode();
        result = 31 * result + this.address.hashCode();
        return result;
    }


    public String toString() {
        return "AddressIDPair{" +
                "id=" + this.id +
                ", address='" + this.address + '\'' +
                '}';
    }
}
