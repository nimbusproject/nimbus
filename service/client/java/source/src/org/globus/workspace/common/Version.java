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

package org.globus.workspace.common;

/**
 * Version number is composed as major.minor.patch
 */
public class Version {

    /** The major release number */
    public static final int MAJOR = 2;

    /** The minor release number */
    public static final int MINOR = 2;

    /** The patchlevel of the current release */
    public static final int PATCH = 0;

    /**
     * Returns the current version as string in the form: major.minor.patch
     * @return major.minor.patch
     */
    public static String getVersion() {
        return getMajor() + "." + getMinor() + "." + getPatch();
    }

    /**
     * Returns the major release number.
     *
     * @return the major release
     */
    public static int getMajor() {
        return MAJOR;
    }

    /**
     * Returns the minor release number.
     *
     * @return the minor release number
     */
    public static int getMinor() {
        return MINOR;
    }

    /**
     * Returns the patch level.
     *
     * @return the patch level
     */
    public static int getPatch() {
        return PATCH;
    }

    /**
     * Prints just the version: major.minor.patch
     *
     * @param args args
     */
    public static void main(String [] args) {
        System.out.println(getVersion());
    }
    
}
