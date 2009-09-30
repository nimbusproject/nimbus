/*
 * Copyright 1999-2006 University of Chicago
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

package org.globus.jython;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

import java.util.Properties;

/**
 * NOTE: this maintains ONE interpreter and therefore should not be used
 * simultaneously by more than one service at this time (environment will
 * be polluted unless this is coordinated).
 *
 * See the WorkspacePythonAuthorization class for a working usage (depending
 * on the intended usage, you should synchronize access to the interpreter).
 */
public class Jython {

    static Log logger =
        LogFactory.getLog(Jython.class.getName());

    private static PythonInterpreter interpreter;

    private static boolean limitEnvironment = true;

    public static void initialize() {
        initialize(limitEnvironment);
    }

    public static void initialize(boolean limitEnvironment) {
        if (interpreter != null) {
            return;
        }
        if (limitEnvironment) {
            PythonInterpreter.initialize(new Properties(),
                                         new Properties(),
                                         null);
        } else {
            // here would be code to get system properties (-D)
            // and to set python home etc.
        }
        interpreter = new PythonInterpreter();
        interpreter.set("hello", new PyString("hello from python"));
        logger.debug(interpreter.get("hello"));
    }

    public static PythonInterpreter getInterpreter() {
        if (interpreter == null) {
            logger.error("interpreter was null?");
            initialize();
        }
        return interpreter;
    }

}
