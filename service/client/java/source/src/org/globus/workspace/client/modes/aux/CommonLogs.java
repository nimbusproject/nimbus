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

package org.globus.workspace.client.modes.aux;

import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.repr.Networking;
import org.globus.workspace.client_core.repr.Nic;
import org.apache.commons.logging.Log;

public class CommonLogs {

    public static void logBoolean(boolean val,
                                  String item,
                                  Print print,
                                  Log logger) {
        
        genericArgCheck(print, logger);

        if (print.enabled()) {
            final String dbg;
            if (val) {
                dbg = item + ": ENABLED";
            } else {
                dbg = item + ": DISABLED";
            }
            if (print.useThis()) {
                print.dbg(dbg);
            } else if (print.useLogging()) {
                logger.debug(dbg);
            }
        }
    }

    public static void printNetwork(Workspace workspace,
                                    Print print,
                                    Log logger) {
        if (workspace == null) {
            throw new IllegalArgumentException("workspace may not be null");
        }

        genericArgCheck(print, logger);

        if (!print.enabled()) {
            return; // *** EARLY RETURN ***
        }

        final Networking net = workspace.getCurrentNetworking();

        if (net == null || net.nicNum() < 1) {
            return; // *** EARLY RETURN ***
        }

        final Nic[] nics = net.nics();
        for (int i = 0; i < nics.length; i++) {
            printNic(nics[i], print, logger);
        }
    }

    public static void printNic(Nic nic,
                                Print print,
                                Log logger) {

        if (nic == null) {
            throw new IllegalArgumentException("nic may not be null");
        }

        genericArgCheck(print, logger);

        if (!print.enabled()) {
            return; // *** EARLY RETURN ***
        }

        final String prSep = "  - ";
        final String logSep = " ";

        final String name = "NIC: " + nic.getName();

        if (print.useThis()) {
            print.infoln();
            print.infoln(PrCodes.INSTANCERPQUERY__NETWORK_NICNAME,
                         name);
        }

        final String assoc = "Association: " + nic.getAssociation();
        if (print.useThis()) {
            print.infoln(PrCodes.INSTANCERPQUERY__NETWORK_ASSOCIATION,
                         prSep + assoc);
        }

        final String ip = "IP: " + nic.getIpAddress();
        if (print.useThis()) {
            print.infoln(PrCodes.INSTANCERPQUERY__NETWORK_IP,
                         prSep + ip);
        }

        final String host = "Hostname: " + nic.getHostname();
        if (print.useThis()) {
            print.infoln(PrCodes.INSTANCERPQUERY__NETWORK_HOSTNAME,
                         prSep + host);
        }

        final String gateway = "Gateway: " + nic.getGateway();
        if (print.useThis()) {
            print.infoln(PrCodes.INSTANCERPQUERY__NETWORK_GATEWAY,
                         prSep + gateway);
        }

        if (print.useLogging()) {
            logger.info(name + logSep + assoc + logSep +
                        ip + logSep + host + logSep + gateway);
        }
        

    }

    private static void genericArgCheck(Print print,
                                        Log logger) {
        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }

        if (logger == null) {
            throw new IllegalArgumentException("logger may not be null");
        }
    }
}
