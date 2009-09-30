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

package org.nimbustools.messaging.gt4_0.ensemble;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.globus.wsrf.InvalidResourceKeyException;
import org.globus.wsrf.NoSuchResourceException;
import org.globus.wsrf.RemoveCallback;
import org.globus.wsrf.RemoveNotSupportedException;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceHome;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.config.ConfigException;
import org.globus.wsrf.jndi.Initializable;

import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.nimbustools.messaging.gt4_0.common.NimbusMasterContext;
import org.nimbustools.messaging.gt4_0.service.InstanceTranslate;

import javax.xml.namespace.QName;
import java.io.File;
import java.net.URL;

public class EnsembleHome implements ResourceHome, Initializable {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private Manager manager;
    private String secDescPath;
    private EnsembleTranslate translate;
    private Cache cache;

    // note: don't change, other classes consult Constants_GT4_0 directly
    private final QName keyTypeName =
            Constants_GT4_0.ENSEMBLE_RESOURCE_KEY_QNAME;


    // -------------------------------------------------------------------------
    // implements Initializable
    // -------------------------------------------------------------------------

    public void initialize() throws Exception {

        final NimbusMasterContext master =
                NimbusMasterContext.discoverApplicationContext();

        this.manager = master.getModuleLocator().getManager();

        final ReprFactory repr = master.getModuleLocator().getReprFactory();

        final InstanceTranslate trInstance = 
                new InstanceTranslate(repr, master.getBaseURL());

        this.translate = new EnsembleTranslate(repr, trInstance);

        this.secDescPath = master.getBaseLocation() + File.separator +
                                Constants_GT4_0.SERVICE_SECURITY_CONFIG;

        final URL url = this.getClass().getResource("ehcache.xml");
        final CacheManager cacheManager = new CacheManager(url);
        this.cache = cacheManager.getCache("ensembleCache");

        // if subscriptions on ensemble resources were supported, here is where
        // you'd go through persistent subscriptions and re-register statechange
        // and destruction listeners with manager
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
        final String coschedid = this.convertKey(rkey);

        try {

            // There can be a race between this exists() check and the next
            // access of the manager+id (which is typically coming in a few
            // nanoseconds).  That is OK, the layer below is authoritative.
            if (!this.manager.exists(coschedid, Manager.COSCHEDULED)) {
                throw new NoSuchResourceException(
                        "unknown ensemble: '" + coschedid + "'");
            }

            final Element el = this.cache.get(coschedid);
            final EnsembleResource resource;
            if (el == null) {
                resource = new EnsembleResource(coschedid,
                                                this.manager,
                                                this.secDescPath,
                                                this.translate);
                resource.load(rkey);
                this.cache.put(new Element(coschedid, resource));
            } else {
                resource = (EnsembleResource) el.getObjectValue();
            }

            return resource;

        } catch (ConfigException e) {
            throw new ResourceException(e.getMessage(), e);
        } catch (ManageException e) {
            throw new ResourceException(e.getMessage(), e);
        } catch (DoesNotExistException e) {
            throw new NoSuchResourceException(e.getMessage(), e);
        }
    }

    /**
     * Converts from ResourceKey to String, and validates in the process.
     *
     * @param key key in ResourceKey form
     * @return key in Integer form
     * @throws InvalidResourceKeyException missing/invalid
     */
    public String convertKey(ResourceKey key) throws InvalidResourceKeyException {

        if (key == null) {
            throw new InvalidResourceKeyException("resource key is missing");
        }

        if (!this.keyTypeName.equals(key.getName())) {
            throw new InvalidResourceKeyException("key is wrong QName, " +
                    "must be '" + this.keyTypeName.toString() + "'");
        }

        final String id = (String) key.getValue();
        if (id == null || id.trim().length() == 0) {
            throw new InvalidResourceKeyException("empty ensemble ID in EPR");
        }

        return id;
    }

    /**
     * Removes a resource. If the resource implements the {@link
     * org.globus.wsrf.RemoveCallback RemoveCallback} interface, the
     * implementation must invoke this the remove operation on the resource.
     *
     * Since the underlying implementation keeps track of destruction tasks
     * etc and not anything in this messaging layer, we can assume this is
     * from a WSRF destroy operation invoked by some client.
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
}
