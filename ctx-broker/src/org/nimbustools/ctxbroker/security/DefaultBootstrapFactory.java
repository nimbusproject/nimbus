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

package org.nimbustools.ctxbroker.security;

import org.globus.gsi.GlobusCredential;
import org.globus.gsi.CertUtil;
import org.globus.wsrf.jndi.Initializable;
import org.globus.wsrf.container.ServiceHost;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.ctxbroker.ContextBrokerException;
import org.nimbustools.ctxbroker.BrokerConstants;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateEncodingException;
import java.security.PrivateKey;
import java.security.KeyPair;
import java.security.SignatureException;
import java.security.InvalidKeyException;
import java.util.Calendar;

public class DefaultBootstrapFactory implements BootstrapFactory,
                                                    Initializable {

    private static final Log logger =
        LogFactory.getLog(DefaultBootstrapFactory.class.getName());

    private CertificateAuthority ca = null;

    // set via jndi
    private String caCertPath = null;
    private String caKeyPath = null;
    private boolean testBootstrapGeneration = false;

    public BootstrapInformation newBootstrap(String uuid,
                                             String ctxServiceURL,
                                             Calendar expires)
                throws ContextBrokerException {

        BootstrapInformation bootstrap = new BootstrapInformation();

        KeyPair keypair = this.ca.createNewKeyPair();

        X509Certificate cert;
        try {
            cert = this.ca.signNewCertificate(uuid,
                                              keypair.getPublic(),
                                              expires);
        } catch (SignatureException e) {
            throw new ContextBrokerException(e.getMessage(), e);
        } catch (InvalidKeyException e) {
            throw new ContextBrokerException(e.getMessage(), e);
        } catch (CertificateException e) {
            throw new ContextBrokerException(e.getMessage(), e);
        } catch (IOException e) {
            throw new ContextBrokerException(e.getMessage(), e);
        }

        try {
            bootstrap.setX509Cert(cert);
        } catch (CertificateEncodingException e) {
            throw new ContextBrokerException(e.getMessage(), e);
        }
        try {
            bootstrap.setKeypair(keypair);
        } catch (IOException e) {
            throw new ContextBrokerException(e.getMessage(), e);
        }

        X500Principal subjectDN = cert.getSubjectX500Principal();
        String DN = subjectDN.getName(X500Principal.RFC2253);
        String globusDN = CertUtil.toGlobusID(DN, false);
        bootstrap.setBootstrapDN(globusDN);

        return bootstrap;
    }

    public void initialize() throws Exception {

        if (this.caCertPath == null) {
            final String msg = "No CA certificate path was provided.";
            logger.error(msg); // hard to see amidst JNDI problem
            throw new ContextBrokerException(msg);
        }
        logger.debug("caCertPath provided: '" + this.caCertPath + "'");

        if (this.caKeyPath == null) {
            final String msg = "No CA key path was provided.";
            logger.error(msg); // hard to see amidst JNDI problem
            throw new ContextBrokerException(msg);
        }
        logger.debug("caKeyPath provided: '" + this.caKeyPath + "'");

        File cert = new File(this.caCertPath);
        if (!cert.isAbsolute()) {
            final String msg = "Configured CA certificate path ('" + 
                               this.caCertPath + "') is not an absolute path.";
            logger.error(msg); // hard to see amidst JNDI problem
            throw new ContextBrokerException(msg);
        }

        if (!cert.canRead()) {
            final String msg = "Configured CA certificate path ('" +
                               this.caCertPath + "') can not be read.";
            logger.error(msg); // hard to see amidst JNDI problem
            throw new ContextBrokerException(msg);
        }

        cert = new File(this.caKeyPath);
        if (!cert.isAbsolute()) {
            final String msg = "Configured CA key path ('" + this.caKeyPath +
                               "') is not an absolute path.";
            logger.error(msg); // hard to see amidst JNDI problem
            throw new ContextBrokerException(msg);
        }

        if (!cert.canRead()) {
            final String msg = "Configured CA key path ('" +
                               this.caKeyPath + "') can not be read.";
            logger.error(msg); // hard to see amidst JNDI problem
            throw new ContextBrokerException(msg);
        }

        final GlobusCredential caGlobusCred =
                new GlobusCredential(this.caCertPath, this.caKeyPath);

        logger.debug("read in CA credential: '" +
                                caGlobusCred.getIdentity() + "'");

        final X509Certificate caCert = caGlobusCred.getIdentityCertificate();
        final PrivateKey caPrivateKey = caGlobusCred.getPrivateKey();
        
        this.ca = new CertificateAuthority(caCert,
                                           caPrivateKey,
                                           caGlobusCred.getIdentity());

        // make a test certificate, to see if all is well rather than waiting
        // for a deployment to fail
        if (this.testBootstrapGeneration) {
            this.testBootstrapGeneration();
        }

        /*
        // key generation is faster after intiialization, test routine:
        if (logger.isDebugEnabled()) {
            for (int i = 0; i < 15; i++) {
                this.ca.createNewKeyPair();
            }
        }
        */
    }

    private void testBootstrapGeneration()
            throws ContextBrokerException {

        Calendar expires = Calendar.getInstance();
		expires.add(Calendar.MINUTE, 1);
        String url;
        try {
            url = ServiceHost.getBaseURL()
                            + BrokerConstants.CTX_BROKER_PATH;
        } catch (IOException e) {
            throw new ContextBrokerException(e.getMessage(), e);
        }

        this.newBootstrap("fake-UUID", url, expires);

        // for now, not going through and checking validity of cert
        // serialization etc.
        logger.trace("Bootstrap generation test succeeded.");
    }

    // jndi
    public void setCaCertPath(String caCertPath) {
        this.caCertPath = caCertPath;
    }

    // jndi
    public void setCaKeyPath(String caKeyPath) {
        this.caKeyPath = caKeyPath;
    }

    // jndi
    public void setTestBootstrapGeneration(String testBootstrapGeneration) {
        this.testBootstrapGeneration =
                testBootstrapGeneration.trim().equalsIgnoreCase("true");
    }
}
