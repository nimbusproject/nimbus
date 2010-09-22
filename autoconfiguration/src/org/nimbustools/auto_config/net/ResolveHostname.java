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

package org.nimbustools.auto_config.net;

import java.net.InetAddress;

public class ResolveHostname {


    // -------------------------------------------------------------------------
    // LOOKUP
    // -------------------------------------------------------------------------

    public String lookup(String hostname) throws Exception {

        if (hostname == null || hostname.trim().length() == 0) {
            throw new Exception("no hostname given");
        }

        final InetAddress host = InetAddress.getByName(hostname);
        return host.getHostAddress();
    }


    // -------------------------------------------------------------------------
    // MAIN
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        if (args == null || args.length != 1) {
            System.err.println("Needs these arguments:\n" +
                    "1 - the hostname");
            System.exit(1);
        }

        try {
            ResolveHostname lookup = new ResolveHostname();
            System.out.println(lookup.lookup(args[0]));
        } catch (Exception e) {
            System.err.println(
                    "Problem with attempting hostname resolution: " + e.getMessage());
            System.exit(1);
        }
    }
}
