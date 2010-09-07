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

package org.globus.workspace.common.client;

import org.globus.workspace.common.print.Print;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class CLIUtils {

    // not drawing on Opts strings to maintain autonomy (with the risks that
    // duplication brings), also the debug strings are not public in core's
    // BaseClient (which is on its way out of here at any rate...)

    public static final String SHORT_DEBUG_SEARCH_STRING = "-d";

    public static final String LONG_DEBUG_SEARCH_STRING = "--debug";

    public static final String DEBUGGER_HANG_SEARCH_STRING = "--debuggerhang";

    public static boolean containsDebug(String[] args) {
        if (args == null) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            if (SHORT_DEBUG_SEARCH_STRING.equals(args[i])) {
                return true;
            }
            if (LONG_DEBUG_SEARCH_STRING.equals(args[i])) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsDebuggerHang(String[] args) {
        if (args == null) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            if (DEBUGGER_HANG_SEARCH_STRING.equals(args[i])) {
                return true;
            }
        }
        return false;
    }

    // (for development only, to attach a remote debugger etc)
    public static void hangForInput(Print pr) throws IOException {

        if (pr != null && pr.enabled()) {
            pr.info("Hit a key to proceed...");
            pr.flush();
        }
        
        BufferedReader br = null;
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(System.in);
            br = new BufferedReader(isr);
            br.readLine();
        } finally {
            if (isr != null) {
                isr.close();
            }
            if (br != null) {
                br.close();
            }
        }
    }
}
