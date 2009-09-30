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

package org.nimbustools.messaging.gt4_0.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.wsrf.Constants;
import org.globus.wsrf.container.ServiceHost;
import org.globus.wsrf.config.ContainerConfig;
import org.globus.wsrf.jndi.Initializable;
import org.nimbustools.api.brain.ModuleLocator;
import org.nimbustools.api.brain.BreathOfLife;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import commonj.timers.TimerManager;

import javax.naming.InitialContext;

public class NimbusMasterContext implements Initializable {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(NimbusMasterContext.class.getName());

    public static final String MASTER_JNDI_BASE =
            Constants.JNDI_SERVICES_BASE_NAME + "NimbusMasterContext/";

    public static final String THIS_JNDI_LOOKUP =
            MASTER_JNDI_BASE + "masterContext";
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private ModuleLocator moduleLocator;
    private String baseLocation;
    private String masterConf;
    

    // -------------------------------------------------------------------------
    // SET (via jndi)
    // -------------------------------------------------------------------------

    public synchronized void setMasterConf(String path) {
        this.masterConf = path;
    }
    

    // -------------------------------------------------------------------------
    // implements Initializable
    // -------------------------------------------------------------------------

    public synchronized void initialize() throws Exception {

        if (this.moduleLocator != null) {
            logger.warn("MasterContext.initialize() should not have been " +
                    "invoked more than once");
            return;
        }

        this.checkConf(this.masterConf);

        // spring interprets "/" as a relative path unless it is "//"
        final String appCtxPath = this.fixAbsolute(this.masterConf);

        this.moduleLocator = new BreathOfLife().breathe(appCtxPath);
    }

    
    // -------------------------------------------------------------------------
    // APPLICATION CONTEXT DISCOVERY
    // -------------------------------------------------------------------------

    /**
     * @return ApplicationContext, never null
     * @throws Exception could not locate
     */
    public static NimbusMasterContext discoverApplicationContext() throws Exception {

        InitialContext ctx = null;
        try {
            ctx = new InitialContext();

            final NimbusMasterContext masterContext =
                    (NimbusMasterContext) ctx.lookup(THIS_JNDI_LOOKUP);

            if (masterContext == null) {
                // should be NameNotFoundException if missing
                throw new Exception("null from JNDI for MasterContext (?)");
            }

            return masterContext;

        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }


    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    public ModuleLocator getModuleLocator() {
        if (this.moduleLocator == null) {
            throw new IllegalStateException("The application was not " +
                    "initialized correctly.");
        }
        return this.moduleLocator;
    }


    /**
     * In Globus environment, GL is present.
     *
     * @return baseLocation never null
     * @throws Exception problem
     */
    public String getBaseLocation()

            throws Exception {

        if (this.baseLocation != null) {
            return this.baseLocation;
        }

        this.baseLocation = ContainerConfig.getBaseDirectory();

        if (this.baseLocation == null) {
            throw new Exception("cannot find base system " +
                                         "location (GLOBUS_LOCATION)");
        }

        return this.baseLocation;
    }

    /**
     * @return base URL, never null
     * @throws java.io.IOException problem getting URL from GT container
     */
    public URL getBaseURL() throws IOException {
        return ServiceHost.getBaseURL();
    }
    

    /**
     * @return TimerManager, never null
     * @throws Exception could not locate
     */
    public TimerManager discoverTimerManager() throws Exception {

        InitialContext ctx = null;
        try {
            ctx = new InitialContext();

            final TimerManager timerManager =
                    (TimerManager) ctx.lookup(Constants.DEFAULT_TIMER);

            if (timerManager == null) {
                // should be NameNotFoundException if missing
                throw new Exception("null from JNDI for TimerManager (?)");
            }

            return timerManager;
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    
     // -------------------------------------------------------------------------
    // CONF FILE
    // -------------------------------------------------------------------------

    protected String fixAbsolute(String path) throws Exception {

        final File conf = new File(path);
        if (conf.isAbsolute()) {
            // spring interprets "/" as a relative path unless it is "//"
            return "/" + path;
        } else {
            return path;
        }
    }

    protected void checkConf(String path) throws Exception {

        final String jndiAdvice =
            "** The system is bootstrapped from a configuration called " +
            "'masterConf' near the top of a file usually located at " +
            "'$GLOBUS_LOCATION/etc/nimbus/jndi-config.xml'";

        final String confadvice =
            "** The 'masterConf' parameter is usually set to a file like " +
            "'$GLOBUS_LOCATION/etc/nimbus/workspace-service/other/spring.xml'.  A " +
            "configuration is present for this but it is not usable.";

        // -----------------------

        if (path == null) {
            throw new Exception("No masterConf setting.\n" + jndiAdvice);
        }

        String confInvalidError = null;

        final File conf = new File(path);

        String debugHelp = "[[ setting was '" + path + "' ]]";
        if (conf.isAbsolute()) {
            debugHelp += " [[ that is an absolute path ]]";
        } else {
            debugHelp += " [[ that is a relative path which resolves to " +
                    "absolute path '" + conf.getAbsolutePath() + "' ]]";
        }

        if (!conf.exists()) {
            confInvalidError = "File does not exist. " + debugHelp;
        } else if (!conf.canRead()) {
            confInvalidError = "File can not be read. " + debugHelp;
        }

        if (confInvalidError != null) {
            throw new Exception(
                    "Unusable masterConf setting.\n" + jndiAdvice +
                    "\n" + confadvice +
                    "\n** Problem: " + confInvalidError);
        }
    }
}
