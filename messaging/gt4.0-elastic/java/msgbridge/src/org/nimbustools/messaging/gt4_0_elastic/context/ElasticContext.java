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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nimbustools.messaging.gt4_0.common.NimbusMasterContext;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceRM;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceGeneral;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceSecurity;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceImage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.globus.wsrf.jndi.Initializable;

import javax.naming.InitialContext;
import java.io.File;

public class ElasticContext implements Initializable {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(ElasticContext.class.getName());

    public static final String THIS_JNDI_LOOKUP =
            NimbusMasterContext.MASTER_JNDI_BASE + "elasticContext";

    private static final String ID_RM = "nimbus-elastic.rm";
    private static final String ID_GENERAL = "nimbus-elastic.general";
    private static final String ID_SECURITY = "nimbus-elastic.security";
    private static final String ID_IMAGE = "nimbus-elastic.image";


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private ApplicationContext appCtx;
    private String elasticConf;
    private boolean initialized;


    // -------------------------------------------------------------------------
    // SET
    // -------------------------------------------------------------------------

    public synchronized void setElasticConf(String conf) {
        this.elasticConf = conf;
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
        this.checkConf(this.elasticConf);

        // spring interprets "/" as a relative path unless it is "//"
        final String elasticCtxPath = this.fixAbsolute(this.elasticConf);

        // instantiate spring container
        this.appCtx = new FileSystemXmlApplicationContext(elasticCtxPath);
    }

    
    // -------------------------------------------------------------------------
    // FIND THINGS FOR CLASSES THAT CAN'T BE IN THE DI SYSTEM
    // -------------------------------------------------------------------------

    public synchronized ServiceRM findRM() throws Exception {

        if (this.appCtx == null) {
            throw new Exception("No elastic context was instantiated");
        }

        return (ServiceRM) this.appCtx.getBean(ID_RM);
    }

    public synchronized ServiceGeneral findGeneral() throws Exception {

        if (this.appCtx == null) {
            throw new Exception("No elastic context was instantiated");
        }

        return (ServiceGeneral) this.appCtx.getBean(ID_GENERAL);
    }

    public synchronized ServiceSecurity findSecurity() throws Exception {

        if (this.appCtx == null) {
            throw new Exception("No elastic context was instantiated");
        }

        return (ServiceSecurity) this.appCtx.getBean(ID_SECURITY);
    }

    public synchronized ServiceImage findImage() throws Exception {

        if (this.appCtx == null) {
            throw new Exception("No elastic context was instantiated");
        }

        return (ServiceImage) this.appCtx.getBean(ID_IMAGE);
    }


    // -------------------------------------------------------------------------
    // ELASTIC CONTEXT DISCOVERY
    // -------------------------------------------------------------------------

    /**
     * @return ElasticContext, never null
     * @throws Exception could not locate
     */
    public static ElasticContext discoverElasticContext() throws Exception {

        InitialContext ctx = null;
        try {
            ctx = new InitialContext();

            final ElasticContext masterContext =
                    (ElasticContext) ctx.lookup(THIS_JNDI_LOOKUP);

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
            "'elasticConf' near the top of a file usually located at " +
            "'$GLOBUS_LOCATION/etc/nimbus/jndi-config.xml'";

        final String confadvice =
            "** The 'elasticConf' parameter is usually set to a file like " +
            "'$GLOBUS_LOCATION/etc/nimbus/workspace-service/other/elastic.xml'" +
            ".  A configuration is present for this but it is not usable.";

        // -----------------------

        if (path == null) {
            throw new Exception("No elasticConf setting.\n" + jndiAdvice);
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
                    "Unusable elasticConf setting.\n" + jndiAdvice +
                    "\n" + confadvice +
                    "\n** Problem: " + confInvalidError);
        }
    }
}
