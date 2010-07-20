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

package org.globus.workspace.cloud.client;

import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.utils.NimbusCredential;
import org.globus.wsrf.impl.security.authorization.Authorization;
import org.globus.wsrf.impl.security.authentication.Constants;
import org.globus.axis.gsi.GSIConstants;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.ietf.jgss.GSSCredential;

import javax.xml.rpc.Stub;

public class StubConf implements StubConfigurator {


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected EndpointReferenceType endpoint;
    protected String mechanism;
    protected Object protection = Constants.SIGNATURE;
    protected Authorization authorization;

    
    // -------------------------------------------------------------------------
    // GET (implements StubConfigurator)
    // -------------------------------------------------------------------------

    public EndpointReferenceType getEPR() {
        return this.endpoint;
    }

    public Object getMechanism() {
        return this.mechanism;
    }

    public Object getProtection() {
        return this.protection;
    }

    public Authorization getAuthorization() {
        return this.authorization;
    }

    // -------------------------------------------------------------------------
    // SET (implements StubConfigurator)
    // -------------------------------------------------------------------------

    public void setEPR(EndpointReferenceType epr) {
        this.endpoint = epr;
    }

    public void setMechanism(String mech) {
        this.mechanism = null;
    }

    public void setProtection(Object protect) {
        this.protection = protect;
    }

    public void setAuthorization(Authorization authz) {
        this.authorization = authz;
    }

    // -------------------------------------------------------------------------
    // implements StubConfigurator
    // -------------------------------------------------------------------------

    public void setOptions(Stub stub) throws Exception {

        if (this.protection != null && this.endpoint == null) {
            throw new Exception("endpoint required");
        }

        if (this.protection != null) {
            // this means if both transport security and message security
            // are enabled both will get the same protection
            if (this.endpoint.getAddress().getScheme().equals("https")) {
                stub._setProperty(GSIConstants.GSI_TRANSPORT,
                                  this.protection);
            }
            if (this.mechanism != null) {
                stub._setProperty(this.mechanism,
                                  this.protection);
            }
        }

        if (this.authorization != null) {
            stub._setProperty(Constants.AUTHORIZATION, this.authorization);
        }

        final GSSCredential usercred = NimbusCredential.getGSSCredential();
        if (usercred != null) {
            stub._setProperty(GSIConstants.GSI_CREDENTIALS, usercred);
        }
    }
}
