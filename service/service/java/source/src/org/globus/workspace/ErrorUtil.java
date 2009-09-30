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

package org.globus.workspace;

public class ErrorUtil {

    public static String excString(Throwable t) {
        if (t == null) {
            return "null";
        }

        final String msg = recurseForSomeString(t);
        if (msg == null) {
            return "[no problem cause message found, error type: '" +
                            t.getClass().toString() + "']";
        } else {
            return "'" + msg + "'";
        }
    }

    public static String recurseForSomeString(Throwable thr) {
        Throwable t = thr;
        while (true) {
            if (t == null) {
                return null;
            }
            final String msg = t.getMessage();
            if (msg != null) {
                return msg;
            }
            t = t.getCause();
        }
    }
}
