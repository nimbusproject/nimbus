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

package org.globus.workspace.client_core;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.common.print.Print;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;

import java.rmi.Remote;

/**
 * Abstract base class for typically-implemented WS action classes
 */
public abstract class WSAction extends Action {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(WSAction.class.getName());

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected EndpointReferenceType epr;
    protected final StubConfigurator stubConf;
    protected Remote portType;
    protected final Print pr;
    
    protected String logName;

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * New portType object will be created from given parameters during the
     * validation phase.
     *
     * @param endpoint  endpoint to create portType with, may not be null
     * @param stubConfigurator  to configure portType with, may not be null
     * @param debug  for debug messages; if null, a new instance of disabled
     *               print impl will be created for <code>this.pr</code>
     */
    public WSAction(EndpointReferenceType endpoint,
                    StubConfigurator stubConfigurator,
                    Print debug) {

        if (endpoint == null) {
            throw new IllegalArgumentException(
                    "endpoint may not be null");
        }
        this.epr = endpoint;

        if (stubConfigurator == null) {
            throw new IllegalArgumentException(
                    "stubConfigurator may not be null");
        }
        this.stubConf = stubConfigurator;

        this.portType = null;

        if (debug == null) {
            this.pr = new Print(); // convenience constructor for DISABLE mode
        } else {
            this.pr = debug;
        }
    }

    /**
     * Re-use pre-created portType object.
     *
     * @param genericPortType portType to use for action, may not be null
     * @param debug  for debug messages; if null, a new instance of disabled
     *               print impl will be created for <code>this.pr</code>
     */
    public WSAction(Remote genericPortType,
                    Print debug) {

        if (genericPortType == null) {
            throw new IllegalArgumentException(
                    "genericPortType may not be null");
        }
        this.portType = genericPortType;
        
        this.epr = null;
        this.stubConf = null;

        if (debug == null) {
            this.pr = new Print(); // convenience constructor for DISABLE mode
        } else {
            this.pr = debug;
        }
    }


    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------
    
    /**
     * Calls the action.  This is an implementation of the Callable interface,
     * to be used with util.concurrent (currently the backport).
     *
     * When used in separate thread this way, there is debug logging for start,
     * stop, and error events.
     *
     * Implementation:
     *
     * <pre>
       final Object o;
       try {
            this._logStart();
            o = this.action();
            this._logEnd();
        } catch (Exception e) {
            this._logEndWithException(e);
            throw e;
        }
        return o;
        </pre>
     *
     *
     *
     *
     *
     * @return might be null, see each classes "action()" docs
     * @throws Exception problem
     */
    public Object call() throws Exception {
        final Object o;
        try {
            this._logStart();
            o = this.action();
            this._logEnd();
        } catch (Exception e) {
            this._logEndWithException(e);
            throw e;
        }
        return o;
    }

    protected abstract Object action() throws Exception;


    // -------------------------------------------------------------------------
    // MISC
    // -------------------------------------------------------------------------

    protected void _logStart() {
        
        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }
        
        this._logNameSet();

        final String dbg = "[ started: " + this.logName + " ]";
        if (this.pr.useThis()) {
            this.pr.debugln(dbg);
        } else if (this.pr.useLogging()) {
            logger.debug(dbg);
        }
    }

    protected void _logEnd() {

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        this._logNameSet();

        final String dbg = "[   ended: " + this.logName + " ]";
        if (this.pr.useThis()) {
            this.pr.debugln(dbg);
        } else if (this.pr.useLogging()) {
            logger.debug(dbg);
        }
    }

    protected void _logEndWithException(Exception e) {

        if (!this.pr.enabled()) {
            return; // *** EARLY RETURN ***
        }

        this._logNameSet();

        final String dbg =
                "[ ended with exception: " + this.logName + " ] (message: '" +
                CommonUtil.genericExceptionMessageWrapper(e) + "')";
        if (this.pr.useThis()) {
            this.pr.debugln(dbg);
        } else if (this.pr.useLogging()) {
            logger.debug(dbg);
        }
    }

    protected void _logNameSet() {
        this._logNameSet(null);
    }

    protected void _logNameSet(String overrideName) {

        if (overrideName != null) {
            this.logName = overrideName;
            return; // *** EARLY RETURN ***
        }

        if (this.logName != null) {
            return; // *** EARLY RETURN ***
        }

        final String fullClassName = this.getClass().getName();

        this.logName = fullClassName;

        // TODO: iob exc
        //final String[] names = fullClassName.split(".");
        //this.logName = names[names.length - 1];
    }
}
