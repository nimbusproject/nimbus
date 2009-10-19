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

import org.nimbustools.gateway.admin.*;
import org.nimbustools.gateway.accounting.manager.Accountant;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.CommandLine;

public class AddUserCommand implements Command {
    private final AdminTool admin;

    public AddUserCommand(AdminTool admin) {
        if (admin == null) {
            throw new IllegalArgumentException("admin may not be null");
        }
        this.admin = admin;
    }

    public void run(String[] args) throws ParameterProblem, CommandProblem {

        final Accountant accountant = admin.getAccountant();
        final CommandLine line;
        try {
            PosixParser parser = new PosixParser();
            line = parser.parse(getOptions(), args, false);
        } catch (ParseException e) {
            throw new ParameterProblem("Problem parsing 'add' arguments: "+
                    e.getMessage(), e);
        }

        if (line.getArgs().length == 0) {
            throw new ParameterProblem("DN must be specified");
        }

        final String dn = line.getArgs()[0];

        if (line.hasOption(Opts.MAX_CREDITS_STRING)) {
            String maxCreditsStr = line.getOptionValue(Opts.MAX_CREDITS_STRING);
            final int maxCredits;

            try {
            maxCredits = Integer.parseInt(maxCreditsStr);
            } catch (NumberFormatException e) {
                throw new ParameterProblem("max credits must be an integer");
            }

            accountant.addLimitedAccount(dn, maxCredits);

        } else {
            accountant.addUnlimitedAccount(dn);
        }
    }

    public String getName() {
        return "add";
    }

    public String getDescription() {
        return "Authorize a new user for gateway";
    }

    public String getUsage() {
        return "add DN [--max-credits MAX] [--ec2 ACCESS_ID]";
    }

    private Options getOptions() {
        final Options opts = new Options();

        opts.addOption(Opts.getMaxCreditsOpt());
        opts.addOption(Opts.getEc2Opt());

        return opts;
    }
}
