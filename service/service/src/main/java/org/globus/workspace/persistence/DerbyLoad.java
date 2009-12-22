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

package org.globus.workspace.persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * If "derby.system.home" is not registered as a System property, this class
 * will register it with the configured value.
 */
public class DerbyLoad implements DBLoader {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------
    
    private static final Log logger =
            LogFactory.getLog(DerbyLoad.class.getName());

    public static final String DERBY_HOME_PROP_KEY = "derby.system.home";


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private String derbySystemHome;
    private boolean loaded;

    
    // -------------------------------------------------------------------------
    // CONFIG SET
    // -------------------------------------------------------------------------
    
    public void setDerbySystemHome(String derbySystemHome) {
        this.derbySystemHome = derbySystemHome;
    }


    // -------------------------------------------------------------------------
    // implements DBLoader
    // -------------------------------------------------------------------------
    
    public boolean isLoaded() {
        return this.loaded;
    }

    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    public synchronized void setDerbySystemProperty() throws Exception {
        // don't overwrite if defined elsewhere
        if (System.getProperty(DERBY_HOME_PROP_KEY) == null) {
            if (this.derbySystemHome != null) {
                System.setProperty(DERBY_HOME_PROP_KEY, this.derbySystemHome);
            }
        }

        this.loaded = true;

        if (logger.isDebugEnabled()) {
            final String val = System.getProperty(DERBY_HOME_PROP_KEY);
            logger.debug(DERBY_HOME_PROP_KEY + " = '" + val + "'");
        }
    }
}
