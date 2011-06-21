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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import java.io.IOException;
import java.util.Collection;


public class AsyncRequestMap {

    // -----------------------------------------------------------------------------------------
    // STATIC VARIABLES
    // -----------------------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(AsyncRequestMap.class.getName());



    // -----------------------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -----------------------------------------------------------------------------------------

    private PersistenceAdapter persistence;
    

    // -----------------------------------------------------------------------------------------
    // CONSTRUCTORS
    // -----------------------------------------------------------------------------------------

    public AsyncRequestMap(PersistenceAdapter persistenceAdapter) throws IOException {

        if (persistenceAdapter == null) {
            throw new IllegalArgumentException("persistenceAdapter may not be null");
        }

        this.persistence = persistenceAdapter;
        this.loadAllFromDisk();
    }

    
    // -----------------------------------------------------------------------------------------
    // IMPL
    // -----------------------------------------------------------------------------------------

    synchronized public void addOrReplace(AsyncRequest asyncRequest) {
        if (asyncRequest == null) {
            throw new IllegalArgumentException("asyncRequest is missing");
        }
        final String id = asyncRequest.getId();
        if (id == null) {
            throw new IllegalArgumentException("asyncRequest ID is missing");
        }

        try {
            this.persistence.addAsyncRequest(asyncRequest);
        } catch(WorkspaceDatabaseException e) {
            logger.error("Problem persisting AsyncRequest: ", e);
        }

        logger.debug("saved spot request, id: '" + id + "'");
    }

    synchronized public AsyncRequest getByID(String id) {
        if (id == null) {
            return null;
        }

        try {
            final AsyncRequest asyncRequest = this.persistence.getAsyncRequest(id);
            if (asyncRequest != null) {
                return asyncRequest;
            }
        } catch(WorkspaceDatabaseException e) {
            logger.error("Couldn't retrieve " + id + " from persistence");
        }


        logger.fatal("illegal object extension, no null values allowed");
        return null;
    }

    synchronized public Collection<AsyncRequest> getAll() {

        Collection<AsyncRequest> all = null;
        try {
            all = this.persistence.getAllAsyncRequests();
        } catch(WorkspaceDatabaseException e) {
            logger.error("Unable to load spot instances from persistence");
        }

        return all;
    }

    private void loadAllFromDisk() throws IOException {
        Collection<AsyncRequest> all = this.getAll();
        int count = 0;
        if (all != null) {
            count = all.size();
        }
        logger.info("Found " + count + " spot requests on disk.");
    }

    void shutdownImmediately() {
        logger.debug("Shut down stub");
    }
}
