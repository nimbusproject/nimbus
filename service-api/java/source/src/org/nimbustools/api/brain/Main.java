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

package org.nimbustools.api.brain;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;

import java.net.URL;

/**
 * Something you can run standalone from IDE/console to run the API without
 * any messaging layer at all.
 */
public class Main {
    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final String LOG4J_KEY = "log4j.configuration";
    private static final String DEFAULT_LOG4J =
            "org/nimbustools/api/defaults/log4j.properties";

    
    // -------------------------------------------------------------------------
    // STANDALONE
    // -------------------------------------------------------------------------
    
    public static void main(String[] args) {
        logImpl();
        new BreathOfLife().breathe(null);
    }


    // -------------------------------------------------------------------------
    // LOGGING
    // -------------------------------------------------------------------------

    // TODO: looking at internal log system based on SLF4J (and maybe logback),
    //       esp. for MDC support (Mapped Diagnostic Context)
    private static void logImpl() {
        final String config = System.getProperty(LOG4J_KEY);
        if (config == null) {
            final URL loadURL = Loader.getResource(DEFAULT_LOG4J);
            if (loadURL != null) {
                PropertyConfigurator.configure(loadURL);
            } else {
                System.err.println("could not locate '" + DEFAULT_LOG4J + "'");
            }
        } else {
            // do nothing.
            Logging.debug("logging was configured via -D" + LOG4J_KEY);
        }
    }
}
