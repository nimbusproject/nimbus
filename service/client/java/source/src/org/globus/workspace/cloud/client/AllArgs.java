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

package org.globus.workspace.cloud.client;

import org.globus.workspace.common.print.Print;
import org.globus.workspace.common.client.CommonPrint;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.cloud.client.util.CloudClientUtil;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.CommandLine;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Enumeration;
import java.util.List;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;

public class AllArgs {

    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public AllArgs(Print print) {
        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        this.print = print;
    }

    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final Print print;

    private List actions = new ArrayList(8);

    /* ACTIONS: */
    public static final Integer ACTION_HELP = new Integer(0);
    public static final Integer ACTION_EXTRAHELP = new Integer(1);
    public static final Integer ACTION_TRANSFER = new Integer(2);
    public static final Integer ACTION_LIST = new Integer(3);
    public static final Integer ACTION_RUN = new Integer(4);
    public static final Integer ACTION_TARGET_PRINT = new Integer(5);
    public static final Integer ACTION_SERVICE_PRINT = new Integer(6);
    public static final Integer ACTION_SECURITY_PRINT = new Integer(7);
    public static final Integer ACTION_DESTROY = new Integer(8);
    public static final Integer ACTION_STATUS_CHECK = new Integer(9);
    public static final Integer ACTION_HASH_PRINT = new Integer(10);
    public static final Integer ACTION_SAVE = new Integer(11);
    public static final Integer ACTION_DELETE = new Integer(12);
    public static final Integer ACTION_DOWNLOAD = new Integer(13);
    public static final Integer ACTION_RUN_SINGLE = new Integer(14);
    public static final Integer ACTION_RUN_CLUSTER = new Integer(15);
    public static final Integer ACTION_ASSOC_QUERY = new Integer(16);
    public static final Integer ACTION_USAGE = new Integer(17);
    public static final Integer ACTION_EC2_CLUSTER = new Integer(18);

    // ------------------------------------

    private String caAppendDir;
    private String caHash;
    private String clusterPath;
    private String ec2ScriptPath;
    private int durationMinutes;
    private String eprGivenFilePath;
    private String factoryHostPort;
    private String factoryID;
    private String gridftpHostPort;
    private String gridftpID;
    private String handle;
    private String hashPrintDN;
    private String historyDirectory;
    private String historySubDir;
    private String localfile;
    private int memory;
    private String name;
    private String newname;
    private boolean noContextLock;
    private boolean useNotifications;
    private int pollMs;
    private boolean propagationKeepPort = true;
    private String propagationScheme;
    private String propertiesPath;
    private String sourcefile;
    private String sshfile;
    private String ssh_hostsfile;
    private String targetBaseDirectory;
    private String brokerURL;
    private String brokerID;
    private int timeoutMinutes;

    // ------------------------------------

    private String metadata_mountAs;
    private String metadata_association;
    private String metadata_nicName;
    private String metadata_cpuType;
    private String metadata_vmmVersion;
    private String metadata_vmmType;
    private String metadata_fileName;
    private String deploymentRequest_fileName;

    // ------------------------------------

    private String brokerPublicNicPrefix;
    private String brokerLocalNicPrefix;

    // ------------------------------------

    // set if something has already configured useNotifications
    private boolean useNotificationsConfigured;

    // set if something has already configured pollMs
    private boolean pollMsConfigured;

    // set if something has already configured timeoutMinutes
    private boolean timeoutMinutesConfigured;

    // set if something has already configured durationMinutes
    private boolean durationMinutesConfigured;

    // set if something has already configured memory
    private boolean memoryConfigured;

    // set if something has already configured propagationKeepPort
    private boolean propagationKeepPortConfigured;

    // set if user wants every ctx hostkey written to special subdir of history
    private boolean hostkeyDir;
    
    
    // -------------------------------------------------------------------------
    // INTAKE COMMANDLINES
    // -------------------------------------------------------------------------

    private void gotCmdLine(String optionName, String value) {
        this.print.dbg("[*] Set '" + optionName + "' from command line, " +
                       "value: '" + value + "'");
    }

    public void intakeCmdlineOptions(String[] args)

            throws ParameterProblem, ParseException {

        // (debug was fished out already)
        final String sectionTitle = "COMMANDLINE INTAKE";
        CommonPrint.printDebugSection(this.print, sectionTitle);

        final Options options = new Options();
        final Opts opts = new Opts();

        for (int i = 0; i < opts.ALL_ENABLED_OPTIONS.length; i++) {
            options.addOption(opts.ALL_ENABLED_OPTIONS[i]);
        }

        final CommandLineParser parser = new PosixParser();
        final CommandLine line = parser.parse(options, args);

        // note debug was already recognized and configured, keeping this
        // block here for completeness (and for gotCmdLine logging)
        if (line.hasOption(Opts.DEBUG_OPT_STRING)) {
            this.print.setDebugStream(System.out);
            this.gotCmdLine(Opts.DEBUG_OPT_STRING_LONG,
                            "enabled");
        }

        if (line.hasOption(Opts.BROKER_ID_OPT_STRING)) {
            this.brokerID =
                    line.getOptionValue(Opts.BROKER_ID_OPT_STRING);
            this.gotCmdLine(Opts.BROKER_ID_OPT_STRING,
                            this.brokerID);
        }

        if (line.hasOption(Opts.BROKER_URL_OPT_STRING)) {
            this.brokerURL =
                    line.getOptionValue(Opts.BROKER_URL_OPT_STRING);
            this.gotCmdLine(Opts.BROKER_URL_OPT_STRING,
                            this.brokerURL);
        }

        if (line.hasOption(Opts.CADIR_OPT_STRING)) {
            this.caAppendDir =
                    line.getOptionValue(Opts.CADIR_OPT_STRING);
            this.gotCmdLine(Opts.CADIR_OPT_STRING,
                            this.caAppendDir);
        }

        if (line.hasOption(Opts.CAHASH_OPT_STRING)) {
            this.caHash =
                    line.getOptionValue(Opts.CAHASH_OPT_STRING);
            this.gotCmdLine(Opts.CAHASH_OPT_STRING,
                            this.caHash);
        }

        if (line.hasOption(Opts.CLUSTER_OPT_STRING)) {
            this.clusterPath = line.getOptionValue(Opts.CLUSTER_OPT_STRING);
            this.gotCmdLine(Opts.CLUSTER_OPT_STRING,
                            this.clusterPath);
        }

        if (line.hasOption(Opts.EC2SCRIPT_OPT_STRING)) {
            this.ec2ScriptPath = line.getOptionValue(Opts.EC2SCRIPT_OPT_STRING);
            this.gotCmdLine(Opts.EC2SCRIPT_OPT_STRING,
                            this.ec2ScriptPath);
        }

        if (line.hasOption(Opts.ASSOC_QUERY_OPT_STRING)) {
            this.actions.add(ACTION_ASSOC_QUERY);
            this.gotCmdLine(Opts.ASSOC_QUERY_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.DELETE_OPT_STRING)) {
            this.actions.add(ACTION_DELETE);
            this.gotCmdLine(Opts.DELETE_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.DESTROY_OPT_STRING)) {
            this.actions.add(ACTION_DESTROY);
            this.gotCmdLine(Opts.DESTROY_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.DOWNLOAD_OPT_STRING)) {
            this.actions.add(ACTION_DOWNLOAD);
            this.gotCmdLine(Opts.DOWNLOAD_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.EPR_FILE_OPT_STRING)) {
            this.eprGivenFilePath =
                    line.getOptionValue(Opts.EPR_FILE_OPT_STRING);
            this.gotCmdLine(Opts.EPR_FILE_OPT_STRING,
                            this.eprGivenFilePath);
        }

        if (line.hasOption(Opts.EXTRAHELP_OPT_STRING)) {
            this.actions.add(ACTION_EXTRAHELP);
            this.gotCmdLine(Opts.EXTRAHELP_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.FACTORY_OPT_STRING)) {
            this.factoryHostPort =
                    line.getOptionValue(Opts.FACTORY_OPT_STRING);
            this.gotCmdLine(Opts.FACTORY_OPT_STRING,
                            this.factoryHostPort);
        }

        if (line.hasOption(Opts.FACTORY_ID_OPT_STRING)) {
            this.factoryID =
                    line.getOptionValue(Opts.FACTORY_ID_OPT_STRING);
            this.gotCmdLine(Opts.FACTORY_ID_OPT_STRING,
                            this.factoryID);
        }

        if (line.hasOption(Opts.GRIDFTP_OPT_STRING)) {
            this.gridftpHostPort =
                    line.getOptionValue(Opts.GRIDFTP_OPT_STRING);
            this.gotCmdLine(Opts.GRIDFTP_OPT_STRING,
                            this.gridftpHostPort);
        }

        if (line.hasOption(Opts.GRIDFTP_ID_OPT_STRING)) {
            this.gridftpID =
                    line.getOptionValue(Opts.GRIDFTP_ID_OPT_STRING);
            this.gotCmdLine(Opts.GRIDFTP_ID_OPT_STRING,
                            this.gridftpID);
        }

        if (line.hasOption(Opts.HANDLE_OPT_STRING)) {
            this.handle =
                    line.getOptionValue(Opts.HANDLE_OPT_STRING);
            this.gotCmdLine(Opts.HANDLE_OPT_STRING,
                            this.handle);
        }

        if (line.hasOption(Opts.HASH_PRINT_OPT_STRING)) {
            this.actions.add(ACTION_HASH_PRINT);
            this.hashPrintDN =
                    line.getOptionValue(Opts.HASH_PRINT_OPT_STRING);
            this.gotCmdLine(Opts.HASH_PRINT_OPT_STRING,
                            this.hashPrintDN);
        }

        if (line.hasOption(Opts.HELP_OPT_STRING)) {
            this.actions.add(ACTION_HELP);
            this.gotCmdLine(Opts.HELP_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.HISTORY_DIR_OPT_STRING)) {
            this.historyDirectory =
                    line.getOptionValue(Opts.HISTORY_DIR_OPT_STRING);
            this.gotCmdLine(Opts.HISTORY_DIR_OPT_STRING,
                            this.historyDirectory);
        }

        if (line.hasOption(Opts.HISTORY_SUBDIR_OPT_STRING)) {
            this.historySubDir =
                    line.getOptionValue(Opts.HISTORY_SUBDIR_OPT_STRING);
            this.gotCmdLine(Opts.HISTORY_SUBDIR_OPT_STRING,
                            this.historySubDir);
        }

        if (line.hasOption(Opts.HOSTKEYDIR_OPT_STRING)) {
            this.hostkeyDir = true;
            this.gotCmdLine(Opts.HOSTKEYDIR_OPT_STRING,
                            "enabled");
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

            this.durationMinutesConfigured = true;
            this.gotCmdLine(Opts.HOURS_OPT_STRING + " (converted to minutes)",
                            Integer.toString(this.durationMinutes));
        }

        if (line.hasOption(Opts.LIST_OPT_STRING)) {
            this.actions.add(ACTION_LIST);
            this.gotCmdLine(Opts.LIST_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.LOCAL_FILE_OPT_STRING)) {
            this.localfile = line.getOptionValue(Opts.LOCAL_FILE_OPT_STRING);
            this.gotCmdLine(Opts.LOCAL_FILE_OPT_STRING,
                            this.localfile);
        }

        if (line.hasOption(Opts.NAME_OPT_STRING)) {
            this.name = line.getOptionValue(Opts.NAME_OPT_STRING);
            this.gotCmdLine(Opts.NAME_OPT_STRING,
                            this.name);
        }

        if (line.hasOption(Opts.NEWNAME_OPT_STRING)) {
            this.newname = line.getOptionValue(Opts.NEWNAME_OPT_STRING);
            this.gotCmdLine(Opts.NEWNAME_OPT_STRING,
                            this.newname);
        }

        if (line.hasOption(Opts.NOCTXLOCK_OPT_STRING)) {
            this.noContextLock = true;
            this.gotCmdLine(Opts.NOCTXLOCK_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.NOTIFICATIONS_OPT_STRING)) {
            this.useNotifications = true;
            this.useNotificationsConfigured = true;
            this.gotCmdLine(Opts.NOTIFICATIONS_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.POLL_INTERVAL_OPT_STRING)) {
            final String msString =
                    line.getOptionValue(Opts.POLL_INTERVAL_OPT_STRING);
            this.pollMs = Integer.parseInt(msString);
            this.pollMsConfigured = true;
            this.gotCmdLine(Opts.POLL_INTERVAL_OPT_STRING,
                            msString);
        }

        if (line.hasOption(Opts.PRINT_TARGET_OPT_STRING)) {
            this.actions.add(ACTION_TARGET_PRINT);
            this.gotCmdLine(Opts.PRINT_TARGET_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.PROPFILE_OPT_STRING)) {
            this.propertiesPath =
                    line.getOptionValue(Opts.PROPFILE_OPT_STRING);
            this.gotCmdLine(Opts.PROPFILE_OPT_STRING,
                            this.propertiesPath);
        }

        if (line.hasOption(Opts.RUN_OPT_STRING)) {
            this.actions.add(ACTION_RUN);
            this.gotCmdLine(Opts.RUN_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.SAVE_OPT_STRING)) {
            this.actions.add(ACTION_SAVE);
            this.gotCmdLine(Opts.SAVE_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.SECURITY_OPT_STRING)) {
            this.actions.add(ACTION_SECURITY_PRINT);
            this.gotCmdLine(Opts.SECURITY_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.SOURCEFILE_OPT_STRING)) {
            this.sourcefile =
                    line.getOptionValue(Opts.SOURCEFILE_OPT_STRING);
            this.gotCmdLine(Opts.SOURCEFILE_OPT_STRING,
                            this.sourcefile);
        }

        if (line.hasOption(Opts.SSH_FILE_OPT_STRING)) {
            this.sshfile =
                    line.getOptionValue(Opts.SSH_FILE_OPT_STRING);
            this.gotCmdLine(Opts.SSH_FILE_OPT_STRING,
                            this.sshfile);
        }

        if (line.hasOption(Opts.STATUS_CHECK_OPT_STRING)) {
            this.actions.add(ACTION_STATUS_CHECK);
            this.gotCmdLine(Opts.STATUS_CHECK_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.TARGETDIR_OPT_STRING)) {
            this.targetBaseDirectory =
                    line.getOptionValue(Opts.TARGETDIR_OPT_STRING);
            this.gotCmdLine(Opts.TARGETDIR_OPT_STRING,
                            this.targetBaseDirectory);
        }

        if (line.hasOption(Opts.TIMEOUT_OPT_STRING)) {
            final String timeoutString =
                    line.getOptionValue(Opts.TIMEOUT_OPT_STRING);
            final double hours = Double.parseDouble(timeoutString);
            final double minutesDouble = hours * 60;
            this.print.dbg("Timeout minutes given: " + minutesDouble);

            // intentional loss of precision
            this.timeoutMinutes = (int) minutesDouble;
            this.print.dbg("Timeout minutes used: " + this.timeoutMinutes);

            this.timeoutMinutesConfigured = true;
            this.gotCmdLine(Opts.TIMEOUT_OPT_STRING,
                            Integer.toString(this.timeoutMinutes));
        }

        if (line.hasOption(Opts.TRANSFER_OPT_STRING)) {
            this.actions.add(ACTION_TRANSFER);
            this.gotCmdLine(Opts.TRANSFER_OPT_STRING,
                            "enabled");
        }

        if (line.hasOption(Opts.USAGE_OPT_STRING)) {
            this.actions.add(ACTION_USAGE);
            this.gotCmdLine(Opts.USAGE_OPT_STRING,
                            "enabled");
        }

        CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
    }


    // -------------------------------------------------------------------------
    // INTAKE PROPERTIES
    // -------------------------------------------------------------------------

    private void gotProp(String optionName, String value, String sourceName) {
        if (value == null) {
            return;
        }
        this.print.dbg("[*] Set '" + optionName + "' from " + sourceName +
                       ", value: '" + value + "'");
    }

    public void intakeUserProperties() throws ParameterProblem, IOException {

        final String sectionTitle = "USER SUPPLIED PROPERTIES";
        CommonPrint.printDebugSection(this.print, sectionTitle);

        if (this.propertiesPath == null) {
            this.print.dbg("user properties file not specified");
            return;
        }

        final File f = new File(this.propertiesPath);
        if (!CloudClientUtil.fileExistsAndReadable(f)) {
            throw new ParameterProblem(
                    "Properties file specified but file does not exist or " +
                            "is not readable: '" + this.propertiesPath + "'");
        }

        this.print.dbg("Loading supplied properties file: '" +
                               this.propertiesPath + "'\nAbsolute path: '" +
                               f.getAbsolutePath() + "'");

        InputStream is = null;
        final Properties userProps = new Properties();
        try {
            is = new FileInputStream(f);
            userProps.load(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }

        this.intakeProperties(userProps, "user-supplied properties");

        CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
    }

    public void intakeDefaultProperties() throws ParameterProblem,
                                                  IOException {

        final String sectionTitle = "DEFAULT PROPERTIES";
        CommonPrint.printDebugSection(this.print, sectionTitle);

        final Properties defaultProps = new Properties();

        InputStream is = null;
        try {
            is = this.getClass().getResourceAsStream("default.properties");
            if (is == null) {
                throw new IOException("Problem loading default properties");
            }
            defaultProps.load(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }

        this.intakeProperties(defaultProps, "default properties");

        CommonPrint.printDebugSectionEnd(this.print, sectionTitle);
    }

    private void intakeProperties(Properties props,
                                  String sourceName) throws ParameterProblem {

        this.print.dbg("\nAll properties in " + sourceName + " file:\n");
        final Enumeration e = props.keys();
        while (e.hasMoreElements()) {
            final String key = (String) e.nextElement();
            final String val = props.getProperty(key);
            this.print.dbg("  KEY  : " + key);
            this.print.dbg("  VALUE: " + val);
            this.print.dbg("  ");
        }
        this.print.flush();


        // null checks: don't use if option already set. This is how the config
        // precedence is implemented with strings.  With booleans and numbers
        // a matching "isConfigured" variable exists for each option.
        //
        // Config precedence reminder:  cmdline > user props > default props


        if (this.factoryHostPort == null) {
            this.factoryHostPort =
                    CloudClientUtil.getProp(props, Props.KEY_FACTORY_HOSTPORT);
            this.gotProp(Props.KEY_FACTORY_HOSTPORT,
                         this.factoryHostPort,
                         sourceName);
        }

        if (this.factoryID == null) {
            this.factoryID =
                    CloudClientUtil.getProp(props, Props.KEY_FACTORY_IDENTITY);
            this.gotProp(Props.KEY_FACTORY_IDENTITY,
                         this.factoryID,
                         sourceName);
        }

        if (this.gridftpHostPort == null) {
            this.gridftpHostPort =
                    CloudClientUtil.getProp(props, Props.KEY_GRIDFTP_HOSTPORT);
            this.gotProp(Props.KEY_GRIDFTP_HOSTPORT,
                         this.gridftpHostPort,
                         sourceName);
        }

        if (this.gridftpID == null) {
            this.gridftpID =
                    CloudClientUtil.getProp(props, Props.KEY_GRIDFTP_IDENTITY);
            this.gotProp(Props.KEY_GRIDFTP_IDENTITY,
                         this.gridftpID,
                         sourceName);
        }

        if (!this.memoryConfigured) {
            final String mem =
                    CloudClientUtil.getProp(props, Props.KEY_MEMORY_REQ);
            if (mem != null) {
                this.memory = Integer.parseInt(mem);
                if (this.memory < 1) {
                    throw new ParameterProblem("Configured memory (" + mem +
                            ") is invalid (less than one)");
                }
                this.memoryConfigured = true;
            }
        }

        if (this.sshfile == null) {
            this.sshfile =
                    CloudClientUtil.getProp(props, Props.KEY_SSHFILE);
            this.gotProp(Props.KEY_SSHFILE,
                         this.sshfile,
                         sourceName);
        }

        if (this.ssh_hostsfile == null) {
            this.ssh_hostsfile =
                    CloudClientUtil.getProp(props, Props.KEY_SSH_KNOWN_HOSTS);
            this.gotProp(Props.KEY_SSH_KNOWN_HOSTS,
                         this.ssh_hostsfile,
                         sourceName);
        }

        if (this.targetBaseDirectory == null) {
            this.targetBaseDirectory =
                    CloudClientUtil.getProp(props, Props.KEY_TARGET_BASEDIR);
            this.gotProp(Props.KEY_TARGET_BASEDIR,
                         this.targetBaseDirectory,
                         sourceName);
        }

        if (this.caHash == null) {
            this.caHash =
                    CloudClientUtil.getProp(props, Props.KEY_CAHASH);
            this.gotProp(Props.KEY_CAHASH,
                         this.caHash,
                         sourceName);
        }

        if (this.propagationScheme == null) {
            this.propagationScheme =
                    CloudClientUtil.getProp(props,
                                            Props.KEY_PROPAGATION_SCHEME);
            this.gotProp(Props.KEY_PROPAGATION_SCHEME,
                         this.propagationScheme,
                         sourceName);
        }

        if (!this.pollMsConfigured) {
            final String msString =
                    CloudClientUtil.getProp(props,
                                            Props.KEY_POLL_INTERVAL);
            if (msString != null) {
                this.pollMs = Integer.parseInt(msString);
                this.pollMsConfigured = true;
            }
        }

        if (!this.useNotificationsConfigured) {
            final String bull =
                    CloudClientUtil.getProp(props,
                                            Props.KEY_USE_NOTIFICATIONS);
            if (bull != null) {
                final Boolean setting = Boolean.valueOf(bull);
                this.useNotifications = setting.booleanValue();
                this.useNotificationsConfigured = true;

                this.gotProp(Props.KEY_USE_NOTIFICATIONS,
                             setting.toString(),
                             sourceName);
            }
        }

        if (!this.propagationKeepPortConfigured) {
            final String bull =
                    CloudClientUtil.getProp(props,
                                            Props.KEY_PROPAGATION_KEEPPORT);

            if (bull != null) {
                final Boolean setting = Boolean.valueOf(bull);
                this.propagationKeepPort = setting.booleanValue();
                this.propagationKeepPortConfigured = true;

                this.gotProp(Props.KEY_PROPAGATION_KEEPPORT,
                             setting.toString(),
                             sourceName);
            }
        }

        if (!this.timeoutMinutesConfigured) {

            final String timeoutString =
                    CloudClientUtil.getProp(props, Props.KEY_XFER_TIMEOUT);

            if (timeoutString != null) {
                final double hours = Double.parseDouble(timeoutString);
                final double minutesDouble = hours * 60;
                this.print.dbg("Timeout minutes from properties: " +
                                                                minutesDouble);

                // intentional loss of precision
                this.timeoutMinutes = (int) minutesDouble;
                this.print.dbg("Timeout minutes used: " + this.timeoutMinutes);

                this.timeoutMinutesConfigured = true;
                this.gotProp(Props.KEY_XFER_TIMEOUT,
                             Integer.toString(this.timeoutMinutes),
                             sourceName);
            }
        }

        // ----

        if (this.metadata_association == null) {
            final String key = Props.KEY_METADATA_ASSOCIATION;
            final String val = CloudClientUtil.getProp(props, key);
            this.metadata_association = val;
            this.gotProp(key, val, sourceName);
        }

        if (this.metadata_cpuType == null) {
            final String key = Props.KEY_METADATA_CPUTYPE;
            final String val = CloudClientUtil.getProp(props, key);
            this.metadata_cpuType = val;
            this.gotProp(key, val, sourceName);
        }

        if (this.metadata_fileName == null) {
            final String key = Props.KEY_METADATA_FILENAME;
            final String val = CloudClientUtil.getProp(props, key);
            this.metadata_fileName = val;
            this.gotProp(key, val, sourceName);
        }

        if (this.metadata_mountAs == null) {
            final String key = Props.KEY_METADATA_MOUNTAS;
            final String val = CloudClientUtil.getProp(props, key);
            this.metadata_mountAs = val;
            this.gotProp(key, val, sourceName);
        }

        if (this.metadata_nicName == null) {
            final String key = Props.KEY_METADATA_NICNAME;
            final String val = CloudClientUtil.getProp(props, key);
            this.metadata_nicName = val;
            this.gotProp(key, val, sourceName);
        }

        if (this.metadata_vmmType == null) {
            final String key = Props.KEY_METADATA_VMMTYPE;
            final String val = CloudClientUtil.getProp(props, key);
            this.metadata_vmmType = val;
            this.gotProp(key, val, sourceName);
        }

        if (this.metadata_vmmVersion == null) {
            final String key = Props.KEY_METADATA_VMMVERSION;
            final String val = CloudClientUtil.getProp(props, key);
            this.metadata_vmmVersion = val;
            this.gotProp(key, val, sourceName);
        }

        if (this.deploymentRequest_fileName == null) {
            final String key = Props.KEY_DEPREQ_FILENAME;
            final String val = CloudClientUtil.getProp(props, key);
            this.deploymentRequest_fileName = val;
            this.gotProp(key, val, sourceName);
        }

        // ----

        if (this.brokerPublicNicPrefix == null) {
            final String key = Props.KEY_BROKER_PUB;
            final String val = CloudClientUtil.getProp(props, key);
            this.brokerPublicNicPrefix = val;
            this.gotProp(key, val, sourceName);
        }

        if (this.brokerLocalNicPrefix == null) {
            final String key = Props.KEY_BROKER_LOCAL;
            final String val = CloudClientUtil.getProp(props, key);
            this.brokerLocalNicPrefix = val;
            this.gotProp(key, val, sourceName);
        }

        if (this.brokerURL == null) {
            final String key = Props.KEY_BROKER_URL;
            final String val = CloudClientUtil.getProp(props,key);
            this.brokerURL = val;
            this.gotProp(key, val, sourceName);
        }

        if (this.brokerID == null) {
            final String key = Props.KEY_BROKER_IDENTITY;
            final String val = CloudClientUtil.getProp(props,key);
            this.brokerID = val;
            this.gotProp(key, val, sourceName);
        }
    }

    
    // -------------------------------------------------------------------------
    // GET/SET
    // -------------------------------------------------------------------------

    public List getActions() {
        return this.actions;
    }

    public void setActions(List actionsList) {
        this.actions = actionsList;
    }

    public String getCaAppendDir() {
        return this.caAppendDir;
    }

    public void setCaAppendDir(String caAppendDir) {
        this.caAppendDir = caAppendDir;
    }

    public String getCaHash() {
        return this.caHash;
    }

    public void setCaHash(String caHash) {
        this.caHash = caHash;
    }

    public String getClusterPath() {
        return this.clusterPath;
    }

    public void setClusterPath(String clusterPath) {
        this.clusterPath = clusterPath;
    }

    public String getEc2ScriptPath() {
        return this.ec2ScriptPath;
    }

    public void setEc2ScriptPath(String ec2ScriptPath) {
        this.ec2ScriptPath = ec2ScriptPath;
    }

    public int getDurationMinutes() {
        return this.durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getEprGivenFilePath() {
        return this.eprGivenFilePath;
    }

    public void setEprGivenFilePath(String eprGivenFilePath) {
        this.eprGivenFilePath = eprGivenFilePath;
    }

    public String getFactoryHostPort() {
        return this.factoryHostPort;
    }

    public void setFactoryHostPort(String factoryHostPort) {
        this.factoryHostPort = factoryHostPort;
    }

    public String getFactoryID() {
        return this.factoryID;
    }

    public void setFactoryID(String factoryID) {
        this.factoryID = factoryID;
    }

    public String getGridftpHostPort() {
        return this.gridftpHostPort;
    }

    public void setGridftpHostPort(String gridftpHostPort) {
        this.gridftpHostPort = gridftpHostPort;
    }

    public String getGridftpID() {
        return this.gridftpID;
    }

    public void setGridftpID(String gridftpID) {
        this.gridftpID = gridftpID;
    }

    public String getHandle() {
        return this.handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getHashPrintDN() {
        return this.hashPrintDN;
    }

    public void setHashPrintDN(String hashPrintDN) {
        this.hashPrintDN = hashPrintDN;
    }

    public String getHistoryDirectory() {
        return this.historyDirectory;
    }

    public void setHistoryDirectory(String historyDirectory) {
        this.historyDirectory = historyDirectory;
    }

    public String getHistorySubDir() {
        return this.historySubDir;
    }

    public void setHistorySubDir(String historySubDir) {
        this.historySubDir = historySubDir;
    }

    public boolean isHostkeyDir() {
        return hostkeyDir;
    }

    public void setHostkeyDir(boolean hostkeyDir) {
        this.hostkeyDir = hostkeyDir;
    }

    public String getLocalfile() {
        return this.localfile;
    }

    public void setLocalfile(String localfile) {
        this.localfile = localfile;
    }

    public int getMemory() {
        return this.memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNewname() {
        return this.newname;
    }

    public void setNewname(String newname) {
        this.newname = newname;
    }

    public boolean isNoContextLock() {
        return this.noContextLock;
    }

    public void setNoContextLock(boolean noContextLock) {
        this.noContextLock = noContextLock;
    }

    public String getBrokerURL() {
        return this.brokerURL;
    }

    public void setBrokerURL(String brokerURL) {
        this.brokerURL = brokerURL;
    }

    public String getBrokerID() {
        return this.brokerID;
    }

    public void setBrokerID(String brokerID) {
        this.brokerID = brokerID;
    }

    public boolean isUseNotifications() {
        return this.useNotifications;
    }

    public void setUseNotifications(boolean useNotifications) {
        this.useNotifications = useNotifications;
    }

    public int getPollMs() {
        return this.pollMs;
    }

    public void setPollMs(int pollMs) {
        this.pollMs = pollMs;
    }

    public boolean isPropagationKeepPort() {
        return this.propagationKeepPort;
    }

    public void setPropagationKeepPort(boolean propagationKeepPort) {
        this.propagationKeepPort = propagationKeepPort;
    }

    public String getPropagationScheme() {
        return this.propagationScheme;
    }

    public void setPropagationScheme(String propagationScheme) {
        this.propagationScheme = propagationScheme;
    }

    public String getPropertiesPath() {
        return this.propertiesPath;
    }

    public void setPropertiesPath(String propertiesPath) {
        this.propertiesPath = propertiesPath;
    }

    public String getSourcefile() {
        return this.sourcefile;
    }

    public void setSourcefile(String sourcefile) {
        this.sourcefile = sourcefile;
    }

    public String getSshfile() {
        return this.sshfile;
    }

    public void setSshfile(String sshfile) {
        this.sshfile = sshfile;
    }

    public String getSsh_hostsfile() {
        return this.ssh_hostsfile;
    }

    public void setSsh_hostsfile(String ssh_hostsfile) {
        this.ssh_hostsfile = ssh_hostsfile;
    }

    public String getTargetBaseDirectory() {
        return this.targetBaseDirectory;
    }

    public void setTargetBaseDirectory(String targetBaseDirectory) {
        this.targetBaseDirectory = targetBaseDirectory;
    }

    public int getTimeoutMinutes() {
        return this.timeoutMinutes;
    }

    public void setTimeoutMinutes(int timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public String getMetadata_mountAs() {
        return this.metadata_mountAs;
    }

    public void setMetadata_mountAs(String metadata_mountAs) {
        this.metadata_mountAs = metadata_mountAs;
    }

    public String getMetadata_association() {
        return this.metadata_association;
    }

    public void setMetadata_association(String metadata_association) {
        this.metadata_association = metadata_association;
    }

    public String getMetadata_nicName() {
        return this.metadata_nicName;
    }

    public void setMetadata_nicName(String metadata_nicName) {
        this.metadata_nicName = metadata_nicName;
    }

    public String getMetadata_cpuType() {
        return this.metadata_cpuType;
    }

    public void setMetadata_cpuType(String metadata_cpuType) {
        this.metadata_cpuType = metadata_cpuType;
    }

    public String getMetadata_vmmVersion() {
        return this.metadata_vmmVersion;
    }

    public void setMetadata_vmmVersion(String metadata_vmmVersion) {
        this.metadata_vmmVersion = metadata_vmmVersion;
    }

    public String getMetadata_vmmType() {
        return this.metadata_vmmType;
    }

    public void setMetadata_vmmType(String metadata_vmmType) {
        this.metadata_vmmType = metadata_vmmType;
    }

    public String getMetadata_fileName() {
        return this.metadata_fileName;
    }

    public void setMetadata_fileName(String metadata_fileName) {
        this.metadata_fileName = metadata_fileName;
    }

    public String getDeploymentRequest_fileName() {
        return this.deploymentRequest_fileName;
    }

    public void setDeploymentRequest_fileName(
            String deploymentRequest_fileName) {
        this.deploymentRequest_fileName = deploymentRequest_fileName;
    }

    public boolean isUseNotificationsConfigured() {
        return this.useNotificationsConfigured;
    }

    public void setUseNotificationsConfigured(
            boolean useNotificationsConfigured) {
        this.useNotificationsConfigured = useNotificationsConfigured;
    }

    public boolean isPollMsConfigured() {
        return this.pollMsConfigured;
    }

    public void setPollMsConfigured(boolean pollMsConfigured) {
        this.pollMsConfigured = pollMsConfigured;
    }

    public boolean isTimeoutMinutesConfigured() {
        return this.timeoutMinutesConfigured;
    }

    public void setTimeoutMinutesConfigured(boolean timeoutMinutesConfigured) {
        this.timeoutMinutesConfigured = timeoutMinutesConfigured;
    }

    public boolean isDurationMinutesConfigured() {
        return this.durationMinutesConfigured;
    }

    public void setDurationMinutesConfigured(
            boolean durationMinutesConfigured) {
        this.durationMinutesConfigured = durationMinutesConfigured;
    }

    public boolean isMemoryConfigured() {
        return this.memoryConfigured;
    }

    public void setMemoryConfigured(boolean memoryConfigured) {
        this.memoryConfigured = memoryConfigured;
    }

    public boolean isPropagationKeepPortConfigured() {
        return this.propagationKeepPortConfigured;
    }

    public void setPropagationKeepPortConfigured(
            boolean propagationKeepPortConfigured) {
        this.propagationKeepPortConfigured = propagationKeepPortConfigured;
    }

    public String getBrokerPublicNicPrefix() {
        return this.brokerPublicNicPrefix;
    }

    public void setBrokerPublicNicPrefix(String brokerPublicNicPrefix) {
        this.brokerPublicNicPrefix = brokerPublicNicPrefix;
    }

    public String getBrokerLocalNicPrefix() {
        return this.brokerLocalNicPrefix;
    }

    public void setBrokerLocalNicPrefix(String brokerLocalNicPrefix) {
        this.brokerLocalNicPrefix = brokerLocalNicPrefix;
    }
}
