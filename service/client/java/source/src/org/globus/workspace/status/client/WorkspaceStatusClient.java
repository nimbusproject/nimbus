/*
 * Copyright 1999-2007 University of Chicago
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

package org.globus.workspace.status.client;

import org.globus.wsrf.utils.AddressingUtils;
import org.globus.wsrf.utils.FaultHelper;
import org.globus.wsrf.impl.SimpleResourceKey;
import org.globus.wsrf.impl.security.authorization.Authorization;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.nimbustools.messaging.gt4_0.common.InvalidDurationException;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.utils.StringUtils;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.actions.Status_QueryAll;
import org.globus.workspace.common.print.PrintOpts;
import org.globus.workspace.client_common.BaseClient;
import org.nimbustools.messaging.gt4_0.generated.status.WorkspaceStatusPortType;
import org.nimbustools.messaging.gt4_0.generated.status.WorkspaceStatusServiceAddressingLocator;
import org.nimbustools.messaging.gt4_0.generated.status.VoidType;
import org.nimbustools.messaging.gt4_0.generated.status.UsedAndReservedTime_Type;

import org.nimbustools.messaging.gt4_0.common.CommonUtil;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.client.Stub;
import org.apache.axis.types.Duration;

import java.util.Properties;
import java.util.ArrayList;
import java.util.Iterator;
import java.rmi.RemoteException;
import java.io.PrintStream;
import java.io.File;

public class WorkspaceStatusClient extends BaseClient {

    private static String defaultServiceURL =
            "https://localhost:8443/wsrf/services/WorkspaceStatusService";

    private static final Log logger =
            LogFactory.getLog(WorkspaceStatusClient.class.getName());

    // keys match WorkspaceFactoryService (right now this is just singleton,
    // all for future support of multiple implementations)
    public static final SimpleResourceKey defaultResourceKey =
        new SimpleResourceKey(
                Constants_GT4_0.RESOURCE_KEY_QNAME,
                Constants_GT4_0.FACTORY_DEFAULT_RSRC_KEY_NAME);

    public static final Option FACTORYRP_OPT =
        OptionBuilder.withDescription("Query the status service resource " +
                "property information")
        .withLongOpt("queryrp")
        .create("r");

    public static final Option QUERY_USAGE_OPT =
        OptionBuilder.withDescription("Query for used and reserved time")
        .withLongOpt("queryusage")
        .create("q" );

    public static final Option QUERY_VMS_OPT =
        OptionBuilder.withDescription("Query for your current workspaces")
        .withLongOpt("queryvms")
        .create("c" );

    protected WorkspaceStatusClient() {
        options.addOption(FACTORYRP_OPT);
        options.addOption(QUERY_USAGE_OPT);
        options.addOption(QUERY_VMS_OPT);
    }

    public static final Integer ACTION_FACTORYQUERY = new Integer(1);
    public static final Integer ACTION_USAGEQUERY = new Integer(2);
    public static final Integer ACTION_VMQUERY = new Integer(3);

    private ArrayList actions = new ArrayList();

    public static void main(String[] args) {

        Properties defaultOptions = new Properties();

        // GSI Secure Msg (signature)
        //defaultOptions.put(BaseClient.PROTECTION.getOpt(), "sig");
        // no authorization
        defaultOptions.put(BaseClient.AUTHZ.getOpt(),
                           "none");

        WorkspaceStatusClient client = new WorkspaceStatusClient();

        client.setCustomUsage("[--todo]"); //TODO

        CommandLine line = null;

        try {
            try {
                line = client.parse(args, defaultOptions);
            } catch (ParseException e) {
                // not good! ... but cannot set default -s above
                // w/o conflicting with -e
                if (e.getMessage().matches("-s or -e argument is required")) {
                    // default service address
                    defaultOptions.put(
                            BaseClient.SERVICE_URL.getOpt(),
                            defaultServiceURL);

                    line = client.parse(args, defaultOptions);
                } else {
                    throw e;
                }
            }

            if (line.hasOption("r")) {
                client.actions.add(ACTION_FACTORYQUERY);
            }

            if (line.hasOption("q")) {
                client.actions.add(ACTION_USAGEQUERY);
            }

            if (line.hasOption("c")) {
                client.actions.add(ACTION_VMQUERY);
            }

            // default action
            if (client.actions.isEmpty()) {
                client.actions.add(ACTION_USAGEQUERY);
            }
            
        } catch(ParseException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("For help, use --help (-h)");
            System.exit(COMMAND_LINE_ERROR);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("For help, use --help (-h)");
            System.exit(APPLICATION_ERROR);
        }

        try {
            // there's a problem with setting the default resource key
            // option because 'k' needs 2 args, but couldn't (quickly)
            // figure out how to add both w/ defaultOptions.put()

            // instead, add the key manually if factory EPR is not specified

            if (!line.hasOption("e")) {
                client.endpoint = AddressingUtils.createEndpointReference(
                                 line.getOptionValue("s"), defaultResourceKey);
            }
        } catch(Exception e) {
            die(e,"EPR creation problem", client.isDebugMode());
        }

        System.out.println("\nUsing endpoint:\n---------------\n" +
                            client.endpoint + "---------------\n");

        WorkspaceStatusPortType statusPort =
                                    client.initStatusPort(client.endpoint);
        try {
            client.setOptions((Stub)statusPort);
        } catch (Exception e) {
            die(e,"Problem with stub", client.isDebugMode());
        }

        for (Object actionObj : client.actions) {
            Integer action = (Integer) actionObj;

            if (action.equals(ACTION_FACTORYQUERY)) {
                (new RPQuery(client, client.endpoint, System.out)).start();
            }

            if (action.equals(ACTION_USAGEQUERY)) {
                UsedAndReservedTime_Type usedReserved = null;
                try {
                    usedReserved =
                            statusPort.queryUsedAndReservedTime(new VoidType());
                } catch (RemoteException e) {
                    die(e, "Problem with query", client.isDebugMode());
                }

                Duration used = usedReserved.getUsedTime();
                Duration reserved = usedReserved.getReservedTime();

                if (used == null) {
                    System.err.println("No used minutes " +
                            "information returned");
                } else {
                    try {
                        System.out.println("Used minutes: " +
                                CommonUtil.durationToMinutes(used));
                    } catch (InvalidDurationException e) {
                        System.err.println("Error converting used time " +
                                "xsd:duration " +
                                "to minutes: " + e.getMessage());
                        if (client.isDebugMode()) {
                            e.printStackTrace(System.err);
                        }
                    }
                }

                if (reserved == null) {
                    System.err.println("No reserved minutes " +
                            "information returned");
                } else {
                    try {
                        System.out.println("Reserved minutes: " +
                                CommonUtil.durationToMinutes(reserved));
                    } catch (InvalidDurationException e) {
                        System.err.println("Error converting reserved " +
                                "xsd:duration to minutes: " + e.getMessage());
                        if (client.isDebugMode()) {
                            e.printStackTrace(System.err);
                        }
                    }
                }
            }

            if (action.equals(ACTION_VMQUERY)) {
                try {
                    final PrintOpts opts = new PrintOpts(null);
                    PrintStream debug = null;
                    if (client.isDebugMode()) {
                        debug = System.err;
                    }
                    final Print pr =
                            new Print(opts, System.out, System.err, debug);
                    final Status_QueryAll queryAll =
                            new Status_QueryAll(statusPort, pr);
                    Workspace[] workspaces = queryAll.queryAll();
                    printCurrent(workspaces, pr);
                } catch (Exception e) {
                    die(e, "Problem with query", client.isDebugMode());
                }
            }
        }
    }

    public static void printCurrent(Workspace[] workspaces,
                                    Print print) {

        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }

        if (!print.enabled()) {
            return; // *** EARLY RETURN ***
        }

        if (workspaces == null || workspaces.length == 0) {

            final String msg = "There are no workspaces running that you own.";

            if (print.useThis()) {
                print.infoln(msg);
            } else {
                logger.info(msg);
            }

            return; // *** EARLY RETURN ***
        }

        for (Workspace workspace : workspaces) {
            final String msg = StringUtils.
                    shortStringReprMultiLine(workspace);
            if (msg == null) {
                final String err = "No string representation?";
                if (print.useThis()) {
                    print.errln(err);
                } else {
                    logger.error(err);
                }
            } else {
                if (print.useThis()) {
                    print.infoln(msg);
                } else {
                    logger.info(msg);
                }
            }
        }
    }
    
    protected WorkspaceStatusPortType initStatusPort(
                EndpointReferenceType epr) {
        
        WorkspaceStatusServiceAddressingLocator locator =
            new WorkspaceStatusServiceAddressingLocator();

        WorkspaceStatusPortType port = null;

        try {
            port = locator.getWorkspaceStatusPortTypePort(epr);
            //setSecurity((Stub) factoryPort);
        } catch (Exception e) {
            die(e,"Port creation error", this.isDebugMode());
        }

        return port;
    }

    static void die(Exception e,
                    String type,
                    boolean debug) {

        FaultHelper helper = null;
        if (e != null) {
            helper = new FaultHelper(FaultHelper.toBaseFault(e));
        } else {
            System.err.println("Error, but no exception");
        }

        if (helper != null) {
            if (debug) {
                System.err.println("\n------\nError:\n------\n");
                System.err.println(type + ":\n" +
                        helper.getDescriptionAsString() + "\n");
                System.err.println("\n-----------\nStacktrace:\n-----------\n");
                FaultHelper.printStackTrace(e);
            } else {
                System.err.println("\n------\nError:\n------\n");
                System.err.println();
                System.err.println(type + ":\n" +
                        helper.getDescriptionAsString() + "\n");
            }
        }
        System.exit(APPLICATION_ERROR);
    }

    // -------------------------------------------------------------------------
    // implements StubConfigurator
    // -------------------------------------------------------------------------

    public Object getMechanism() {
        return this.mechanism;
    }

    public Object getProtection() {
        return this.protection;
    }

    public Authorization getAuthorization() {
        return (Authorization) this.authorization;
    }

    public void setEPR(EndpointReferenceType epr) {
        this.endpoint = epr;
    }

    public void setMechanism(String mech) {
        this.mechanism = mech;
    }

    public void setProtection(Object protect) {
        this.protection = protect;
    }

    public void setAuthorization(Authorization authz) {
        this.authorization = authz;
    }
}
