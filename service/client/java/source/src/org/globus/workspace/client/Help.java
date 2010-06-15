/*
 * Copyright 1999-2007 University of Chicago
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

package org.globus.workspace.client;

import org.globus.workspace.common.Version;
import org.globus.workspace.client_core.utils.StringUtils;

import java.io.InputStream;
import java.io.IOException;

/**
 * The help printing via BaseClient and commons CLI is not helpful.
 */
public class Help {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    public static final String FIRST_LINE =
            "Workspace Service CLI, " + Version.getVersion() +
                    " - http://www.nimbusproject.org\n";


    // -------------------------------------------------------------------------
    // GET HELP STRINGS
    // -------------------------------------------------------------------------

    public String getHelpString() throws IOException {
        return this.getStringFromJar("client-help.txt");
    }

    public String getExtraHelpString() throws IOException {
        return this.getStringFromJar("client-help-extra.txt");
    }

    public String getModeHelpString(AllArguments args) throws IOException {

        if (args == null) {
            return null;
        }

        final String filename;
        if (args.mode_deploy) {
            filename = "client-help-mode-deploy.txt";
        } else if (args.mode_subscribe) {
            filename = "client-help-mode-subscribe.txt";
        } else if (args.mode_factoryRpQuery) {
            filename = "client-help-mode-factoryrp.txt";
        } else if (args.mode_destroy) {
            filename = "client-help-mode-destroy.txt";
        } else if (args.mode_pause) {
            filename = "client-help-mode-shared1.txt";
        } else if (args.mode_reboot) {
            filename = "client-help-mode-shared1.txt";
        } else if (args.mode_rpquery) {
            filename = "client-help-mode-shared1.txt";
        } else if (args.mode_shutdown) {
            filename = "client-help-mode-shared1.txt";
        } else if (args.mode_shutdown_save) {
            filename = "client-help-mode-shutdownsave.txt";
        } else if (args.mode_doneEnsemble) {
            filename = "client-help-mode-done.txt";
        } else if (args.mode_monitorEnsemble) {
            filename = "client-help-mode-ensmonitor.txt";
        } else if (args.mode_monitorContext) {
            filename = "client-help-mode-ctxmonitor.txt";
        } else if (args.mode_createContext) {
            filename = "client-help-mode-ctxcreate.txt";
        } else if (args.mode_createInjectableContext) {
            filename = "client-help-mode-ctxcreate-injectable.txt";
        } else if (args.mode_noMoreContextInjections) {
            filename = "client-help-mode-ctx-no-more.txt";
        } else if (args.mode_injectContextData) {
            filename = "client-help-mode-ctxdata.txt";
        } else if (args.mode_start) {
            filename = "client-help-mode-shared1.txt";
        } else if (args.mode_ctxPending) {
            filename = "client-help-mode-ctxpendingprint.txt";
        } else {
            return null;
        }

        return this.getStringFromJar(filename);
    }

    private String getStringFromJar(String path) throws IOException {
        InputStream is = null;
        try {
            is = this.getClass().getResourceAsStream(path);
            if (is == null) {
                return "Sorry, cannot find '" + path + "' in the jar file " +
                        "alongside " + this.getClass();
            }
            return StringUtils.getTextFileViaInputStream(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
}
