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

package org.globus.workspace.service.impls.site;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.security.HashUserRealm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;

import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;

/**
 * Consumes notifications from site entities.  Only authentication mechanism
 * supported is digest access authentication.
 */
public class HTTPListener {

    private static final Log logger =
            LogFactory.getLog(HTTPListener.class.getName());

    public static final String LOGGING_CLASS =
            "org.globus.workspace.service.impls.site.HTTPLogging";

    public final static String pilotAccountName = "pilotaccount";

    public final static int minPasswordLength = 15;
    
    // takes ",pilotrole" into account
    // (need to see how that can be avoided entirely)
    public final static int minValueLength = 25;

    private final Server server;
    private final String contactURL;
    private final Lager lager;

    public HTTPListener(String contactPort,
                        String accountsPath,
                        SlotPollCallback slotcall,
                        ExecutorService execService,
                        Lager lagerImpl) throws Exception {

        if (slotcall == null) {
            throw new IllegalArgumentException("slotcall is null");
        }
        
        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;

        if (contactPort == null || contactPort.trim().length() == 0) {
            throw new Exception("contactPort setting is empty or missing");
        }

        // the only currently supported protocol version is v01
        this.contactURL = "http://" + contactPort +
                          PilotNotificationHTTPHandler_v01.urlPath;

        logger.info("HTTP notification listener URL: '" +
                    this.contactURL + "'");

        final URL url = new URL(this.contactURL);
        
        final Properties realmProps = new Properties();
        InputStream is = null;
        
        logger.info("HTTP notification credential file: '" +
                    accountsPath + "'");
        
        try {
            is = new FileInputStream(accountsPath);
            realmProps.load(is);
            final String pw = realmProps.getProperty(pilotAccountName);
            if (pw == null || pw.trim().length() < minValueLength) {
                throw new Exception("password for '" + pilotAccountName +
                                    "' is missing, blank, or too short. " +
                                    "Must be at least " + minPasswordLength +
                                    " characters.");
            }
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw e;
        } finally {
            if (is != null) {
                is.close();
            }
        }
        this.server = new Server();
        this.initServer(url, accountsPath, slotcall, execService);
    }

    private void initServer(URL url,
                            String accountsPath,
                            SlotPollCallback slotcall,
                            ExecutorService execService) throws IOException {

        System.setProperty("org.mortbay.log.class", LOGGING_CLASS);
        
        final Connector connector = new SocketConnector();
        connector.setHost(url.getHost());
        connector.setPort(url.getPort());
        this.server.setConnectors(new Connector[]{connector});

        final Constraint constraint = new Constraint();
        constraint.setName(Constraint.__DIGEST_AUTH);
        constraint.setRoles(new String[]{"pilotrole"});
        constraint.setAuthenticate(true);

        final ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");

        final SecurityHandler sh = new SecurityHandler();
        sh.setHandler(new PilotNotificationHTTPHandler_v01(slotcall,
                                                           execService,
                                                           this.lager));
        sh.setUserRealm(new HashUserRealm("pilotrealm", accountsPath));
        sh.setConstraintMappings(new ConstraintMapping[]{cm});
        sh.setAuthMethod(Constraint.__DIGEST_AUTH);

        this.server.setHandler(sh);
    }

    public String getContactURL() {
        return this.contactURL;
    }

    public void start() throws Exception {
        this.server.start();
    }
}
