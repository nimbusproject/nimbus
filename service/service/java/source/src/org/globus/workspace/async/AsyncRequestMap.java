/*
 * Copyright 1999-2010 University of Chicago
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

package org.globus.workspace.async;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class AsyncRequestMap {

    // -----------------------------------------------------------------------------------------
    // STATIC VARIABLES
    // -----------------------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(AsyncRequestMap.class.getName());

    private static final String CACHE_NAME = "nimbus-siCache";
    private static final String DISK_PROPKEY = "ehcache.disk.store.dir";
    private static final String SHUTDOWN_PROPKEY = "net.sf.ehcache.enableShutdownHook";


    // -----------------------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -----------------------------------------------------------------------------------------

    private final Cache cache;
    

    // -----------------------------------------------------------------------------------------
    // CONSTRUCTORS
    // -----------------------------------------------------------------------------------------

    public AsyncRequestMap(Resource diskStoreResource) throws IOException {

        if (diskStoreResource == null) {
            throw new IllegalArgumentException("diskStoreResource may not be null");
        }

        final String diskStorePath = diskStoreResource.getFile().getAbsolutePath();

        // TODO: Do dynamically. This must not be in conflict with other ehcache+diskstore
        //       users (and there is at least one other), could not see how to set
        //       on per-manager basis via spring (specifically, the to-disk part).
        System.setProperty(DISK_PROPKEY, diskStorePath);

        // TODO: We need a shutdown hook with disk-based.  This creates a jvm
        //       shutdown hook and is the least-recommended solution.
        System.setProperty(SHUTDOWN_PROPKEY, "true");

        final URL url = this.getClass().getResource("ehcache.xml");
        final CacheManager cacheManager = new CacheManager(url);
        this.cache = cacheManager.getCache(CACHE_NAME);
        if (this.cache == null) {
            throw new IllegalArgumentException(
                    "cacheManager does not provide '" + CACHE_NAME + "'");
        }
        this.loadAllFromDisk();
    }

    
    // -----------------------------------------------------------------------------------------
    // IMPL
    // -----------------------------------------------------------------------------------------

    synchronized void addOrReplace(AsyncRequest asyncRequest) {
        if (asyncRequest == null) {
            throw new IllegalArgumentException("asyncRequest is missing");
        }
        final String id = asyncRequest.getId();
        if (id == null) {
            throw new IllegalArgumentException("asyncRequest ID is missing");
        }
        final Element el = new Element(id, asyncRequest);
        this.cache.put(el);
        this.cache.flush();
        logger.debug("saved spot request, id: '" + id + "'");
    }

    synchronized AsyncRequest getByID(String id) {
        if (id == null) {
            return null;
        }
        final Element el = this.cache.get(id);
        if (el == null) {
            return null;
        }
        final AsyncRequest req = (AsyncRequest) el.getObjectValue();
        if (req != null) {
            return req;
        }
        logger.fatal("illegal object extension, no null values allowed");
        return null;
    }

    synchronized Collection<AsyncRequest> getAll() {
        final List allIDs = this.cache.getKeys();
        Collection<AsyncRequest> all = new HashSet<AsyncRequest>();
        for (int i = 0; i < allIDs.size(); i++) {
            String id = (String)allIDs.get(i);
            all.add(this.getByID(id));
        }
        return all;
    }

    private void loadAllFromDisk() throws IOException {
        final List allIDs = this.cache.getKeys();
        int count = 0;
        for (int i = 0; i < allIDs.size(); i++) {
            String id = (String)allIDs.get(i);
            AsyncRequest ar = this.getByID(id);
            if (ar == null) {
                throw new IOException("SI Cache is inconsistent or corrupted");
            }
            count += 1;
        }
        logger.info("Found " + count + " spot requests on disk.");
    }

    void shutdownImmediately() {
        if (this.cache != null) {
            this.cache.removeAll();
        }
    }
}
