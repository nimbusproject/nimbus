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

package org.globus.workspace.client.modes;

import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.utils.EPRUtils;
import org.globus.workspace.client_core.subscribe_tools.ListeningSubscriptionMaster;
import org.globus.workspace.client_core.subscribe_tools.PollingSubscriptionMaster;
import org.globus.workspace.client_core.subscribe_tools.SubscriptionMaster;
import org.globus.workspace.client_core.subscribe_tools.NotificationImplementationException;
import org.globus.workspace.client_core.subscribe_tools.SubscriptionMasterFactory;
import org.globus.workspace.client_core.repr.State;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client.AllArguments;
import org.globus.workspace.client.modes.aux.SubscribeLaunch;
import org.globus.workspace.client.modes.aux.CommonLogs;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis.message.addressing.EndpointReferenceType;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;

import java.net.URL;
import java.net.MalformedURLException;

public class Subscribe extends Mode {


    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(Subscribe.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------
    
    EndpointReferenceType epr;
    boolean dryrun;
    boolean autodestroy;
    boolean isGroupRequest;
    String nameToPrint;
    String shortName;
    Workspace[] workspaces;

    // note that "subscribe" has same meaning here whether polling or not
    boolean pollDontListen; // true: poll, false: async listen
    State exitState;
    State veryTerseNotifyState;
    SubscribeLaunch subscribeLaunch;
    ListeningSubscriptionMaster listeningSubscriptionMaster;
    PollingSubscriptionMaster pollingSubscriptionMaster;
    SubscriptionMaster eitherSubscriptionMaster;
    String notificationListenerOverride_IPorHost;
    Integer notificationListenerOverride_Port;
    long pollMS = -1;
    int pollMaxThreads = -1;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public Subscribe(Print print,
                     AllArguments arguments,
                     StubConfigurator stubConfigurator) {
        super(print, arguments, stubConfigurator);
    }


    // -------------------------------------------------------------------------
    // GENERAL
    // -------------------------------------------------------------------------

    public String name() {
        return "Subscribe";
    }

    // -------------------------------------------------------------------------
    // VALIDATE
    // -------------------------------------------------------------------------

    public void validateOptionsImpl() throws ParameterProblem {
        this.validateEndpoint();
        this.validateName();
        this._handleExitState();
        this._handleVeryTerseNotifyState();
        this._handlePollDelay();
        this._handlePollMaxThreads();
        this._handleListenerOverride();

        this.dryrun = this.args.dryrun;
        CommonLogs.logBoolean(this.dryrun, "dryrun mode",
                              this.pr, logger);

        this.autodestroy = this.args.autodestroy;
        CommonLogs.logBoolean(this.autodestroy, "autodestroy mode",
                              this.pr, logger);

        this._logSubscribeStatus();
        
        if (this.pollDontListen) {
            this._handleSubscribeWithPoll();
        } else {
            this._handleSubscribeWithListen();
        }

        this._handleWorkspaces();
    }

    protected void validateEndpoint() throws ParameterProblem {

        this.epr = this.stubConf.getEPR();

        if (this.epr == null) {
            throw new ParameterProblem(name() + " requires EPR");
        }

        final String eprStr;
        try {
            eprStr = EPRUtils.eprToString(this.epr);
        } catch (Exception e) {
            final String err = CommonUtil.genericExceptionMessageWrapper(e);
            throw new ParameterProblem(err, e);
        }

        if (this.pr.enabled()) {
            // xml print
            final String dbg =
                    "\nGiven EPR:\n----------\n" + eprStr + "----------\n";

            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }

        final String kind;
        if (EPRUtils.isInstanceEPR(this.epr)) {
            this.isGroupRequest = false;
            kind = "an instance";
        } else if (EPRUtils.isGroupEPR(this.epr)) {
            this.isGroupRequest = true;
            kind = "a group";
        } else {
            throw new ParameterProblem(name() + " requires a valid EPR.");
        }

        if (this.pr.enabled()) {
            final String dbg = "Given EPR is " + kind + " EPR";
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }

        if (this.isGroupRequest) {
            throw new ParameterProblem(name() + " can not handle group " +
                    "subscribe yet (only after deploying).  Stay tuned.");
        }
    }

    protected void validateName() throws ParameterProblem {

        String eprName;
        if (this.isGroupRequest) {
            eprName = EPRUtils.getGroupIdFromEPR(this.epr);
            eprName = "group-" + eprName;
        } else {
            eprName = Integer.toString(EPRUtils.getIdFromEPR(this.epr));
            eprName = "workspace-" + eprName;
        }

        this.shortName = this.args.shortName;
        if (this.pr.enabled()) {
            final String dbg;
            if (this.shortName == null) {
                dbg = "Given display name is null";
            } else {
                dbg = "Given display name: " + this.shortName + "'";
            }
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }

        if (this.shortName != null) {
            this.nameToPrint = this.shortName;
        } else {
            this.nameToPrint = eprName;
        }
    }

    protected void _handleExitState() throws ParameterProblem {

        if (this.args.exitStateString == null) {
            return;  // *** EARLY RETURN ***
        }

        if (!State.testValidState(this.args.exitStateString)) {
            throw new ParameterProblem("Provided exit string is not a " +
                    "valid state: '" + this.args.exitStateString + "'");
        }

        this.exitState = new State(this.args.exitStateString);
        if (this.pr.enabled()) {
            final String dbg = "Exit state: " + this.exitState.toString();
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }
    }

    protected void _handleVeryTerseNotifyState() throws ParameterProblem {

        if (this.args.veryTerseNotifyStateString == null) {
            return;  // *** EARLY RETURN ***
        }

        if (!State.testValidState(this.args.veryTerseNotifyStateString)) {
            throw new ParameterProblem(
                    "Provided very-terse-notify state string is not a " +
                    "valid state: '" +
                            this.args.veryTerseNotifyStateString + "'");
        }

        this.veryTerseNotifyState =
                new State(this.args.veryTerseNotifyStateString);
        if (this.pr.enabled()) {
            final String dbg = "very-terse-notify state: " +
                    this.veryTerseNotifyState.toString();
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }
    }

    protected void _handlePollDelay() throws ParameterProblem {

        if (this.args.pollDelayString == null) {
            return;  // *** EARLY RETURN ***
        }

        // default subscription mode is notification listener
        // setting the poll delay sets the subscription mode to poll
        this.pollDontListen = true;

        try {
            this.pollMS = Long.parseLong(this.args.pollDelayString);
        } catch (NumberFormatException e) {
            throw new ParameterProblem("Given poll delay is not valid: '" +
                    this.args.pollDelayString + "': " + e.getMessage(), e);
        }

        if (this.pollMS < 1) {
            throw new ParameterProblem("Given poll delay is less than 1ms: " +
                    this.pollMS + "ms");
        }
    }

    protected void _handlePollMaxThreads() throws ParameterProblem {

        if (this.args.pollMaxThreadsString == null) {
            return;  // *** EARLY RETURN ***
        }

        try {
            this.pollMaxThreads =
                    Integer.parseInt(this.args.pollMaxThreadsString);
        } catch (NumberFormatException e) {
            throw new ParameterProblem(
                    "Given poll max-threads number is not valid: '" +
                    this.args.pollMaxThreadsString + "': " + e.getMessage(), e);
        }

        if (this.pollMaxThreads < 1) {
            throw new ParameterProblem("Given poll max-threads is invalid, " +
                    "it is less than 1: " + this.pollMaxThreads);
        }
    }

    protected void _handleListenerOverride() throws ParameterProblem {

        final String given = this.args.listenerOverride;
        if (given == null) {
            return;  // *** EARLY RETURN ***
        }

        if (given.indexOf('/') >= 0) {
            throw new ParameterProblem("Listener override address has a '/' " +
                    "in it, use just a host or IP, not an URL");
        }

        final int splt = given.indexOf(':'); // first occurence only
        final String host;
        final String port;
        if (splt >= 0) {
            host = given.substring(0, splt).trim();
            port = given.substring(splt).trim();
        } else {
            host = given.trim();
            port = null;
        }

        if (host.length() == 0) {
            throw new ParameterProblem("Listener override address " +
                    "has zero-length hostname, given: \"" + given + "\"");
        }

        if (port != null && port.length() == 0) {
            throw new ParameterProblem("Listener override address " +
                    "has zero-length port, given: \"" + given + "\"");
        }

        final String urlString;
        if (port == null) {
            urlString = "http://" + host;
        } else {
            urlString = "http://" + host + ":" + port;
        }

        final String usingHost;
        int usingPort = -1;
        try {
            final URL url = new URL(urlString);
            usingHost = url.getHost();
            if (port != null) {
                usingPort = url.getPort();
            }
        } catch (MalformedURLException e) {
            throw new ParameterProblem("Falied to test validity of listener " +
                    "override address using given input '" + given + "', " +
                    "testing with URL constructed from '" + urlString +
                    "': " + e.getMessage(), e);
        }

        this.notificationListenerOverride_IPorHost = usingHost;
        if (usingPort > 0) {
            this.notificationListenerOverride_Port = new Integer(usingPort);
        }
    }

    protected void _logSubscribeStatus() {

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        String listenTail = "";
        if (this.notificationListenerOverride_IPorHost != null
                && this.notificationListenerOverride_Port != null) {

            final String addr =
                    this.notificationListenerOverride_IPorHost + ":" +
                            this.notificationListenerOverride_Port.intValue();

            listenTail = " (listener override host+port: '" + addr + "')";

        } else if (this.notificationListenerOverride_IPorHost != null) {

            listenTail = " (listener override host: '" +
                    this.notificationListenerOverride_IPorHost + "')";

        }

        final String dbg;
        if (this.pollDontListen) {
            dbg = "subscription mode: POLL (" + this.pollMS + "ms delay)";
        } else {
            dbg = "subscription mode: LISTENER" + listenTail;
        }
        if (this.pr.useThis()) {
            this.pr.dbg(dbg);
        } else if (this.pr.useLogging()) {
            logger.debug(dbg);
        }
    }

    protected void _handleSubscribeWithListen() throws ParameterProblem {

        if (this.eitherSubscriptionMaster != null) {
            throw new IllegalStateException("you may only make one " +
                    "subscription master non-null (and copy its reference " +
                    "to 'either' variable)");
        }

        this.listeningSubscriptionMaster =
                SubscriptionMasterFactory.newListeningMaster(null, this.pr);

        this.eitherSubscriptionMaster = this.listeningSubscriptionMaster;


        if (this.dryrun) {
            final String dbg =
                    "Dryrun, not starting to listen for notifications.";
            if (this.pr.useThis()) {
                this.pr.info(PrCodes.CREATE__DRYRUN, dbg);
            } else if (this.pr.useLogging()) {
                logger.info(dbg);
            }

            return; // *** EARLY RETURN ***
        }

        final String errPrefix =
                "Problem starting to listen for notifications: ";
        try {
            this.listeningSubscriptionMaster.listen(
                this.notificationListenerOverride_IPorHost,
                    this.notificationListenerOverride_Port);
        } catch (IllegalStateException e) {
            throw new ParameterProblem(errPrefix + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ParameterProblem(errPrefix + e.getMessage(), e);
        } catch (Exception e) {
            throw new ParameterProblem(errPrefix + e.getMessage(), e);
        }

        this.subscribeLaunch =
                new SubscribeLaunch(this.listeningSubscriptionMaster,
                                    this.nameToPrint,
                                    this.pr,
                                    this.stubConf,
                                    null);
    }

    protected void _handleSubscribeWithPoll() throws ParameterProblem {

        if (this.eitherSubscriptionMaster != null) {
            throw new IllegalStateException("you may only make one " +
                    "subscription master non-null (and copy its reference " +
                    "to 'either' variable)");
        }

        final long ms = this.pollMS;
        final int thrNum = this.pollMaxThreads;
        final ExecutorService execService = null;
        final StubConfigurator conf = this.stubConf;

        final PollingSubscriptionMaster master;

        if (thrNum < 1) {
            // default maxThreads
            master = SubscriptionMasterFactory.newPollingMaster(ms,
                                                                conf,
                                                                execService,
                                                                this.pr);
        } else {
            // cap the maxThreads
            master = SubscriptionMasterFactory.newPollingMaster(ms,
                                                                thrNum,
                                                                conf,
                                                                execService,
                                                                this.pr);
        }

        this.pollingSubscriptionMaster = master;
        this.eitherSubscriptionMaster = master;

        this.subscribeLaunch = new SubscribeLaunch(master,
                                                   this.nameToPrint,
                                                   this.pr,
                                                   this.stubConf,
                                                   null);
    }


    protected void _handleWorkspaces() {

        if (this.isGroupRequest) {
            throw new IllegalStateException(
                    "group requests should have been rejected already");
        }

        final Workspace workspace = new Workspace();

        workspace.setEpr(this.epr);
        workspace.setCurrentState(new State());

        this.workspaces = new Workspace[1];
        this.workspaces[0] = workspace;
    }
    
    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    public void runImpl() throws ParameterProblem, ExecutionProblem, ExitNow {
        try {
            _runImpl();
        } finally {
            this.doneCleanupSubscriptions();
        }
    }

    private void _runImpl() throws ParameterProblem, ExecutionProblem, ExitNow {
        this.subscribeLaunch.subscribe(this.workspaces,
                                       this.exitState,
                                       this.veryTerseNotifyState,
                                       this.autodestroy,
                                       false,
                                       false);
    }

    protected void doneCleanupSubscriptions() {

        if (this.listeningSubscriptionMaster != null) {
            try {
                this.listeningSubscriptionMaster.stopListening();
            } catch (NotificationImplementationException e) {

                if (this.pr.enabled()) {

                    final String err =
                            "Problem stopping notification listener: " +
                                    e.getMessage();

                    if (this.pr.useThis()) {
                        this.pr.errln(PrCodes.ANY_ERROR_CATCH_ALL,
                                      err);
                    } else if (this.pr.useLogging()) {
                        if (logger.isDebugEnabled()) {
                            logger.error(err, e);
                        } else {
                            logger.error(err);
                        }
                    }
                }
            }
        }

        if (this.pollingSubscriptionMaster != null) {
            this.pollingSubscriptionMaster.stopPolling();
        }
    }


}
