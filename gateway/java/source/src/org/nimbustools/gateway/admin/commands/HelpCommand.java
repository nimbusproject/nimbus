/*
 * Copyright 1999-2009 University of Chicago
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
package org.nimbustools.gateway.admin.commands;

import org.nimbustools.gateway.admin.Command;
import org.nimbustools.gateway.admin.ParameterProblem;
import org.nimbustools.gateway.admin.CommandProblem;
import org.nimbustools.gateway.admin.AdminTool;

public class HelpCommand implements Command {

    public static final String NAME = "help";

    private final AdminTool admin;

    public HelpCommand(AdminTool admin) {
        if (admin == null) {
            throw new IllegalArgumentException("admin may not be null");
        }
        this.admin = admin;
    }

    public void run(String[] args) {

        if (args.length == 0) {
            System.out.println(getProgramUsage());
            return;
        }

        Command cmd = admin.getCommandByName(args[0]);
        if (cmd == null) {
            System.out.println("Unknown command: "+args[0]);
            System.out.println(getProgramUsage());
            return;
        }

        System.out.println(cmd.getName() + ": "+cmd.getDescription());
        System.out.println("Usage: "+cmd.getUsage());


    }
    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Describe the usage of program or subcommands";
    }
    public String getUsage() {
        return "help [command]";
    }

    private String getProgramUsage() {
        return ""; // TODO
    }
}
