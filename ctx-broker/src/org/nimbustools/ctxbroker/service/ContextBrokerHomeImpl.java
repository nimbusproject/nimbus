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

import org.nimbustools.ctxbroker.Identity;
import org.nimbustools.ctxbroker.BrokerConstants;
import org.nimbustools.ctxbroker.ContextBrokerException;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Contextualization_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Provides_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Requires_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.IdentityProvides_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Requires_TypeIdentity;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudcluster_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Cloudworkspace_Type;
import org.nimbustools.ctxbroker.generated.gt4_0.description.AgentDescription_Type;
import org.nimbustools.ctxbroker.security.BootstrapFactory;
import org.nimbustools.ctxbroker.security.BootstrapInformation;
import org.globus.wsrf.impl.ResourceHomeImpl;
import org.globus.wsrf.impl.SimpleResourceKey;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.Constants;
import org.globus.wsrf.config.ConfigException;
import org.globus.wsrf.utils.AddressingUtils;
import org.globus.wsrf.container.ServiceHost;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis.components.uuid.UUIDGen;
import org.apache.axis.components.uuid.UUIDGenFactory;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.message.MessageElement;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.Calendar;
import java.util.Hashtable;

public class ContextBrokerHomeImpl extends ResourceHomeImpl
                                    implements ContextBrokerHome {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------
    
    private static final Log logger =
        LogFactory.getLog(ContextBrokerHomeImpl.class.getName());

    public static final String CONTEXTUALIZATION_BOOTSTRAP_FACTORY =
                        Constants.JNDI_SERVICES_BASE_NAME +
                                BrokerConstants.CTX_BROKER_PATH +
                        "/ctxBrokerBootstrapFactory";


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final String serviceAddress;

    private boolean initialized;

    private UUIDGen uuidGen;
    
    private BootstrapFactory bootstrapFactory;
    
    // String hostname --> Integer workspID
    private final Hashtable hostnameMap = new Hashtable();

    // String ip-address --> Integer workspID
    private final Hashtable ipMap = new Hashtable();

    // the lowest unused integer ID
    private int nextID = 1;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public ContextBrokerHomeImpl() throws IOException {
        super();
        this.keyTypeName =
                BrokerConstants.CONTEXTUALIZATION_RESOURCE_KEY_QNAME;
        this.serviceAddress = ServiceHost.getBaseURL() +
                BrokerConstants.CTX_BROKER_PATH;
    }


    // -------------------------------------------------------------------------
    // SETUP
    // -------------------------------------------------------------------------

    public synchronized void initialize() throws Exception {

        super.initialize();

        if (this.initialized) {
            logger.warn("already initialized: Nimbus Context Broker");
            return;
        }

        logger.info("Setting up Nimbus Context Broker");

        this.uuidGen = UUIDGenFactory.getUUIDGen();

        this.bootstrapFactory = discoverBootstrapFactory();

        this.initialized = true;

        logger.info("Ready: Nimbus Context Broker");
    }

    public static BootstrapFactory discoverBootstrapFactory() throws Exception {

        InitialContext ctx = null;
        try {
            ctx = new InitialContext();

            final BootstrapFactory factory =
                    (BootstrapFactory) ctx.lookup(
                            CONTEXTUALIZATION_BOOTSTRAP_FACTORY);

            if (factory == null) {
                throw new Exception("null from JNDI for BootstrapFactory (?)");
            }

            return factory;

        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    // -------------------------------------------------------------------------
    // implements ContextBrokerHome
    // -------------------------------------------------------------------------

    public String getBrokerURL() throws ContextBrokerException {
        return this.serviceAddress;
    }

    public String getContextSecret(EndpointReferenceType ref)
            throws ContextBrokerException {
        final ResourceKey ctxKey = this.getResourceKey(ref);
        final ContextBrokerResource resource =
                                        this.findResourceNoSecurity(ctxKey);
        BootstrapInformation bootstrap = resource.getBootstrap();

        // current secret string is a separated list:
        /*
           Separator
           Position 0: Public certificate (pem) to contact ctx service
           Separator
           Position 1: Private key (pem) to contact ctx service
           Separator
        */

        StringBuffer buf = new StringBuffer();
        buf.append(ContextBrokerHome.FIELD_SEPARATOR)
           .append(bootstrap.getPublicX509String())
           .append(ContextBrokerHome.FIELD_SEPARATOR)
           .append(bootstrap.getPrivateString())
           .append(ContextBrokerHome.FIELD_SEPARATOR);
        return buf.toString();
    }

    /**
     * @see ContextBrokerHome#addWorkspace
     */
    public void addWorkspace(ContextBrokerResource resource,
                             Integer workspaceID,
                             Identity[] identities,
                             AgentDescription_Type ctxDocAndID)
            
            throws ContextBrokerException {

        if (identities == null) {
            throw new IllegalArgumentException("identities may not be null");
        }

        if (ctxDocAndID == null) {
            final String err = "A previously unknown workspace's context " +
                    "agent is invoking " +
                    "an operation before retrieve, we cannot register it.\n" +
                    "Identity dump: " + this.identitiesDump(identities);
            throw new ContextBrokerException(err);
        }

        if (workspaceID == null) {
            throw new IllegalArgumentException("workspaceID may not be null");
        }

        if (identities.length > 2 || identities.length == 0) {
            throw new ContextBrokerException(
                 "these arbitrary identities are not supported at the moment," +
                         "you may only use 1 or 2 NICs");
        }

        Cloudcluster_Type cluster = ctxDocAndID.getCluster();
        
        // checks that there is nothing illegal and that one and only
        // one section will be marked active
        int totalNodes = this.clusterSanityAndCount(cluster);
        logger.debug("total nodes: " + totalNodes);

        final Cloudworkspace_Type[] vms = cluster.getWorkspace();
        Contextualization_Type ctxDoc = null;
        for (Cloudworkspace_Type vm : vms) {
            if (Boolean.TRUE.equals(vm.getActive())) {
                ctxDoc = vm.getCtx(); // clusterSanityAndCount guarantees one and only one
                break;
            }
        }

        // clusterSanity checked this already, this is to satisfy automatic
        // code analysis
        if (ctxDoc == null) {
            throw new NullPointerException();
        }

        this.basicCtxdocValidate(ctxDoc);

        for (Identity identity : identities) {
            final String hostname = identity.getHostname();
            if (hostname != null) {
                logger.debug("hostname: " + hostname);
                this.newHostname(workspaceID, hostname, resource);
            }
            final String ip = identity.getIp();
            if (ip != null) {
                logger.debug("ip: " + ip);
                this.newIP(workspaceID, ip, resource);
            }
        }

        resource.addWorkspace(workspaceID,
                              identities,
                              ctxDoc.getRequires(),
                              ctxDoc.getProvides(),
                              totalNodes);
    }


    /**
     * @see ContextBrokerHome#getResourceKey
     */
    public ResourceKey getResourceKey(EndpointReferenceType epr)
            throws ContextBrokerException {

        return new SimpleResourceKey(this.getKeyTypeName(),
                                     this.getID(epr));
    }

    /**
     * @see ContextBrokerHome#createNewResource
     */
    public EndpointReferenceType createNewResource(String callerDN,
                                                   boolean allowInjections)

            throws ContextBrokerException {

        if (!this.initialized) {
            throw new ContextBrokerException(
                    ContextBrokerHomeImpl.class.getName() +
                            " not initialized.");
        }

        final ContextBrokerResource resource;
        try {
            resource = (ContextBrokerResource)createNewInstance();
        } catch (Exception e) {
            throw new ContextBrokerException("", e);
        }

        final String uuid = this.uuidGen.nextUUID();
        resource.setID(uuid);

        resource.setAllowInjections(allowInjections);


        // TODO: make this limit configurable and/or possibly make a dynamic
        //       decision.  For example, if the scheduler is NOT best effort
        //       then it is probably OK to just make this expire in a day or
        //       even an hour.  Also, if re-contextualization is implemented,
        //       presumbaly the VMs (for now) would contact with same cred
        //       so expiration will affect things in that situation
        final Calendar expires = Calendar.getInstance();
		expires.add(Calendar.MONTH, 1);
        
        final BootstrapInformation bootstrap =
                this.bootstrapFactory.newBootstrap(uuid,
                                                   this.serviceAddress,
                                                   expires);

        resource.setBootstrap(bootstrap);

        // in the future, policy should come from elsewhere
        try {
            resource.initSecureResource(callerDN, bootstrap.getBootstrapDN());
        } catch (ConfigException e) {
            throw new ContextBrokerException("", e);
        }

        logger.info("WS-CTX created new contextualization " +
                    "resource: '" + uuid + "' for DN = '" + callerDN + "'");

        ResourceKey key = this.getResourceKey(uuid);
        this.add(key, resource);
        return getEPR(key);
    }


    // -------------------------------------------------------------------------
    // OTHER/IMPL
    // -------------------------------------------------------------------------

    private void newHostname(Integer vmid, String hostname,
                             ContextBrokerResource resource) {

        if (vmid == null) {
            throw new IllegalArgumentException("vmid may not be null");
        }

        if (hostname == null) {
            throw new IllegalArgumentException("hostname may not be null");
        }

        final String rsrc = (String) resource.getID();
        if (rsrc == null) {
            throw new IllegalArgumentException("resource ID may not be null");
        }

        this.noPersistenceHostnameAddition(rsrc, vmid, hostname);
    }

    private void newIP(Integer vmid, String ip,
                       ContextBrokerResource resource) {

        if (vmid == null) {
            throw new IllegalArgumentException("vmid may not be null");
        }

        if (ip == null) {
            throw new IllegalArgumentException("ip may not be null");
        }

        final String rsrc = (String) resource.getID();
        if (rsrc == null) {
            throw new IllegalArgumentException("resource ID may not be null");
        }

        this.noPersistenceIPAddition(rsrc, vmid, ip);
    }

    public synchronized Integer getID(ContextBrokerResource resource,
                                      IdentityProvides_Type[] identities,
                                      AgentDescription_Type sent) throws ContextBrokerException {

        if (resource == null) {
            throw new IllegalArgumentException("resource may not be null");
        }

        if (identities == null || identities.length == 0) {
            throw new IllegalArgumentException("no identites");
        }

        final String rsrc = (String) resource.getID();
        if (rsrc == null) {
            throw new IllegalArgumentException("resource ID may not be null");
        }

        for (IdentityProvides_Type identity : identities) {
            if (identity == null) {
                logger.error("Null identity in request, ID dump: " +
                        this.identitiesDump(identities));
                return null;
            }
            String ip = identity.getIp();
            if (ip != null) {
                final Integer result = noPersistenceIPQuery(rsrc, ip);
                if (result != null) {
                    return result;
                }
            } else {
                logger.error("Null IP in request, ID dump: " +
                        this.identitiesDump(identities));
                return null;
            }
        }

        for (IdentityProvides_Type identity : identities) {
            String hostname = identity.getHostname();
            if (hostname != null) {
                final Integer result = noPersistenceHostnameQuery(rsrc,
                                                                  hostname);
                if (result != null) {
                    return result;
                }
            } else {
                logger.error("Null hostname in request, ID dump: " +
                        this.identitiesDump(identities));
                return null;
            }
        }

        for (IdentityProvides_Type identity : identities) {
            String iface = identity.get_interface();
            if (iface == null) {
                logger.error("Null interface in request, ID dump: " +
                        this.identitiesDump(identities));
                return null;
            }
        }

        // create an integer ID for this member of the context that we have not
        // seen before

        synchronized (this) {

            // nothing is null or missing except possibly pubkey

            final StringBuffer buf = new StringBuffer();
            buf.append("Broker hearing from a VM for first time, IP(s)=");

            Identity[] ids = new Identity[identities.length];
            for (int i = 0; i < identities.length; i++) {
                ids[i] = new Identity();
                ids[i].setIface(identities[i].get_interface());
                ids[i].setIp(identities[i].getIp());
                buf.append(" '").append(identities[i].getIp()).append("'");
                ids[i].setHostname(identities[i].getHostname());
                ids[i].setPubkey(identities[i].getPubkey());
            }

            final Integer thisID = this.nextID;
            this.nextID = this.nextID + 1;

            buf.append(" and internal ID # = ").append(thisID.toString());
            logger.info(buf.toString());

            this.addWorkspace(resource,
                              thisID,
                              ids,
                              sent);
        }

        return null;
    }

    // for errors
    private String identitiesDump(IdentityProvides_Type[] identities) {
        final StringBuffer buf = new StringBuffer();
        for (int i = 0; i < identities.length; i++) {
            buf.append("IdentityProvides_Type #")
               .append(i);
            IdentityProvides_Type id = identities[i];
            if (id == null) {
                buf.append(" is null.\n");
            } else {
               buf.append(":\n")
                  .append(" - interface: '")
                  .append(id.get_interface())
                  .append("'\n");
               buf.append(" - ip: '")
                  .append(id.getIp())
                  .append("'\n");
                buf.append(" - hostname: '")
                  .append(id.getHostname())
                  .append("'\n");
                buf.append(" - ssh pubkey: '")
                  .append(id.getPubkey())
                  .append("'\n");
            }
        }
        return buf.toString();
    }
    private String identitiesDump(Identity[] identities) {
        final StringBuffer buf = new StringBuffer();
        for (int i = 0; i < identities.length; i++) {
            buf.append("IdentityProvides_Type #")
               .append(i);
            Identity id = identities[i];
            if (id == null) {
                buf.append(" is null.\n");
            } else {
               buf.append(":\n")
                  .append(" - interface: '")
                  .append(id.getIface())
                  .append("'\n");
               buf.append(" - ip: '")
                  .append(id.getIp())
                  .append("'\n");
                buf.append(" - hostname: '")
                  .append(id.getHostname())
                  .append("'\n");
                buf.append(" - ssh pubkey: '")
                  .append(id.getPubkey())
                  .append("'\n");
            }
        }
        return buf.toString();
    }

    private Integer noPersistenceIPQuery(String rsrc,
                                         String ip) {
        return (Integer) this.ipMap.get(rsrc+ip);
    }

    private void noPersistenceIPAddition(String rsrc,
                                         Integer id,
                                         String ip) {
        this.ipMap.put(rsrc+ip, id);
    }

    private Integer noPersistenceHostnameQuery(String rsrc,
                                               String hostname) {
        return (Integer) this.hostnameMap.get(rsrc+hostname);
    }

    private void noPersistenceHostnameAddition(String rsrc,
                                               Integer id,
                                               String hostname) {
        this.hostnameMap.put(rsrc+hostname, id);
    }

    // -------------------------------------------------------------------------
    // WS stuff
    // -------------------------------------------------------------------------
    
    private ContextBrokerResource findResourceNoSecurity(ResourceKey ctxKey)
            throws ContextBrokerException {

        try {
            return (ContextBrokerResource) this.find(ctxKey);
        } catch (ResourceException e) {
            String msg = "Cannot find contextualization resource: '" +
                          getID(ctxKey) + "'";
            throw new ContextBrokerException(msg, e);
        }
    }

    private ContextBrokerResource findResource(ResourceKey ctxKey)
            throws ContextBrokerException {

        ContextBrokerResource resource;
        try {
            resource = (ContextBrokerResource) this.find(ctxKey);
        } catch (ResourceException e) {
            String msg = "Cannot find contextualization resource: '" +
                          getID(ctxKey) + "'";
            throw new ContextBrokerException(msg, e);
        }

        return resource;
    }

    private int clusterSanityAndCount(Cloudcluster_Type cluster)
            throws ContextBrokerException {
        
        if (cluster == null) {
            throw new ContextBrokerException("Invalid cluster description " +
                    "section, <cluster> is missing.");
        }

        Cloudworkspace_Type[] vms = cluster.getWorkspace();
        if (vms == null || vms.length == 0) {
            throw new ContextBrokerException("Invalid cluster description " +
                    "section, missing workspace elements.");
        }

        int totalNodeCount = 0;

        int numActive = 0;
        for (Cloudworkspace_Type vm : vms) {

            if (vm == null) {
                throw new ContextBrokerException(
                        "Invalid cluster description section, missing a " +
                                "workspace element in array.");
            }
            
            if (vm.getCtx() == null) {
                throw new ContextBrokerException(
                        "Invalid cluster description section, missing a " +
                                "ctx element in the workspace list.");
            }

            if (vm.getQuantity() <= 0) {
                throw new ContextBrokerException(
                        "Invalid cluster description section, missing " +
                                "quantity.");
            }

            totalNodeCount += vm.getQuantity();

            // active can be null which equals False
            final Boolean active = vm.getActive();
            if (Boolean.TRUE.equals(active)) {
                numActive += 1;
            }
        }

        if (numActive < 1) {
            throw new ContextBrokerException(
                        "Invalid cluster description, there is no " +
                                "section marked with <active>");
        }

        if (numActive > 1) {
            throw new ContextBrokerException(
                        "Invalid cluster description, more than one " +
                                "section is marked with <active> (there are " +
                                numActive + " marked active)");
        }

        return totalNodeCount;
    }

    private void basicCtxdocValidate(Contextualization_Type ctx)
            throws ContextBrokerException {

        if (ctx == null) {
            throw new IllegalArgumentException("contextualization " +
                    "document given to validate is null");
        }
        
        Provides_Type provides = ctx.getProvides();
        Requires_Type requires = ctx.getRequires();

        if (provides == null && requires == null) {
            throw new ContextBrokerException("Both provides and " +
                    "requires are missing. Will not contextualize this. " +
                    "If there is nothing to do, do not include " +
                    "contextualization document.");
        }

        if (provides != null) {
            IdentityProvides_Type[] givenIDs = provides.getIdentity();
            if (givenIDs == null || givenIDs.length == 0) {
                throw new ContextBrokerException("Provides section is " +
                        "present but has no identity elements.  Will not " +
                        "contextualize.");
            }
        }

        if (requires == null) {
            return;
        }

        Requires_TypeIdentity[] givenID = requires.getIdentity();

        if (givenID == null || givenID.length == 0) {
            return;
        }
        
        // next two exceptions are for forwards compatibility where it
        // may be possible to specify specific identities required
        // (without going through role finding which will always place
        // identities in the filled requires document for a role,
        // regardless if all identities are required or not).

        if (givenID.length > 1) {
            throw new ContextBrokerException("Given requires " +
                    "section has multiple identity elements? Currently " +
                    "only supporting zero or one empty identity element " +
                    "in requires section (which signals all identities " +
                    "are desired).  Will not contextualize.");
        }

        if (givenID[0].getHostname() != null ||
            givenID[0].getIp() != null ||
            givenID[0].getPubkey() != null) {

            throw new ContextBrokerException("Given requires " +
                    "section has an identity element with information " +
                    "in it? Currently only supporting zero or one " +
                    "*empty* identity element in requires section " +
                    "(which signals all identities are desired).  Will " +
                    "not contextualize.");
        }
    }

    public ResourceKey getResourceKey(String id) {
        return new SimpleResourceKey(this.getKeyTypeName(), id);
    }

    public EndpointReferenceType getEPR(ResourceKey key)
            throws ContextBrokerException {

        final EndpointReferenceType epr;
        try {
            epr = AddressingUtils.createEndpointReference(
                                        this.serviceAddress, key);
        } catch (Exception exp) {
            throw new ContextBrokerException("Error creating " +
                    "contextualization endpoint reference", exp);
        }
        return epr;
    }

    public String getID(EndpointReferenceType epr)
            throws ContextBrokerException {

        if (epr == null) {
            throw new ContextBrokerException("epr is null");
        }

        if (epr.getProperties() == null) {
            throw new ContextBrokerException("epr properties are null");
        }

        MessageElement key = epr.getProperties().get(this.getKeyTypeName());
        if (key == null) {
            throw new ContextBrokerException("contextualization " +
                    "resource key not present in EPR");
        }

        return key.getValue();
    }

    public static String getID(ResourceKey key)
                                    throws ContextBrokerException {
        if (key == null) {
            throw new ContextBrokerException("key is null");
        }
        return (String)key.getValue();
    }
}
