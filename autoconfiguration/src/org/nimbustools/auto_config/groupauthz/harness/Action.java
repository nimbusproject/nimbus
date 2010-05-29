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

import org.springframework.context.ApplicationContext;
import org.globus.workspace.groupauthz.GroupAuthz;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class Action {

    protected final String SPRING_ID =
            "nimbus-rm.service.binding.AuthorizationCallout";
    
    protected final String confPath;
    protected final String[] args;
    protected final GroupAuthz groupAuthz;
    protected final boolean debug;

    public Action(String confPath, String[] args, boolean debug) throws Exception {
        if (confPath == null) {
            throw new IllegalArgumentException("confPath may not be null");
        }
        if (args == null) {
            throw new IllegalArgumentException("args may not be null");
        }
        this.confPath = confPath;
        this.args = args;
        this.debug = debug;

        // NOT using NimbusFileSystemXmlApplicationContext, absolute path expected from cmdline
        final ApplicationContext ctx =
                new FileSystemXmlApplicationContext(this.confPath);

        try {
            this.groupAuthz = (GroupAuthz) ctx.getBean(SPRING_ID);
        } catch (Throwable t) {
            if (this.debug) {
                System.err.println("");
                if (t instanceof ClassCastException) {
                    System.err.println("Wrong class is defined:");
                }
                System.err.println(t.getMessage());
                System.err.println("");
                System.err.println("STACKTRACE:");
                t.printStackTrace(System.err);
                System.err.println("");
            }
            throw new Exception("Could not find a proper GroupAuthz " +
                    "module definition in this file:\n   '" + confPath +
                    "'\n... did you enable the group authorization module?" +
                    "\n... see -h");
        }
    }
}
