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

package org.globus.workspace.client;

import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.common.print.PrintOpts;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.common.client.CommonPrint;
import org.globus.workspace.client_common.BaseClient;
import org.globus.workspace.common.client.CLIUtils;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.PrintStream;
import java.io.IOException;

public class WorkspaceCLIMain {


    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(WorkspaceCLIMain.class.getName());

    
    // -------------------------------------------------------------------------
    // ENTRY POINTS
    // -------------------------------------------------------------------------

    /**
     * <p>This is the only main routine in the package.  It contains the only
     * call to System.exit in the package.</p>
     *
     * <p>Implementation:</p>
     *
     * <pre>
     * System.exit(mainNoExit(args));
     * </pre>
     *
     * @param args args
     * @see #mainNoExit(String[]) 
     */
    public static void main(String[] args) {
        System.exit(mainNoExit(args));
    }

    /**
     * Uses default print system.
     *
     * INFO: <code>System.out</code>
     * ERR: <code>System.err</code>
     * DBG: <code>System.err</code> (if enabled)
     *
     * @param args args
     * @return return code
     */
    public static int mainNoExit(String[] args) {
        // get debug early for diagnosing problems with arg parsing
        PrintStream debug = null;
        if (CLIUtils.containsDebug(args)) {
            debug = System.err;
        }
        return mainNoExit(args, System.out, System.err, debug);
    }

    /**
     * Provide your own print implementation.
     *
     * @param args args
     * @param print may not be null
     * @return return code
     */
    public static int mainNoExit(String[] args,
                                 Print print) {

        final WorkspaceCLI cli = new WorkspaceCLI(print);

        return mainImpl(args, cli);
    }

    /**
     * Set up print implementation from particular PrintStream objects.
     *
     * @param args args
     * @param infoStream may be null
     * @param errStream may be null
     * @param debugStream may be null
     * @return return code
     */
    public static int mainNoExit(String[] args,
                                 PrintStream infoStream,
                                 PrintStream errStream,
                                 PrintStream debugStream) {

        final PrintOpts printOpts = new PrintOpts(null);
        final Print print =
                new Print(printOpts, infoStream, errStream, debugStream);

        final WorkspaceCLI cli = new WorkspaceCLI(print);

        final int retCode = mainImpl(args, cli);

        print.flush();
        print.close();

        return retCode;
    }


    // -------------------------------------------------------------------------
    // MAIN() IMPLEMENTATION
    // -------------------------------------------------------------------------

    private static int mainImpl(String[] args,
                                WorkspaceCLI cli) {

        // used:
        ParameterProblem parameterProblem = null;
        ExitNow exitNow = null;
        Throwable any = null;

        // unused currently:
        //Throwable throwable = null;
        //ExecutionProblem executionProblem = null;

        int retCode;
        try {
            retCode = _mainImpl(args, cli);
        } catch (ExitNow e) {
            exitNow = e;
            any = e;
            retCode = exitNow.exitCode;
        } catch (ParameterProblem e) {
            parameterProblem = e;
            any = e;
            retCode = BaseClient.COMMAND_LINE_EXIT_CODE;
        } catch (ExecutionProblem e) {
            //executionProblem = e;
            any = e;
            retCode = BaseClient.APPLICATION_EXIT_CODE;
        } catch (Throwable t) {
            //throwable = t;
            any = t;
            retCode = BaseClient.UNKNOWN_EXIT_CODE;
        }

        final Print pr = cli.getPr();

        if (!pr.enabled()) {
            // the rest of this method is for printing
            return retCode; // *** EARLY RETURN ***
        }

        if (exitNow != null) {
            pr.debugln("[exiting via exitnow system]");
            pr.debugln(BaseClient.retCodeDebugStr(retCode));
            return retCode; // *** EARLY RETURN ***
        }

        if (any == null) {
            pr.debugln(BaseClient.retCodeDebugStr(retCode));
            return retCode; // *** EARLY RETURN ***
        }

        CommonPrint.printDebugSection(pr, "PROBLEM");

        final String message = CommonUtil.genericExceptionMessageWrapper(any);

        if (pr.useThis()) {

            String err = "\nProblem: " + message;

            if (parameterProblem != null && !pr.useLogging()) {
                err += "\nSee help (-h).";
            }

            pr.errln(PrCodes.ANY_ERROR_CATCH_ALL, err);

        } else if (pr.useLogging()) {

            final String err = "Problem: " + message;

            if (logger.isDebugEnabled()) {
                logger.error(err, any);
            } else {
                logger.error(err);
            }
        }

        pr.debugln("\n");

        final String sectionTitle = "STACKTRACE";
        CommonPrint.printDebugSection(pr, sectionTitle);

        any.printStackTrace(pr.getDebugProxy());

        CommonPrint.printDebugSectionEnd(pr, sectionTitle);

        pr.debugln("\n");

        pr.debugln("Stacktrace was from: " + any.getMessage());

        pr.debugln(BaseClient.retCodeDebugStr(retCode));

        return retCode;
    }

    

    private static int _mainImpl(String[] args,
                                 WorkspaceCLI cli) throws ParameterProblem,
                                                          ExecutionProblem,
                                                          ExitNow {

        // (for development only, to attach a remote debugger etc)
        if (CLIUtils.containsDebuggerHang(args)) {
            try {
                CLIUtils.hangForInput(cli.getPr());
            } catch (IOException e) {
                throw new ExecutionProblem("", e);
            }
        }

        CommonPrint.logArgs(args, cli.getPr());

        cli.intakeCmdlineOptions(args);

        // if help was requested, it prints and an ExitNow is thrown
        cli.helpFirst();

        cli.validate();

        cli.run();

        return BaseClient.SUCCESS_EXIT_CODE;
    }
}
