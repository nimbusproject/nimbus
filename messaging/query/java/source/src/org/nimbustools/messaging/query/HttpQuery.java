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
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.io.IOException;

public class HttpQuery implements ApplicationContextAware {

    private boolean enabled;
    private int port;
    private int headerBufferBytes;
    private String springConfig;
    private Server server;
    private String keystoreLocation;
    private String keystorePassword;
    private ApplicationContext appContext;

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

        ServletHolder servletHolder = new ServletHolder(new CXFServlet());
        servletHolder.setInitOrder(1);
        servletHolder.setName("CXFServlet");
        servletHolder.setDisplayName("CXF Servlet");
        
        WebAppContext webappcontext = new WebAppContext();
        webappcontext.setContextPath("/");

        // This sets up the jetty server inside a preexisting Spring context
        final GenericWebApplicationContext gwac = new GenericWebApplicationContext();
        gwac.setServletContext(webappcontext.getServletContext());
        gwac.setParent(appContext);
        final String rootweb = WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE;
        webappcontext.getServletContext().setAttribute(rootweb, gwac);
        gwac.refresh();
        webappcontext.setServletHandler(servletHolder.getServletHandler());
        server.addHandler(webappcontext);
        
        server.start();
    }

    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.appContext = applicationContext;
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

    public void setSpringConfigResource(Resource springConfigResource) throws IOException {
        this.springConfig = "file://" + springConfigResource.getFile().getAbsolutePath();
    }

    public void setKeystoreResource(Resource keystoreResource) throws IOException {
        this.keystoreLocation = keystoreResource.getFile().getAbsolutePath();
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
