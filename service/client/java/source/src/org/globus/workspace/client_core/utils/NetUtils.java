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

package org.globus.workspace.client_core.utils;

import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.repr.Networking;
import org.globus.workspace.client_core.repr.Nic;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NetUtils {

    private static final Log logger =
            LogFactory.getLog(NetUtils.class.getName());

    public static String oneLineNetString(Workspace workspace) {
        if (workspace == null) {
            return null; // *** EARLY RETURN ***
        }
        final Networking networking = workspace.getCurrentNetworking();
        String netStr = "[no networking]";
        if (networking != null) {
            // assuming IP presence at least
            final Nic[] nics = networking.nics();
            if (nics.length > 1) {
                netStr = networking.nicNum() + " NICs: ";
                netStr += nics[0].getIpAddress();
                final String host = nics[0].getHostname();
                if (host != null) {
                    netStr += " ['" + host + "']";
                }
                for (int i = 1; i < nics.length; i++) {
                    netStr += ", " + nics[i].getIpAddress();
                    final String ahost = nics[i].getHostname();
                    if (ahost != null) {
                        netStr += " ['" + ahost + "']";
                    }
                }
            } else if (nics.length == 1) {
                netStr = nics[0].getIpAddress();
                final String host = nics[0].getHostname();
                if (host != null) {
                    netStr += " [ " + host + " ]";
                }
            }
        }
        return netStr;
    }

    public static void instanceCreateResultNetPrint(Print pr,
                                                    Workspace workspace) {

        if (pr == null) {
            throw new IllegalArgumentException("print may not be null");
        }

        if (!pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        if (workspace == null) {
            throw new IllegalArgumentException("workspace may not be null");
        }

        final String logID = workspace.getDisplayName();

        final Networking networking = workspace.getCurrentNetworking();
        if (networking == null) {

            final String msg = "No network";
            if (pr.useThis()) {
                pr.infoln(PrCodes.CREATE__INSTANCE_CREATING_NET_NONE, msg);
            } else if (pr.useLogging()) {
                logger.info(logID + ": " + msg);
            }

            return; // *** EARLY RETURN ***
        }

        final String logSep = " ; ";
        final boolean logit = pr.useLogging();
        
        final Nic[] nics = networking.nics();
        for (int i = 0; i < nics.length; i++) {
            
            final StringBuffer loggerBuf;
            if (logit) {
                loggerBuf = new StringBuffer(logID);
                loggerBuf.append(logSep);
            } else {
                loggerBuf = null;
            }

            final String name = _instNicName(pr, nics[i]);
            if (logit && name != null) {
                loggerBuf.append(name).append(logSep);
            }
            final String assoc = _instNicAssociation(pr, nics[i]);
            if (logit && assoc != null) {
                loggerBuf.append(assoc).append(logSep);
            }
            final String ip = _instNicIP(pr, nics[i]);
            if (logit && ip != null) {
                loggerBuf.append(ip).append(logSep);
            }
            final String host = _instNicHostname(pr, nics[i]);
            if (logit && host != null) {
                loggerBuf.append(host).append(logSep);
            }
            final String gwy = _instNicGateway(pr, nics[i]);
            if (logit && gwy != null) {
                loggerBuf.append(gwy).append(logSep);
            }

            if (logit) {
                logger.info(loggerBuf.toString());
            }
        }
    }

    private static String _instNicName(Print pr, Nic nic) {
        final String msg = nic.getName();
        if (msg != null) {
            if (pr.useThis()) {
                pr.infoln(PrCodes.CREATE__INSTANCE_CREATING_NET_NAME, msg);
            }
        }
        return msg;
    }
    
    private static String _instNicAssociation(Print pr, Nic nic) {
        final String assoc = nic.getAssociation();
        if (assoc != null) {
            final String msg = "Association: " + assoc;
            if (pr.useThis()) {
                pr.infoln(PrCodes.CREATE__INSTANCE_CREATING_NET_ASSOCIATION,
                          "      " + msg);
            }
            return msg;
        }
        return null;
    }

    private static String _instNicIP(Print pr, Nic nic) {
        final String ip = nic.getIpAddress();
        if (ip != null) {
            final String msg = "IP address: " + ip;
            pr.infoln(PrCodes.CREATE__INSTANCE_CREATING_NET_IP,
                      "       " + msg);
            return msg;
        }
        return null;
    }

    private static String _instNicHostname(Print pr, Nic nic) {
        final String host = nic.getHostname();
        if (host != null) {
            final String msg = "Hostname: " + host;
            pr.infoln(PrCodes.CREATE__INSTANCE_CREATING_NET_HOST,
                      "         " + msg);
            return msg;
        }
        return null;
    }

    private static String _instNicGateway(Print pr, Nic nic) {
        final String gwy = nic.getGateway();
        if (gwy != null) {
            final String msg = "Gateway: " + gwy;
            pr.infoln(PrCodes.CREATE__INSTANCE_CREATING_NET_GATEWAY,
                      "          " + msg);
            return msg;
        }
        return null;
    }
}
