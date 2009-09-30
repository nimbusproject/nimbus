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

package org.globus.workspace.service.impls;

import org.globus.workspace.service.WorkspaceGroupHome;
import org.globus.workspace.service.GroupResource;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.LockManager;
import org.globus.workspace.Lager;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.DoesNotExistException;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.safehaus.uuid.UUIDGenerator;

import java.util.List;
import java.util.LinkedList;

import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;

public abstract class GroupHomeImpl implements WorkspaceGroupHome {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(GroupHomeImpl.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    // collaborators
    protected final PersistenceAdapter persistence;
    protected final WorkspaceHome whome;
    protected final Cache cache;
    protected final LockManager lockManager;
    protected final Lager lager;

    // other
    protected final UUIDGenerator uuidGen = UUIDGenerator.getInstance();

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public GroupHomeImpl(PersistenceAdapter persistenceAdapter,
                         LockManager lockManagerImpl,
                         CacheManager cacheManager,
                         WorkspaceHome workspaceHome,
                         Lager lagerImpl) {
        
        if (persistenceAdapter == null) {
            throw new IllegalArgumentException("persistenceAdapter may not be null");
        }
        this.persistence = persistenceAdapter;

        if (workspaceHome == null) {
            throw new IllegalArgumentException("workspaceHome may not be null");
        }
        this.whome = workspaceHome;

        if (lockManagerImpl == null) {
            throw new IllegalArgumentException("lockManagerImpl may not be null");
        }
        this.lockManager = lockManagerImpl;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;

        if (cacheManager == null) {
            throw new IllegalArgumentException("cacheManager may not be null");
        }

        this.cache = cacheManager.getCache("groupCache");
        if (this.cache == null) {
            throw new IllegalArgumentException(
                    "cacheManager does not provide 'groupCache'");
        }
    }

    
    // -------------------------------------------------------------------------
    // NEW GROUP RESOURCES
    // -------------------------------------------------------------------------

    // default configuration has this provided on the fly via IoC
    protected abstract GroupResource newEmptyResource();

    public GroupResource newGroup(String creatorID) throws ManageException {

        final GroupResource resource = this.newEmptyResource();
        
        final String idStr = this.uuidGen.generateRandomBasedUUID().toString();

        final Lock lock = this.lockManager.getLock(idStr);
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new ManageException(e.getMessage(), e);
        }

        try {

            resource.setID(idStr);
            resource.setCreatorID(creatorID);
            this.persistence.addGroup(resource);

            final Element el = this.cache.get(idStr);
            if (el == null) {
                this.cache.put(new Element(idStr, resource));
            } else {
                throw new ManageException("UUID collision in groups cache, " +
                        "ID '" + idStr + "' already exists (seriously?)");
            }

        } finally {
            lock.unlock();
        }

        if (this.lager.eventLog) {
            logger.info(Lager.groupev(idStr) +
                    " created on behalf of '" + creatorID + "'");
        }

        return resource;
    }


    // -------------------------------------------------------------------------
    // FIND
    // -------------------------------------------------------------------------

    public GroupResource find(String groupid)

            throws ManageException, DoesNotExistException {
        
        if (groupid == null) {
            throw new ManageException("groupid may not be null");
        }

        final GroupResource resource;

        final Lock lock = this.lockManager.getLock(groupid);
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new ManageException(e.getMessage(), e);
        }

        try {

            final Element el = this.cache.get(groupid);
            if (el == null) {
                resource = this.newEmptyResource();
                // throws DoesNotExistException if not in db
                this.persistence.loadGroup(groupid, resource);

            } else {
                resource = (GroupResource) el.getObjectValue();
            }

        } finally {
            lock.unlock();
        }

        return resource;
    }

    public int[] findMemberIDs(String groupid)

            throws ManageException, DoesNotExistException {

        if (groupid == null || groupid.trim().length() == 0) {
            throw new DoesNotExistException("groupid is missing/empty");
        }
        return this.persistence.findVMsInGroup(groupid);
    }

    public InstanceResource[] findMembers(String groupid)

            throws ManageException, DoesNotExistException {

        // don't know number of valid members yet because three could be a
        // check-then-act problem with findVMsInGroup result and some instance
        // destruction in the meantime (which is no big deal, groups are just
        // shortcuts for dispatching work to the locked, consistent instances)
        final List retlist = new LinkedList();

        // will never be null return array, just empty
        final int[] ids = this.findMemberIDs(groupid);

        for (int i = 0; i < ids.length; i++) {

            final InstanceResource rsrc;
            
            try {
                rsrc = this.whome.find(ids[i]);
            } catch (DoesNotExistException e) {
                continue; // *** SKIP ***
            } catch (Throwable t) {
                if (logger.isDebugEnabled()) {
                    logger.error(t.getMessage(), t);
                } else {
                    logger.error(t.getMessage());
                }
                continue; // *** SKIP ***
            }

            retlist.add(rsrc);
        }

        return (InstanceResource[]) retlist.toArray(
                    new InstanceResource[retlist.size()]);
    }


    // -------------------------------------------------------------------------
    // DESTROY
    // -------------------------------------------------------------------------

    public void destroy(String groupid) throws ManageException,
                                               DoesNotExistException {
        
        if (groupid == null) {
            throw new ManageException("groupid may not be null");
        }

        final Lock lock = this.lockManager.getLock(groupid);
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new ManageException(e.getMessage(), e);
        }

        try {
            final GroupResource resource = this.find(groupid);
            resource.remove();
            this.cache.remove(groupid);

        } finally {
            lock.unlock();
        }
    }
}
