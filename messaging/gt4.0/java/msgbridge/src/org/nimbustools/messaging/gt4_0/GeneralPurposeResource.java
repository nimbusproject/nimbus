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

package org.nimbustools.messaging.gt4_0;

import org.globus.security.gridmap.GridMap;
import org.globus.wsrf.ResourceLifetime;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.PersistentResource;
import org.globus.wsrf.NoSuchResourceException;
import org.globus.wsrf.InvalidResourceKeyException;
import org.globus.wsrf.config.ConfigException;
import org.globus.wsrf.impl.security.descriptor.ResourceSecurityDescriptor;
import org.globus.wsrf.impl.security.descriptor.ResourceSecurityConfig;
import org.globus.wsrf.security.SecurityManager;
import org.globus.wsrf.security.SecureResource;

import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.DoesNotExistException;

import org.nimbustools.api.repr.Caller;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Calendar;

/**
 * Base class for instance-style resources (ensemble, group, instance) 
 *
 * This only works for things with simple String resource keys.
 * 
 */
public class GeneralPurposeResource implements PersistentResource,
                                               ResourceLifetime,
                                               SecureResource {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
        LogFactory.getLog(GeneralPurposeResource.class.getName());
    
    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final String id;
    protected final int type;
    protected final Manager manager;
    protected final String secDescPath;
    protected final BaseTranslate baseTranslate;
    
    protected ResourceSecurityDescriptor securityDescriptor;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    /**
     *
     * @param idString instance key
     * @param type entity type 
     * @param manager Manager impl
     * @param secDescPath path to resource-security descriptor template
     * @param baseTranslate basic translate impl(s)
     * @throws ConfigException problem with secDescFilename
     * @throws DoesNotExistException gone (race with a destroyer)
     * @throws ManageException general problem
     */
    public GeneralPurposeResource(String idString,
                                  int type,
                                  Manager manager,
                                  String secDescPath,
                                  BaseTranslate baseTranslate)
            throws ConfigException, ManageException, DoesNotExistException {

        if (idString == null) {
            throw new IllegalArgumentException("idString may not be null");
        }
        if (manager == null) {
            throw new IllegalArgumentException("manager may not be null");
        }
        if (secDescPath == null) {
            throw new IllegalArgumentException("secDescPath may not be null");
        }
        if (baseTranslate == null) {
            throw new IllegalArgumentException("baseTranslate may not be null");
        }

        this.id = idString;
        this.type = type;
        this.manager = manager;
        this.secDescPath = secDescPath;
        this.baseTranslate = baseTranslate;

        if (!this.manager.exists(idString, type)) {
            throw new DoesNotExistException();
        }

        initSecureResource();
    }

    private void initSecureResource()
            throws ConfigException, ManageException, DoesNotExistException {

        final ResourceSecurityConfig securityConfig =
                    new ResourceSecurityConfig(this.secDescPath);
        securityConfig.init();
        
        this.securityDescriptor = securityConfig.getSecurityDescriptor();
        this.securityDescriptor.setInitialized(false);
        final GridMap map = new GridMap();

        final Caller[] callers =
                this.manager.getAuthorizedManagers(this.id, this.type);
        if (callers != null) {
            for (int i = 0; i < callers.length; i++) {
                final Caller caller = callers[i];
                if (caller != null && !caller.isSuperUser()) {
                    map.map(caller.getIdentity(), "fakeuserid");
                }
            }
        }
        this.securityDescriptor.setGridMap(map);
    }

    
    // -------------------------------------------------------------------------
    // implements SecureResource
    // -------------------------------------------------------------------------

    /**
     * Method to retrieve the security descriptor for this
     * resource. If the descriptor does not have Subject and GridMap
     * set, then it is recommended that
     * <code>ResourceSecurityConfig</code> be used initialize the
     * descriptor object. If the <i>initialized</i> is set to true, in
     * the returned descriptor, then no initialization is done.
     *
     * @return resource security descriptor for the resource. Can be null.
     */
    public ResourceSecurityDescriptor getSecurityDescriptor() {
        // there is no dynamic policy support yet, it's set once
        return this.securityDescriptor;
    }


    // -------------------------------------------------------------------------
    // implements ResourceLifetime
    // -------------------------------------------------------------------------

    public Calendar getCurrentTime() {
        return Calendar.getInstance();
    }

    public Calendar getTerminationTime() {
        try {
            return this.manager.getDestructionTime(this.id, this.type);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            return Calendar.getInstance(); // destroy holder object now
        }
    }

    public void setTerminationTime(Calendar time) {
        try {
            this.manager.setDestructionTime(this.id, this.type, time);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
        }
    }
    
    
    // -------------------------------------------------------------------------
    // implements ResourceIdentifier (via PersistentResource)
    // -------------------------------------------------------------------------

    /**
     * Returns the unique id of the resource. In most cases this value should
     * match value returned by {@link ResourceKey#getValue
     * ResourceKey.getValue()}.
     *
     * @return the id of the resource.
     */
    public Object getID() {
        return this.id;
    }

    
    // -------------------------------------------------------------------------
    // implements PersistenceCallback (via PersistentResource)
    // -------------------------------------------------------------------------

    public void load(ResourceKey key) throws ResourceException,
                                             NoSuchResourceException,
                                             InvalidResourceKeyException {
        // this gets overriden when necessary
    }

    public void store() throws ResourceException {
        // this gets overriden when necessary
    }

    
    // -------------------------------------------------------------------------
    // implements RemoveCallback (via PersistentResource)
    // -------------------------------------------------------------------------

    /**
     * Notifies that the resource was removed. This function must not be
     * called directly on the resource object. Only
     * {@link org.globus.wsrf.ResourceHome#remove(ResourceKey) ResourceHome.remove()} is
     * allowed to call that method during the remove operation.
     *
     * Since the underlying implementation keeps track of destruction tasks
     * etc and not anything in this messaging layer, we can assume this is
     * from a WSRF destroy operation invoked by some client.
     *
     * @throws ResourceException if the remove operation fails.
     */
    public void remove() throws ResourceException {

        final String callerDN = SecurityManager.getManager().getCaller();

        if (callerDN == null) {
            logger.error("REMOVE: no caller identity");
            throw new ResourceException("no caller identity", null);
        }

        final Caller caller = this.baseTranslate.getCaller(callerDN);

        try {
            this.manager.trash(this.id, this.type, caller);
        } catch (DoesNotExistException e) {
            throw new NoSuchResourceException(e.getMessage(), e);
        } catch (ManageException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }

            throw new ResourceException(e.getMessage(), e);
        }
    }
}
