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
package org.nimbustools.messaging.query;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.filter.DelegatingFilterProxy;

import java.util.HashMap;
import java.util.Map;

public class HttpQuery {

    private boolean enabled;
    private int port;
    private int headerBufferBytes;
    private String springConfig;
    private Server server;
    private String keystoreLocation;
    private String keystorePassword;

    public synchronized void startListening() throws Exception {

        if (!enabled) {
            return;
        }

        if (port <= 0) {
             throw new IllegalStateException("port is invalid");
        }
        if (springConfig == null) {
            throw new IllegalStateException("springConfig may not be null");
        }

        if (keystoreLocation == null) {
            throw new IllegalStateException("keystoreLocation may not be null");
        }

        if (keystorePassword == null) {
            throw new IllegalStateException("keystorePassword may not be null");
        }


        this.server = new Server();

        SslSocketConnector sslConnector = new SslSocketConnector();
        sslConnector.setPort(port);
        if (this.headerBufferBytes > 0) {
            sslConnector.setHeaderBufferSize(this.headerBufferBytes);
        }
        sslConnector.setKeystore(keystoreLocation);
        sslConnector.setKeyPassword(keystorePassword);
        sslConnector.setPassword(keystorePassword);
        sslConnector.setTruststore(keystoreLocation);
        sslConnector.setTrustPassword(keystorePassword);
        server.setConnectors(new Connector[] {sslConnector});

        Context context = new Context(server, "/",  Context.NO_SESSIONS);
        Map<String, String> initParams = new HashMap<String,String>();
        initParams.put("contextConfigLocation", springConfig);
        context.setInitParams(initParams);
        context.addEventListener(new ContextLoaderListener());
        FilterHolder filterHolder = new FilterHolder(new DelegatingFilterProxy());
        filterHolder.setName("filterChainProxy");
        context.addFilter(filterHolder, "/*", Handler.DEFAULT);
        ServletHolder servletHolder = new ServletHolder(new CXFServlet());
        servletHolder.setInitOrder(1);
        servletHolder.setName("CXFServlet");
        servletHolder.setDisplayName("CXF Servlet");
        context.addServlet(servletHolder, "/*");
        WebAppContext webappcontext = new WebAppContext();
        webappcontext.setContextPath("/");
        server.start();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getHeaderBufferBytes() {
        return headerBufferBytes;
    }

    public void setHeaderBufferBytes(int headerBufferBytes) {
        this.headerBufferBytes = headerBufferBytes;
    }

    public String getSpringConfig() {
        return springConfig;
    }

    public void setSpringConfig(String springConfig) {
        this.springConfig = springConfig;
    }

    public String getKeystoreLocation() {
        return this.keystoreLocation;
    }

    public void setKeystoreLocation(String keystoreLocation) {
        this.keystoreLocation  = keystoreLocation;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public boolean isRunning() {
        return server != null && server.isRunning();
    }
}
