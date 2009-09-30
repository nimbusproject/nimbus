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
import java.net.MalformedURLException;

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
    private final String listenSocket;
    private URL url;
    

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public HTTPListener(String listenSocket) throws Exception {

        if (listenSocket == null || listenSocket.trim().length() == 0) {
            logger.warn("listenSocket setting is empty or missing");
        }

        this.listenSocket = listenSocket;
        this.server = new Server();
    }


    // -------------------------------------------------------------------------
    // ...
    // -------------------------------------------------------------------------

    public URL getURL() {
        return this.url;
    }

    public void initServer(AbstractHandler handler) throws MalformedURLException {

        // hardcoding http here on purpose, to make it obvious that https is
        // not supported if someone tries to put more than a host+port in the
        // configuration
        this.url = new URL("http://" + this.listenSocket);

        logger.info("Metadata server URL: '" + this.url.toString() + "'");

        System.setProperty("org.mortbay.log.class", LOGGING_CLASS);

        final Connector connector = new SocketConnector();
        connector.setHost(url.getHost());
        connector.setPort(url.getPort());
        this.server.setConnectors(new Connector[]{connector});
        this.server.setHandler(handler);
    }

    public void start() throws Exception {
        this.server.start();
    }
}
