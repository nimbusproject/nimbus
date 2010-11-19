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

package org.nimbustools.metadataserver.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.bio.SocketConnector;

import java.net.URL;
import java.util.Set;

public class HTTPListener {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(HTTPListener.class.getName());

    public static final String LOGGING_KEY =
            "org.mortbay.log.class";
    
    public static final String LOGGING_CLASS =
            "org.nimbustools.metadataserver.defaults.HTTPLogging";
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final Server server;
    private final URL[] sockets;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------


    public HTTPListener(Set<URL> listenSockets) {
        if (listenSockets == null) {
            throw new IllegalArgumentException("listenSockets may not be null");
        }

        this.sockets = listenSockets.toArray(new URL[listenSockets.size()]);
        if (this.sockets.length == 0) {
            throw new IllegalArgumentException("listenSockets cannot be empty");
        }

        this.server = new Server();
    }


    // -------------------------------------------------------------------------
    // ...
    // -------------------------------------------------------------------------

    public void initServer(AbstractHandler handler) {

        if (handler == null) {
            throw new IllegalArgumentException("handler may not be null");
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Metadata server socket").
                append(sockets.length > 1 ? "s: " : ": ");

        final Connector[] connectors = new Connector[sockets.length];
        for (int i = 0; i < sockets.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            final URL url = sockets[i];
            sb.append(url.toString());

            final Connector connector = new SocketConnector();
            connector.setHost(url.getHost());
            connector.setPort(url.getPort());
            connectors[i] = connector;
        }
        logger.info(sb.toString());

        System.setProperty(LOGGING_KEY, LOGGING_CLASS);

        this.server.setConnectors(connectors);
        this.server.setHandler(handler);
    }

    public void start() throws Exception {
        this.server.start();
    }

    public void stop() throws Exception {
        this.server.stop();
        this.server.join();
    }
}
