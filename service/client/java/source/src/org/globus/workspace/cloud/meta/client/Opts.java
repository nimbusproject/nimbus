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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

public class Opts extends org.globus.workspace.cloud.client.Opts {

    public static final String DEPLOY_OPT_STRING = "deploy";
    public final Option DEPLOY_OPT =
        OptionBuilder.hasArg().withLongOpt(DEPLOY_OPT_STRING).create();

    public static final String CLOUD_DIR_OPT_STRING = "cloud-dir";
    public final Option CLOUDDIR_OPT =
        OptionBuilder.hasArg().withLongOpt(CLOUD_DIR_OPT_STRING).create();

    public final Option[] ALL_ENABLED_OPTIONS = { this.HELP_OPT,
        this.RUN_OPT, this.CLUSTER_OPT, this.DEPLOY_OPT, this.PROPFILE_OPT,
        this.CLOUDDIR_OPT, this.HISTORY_DIR_OPT, this.HOURS_OPT,
    };
}
