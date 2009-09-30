/*
 * Copyright 1999-2007 University of Chicago
 *
 * Licensed under the Apache License, MajorMinorVersion 2.0 (the "License"); you may not
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

package org.globus;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class MajorMinorVersion {

    /**
     * Finds GT Java core major.minor.  Any WSRF install without the
     * org.globus.wsrf.utils.Version class is considered to be 4.0
     *
     * @return major.minor version string
     */
    public static String getMajorMinor() throws Exception {
        Object[] args = new String[0];
        try {
            Class Version = Class.forName("org.globus.wsrf.utils.Version");
            Method[] methods = Version.getMethods();
            Object major = null;
            Object minor = null;
            try {
                for (int i = 0; i < methods.length; i++) {
                    if (methods[i].getName().equals("getMajor")) {
                        major = methods[i].invoke(null, args);
                    }
                    if (methods[i].getName().equals("getMinor")) {
                        minor = methods[i].invoke(null, args);
                    }
                }
            } catch (IllegalAccessException e) {
                throw e;
            } catch (InvocationTargetException e) {
                throw e;
            }

            if ((major == null) || (minor == null)) {
                throw new Exception("some reflection problem? " +
                        "major or minor null");
            }

            return major + "." + minor;

        } catch (ClassNotFoundException e) {
            return "4.0";
        }
    }

    /**
     * Outputs major.minor to stdout.  Any WSRF install without the
     * org.globus.wsrf.utils.Version class is considered to be 4.0
     * @param args
     */
    public static void main(String[] args) {
       try {
           System.out.println(getMajorMinor());
       } catch (Exception e) {
           e.printStackTrace();
           System.exit(-1);
       }
   }
}
