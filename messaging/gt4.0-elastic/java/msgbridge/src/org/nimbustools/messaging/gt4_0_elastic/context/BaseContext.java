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

package org.nimbustools.messaging.gt4_0_elastic.context;

import org.nimbustools.api.brain.NimbusFileSystemXmlApplicationContext;
import org.springframework.context.ApplicationContext;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceRM;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceGeneral;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceSecurity;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceImage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.wsrf.jndi.Initializable;

import java.io.File;

public abstract class BaseContext implements Initializable {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(BaseContext.class.getName());

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final String idRM;
    protected final String idGENERAL;
    protected final String idSECURITY;
    protected final String idIMAGE;
    protected final String contextName;
    protected final String confName;

    protected ApplicationContext appCtx;
    protected String springConf;
    protected boolean initialized;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    protected BaseContext(String idRM,
                          String idGENERAL,
                          String idSECURITY,
                          String idIMAGE,
                          String contextName,
                          String confName) {
        
        if (idRM == null) {
            throw new IllegalArgumentException("idRM may not be null");
        }
        if (idGENERAL == null) {
            throw new IllegalArgumentException("idGENERAL may not be null");
        }
        if (idIMAGE == null) {
            throw new IllegalArgumentException("idIMAGE may not be null");
        }
        if (idSECURITY == null) {
            throw new IllegalArgumentException("idSECURITY may not be null");
        }
        if (contextName == null) {
            throw new IllegalArgumentException("contextName may not be null");
        }
        if (confName == null) {
            throw new IllegalArgumentException("confName may not be null");
        }
        
        this.idRM = idRM;
        this.idGENERAL = idGENERAL;
        this.idSECURITY = idSECURITY;
        this.idIMAGE = idIMAGE;
        this.contextName = contextName;
        this.confName = confName;
    }


    // -------------------------------------------------------------------------
    // implements Initializable
    // -------------------------------------------------------------------------

    public synchronized void initialize() throws Exception {

        if (this.initialized) {
            logger.warn("ElasticContext.initialize() should not have been " +
                    "invoked more than once");
            return; // *** EARLY RETURN ***
        } else {
            this.initialized = true;
        }

        // see if file is valid
        this.checkConf(this.springConf);

        // spring interprets "/" as a relative path unless it is "//"
        final String elasticCtxPath = this.fixAbsolute(this.springConf);

        // instantiate spring container
        this.appCtx = new NimbusFileSystemXmlApplicationContext(elasticCtxPath);
    }


    // -------------------------------------------------------------------------
    // FIND THINGS FOR CLASSES THAT CAN'T BE IN THE DI SYSTEM
    // -------------------------------------------------------------------------

    public synchronized ServiceRM findRM() throws Exception {

        if (this.appCtx == null) {
            throw new Exception("No " + this.contextName + " was instantiated");
        }

        return (ServiceRM) this.appCtx.getBean(this.idRM);
    }

    public synchronized ServiceGeneral findGeneral() throws Exception {

        if (this.appCtx == null) {
            throw new Exception("No " + this.contextName + " was instantiated");
        }

        return (ServiceGeneral) this.appCtx.getBean(this.idGENERAL);
    }

    public synchronized ServiceSecurity findSecurity() throws Exception {

        if (this.appCtx == null) {
            throw new Exception("No " + this.contextName + " was instantiated");
        }

        return (ServiceSecurity) this.appCtx.getBean(this.idSECURITY);
    }

    public synchronized ServiceImage findImage() throws Exception {

        if (this.appCtx == null) {
            throw new Exception("No " + this.contextName + " was instantiated");
        }

        return (ServiceImage) this.appCtx.getBean(this.idIMAGE);
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
            "** The system is bootstrapped from a configuration called '" +
            this.confName +
            "' near the top of a file usually located at " +
            "'$GLOBUS_LOCATION/etc/nimbus/jndi-config.xml'";

        final String confadvice =
            "** The '" + this.confName +
                    "' parameter is usually set to a file like " +
            "'$GLOBUS_LOCATION/etc/nimbus/workspace-service/other/*xml'" +
            ".  A configuration is present for this but it is not usable.";

        // -----------------------

        if (path == null) {
            throw new Exception(
                    "No " + this.confName + " setting.\n" + jndiAdvice);
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
                    "Unusable " + this.confName + " setting.\n" + jndiAdvice +
                    "\n" + confadvice +
                    "\n** Problem: " + confInvalidError);
        }
    }
}
