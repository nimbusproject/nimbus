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

package org.nimbustools.auto_config.groupauthz.harness;

public class DeleteDN extends AddDeleteCommon {

    public DeleteDN(String confPath, String[] args, boolean debug)
            throws Exception {
        super(confPath, args, debug);
    }

    protected String dnToDelete() throws Exception {
        return this.dnInQuestion("delete");
    }

    public void run() throws Exception {
        final String dn = this.dnToDelete();
        System.out.println("Deleting '" + dn + "'");
        if (!this.deleteAllInstances(dn, this.getGroups())) {
            System.out.println("\nNothing removed, DN not found.");
        }
    }
}
