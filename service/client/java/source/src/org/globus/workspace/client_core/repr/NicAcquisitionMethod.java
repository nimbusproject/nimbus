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
 * todo: use an enumeration impl
 */
public class NicAcquisitionMethod {

    public static final String AcceptAndConfigure = "AcceptAndConfigure";
    public static final String Advisory = "Advisory";
    public static final String AllocateAndConfigure = "AllocateAndConfigure";

    public static final String[] VALID_METHODS =
                    { AcceptAndConfigure, Advisory, AllocateAndConfigure };

    private final String method;

    public NicAcquisitionMethod(String acquisitionMethod) {

        if (acquisitionMethod == null) {
            throw new IllegalArgumentException("acquisitionMethod is null");
        }

        if (!testValidMethod(acquisitionMethod)) {
            throw new IllegalArgumentException(
                            "acquisitionMethod is not a valid state string");
        }

        this.method = acquisitionMethod;
    }

    public String getMethod() {
        return method;
    }

    public static boolean testValidMethod(String input) {

        // Not using VALID_METHODS, so we can use this method from constructor
        // (where VALID_METHODS is not initialized yet in the first run).

        final String[] valid =
                    { AcceptAndConfigure, Advisory, AllocateAndConfigure };

        for (int i = 0; i < valid.length; i++) {
            if (valid[i].equals(input)) {
                return true;
            }
        }
        return false;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NicAcquisitionMethod that = (NicAcquisitionMethod) o;

        if (this.method != null ? !this.method.equals(that.method) : that.method != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return (this.method != null ? this.method.hashCode() : 0);
    }
}
