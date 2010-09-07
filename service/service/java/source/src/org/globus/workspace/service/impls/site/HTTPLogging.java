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

package org.globus.workspace.service.impls.site;

import org.mortbay.log.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Suppresses jetty INFO level log statements and translates its debugs into
 * trace statements (there are a lot of them).
 *
 * TODO: In the past this used the static 'lager' mechanisms for translating
 *       jetty debug statements into trace statements.  There is no good way
 *       to get the new instance based 'lager' mechanism into this class yet.
 *       For now, trace is disabled here unless recompiling :-\
 */
public class HTTPLogging implements Logger {

    private static final Log logger =
            LogFactory.getLog(HTTPLogging.class.getName());

    public static final boolean TRACE_ENABLED = false;

    // translate this into is trace enabled
    public boolean isDebugEnabled() {
        return TRACE_ENABLED;
    }

    public void setDebugEnabled(boolean b) {
        logger.debug("ignoring Jetty set-debug; does it want enabled? " + b);
    }

    public void info(String s, Object o, Object o1) {
        // disabled
    }

    public void debug(String s, Throwable throwable) {
        if (TRACE_ENABLED) {
            logger.trace(s, throwable);
        }
    }

    public void debug(String s, Object o, Object o1) {
        if (TRACE_ENABLED) {
            logger.trace(s);
        }
    }

    public void warn(String s, Object o, Object o1) {
        logger.warn(s);
    }

    public void warn(String s, Throwable throwable) {
        logger.warn(s, throwable);
    }

    public Logger getLogger(String s) {
        return this;
    }
}
