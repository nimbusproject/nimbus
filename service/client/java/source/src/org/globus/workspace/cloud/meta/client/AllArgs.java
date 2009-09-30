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

package org.globus.workspace.cloud.meta.client;

import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.ParameterProblem;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.EnumSet;

public class AllArgs {

    public enum Action { HELP, RUN };

    private final Print print;


    private final Set<Action> actions = EnumSet.noneOf(Action.class);


    private String clusterPath;
    private String deployPath;
    private int durationMinutes;
    private String propertiesPath;
    private String historyDirectory;
    private String cloudConfDir;

    public String getPropertiesPath() {
        return propertiesPath;
    }

    public String getHistoryDirectory() {
        return historyDirectory;
    }

    public String getCloudConfDir() {
        return cloudConfDir;
    }

    public Set<Action> getActions() {
        return actions;
    }

    public String getClusterPath() {
        return clusterPath;
    }

    public String getDeployPath() {
        return deployPath;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }


    public AllArgs(Print pr) {

        this.print = pr;

    }

    public void intakeCmdlineOptions(String[] args)
        throws ParameterProblem {

        final Options options = new Options();
        final Opts opts = new Opts();

        for (int i = 0; i < opts.ALL_ENABLED_OPTIONS.length; i++) {
            options.addOption(opts.ALL_ENABLED_OPTIONS[i]);
        }

        final CommandLineParser parser = new PosixParser();
        final CommandLine line;

        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            throw new ParameterProblem("Problem parsing parameters", e);
        }

        if (line.hasOption(Opts.HELP_OPT_STRING)) {
            this.actions.add(Action.HELP);
        }

        if (line.hasOption(Opts.RUN_OPT_STRING)) {
            this.actions.add(Action.RUN);
        }

        if (line.hasOption(Opts.CLUSTER_OPT_STRING)) {
            this.clusterPath =
                line.getOptionValue(Opts.CLUSTER_OPT_STRING);
        }

        if (line.hasOption(Opts.DEPLOY_OPT_STRING)) {
            this.deployPath =
                line.getOptionValue(Opts.DEPLOY_OPT_STRING);
        }

        if (line.hasOption(Opts.PROPFILE_OPT_STRING)) {
            this.propertiesPath =
                    line.getOptionValue(Opts.PROPFILE_OPT_STRING);
        }

        if (line.hasOption(Opts.HISTORY_DIR_OPT_STRING)) {
            this.historyDirectory =
                    line.getOptionValue(Opts.HISTORY_DIR_OPT_STRING);
        }

        if (line.hasOption(Opts.CLOUDDIR_OPT_STRING)) {
            this.cloudConfDir =
                line.getOptionValue(Opts.CLOUDDIR_OPT_STRING);
        }

        if (line.hasOption(Opts.HOURS_OPT_STRING)) {
            final String hoursString =
                    line.getOptionValue(Opts.HOURS_OPT_STRING);
            final double hours = Double.parseDouble(hoursString);
            final double minutesDouble = hours * 60;
            this.print.dbg("Duration minutes given: " + minutesDouble);

            // intentional loss of precision
            this.durationMinutes = (int) minutesDouble;
            this.print.dbg("Duration minutes used: " + this.durationMinutes);
        }

    }


}
