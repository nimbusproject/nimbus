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

package org.nimbustools.auto_config.groupauthz.harness;

/**
 * A program that instantiates the group authorization module just as the
 * service will (unless someone alters the Spring configurations to do
 * something vastly different).
 *
 * First argument dictates action
 *
 * Second argument is the *absolute* path to Spring file for the 
 * authorization module.
 *
 */
public class Main {

    public static String OPT_FINDHASH = "findhash";
    public static String OPT_HASH = "hash";
    public static String OPT_REPORT = "report";
    public static String OPT_REPORT_ALL = "reportAll";
    public static String OPT_ADD = "add";
    public static String OPT_DEL = "del";

    private static void dispatch(String action,
                                 String path,
                                 String[] otherArgs,
                                 boolean debug) throws Exception {

        if (OPT_REPORT.equalsIgnoreCase(action)) {
            new Report(path, otherArgs, debug).run();
        } else if (OPT_REPORT_ALL.equalsIgnoreCase(action)) {
            new ReportAll(path, otherArgs, debug).run();
        } else if (OPT_HASH.equalsIgnoreCase(action)) {
            new Hash(path, otherArgs, debug).run();
        } else if (OPT_FINDHASH.equalsIgnoreCase(action)) {
            new FindHash(path, otherArgs, debug).run();
        } else if (OPT_ADD.equalsIgnoreCase(action)) {
            new AddDN(path, otherArgs, debug).run();
        } else if (OPT_DEL.equalsIgnoreCase(action)) {
            new DeleteDN(path, otherArgs, debug).run();
        } else {
            throw new Exception("unknown action: '" + action + "'");
        }
    }

    public static void main(String[] args) {

        boolean debug = false;
        final String debugStr = System.getProperty("nimbus.wizard.debug");
        if (debugStr != null && debugStr.trim().equalsIgnoreCase("true")) {
            debug = true;
        }

        if (args == null || args.length < 2) {
            System.err.println("You need to supply at least two arguments:");
            System.err.println("  1 - action name");
            System.err.println("  2 - absolute path to spring bean def for groupauthz");
            System.exit(1);
        }

        final String[] sendargs;
        if (args.length > 2) {
            final int newlen = args.length - 2;
            sendargs = new String[newlen];
            System.arraycopy(args, 2, sendargs, 0, newlen);
        } else {
            sendargs = new String[0];
        }

        try {
            // Spring needs "//" to consider it an absolute path 
            dispatch(args[0], "/" + args[1], sendargs, debug);
        } catch (Exception e) {
            System.err.println("Problem: " + e.getMessage());
            System.exit(1);
        }
    }
}
