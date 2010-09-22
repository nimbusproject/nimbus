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

package org.nimbustools.auto_config.confmgr;

public class AlterControlUser extends AlterCommon {

    public void alterControlUser(String newvalue) throws Exception {
        if (newvalue == null || newvalue.trim().length() == 0) {
            throw new Exception(
                    "New control SSH user value may not be empty");
        }
        this.alter(ConfConstants.CONF_SSH,
                   ConfConstants.KEY_SSH_CONTROLUSER,
                   newvalue);
    }

    public static void mainImpl(String[] args) throws Exception {
        if (args == null || args.length != 1) {
            throw new Exception("Needs these arguments:\n" +
                    "1 - the new control user value (never missing)");
        }
        new AlterControlUser().
                    alterControlUser(args[0]);
    }

    public static void main(String[] args) {
        try {
            mainImpl(args);
        } catch (Throwable t) {
            System.err.println("Problem: " + t.getMessage());
            System.exit(1);
        }
    }
}
