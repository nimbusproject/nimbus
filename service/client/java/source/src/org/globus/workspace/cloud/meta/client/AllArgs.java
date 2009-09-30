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
import org.globus.workspace.cloud.client.util.CloudClientUtil;
import org.globus.workspace.cloud.client.Props;
import org.globus.workspace.cloud.client.CloudClient;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.util.Set;
import java.util.EnumSet;
import java.util.Properties;
import java.io.*;

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
    private int pollMs;
    private String brokerURL;
    private String brokerID;
    private String sshfile;

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

    public String getBrokerID() {
        return brokerID;
    }

    public String getBrokerURL() {
        return brokerURL;
    }

    public int getPollMs() {
        return pollMs;
    }

    public String getSshfile() {
        return sshfile;
    }


    public static AllArgs create(String argv[], Print print)
        throws ParameterProblem {

        AllArgs args = new AllArgs(print);
        args.intakeOptions(argv);
        return args;
    }

    private AllArgs(Print pr) {
        this.print = pr;
    }

    private void intakeOptions(String[] args)
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

        // first, parse out actions

        if (line.hasOption(Opts.RUN_OPT_STRING)) {
            this.actions.add(Action.RUN);
        }
        if (line.hasOption(Opts.HELP_OPT_STRING)) {
            this.actions.add(Action.HELP);

            // don't bother further parsing if help is specified
            return;
        }

        // look for propfile option next and load it up
        if (line.hasOption(Opts.PROPFILE_OPT_STRING)) {
            this.propertiesPath =
                    line.getOptionValue(Opts.PROPFILE_OPT_STRING);
            try {
                this.intakePropertiesFile(this.propertiesPath);
            } catch (IOException e) {
                throw new ParameterProblem("Failed to load properties file '"+
                    this.propertiesPath+"'", e);
        }
        }

        // now everything else. Note that these params may override
        // some that were just taken in from config file

        if (line.hasOption(Opts.CLUSTER_OPT_STRING)) {
            this.clusterPath =
                line.getOptionValue(Opts.CLUSTER_OPT_STRING);
        }

        if (line.hasOption(Opts.DEPLOY_OPT_STRING)) {
            this.deployPath =
                line.getOptionValue(Opts.DEPLOY_OPT_STRING);
        }

        if (line.hasOption(Opts.HISTORY_DIR_OPT_STRING)) {
            this.historyDirectory =
                    line.getOptionValue(Opts.HISTORY_DIR_OPT_STRING);
        }

        if (line.hasOption(Opts.CLOUD_DIR_OPT_STRING)) {
            this.cloudConfDir =
                line.getOptionValue(Opts.CLOUD_DIR_OPT_STRING);
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

    private void intakePropertiesFile(String propPath) throws ParameterProblem, IOException {
        final File f = new File(propPath);
        if (!CloudClientUtil.fileExistsAndReadable(f)) {
            throw new ParameterProblem(
                    "Properties file specified but file does not exist or " +
                            "is not readable: '" + this.propertiesPath + "'");
        }

        this.print.dbg("Loading supplied properties file: '" +
                               this.propertiesPath + "'\nAbsolute path: '" +
                               f.getAbsolutePath() + "'");

        Properties defaultProps = loadDefaultProperties();

        InputStream is = null;
        final Properties userProps = new Properties(defaultProps);
        try {
            is = new FileInputStream(f);
            userProps.load(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        intakeProperties(userProps);
    }

    private Properties loadDefaultProperties() throws IOException {
        Properties defaultProps = new Properties();
        InputStream is = null;
        try {
            is = CloudClient.class.getResourceAsStream("default.properties");
            if (is == null) {
                throw new IOException("Problem loading default properties");
            }
            defaultProps.load(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return defaultProps;
    }

    private void intakeProperties(Properties props) throws ParameterProblem {

        this.brokerURL = CloudClientUtil.getProp(props, Props.KEY_BROKER_URL);
        this.brokerID = CloudClientUtil.getProp(props, Props.KEY_BROKER_IDENTITY);

        this.sshfile = CloudClientUtil.getProp(props, Props.KEY_SSHFILE);
        if (this.sshfile != null) {
            this.sshfile = CloudClientUtil.expandSshPath(this.sshfile);
        }


        String pollMsStr = CloudClientUtil.getProp(props, Props.KEY_POLL_INTERVAL);
        this.pollMs = Integer.parseInt(pollMsStr);

    }


}
