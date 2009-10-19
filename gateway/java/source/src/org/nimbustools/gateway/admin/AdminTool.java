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
package org.nimbustools.gateway.admin;

import org.apache.commons.cli.*;
import org.nimbustools.gateway.admin.commands.HelpCommand;
import org.nimbustools.gateway.admin.commands.AddUserCommand;
import org.nimbustools.gateway.accounting.manager.Accountant;

public class AdminTool {

    private static final int EXIT_OK = 0;
    private static final int EXIT_PARAM_ERROR = 1;
    private static final int EXIT_RUNTIME_ERROR = 2;


    public static void main(String[] args) {

        int retCode;

        try {
        retCode = new AdminTool().run(args) ;
        } catch (Throwable t) {
            System.out.println("Got uncaught runtime exception. This is a bug.");
            System.out.println(t.toString());
            retCode = EXIT_RUNTIME_ERROR;
        }

        System.exit(retCode);
    }


    Accountant accountant;

    public Accountant getAccountant() {
        return accountant;
    }

    public int run(String[] args) {

        // first get global options
        CommandLineParser parser = new PosixParser();
        final CommandLine line;
        try {
            line = parser.parse(getGlobalOptions(), args);
        } catch (ParseException e) {
            System.out.println("Parameter error: "+e.getMessage());
            printGeneralHelp();
            return EXIT_PARAM_ERROR;
        }

        // do some stuff based on those options??
        if (line.hasOption(Opts.HELP_STRING)) {
            printGeneralHelp();
            return EXIT_OK;
        }

        String[] extraArgs = line.getArgs();

        if (extraArgs.length == 0) {
            printGeneralHelp();
            return EXIT_PARAM_ERROR;
        }

        final String cmdName = extraArgs[0];
        final String[] cmdArgs = new String[extraArgs.length-1];
        System.arraycopy(extraArgs, 1, cmdArgs, 0, cmdArgs.length);

        final Command cmd = getCommandByName(cmdName);
        if (cmd == null) {
            System.out.println("Unknown command '"+cmdName+"'.");
            printGeneralHelp();
            return EXIT_PARAM_ERROR;
        }

        try {
            cmd.run(cmdArgs);
        } catch (ParameterProblem e) {
            System.out.println("Command parameter error: "+e.getMessage());
            printCommandHelp(cmdName);
            return EXIT_PARAM_ERROR;

        } catch (CommandProblem e) {
            System.out.println("Runtime error: "+e.getMessage());
            return EXIT_RUNTIME_ERROR;
        }
        return EXIT_OK;
    }

    private Options getGlobalOptions() {
        final Options opts = new Options();

        opts.addOption(Opts.getHelpOpt());

        return opts;
    }


    public Command getCommandByName(String cmd) {
        if (cmd.equalsIgnoreCase("help")) {
            return new HelpCommand(this);
        }

        if (cmd.equalsIgnoreCase("add")) {
            return new AddUserCommand(this);
        }
        return null;
    }


    private void printCommandHelp(String cmdName) {
        new HelpCommand(this).run(new String[] {cmdName});
    }


    private void printGeneralHelp() {
        new HelpCommand(this).run(new String[]{});
    }
    
}
