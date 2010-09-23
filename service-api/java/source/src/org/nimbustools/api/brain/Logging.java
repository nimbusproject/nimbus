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

/**
 * Hides logging dependencies, squelches the (very few) debug statements if
 * commons logging is not in the classpath.
 */
public class Logging {

    private static NimbusLog logger;
    private static boolean triedAndFailed;

    public synchronized static void debug(String debug) {

        if (triedAndFailed) {
            return;
        }
        
        if (logger == null) {
            try {
                logger = new NimbusLog();
            } catch (Throwable t) {
                triedAndFailed = true;
                return;
            }
        }

        logger.debug(debug);
    }
}
