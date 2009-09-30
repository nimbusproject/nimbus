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

import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.wsrf.encoding.SerializationException;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Hashtable;
import java.io.IOException;

/**
 * WS Notification listeners need a way to map EPRs to Workspace object(s).
 * EndpointReferenceType has default equals which is no good for key object.
 *
 * Using equals/hashCode on Workspace object with a hashtable etc. would
 * restrict equality definition of Workspace to only EPR.
 *
 * So a simple map is used that uses the pair "EPR service + resource key"
 * to test equality.
 *
 * All methods are synchronized.
 */
class WorkspaceMap {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(WorkspaceMap.class.getName());

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    // key: AddressIDPair
    // value: WorkspaceAndListeners
    private final Hashtable table;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------
    
    WorkspaceMap() {
        this.table = new Hashtable(64);
    }

    
    // -------------------------------------------------------------------------
    // GET/ADD
    // -------------------------------------------------------------------------

    /**
     * @param container may not be null, EPR inside workspace may not be null,
     *                  EPR must be instance EPR & contain service address
     * @param print may not be null
     * @return true if NEW addition, false if REPLACED an old Workspace object
     */
    boolean addWorkspace(WorkspaceAndListeners container,
                         Print print) {

        final AddressIDPair addrID = intake(container, print);

        final WorkspaceAndListeners old =
                (WorkspaceAndListeners) this.table.put(addrID, container);

        if (old != null) {
            if (print.enabled()) {
                final String dbg =
                        "workspace object replaced in epr-->workspace map." +
                        "\nOLD: " + old.getWorkspace().toString() + "\nNEW: " +
                        container.getWorkspace().toString();
                if (print.useThis()) {
                    print.dbg(dbg);
                } else if (print.useLogging()) {
                    logger.debug(dbg);
                }
            }
        }

        return old == null;
    }

    /**
     * @param container may not be null, EPR inside may not be null,
     *                  EPR must be instance EPR & contain service address
     * @param print may not be null
     * @return true if removed, false if it wasn't there
     */
     boolean removeWorkspace(WorkspaceAndListeners container,
                             Print print) {
        
        final AddressIDPair addrID = intake(container, print);

        final Object old = this.table.remove(addrID);

        return old != null;
    }

    /**
     * @param epr if not null, expected to be instance EPR
     * @param print may not be null
     * @return stored WorkspaceAndListeners, null if there is no mapping (this
     *         includes no mappings as a result of null or invalid EPR input)
     */
    synchronized WorkspaceAndListeners getWorkspace(EndpointReferenceType epr,
                                                    Print print) {

        if (epr == null) {
            return null;
        }

        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        
        final AddressIDPair addrID = chooseAddrID(epr, print);

        if (addrID == null) {
            return null;
        }

        final Object o = this.table.get(addrID);
        return (WorkspaceAndListeners)o;
    }

    private static AddressIDPair intake(WorkspaceAndListeners container,
                                        Print print) {

        if (container == null) {
            throw new IllegalArgumentException("container may not be null");
        }

        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }

        final Workspace workspace = container.getWorkspace();
        
        if (workspace.getEpr() == null) {
            throw new IllegalArgumentException("workspace EPR may not be null");
        }

        final AddressIDPair addrID = chooseAddrID(workspace.getEpr(), print);

        if (addrID == null) {
            throw new IllegalArgumentException("invalid workspace EPR");
        }

        return addrID;
    }

    static AddressIDPair chooseAddrID(EndpointReferenceType producer,
                                      Print pr) {

        if (producer == null) {
            if (pr != null && pr.enabled()) {
                final String err =
                    "No EPR, cannot use for workspace object lookup";
                if (pr.useThis()) {
                    pr.errln(PrCodes.WSLISTEN__ERRORS, err);
                } else if (pr.useLogging()) {
                    logger.error(err);
                }
            }

            return null; // *** EARLY RETURN ***
        }

        if (!EPRUtils.isInstanceEPR(producer)) {

            if (pr != null && pr.enabled()) {
                final String err = "Received EPR is not an instance EPR, " +
                        "cannot use for workspace object lookup";
                if (pr.useThis()) {
                    pr.errln(PrCodes.WSLISTEN__ERRORS, err);
                } else if (pr.useLogging()) {
                    logger.error(err);
                }

                final String pre = "Received EPR is not an instance EPR, " +
                        "cannot use for workspace object lookup.  EPR: ";
                String dbg;
                Exception exception = null;
                try {
                    dbg = pre + EPRUtils.eprToString(producer);
                } catch (SerializationException e) {
                    dbg = pre + "[[EPR deserialization caused " +
                            "SerializationException: " + e.getMessage() + "]]";
                    exception = e;
                } catch (IOException e) {
                    dbg = pre + "[[EPR deserialization caused " +
                            "IOException: " + e.getMessage() + "]]";
                    exception = e;
                }

                if (pr.useThis()) {
                    pr.dbg(dbg);
                    if (exception != null) {
                        exception.printStackTrace(pr.getDebugProxy());
                    }
                } else if (pr.useLogging()) {
                    if (exception != null) {
                        logger.debug(dbg, exception);
                    } else {
                        logger.debug(dbg);
                    }
                }
            }

            return null; // *** EARLY RETURN ***
        }

        final int id = EPRUtils.getIdFromEPR(producer);
        final String address = EPRUtils.getServiceURIAsString(producer);

        if (address == null) {
            if (pr != null && pr.enabled()) {
                final String err = "Received EPR has no service address, " +
                        "cannot use for workspace object lookup";
                if (pr.useThis()) {
                    pr.errln(PrCodes.WSLISTEN__ERRORS, err);
                } else if (pr.useLogging()) {
                    logger.error(err);
                }
            }

            return null; // *** EARLY RETURN ***
        }

        return new AddressIDPair(id, address);
    }
}
