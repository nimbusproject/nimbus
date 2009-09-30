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

import org.apache.axis.message.addressing.Address;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.globus.axis.gsi.GSIConstants;
import org.globus.workspace.client.modes.Destroy;
import org.globus.workspace.client.modes.FactoryQuery;
import org.globus.workspace.client.modes.InstanceQuery;
import org.globus.workspace.client.modes.Mode;
import org.globus.workspace.client.modes.Pause;
import org.globus.workspace.client.modes.Reboot;
import org.globus.workspace.client.modes.Shutdown;
import org.globus.workspace.client.modes.Start;
import org.globus.workspace.client.modes.Subscribe;
import org.globus.workspace.client.modes.Deploy;
import org.globus.workspace.client.modes.ShutdownSave;
import org.globus.workspace.client.modes.EnsembleDone;
import org.globus.workspace.client.modes.EnsembleMonitor;
import org.globus.workspace.client.modes.ContextDataInject;
import org.globus.workspace.client.modes.ContextMonitor;
import org.globus.workspace.client.modes.ContextNoMoreInjections;
import org.globus.workspace.client.modes.ContextCreate;
import org.globus.workspace.client.modes.ContextCreate_Injectable;
import org.globus.workspace.client.modes.ContextAgentImpersonate;
import org.globus.workspace.client_common.BaseClient;
import org.globus.workspace.common.client.CommonPrint;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.wsrf.encoding.ObjectDeserializer;
import org.globus.wsrf.impl.security.authentication.Constants;
import org.globus.wsrf.impl.security.authorization.Authorization;
import org.globus.wsrf.impl.security.authorization.HostAuthorization;
import org.globus.wsrf.impl.security.authorization.IdentityAuthorization;
import org.globus.wsrf.impl.security.authorization.NoAuthorization;
import org.globus.wsrf.impl.security.authorization.SelfAuthorization;
import org.xml.sax.InputSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class WorkspaceCLI extends BaseClient {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    // default print system is to print nothing... see entry points
    // NOTE: this field is never null-checked before use. Don't set it to null
    //       in extending objects, set it to do nothing if you want to disable
    //       it.
    protected Print pr = new Print();

    // includes both raw values of everything given over commandline
    // and also any directly derived values
    protected AllArguments cliArgs;

    // client mode, a mode might launch multiple actions
    // only one mode is ever run
    protected Mode mode;
    
    // -------------------------------------------------------------------------
    // GENERAL
    // -------------------------------------------------------------------------

    public WorkspaceCLI(Print print) {
        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        this.pr = print;
    }

    protected Print getPr() {
        return this.pr;
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

    public void setMechanism(String mech) {
        this.mechanism = mech;
    }

    public void setProtection(Object protect) {
        this.protection = protect;
    }

    public void setAuthorization(Authorization authz) {
        this.authorization = authz;
    }

    // -------------------------------------------------------------------------
    // ARG INTAKE
    // -------------------------------------------------------------------------

    protected void intakeCmdlineOptions(String[] args) throws ParameterProblem {

        // (debug was fished out already)
        final String sectionTitle = "COMMANDLINE INTAKE";
        CommonPrint.printDebugSection(this.pr, sectionTitle);

        final Opts opts = new Opts();

        for (int i = 0; i < opts.ALL_ENABLED_OPTIONS.length; i++) {
            this.options.addOption(opts.ALL_ENABLED_OPTIONS[i]);
        }

        final Properties defaultOptions = new Properties();
        // default is host authorization
        defaultOptions.setProperty(BaseClient.AUTHZ.getOpt(), "host");

        // would like to be rid of wsrf BaseClient altogether one day
        final CommandLine line;
        try {
            line = this.parse(args, defaultOptions);
        } catch (Exception e) {
            throw new ParameterProblem(e.getMessage(), e);
        }

        this.cliArgs = new AllArguments(this.pr);
        this.cliArgs.intake(line);

        CommonPrint.printDebugSectionEnd(this.pr, sectionTitle);
    }

    // overrides org.globus.wsrf.client.BaseClient
    // That does a number of unwanted things.  We'll probably stop using
    // that BaseClient altogether in the future. # TODO
    protected CommandLine parse(String[] args, Properties defaultOptions)

            throws Exception {

        final CommandLineParser parser = new PosixParser();
        final CommandLine line =
                parser.parse(this.options, args, defaultOptions);

        String eprFile = null;
        if (line.hasOption(Opts.EPRFILE2_OPT_STRING)) {
            eprFile = line.getOptionValue(Opts.EPRFILE2_OPT_STRING);
        } else if (line.hasOption("e")) {
            eprFile = line.getOptionValue("e");
        }

        if (eprFile != null) {
            
            if (line.hasOption("k")) {
                throw new ParseException("-e and -k arguments are exclusive");
            }
            
            if (line.hasOption("s")) {
                throw new ParseException("-e and -s arguments are exclusive");
            }

            FileInputStream in = null;
            try {
                in = new FileInputStream(eprFile);
                this.endpoint =
                (EndpointReferenceType) ObjectDeserializer.deserialize(
                    new InputSource(in),
                    EndpointReferenceType.class);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } else if (line.hasOption(Opts.SERVICE2_OPT_STRING)) {
            this.endpoint = new EndpointReferenceType();
            this.endpoint.setAddress(
                    new Address(line.getOptionValue(Opts.SERVICE2_OPT_STRING)));
        } else if (line.hasOption("s")) {
            this.endpoint = new EndpointReferenceType();
            this.endpoint.setAddress(new Address(line.getOptionValue("s")));
        }

        this.debugMode = line.hasOption("d");

        // Security mechanism
        if (line.hasOption("m")) {
            String value = line.getOptionValue("m");
            if (value != null) {
                if (value.equals("msg")) {
                    this.mechanism = Constants.GSI_SEC_MSG;
                } else if (value.equals("conv")) {
                    this.mechanism = Constants.GSI_SEC_CONV;
                } else {
                    throw new ParseException(
                        "Unsupported security mechanism: " +  value);
                }
            }
        }

        // Protection
        if (line.hasOption("p")) {
            String value = line.getOptionValue("p");
            if (value != null) {
                if (value.equals("sig")) {
                    this.protection = Constants.SIGNATURE;
                } else if (value.equals("enc")) {
                    this.protection = Constants.ENCRYPTION;
                } else {
                    throw new ParseException("Unsupported protection mode: " +
                                             value);
                }
            }
        }

        // Delegation
        if (line.hasOption("g")) {
            String value = line.getOptionValue("g");
            if (value != null) {
                if (value.equals("limited")) {
                    this.delegation = GSIConstants.GSI_MODE_LIMITED_DELEG;
                } else if (value.equals("full")) {
                    this.delegation = GSIConstants.GSI_MODE_FULL_DELEG;
                } else {
                    throw new ParseException("Unsupported delegation mode: " +
                                             value);
                }
            }
        }

        // Authz
        if (line.hasOption("z")) {
            String value = line.getOptionValue("z");
            if (value != null) {
                if (value.equals("self")) {
                    this.authorization = SelfAuthorization.getInstance();
                } else if (value.equals("host")) {
                    this.authorization = HostAuthorization.getInstance();
                } else if (value.equals("none")) {
                    this.authorization = NoAuthorization.getInstance();
                } else if (authorization == null) {
                    this.authorization = new IdentityAuthorization(value);
                }
            }
        }

        // Anonymous
        if (line.hasOption("a")) {
            this.anonymous = Boolean.TRUE;
        }

        // context lifetime
        if (line.hasOption("l")) {
            final String value = line.getOptionValue("l");
            if (value != null) {
                this.contextLifetime = new Integer(value);
            }
        }

        // msg actor
        if (line.hasOption("x")) {
            this.msgActor = line.getOptionValue("x");
        }

        // conv actor
        if (line.hasOption("y")) {
            this.convActor = line.getOptionValue("y");
        }

        // Server's public key
        if (line.hasOption("c")) {
            this.publicKeyFilename = line.getOptionValue("c");
        }


        if (line.hasOption("f")) {
            this.descriptorFile = line.getOptionValue("f");
        }

        return line;
    }

    public void setEPR(EndpointReferenceType epr) {
        this.endpoint = epr;
    }

    // -------------------------------------------------------------------------
    // HELP SYSTEM
    // -------------------------------------------------------------------------

    protected void helpFirst() throws ExitNow {

        if (this.cliArgs.mode_help) {
            // first see if there is an action flag included
            if (this.displayModeUsage(this.cliArgs)) {
                throw new ExitNow(0);
            } else {
                this.displayUsage();
                throw new ExitNow(0);
            }
        }

        if (this.cliArgs.mode_extraUsage) {
            this.displayExtraUsage();
            throw new ExitNow(0);
        }
    }

    // overrides org.globus.wsrf.client.BaseClient
    public void displayUsage() {
        try {
            this.pr.infoln(Help.FIRST_LINE + new Help().getHelpString());
        } catch (IOException e) {
            this.pr.errln("(?) IOException getting help text");
            e.printStackTrace(this.pr.getErrProxy());
        }
    }

    // returns true if there was also an action flag included
    // (--help already checked for when calling)
    public boolean displayModeUsage(AllArguments args) {
        try {
            final String helpText = new Help().getModeHelpString(args);
            if (helpText == null) {
                return false;
            } else {
                this.pr.infoln(Help.FIRST_LINE + helpText);
                return true;
            }
        } catch (IOException e) {
            this.pr.errln("(?) IOException getting help text");
            e.printStackTrace(this.pr.getErrProxy());
            return false;
        }
    }

    public void displayExtraUsage() {
        try {
            this.pr.infoln(Help.FIRST_LINE + new Help().getExtraHelpString());
        } catch (IOException e) {
            this.pr.errln("(?) IOException getting help text");
            e.printStackTrace(this.pr.getErrProxy());
        }
    }

    
    // -------------------------------------------------------------------------
    // VALIDATE
    // -------------------------------------------------------------------------

    protected void validate() throws ParameterProblem {

        if (this.cliArgs == null) {
            throw new IllegalStateException(
                "this method can not be run before intakeCmdlineOptions");
        }

        final String sectionTitle = "CHOOSE MODE";
        CommonPrint.printDebugSection(this.pr, sectionTitle);

        this.chooseMode();

        CommonPrint.printDebugSectionEnd(this.pr, sectionTitle);

        this.mode.validateOptions();
    }

    /**
     * Set the appropriate Mode object
     *
     * @throws ParameterProblem if 0 or multiple action (mode) flags were supplied
     */
    private void chooseMode() throws ParameterProblem {

        if (this.cliArgs.mode_deploy) {
            this.setMode(new Deploy(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_destroy) {
            this.setMode(new Destroy(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_factoryRpQuery) {
            this.setMode(new FactoryQuery(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_pause) {
            this.setMode(new Pause(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_reboot) {
            this.setMode(new Reboot(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_rpquery) {
            this.setMode(new InstanceQuery(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_shutdown) {
            this.setMode(new Shutdown(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_shutdown_save) {
            this.setMode(new ShutdownSave(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_start) {
            this.setMode(new Start(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_subscribe) {
            this.setMode(new Subscribe(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_doneEnsemble) {
            this.setMode(new EnsembleDone(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_monitorEnsemble) {
            this.setMode(new EnsembleMonitor(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_monitorContext) {
            this.setMode(new ContextMonitor(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_injectContextData) {
            this.setMode(new ContextDataInject(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_noMoreContextInjections) {
            this.setMode(new ContextNoMoreInjections(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_createContext) {
            this.setMode(new ContextCreate(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_createInjectableContext) {
            this.setMode(new ContextCreate_Injectable(this.pr, this.cliArgs, this));
        } else if (this.cliArgs.mode_impersonateContextAgent) {
            this.setMode(new ContextAgentImpersonate(this.pr, this.cliArgs, this));
        }

        if (this.mode == null) {
            throw new ParameterProblem("You must supply an action.");
        }
    }

    private void setMode(Mode attempt) throws ParameterProblem {
        this.pr.dbg("Attempt: " + attempt.name().toUpperCase());
        if (this.mode == null) {
            this.mode = attempt;
            this.pr.dbg("Mode chosen: " + attempt.name().toUpperCase());
        } else {
            final String msg = "You cannot run more than one action at a " +
                    "time.  \"" + this.mode.name().toLowerCase() + "\" is " +
                    "configured already, but you also supplied \"" +
                    attempt.name().toLowerCase() + "\"";
            throw new ParameterProblem(msg);
        }
    }

    
    // -------------------------------------------------------------------------
    // RUN
    // -------------------------------------------------------------------------

    protected void run() throws ParameterProblem,
                                ExecutionProblem,
                                ExitNow {

        if (this.cliArgs == null) {
            throw new IllegalStateException(
                "this method can not be run before validateCmdlineOptions");
        }
        
        this.mode.run();
    }
}
