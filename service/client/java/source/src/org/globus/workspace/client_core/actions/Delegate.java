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

package org.globus.workspace.client_core.actions;

import org.globus.workspace.client_core.Action;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.gsi.GlobusCredential;
import org.globus.wsrf.impl.security.descriptor.ClientSecurityDescriptor;
import org.globus.delegation.DelegationUtil;
import org.globus.delegation.DelegationException;
import org.apache.axis.message.addressing.EndpointReferenceType;

import java.security.cert.X509Certificate;
import java.net.URL;
import java.net.MalformedURLException;

public class Delegate extends Action {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected GlobusCredential issuingCred;
    protected ClientSecurityDescriptor csd;
    protected String factoryUrl;
    protected X509Certificate certToSign;
    protected int lifetime;
    protected boolean fullDeleg;

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @param issuingCredential Credential issuing the proxy
     * @param securityDescriptor Client security descriptor with relevant security properties.
     * @param delegationFactoryUrl Address of delegation service
     * @param certificateToSign The public certificate of the new proxy
     * @param newProxyLifetimeSeconds Lifetime of the new proxy in seconds
     * @param fullDelegation Indicates whether full delegation is required.
     */
    public Delegate(GlobusCredential issuingCredential,
                    ClientSecurityDescriptor securityDescriptor,
                    String delegationFactoryUrl,
                    X509Certificate certificateToSign,
                    int newProxyLifetimeSeconds,
                    boolean fullDelegation) {
        
        this.issuingCred = issuingCredential;
        this.csd = securityDescriptor;
        this.factoryUrl = delegationFactoryUrl;
        this.certToSign = certificateToSign;
        this.lifetime = newProxyLifetimeSeconds;
        this.fullDeleg = fullDelegation;
    }

    // -------------------------------------------------------------------------
    // GET/SET OPTIONS
    // -------------------------------------------------------------------------

    public GlobusCredential getIssuingCredential() {
        return this.issuingCred;
    }

    public void setIssuingCredential(GlobusCredential issuingCredential) {
        this.issuingCred = issuingCredential;
    }

    public ClientSecurityDescriptor getClientSecDesc() {
        return this.csd;
    }

    public void setClientSecDesc(ClientSecurityDescriptor securityDescriptor) {
        this.csd = securityDescriptor;
    }

    public String getDelegationFactoryUrl() {
        return this.factoryUrl;
    }

    public void setDelegationFactoryUrl(String delegationFactoryUrl) {
        this.factoryUrl = delegationFactoryUrl;
    }

    public X509Certificate getCertificateToSign() {
        return this.certToSign;
    }

    public void setCertificateToSign(X509Certificate certificateToSign) {
        this.certToSign = certificateToSign;
    }

    public int getNewProxyLifetimeSeconds() {
        return this.lifetime;
    }

    public void setNewProxyLifetimeSeconds(int newProxyLifetimeSeconds) {
        this.lifetime = newProxyLifetimeSeconds;
    }

    public boolean isFullDelegation() {
        return this.fullDeleg;
    }

    public void setFullDelegation(boolean fullDelegation) {
        this.fullDeleg = fullDelegation;
    }


    // -------------------------------------------------------------------------
    // VALIDATE
    // -------------------------------------------------------------------------

    public void validateAll() throws ParameterProblem {

        if (this.lifetime < 1) {
            throw new ParameterProblem(
                    "newProxyLifetimeSeconds is less than one second");
        }

        if (this.factoryUrl == null) {
            throw new ParameterProblem("delegation factory URL is missing");
        } else {
            try {
                new URL(this.factoryUrl);
            } catch (MalformedURLException e) {
                throw new ParameterProblem(
                        "delegation factory URL is not a valid URL");
            }
        }

        if (this.issuingCred == null) {
            throw new ParameterProblem("issuing credential is missing");
        }

        if (this.certToSign == null) {
            throw new ParameterProblem("certificate to sign is missing");
        }

        if (this.csd == null) {
            throw new ParameterProblem(
                    "client security descriptor is missing");
        }
    }
    

    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    /**
     * CALLS delegate()
     *
     * @return EndpointReferenceType instance, epr of delegated credential
     * @throws Exception (ParameterProblem, ExecutionProblem)
     */
    public Object call() throws Exception {
        return this.delegate();
    }

    /**
     * Delegate, get EPR of delegated credential.
     *
     * @return epr of delegated credential
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem general problem running (connection errors etc)
     */
    public EndpointReferenceType delegate() throws ParameterProblem,
                                                   ExecutionProblem {

        this.validateAll();
        
        try {
            return DelegationUtil.delegate(this.factoryUrl,
                                           this.issuingCred,
                                           this.certToSign,
                                           this.lifetime,
                                           this.fullDeleg,
                                           this.csd);
        } catch (DelegationException e) {
            throw new ExecutionProblem(
                    "Problem delegating: " + e.getMessage(), e);
        }
    }
}
