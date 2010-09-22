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
import java.net.Socket;

public class NetworkPing {

    // -------------------------------------------------------------------------
    // PING
    // -------------------------------------------------------------------------

    public boolean ping(String hostname, String portStr) throws Exception {

        if (hostname == null || hostname.trim().length() == 0) {
            throw new Exception("no hostname given");
        }

        if (portStr == null || portStr.trim().length() == 0) {
            throw new Exception("no portStr given");
        }

        int port = Integer.parseInt(portStr);

        if (port < 1 || port > 65535) {
            throw new Exception(
                    "port number seems out of legal range: " + port);
        }

        final InetAddress host = InetAddress.getByName(hostname);

        Socket socket = null;
        try {
            socket = new Socket(host, port);
        } catch (Exception e) {
            System.err.println("** Problem connecting to " + host + ":" +
                    port + " -- " + e.getMessage());
            return false;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }

        return true;
    }


    // -------------------------------------------------------------------------
    // MAIN
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Needs these arguments:\n" +
                    "1 - the hostname\n" +
                    "2 - the port");
            System.exit(1);
        }

        try {
            NetworkPing ping = new NetworkPing();
            if (ping.ping(args[0], args[1])) {
                System.exit(0);
            } else {
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println(
                    "Problem with attempting network ping: " + e.getMessage());
            System.exit(1);
        }
    }

}
