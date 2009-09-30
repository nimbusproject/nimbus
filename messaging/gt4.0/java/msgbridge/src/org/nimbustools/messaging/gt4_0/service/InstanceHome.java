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

package org.nimbustools.messaging.gt4_0.service;

import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.nimbustools.messaging.gt4_0.common.NimbusMasterContext;
import org.globus.wsrf.ResourceHome;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.NoSuchResourceException;
import org.globus.wsrf.InvalidResourceKeyException;
import org.globus.wsrf.RemoveNotSupportedException;
import org.globus.wsrf.RemoveCallback;
import org.globus.wsrf.impl.lifetime.SetTerminationTimeProvider;
import org.globus.wsrf.config.ConfigException;
import org.globus.wsrf.jndi.Initializable;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.DoesNotExistException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.io.File;
import java.net.URL;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.CacheManager;
import commonj.timers.TimerManager;

public class InstanceHome implements ResourceHome, Initializable {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(InstanceHome.class.getName());
    
    
    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------
    
    private Manager manager;
    private TimerManager timerManager;
    private String secDescPath;
    private InstanceTranslate translate;
    private Cache cache;

    // note: don't change, other classes consult Constants_GT4_0 directly
    private final QName keyTypeName =
            Constants_GT4_0.RESOURCE_KEY_QNAME;

    
    // -------------------------------------------------------------------------
    // implements Initializable
    // -------------------------------------------------------------------------

    public void initialize() throws Exception {

        final NimbusMasterContext master =
                NimbusMasterContext.discoverApplicationContext();

        this.manager = master.getModuleLocator().getManager();

        // something in this layer needs to init the metadata server at
        // container startup (lame)
        master.getModuleLocator().getMetadataServer();

        this.translate = new InstanceTranslate(
                                    master.getModuleLocator().getReprFactory(),
                                    master.getBaseURL());

        this.secDescPath = master.getBaseLocation() + File.separator +
                                Constants_GT4_0.SERVICE_SECURITY_CONFIG;

        final URL url = this.getClass().getResource("ehcache.xml");
        final CacheManager cacheManager = new CacheManager(url);
        this.cache = cacheManager.getCache("instanceCache");

        this.timerManager = master.discoverTimerManager();

        // todo: go through persistent subscriptions and re-register statechange
        //       and destruction listeners with manager

        this.manager.recover_initialize();
    }

    
    // -------------------------------------------------------------------------
    // implements ResourceHome
    // -------------------------------------------------------------------------

    /**
     * The resource key type. The <code>ResourceKey</code> used or passed to
     * this <code>ResourceHome</code> must have match this type (corresponds to
     * {@link org.globus.wsrf.ResourceKey#getValue() ResourceKey.getValue()}).
     *
     * @return the type of the key.
     */
    public Class getKeyTypeClass() {
        return String.class;
    }

    /**
     * The name of the resource key. The <code>ResourceKey</code> used or passed
     * to this <code>ResourceHome</code> must have match this name (corresponds
     * to {@link org.globus.wsrf.ResourceKey#getName() ResourceKey.getName()}).
     *
     * @return the name of the key.
     */
    public QName getKeyTypeName() {
        return this.keyTypeName;
    }

    /**
     * Retrieves a resource. <b>Note:</b> This function must not return null. It
     * must return the resource object or throw an exception if there is no
     * resource with the specified key.
     *
     * @return non-null resource object.
     * @throws org.globus.wsrf.NoSuchResourceException
     *          if no resource exists with the given key
     * @throws org.globus.wsrf.InvalidResourceKeyException
     *          if the resource key is invalid.
     * @throws org.globus.wsrf.ResourceException
     *          if any other error occurs.
     */
    public Resource find(ResourceKey rkey) throws ResourceException,
                                                  NoSuchResourceException,
                                                  InvalidResourceKeyException {
        
        // #convertKey() validates as well
        final Integer key = this.convertKey(rkey);
        final String instanceid = key.toString();
        
        try {

            // There can be a race between this exists() check and the next
            // access of the manager+id (which is typically coming in a few
            // nanoseconds).  That is OK, the layer below is authoritative.
            
            if (!this.manager.exists(instanceid, Manager.INSTANCE)) {
                throw new NoSuchResourceException(
                        "unknown workspace: '" + instanceid + "'");
            }

            final Element el = this.cache.get(instanceid);
            final InstanceResource resource;
            if (el == null) {
                resource = new InstanceResource(instanceid,
                                                this.manager,
                                                this.secDescPath,
                                                this.translate,
                                                this,
                                                this.timerManager);
                resource.load(rkey);
                this.manager.registerDestructionListener(instanceid,
                                                         Manager.INSTANCE,
                                                         resource);
                this.cache.put(new Element(instanceid, resource));
            } else {
                resource = (InstanceResource) el.getObjectValue();
            }

            return resource;
            
        } catch (ConfigException e) {
            logger.error(e.getMessage(), e);
            throw new ResourceException(e.getMessage(), e);
        } catch (ManageException e) {
            logger.error(e.getMessage(), e);
            throw new ResourceException(e.getMessage(), e);
        } catch (DoesNotExistException e) {
            throw new NoSuchResourceException(e.getMessage(), e);
        } catch (NoSuchResourceException e) {
            throw e;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new ResourceException(t.getMessage(), t);
        }
    }

    /**
     * Converts from ResourceKey to Integer, and validates in the process.
     * 
     * @param key key in ResourceKey form
     * @return key in Integer form
     * @throws InvalidResourceKeyException missing/invalid
     */
    public Integer convertKey(ResourceKey key) throws InvalidResourceKeyException {

        if (key == null) {
            throw new InvalidResourceKeyException("resource key is missing");
        }

        if (!this.keyTypeName.equals(key.getName())) {
            throw new InvalidResourceKeyException("key is wrong QName, " +
                    "must be '" + this.keyTypeName.toString() + "'");
        }

        try {
            return new Integer((String) key.getValue());
        } catch (NumberFormatException e) {
            throw new InvalidResourceKeyException(
                    "key value is not integer based");
        }
    }

    /**
     * Removes a resource. If the resource implements the {@link
     * org.globus.wsrf.RemoveCallback RemoveCallback} interface, the
     * implementation must invoke the remove operation on the resource.
     *
     * Since the underlying implementation keeps track of destruction tasks
     * etc and not anything in this messaging layer, we can assume this is
     * from a WSRF destroy operation invoked by some client.
     *
     *
     * @throws org.globus.wsrf.NoSuchResourceException
     *          if no resource exists with the given key
     * @throws org.globus.wsrf.InvalidResourceKeyException
     *          if the resource key is invalid.
     * @throws org.globus.wsrf.RemoveNotSupportedException
     *          if remove operation is not supported.
     * @throws org.globus.wsrf.ResourceException
     *          if any other error occurs.
     */
    public void remove(ResourceKey key) throws ResourceException,
                                               NoSuchResourceException,
                                               InvalidResourceKeyException,
                                               RemoveNotSupportedException {

        // WARNING: RemoveCallback assumed, OK because this class controls the
        //          implementation choice currently
        ((RemoveCallback) this.find(key)).remove();
    }

    
    // -------------------------------------------------------------------------
    // DESTROYED
    // -------------------------------------------------------------------------

    /**
     * The authorative layer is saying the instance is gone, this is our
     * opportunity to send termination notifications if necessary.
     *
     * @param resource resource instance
     */
    public void destroyedNotification(InstanceResource resource) {

        if (resource == null) {
            throw new IllegalArgumentException("resource may not be null");
        }
        
        SetTerminationTimeProvider.sendTerminationNotification(resource);

        // Cache access is not locked, that's fine if there is a race between
        // something finding the instance right now and it being removed right
        // now. The layer below will throw DoesNotExistException for incorrect
        // accesses (if it's coded correctly), this message bridge is just the
        // final leg in getting a message to the authoritative manager impl.
        
        this.cache.remove(resource.getID());

        // Note that RemoveCallback.remove() is not called here, that method
        // is really just a way to receive the manually called WSRF terminate
        // from a remote client -- it translates into manager.trash().  This
        // method being called is the manager saying it's gone already, no
        // need to call trash().
    }
}
