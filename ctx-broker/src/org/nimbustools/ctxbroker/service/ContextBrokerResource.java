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

package org.nimbustools.ctxbroker.service;

import org.globus.wsrf.ResourceLifetime;
import org.globus.wsrf.ResourceIdentifier;
import org.globus.wsrf.ResourceProperties;
import org.globus.wsrf.config.ConfigException;
import org.globus.wsrf.security.SecureResource;
import org.nimbustools.ctxbroker.security.BootstrapInformation;
import org.nimbustools.ctxbroker.Identity;
import org.nimbustools.ctxbroker.ContextBrokerException;
import org.nimbustools.ctxbroker.generated.gt4_0.types.Node_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.IdentityProvides_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Requires_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Provides_Type;

public interface ContextBrokerResource extends ResourceLifetime,
                                               SecureResource,
                                               ResourceProperties,
                                               ResourceIdentifier {

    public void setID(String id);

    public BootstrapInformation getBootstrap();

    public void setBootstrap(BootstrapInformation bootstrap);

    public String getCreatorDN();

    public String getBootstrapDN();    

    public void initSecureResource(String creatorDN, String bootstrapDN)
            throws ConfigException;

    public void noMoreInjections() throws ContextBrokerException;

    public boolean isNoMoreInjections();

    public Requires_Type retrieve(Integer workspaceID,
                                  IdentityProvides_Type[] identities)
            throws ContextBrokerException;

    public void injectData(String dataName, String value)
            throws ContextBrokerException;

    public Node_Type[] identityQueryAll()
            throws ContextBrokerException;

    public Node_Type[] identityQueryHost(String host)
            throws ContextBrokerException;

    public Node_Type[] identityQueryIP(String ip)
            throws ContextBrokerException;

    public void okExit(Integer workspaceID)
            throws ContextBrokerException;

    public void errorExit(Integer workspaceID,
                          short exitCode,
                          String errorMessage)
            throws ContextBrokerException;

    /**
     * Creates new Node, treats provides and requires documents in the
     * empty, provided interpretation.
     *
     * @param workspaceID workspace ID
     * @param identities identity objects filled by factory/service.
     *        What 'is' already based on creation request or initialization.
     *        Once passed to this method, caller must discard pointers.
     * @param requires the provided requires section if it exists
     * @param provides the provided provides section if it exists
     * @param totalNodes total is calculated independently from each agent
     * @throws ContextBrokerException illegalities
     */
    public void addWorkspace(Integer workspaceID,
                             Identity[] identities,
                             Requires_Type requires,
                             Provides_Type provides,
                             int totalNodes)
            throws ContextBrokerException;

    void setAllowInjections(boolean allowInjections);
}
