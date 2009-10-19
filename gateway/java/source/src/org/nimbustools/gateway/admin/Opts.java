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

public class Opts {
    public static final String HELP_STRING = "help";
    public static Option getHelpOpt() {
        return new Option("h", HELP_STRING, false,
            "Display usage information");
    }

    public static final String MAX_CREDITS_STRING = "max-credits";
    public static Option getMaxCreditsOpt() {
        return OptionBuilder
                .withArgName("MAX")
                .hasArg()
                .withLongOpt(MAX_CREDITS_STRING)
                .create();
    }

    public static final String EC2_STRING = "ec2";
    public static Option getEc2Opt() {
        return OptionBuilder
                .withArgName("access ID")
                .hasOptionalArg()
                .withLongOpt(EC2_STRING)
                .create();
    }

}
