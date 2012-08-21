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

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.LockManager;
import org.globus.workspace.RepoFileSystemAdaptor;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.persistence.DataConvert;
import org.globus.workspace.scheduler.Scheduler;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.Sweepable;
import org.globus.workspace.service.WorkspaceHome;
import org.globus.workspace.service.CurrentVMs;
import org.globus.workspace.xen.XenUtil;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.CannotTranslateException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public abstract class WorkspaceHomeImpl implements WorkspaceHome,
                                                   CurrentVMs {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(WorkspaceHomeImpl.class.getName());

    private static final InstanceResource[] EMPTY_RESOURCE_ARRAY =
            new InstanceResource[0];


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final PersistenceAdapter persistence;
    protected final LockManager lockManager;
    protected final ExecutorService executor;
    protected final Cache cache;
    protected final Lager lager;
    protected final DataConvert dataConvert;
    protected Scheduler scheduler;
    protected RepoFileSystemAdaptor repoAdaptor;

    // perhaps quartz in the future
    protected ScheduledThreadPoolExecutor scheduledExecutor;

    // see comment in initialize()
    private boolean initialized;

    // from configs
    private String backendPath;
    private String sshPath;
    private String scpPath;
    private String sshAccount;
    private String sshIdentityFile;
    private String threadPoolInitialSize;
    private String threadPoolMaxSize;
    private long sweeperDelay = 60000;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    /**
     * see class level comments, a little overly coupled in this release
     */
    public WorkspaceHomeImpl(PersistenceAdapter db,
                             LockManager lockManagerImpl,
                             CacheManager cacheManager,
                             DataConvert dataConvert,
                             Lager lagerImpl) {

        if (db == null) {
            throw new IllegalArgumentException("db may not be null");
        }
        this.persistence = db;

        if (lockManagerImpl == null) {
            throw new IllegalArgumentException("lockManager may not be null");
        }
        this.lockManager = lockManagerImpl;

        if (cacheManager == null) {
            throw new IllegalArgumentException("cacheManager may not be null");
        }

        this.cache = cacheManager.getCache("instanceCache");
        if (this.cache == null) {
            throw new IllegalArgumentException(
                    "cacheManager does not provide 'instanceCache'");
        }

        if (dataConvert == null) {
            throw new IllegalArgumentException("dataConvert may not be null");
        }
        this.dataConvert = dataConvert;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lager may not be null");
        }
        this.lager = lagerImpl;

        // todo: options etc. by wrapping as IoC bean
        this.executor = Executors.newCachedThreadPool();
    }


    // -------------------------------------------------------------------------
    // MODULE SET (avoids circular dependency problem)
    // -------------------------------------------------------------------------

    public void setScheduler(Scheduler schedulerImpl) {
        if (schedulerImpl == null) {
            throw new IllegalArgumentException("schedulerImpl may not be null");
        }
        this.scheduler = schedulerImpl;
    }

    public void setRepoAdaptor(RepoFileSystemAdaptor ra) {
        this.repoAdaptor = ra;
        XenUtil.setRepoAdaptor(ra);
    }

    // -------------------------------------------------------------------------
    // SETTERS (from outside config)
    // -------------------------------------------------------------------------

    public void setBackendPath(String path) {
        this.backendPath = path;
    }

    public void setSshPath(String ssh) {
        this.sshPath = ssh;
    }

    public void setScpPath(String scp) {
        this.scpPath = scp;
    }

    public void setSshAccount(String account) {
        this.sshAccount = account;
    }

    public void setSshIdentityFile(String idfile) {
        this.sshIdentityFile = idfile;
    }

    public void setThreadPoolInitialSize(String initialSize) {
        this.threadPoolInitialSize = initialSize;
    }

    public void setThreadPoolMaxSize(String maxSize) {
        this.threadPoolMaxSize = maxSize;
    }

    public void setSweeperDelay(long delay) {
        this.sweeperDelay = delay;
    }


    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    public synchronized void validate() throws Exception {

        logger.debug("validating/initializing");

        if (this.initialized) {
            throw new Exception("already initialized, illegal to initialize " +
                    "more than once");
        }

        // partial init counts
        this.initialized = true;

        // todo: all this static stuff will go away in favor of these things
        //       being IoC beans etc.
        WorkspaceHomeInit.initializeRequestDispatch(this.threadPoolInitialSize,
                                                    this.threadPoolMaxSize);
        WorkspaceHomeInit.initializeSSH(this.sshPath,
                                        this.scpPath,
                                        this.sshAccount,
                                        this.sshIdentityFile);

        // todo: temporary hack, RequiredVMM implementation and configurations
        //       will be more encapsulated in the future
        final InstanceResource vw = this.newEmptyResource();
        if (vw instanceof Xen) {
            if (this.backendPath == null) {
                throw new Exception(
                        "workspace control executable path is not configured");
            }
            XenUtil.setWorksp(this.backendPath);
        }

        logger.debug("validated/initialized");
    }


    // -------------------------------------------------------------------------
    // ID TYPE CONVERSIONS, ID VALUE RESTRICTION CHECKS
    // -------------------------------------------------------------------------

    public int convertID(String id) throws ManageException {
        final int idInt;
        try {
            idInt = Integer.parseInt(id);
            if (idInt < 1) {
                throw new ManageException("ID may not be less than one");
            }
        } catch (NumberFormatException e) {
            throw new ManageException("Requiring instance IDs be " +
                    "integers, for now: " + e.getMessage(), e);
        }
        return idInt;
    }

    public String convertID(int id) throws ManageException {
        if (id < 1) {
            throw new ManageException("ID may not be less than one");
        }
        return String.valueOf(id);
    }


    // -------------------------------------------------------------------------
    // NEW WORKSPACE RESOURCES
    // -------------------------------------------------------------------------

    // default configuration has this provided on the fly via IoC
    protected abstract InstanceResource newEmptyResource();

    public InstanceResource newInstance(int id) throws CreationException {
        final String idStr;
        try {
            idStr = this.convertID(id);
            return this.newInstance(idStr);
        } catch (ManageException e) {
            throw new CreationException(e.getMessage(), e);
        }
    }

    protected InstanceResource newInstance(String idStr)
            throws CreationException, ManageException {

        if (idStr == null) {
            throw new IllegalArgumentException("idStr may not be null");
        }

        final InstanceResource resource;

        final Lock lock = this.lockManager.getLock(idStr);
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new CreationException(e.getMessage(), e);
        }

        try {

            final Element el = this.cache.get(idStr);
            if (el == null) {
                resource = this.newEmptyResource();
                this.cache.put(new Element(idStr, resource));
            } else {
                throw new CreationException(
                        "ID collision, ID '" + idStr + "' already in cache");
            }

        } finally {
            lock.unlock();
        }

        return resource;
    }


    // -------------------------------------------------------------------------
    // FIND
    // -------------------------------------------------------------------------

    public InstanceResource find(String id)

            throws ManageException, DoesNotExistException {

        if (id == null) {
            throw new ManageException("id may not be null");
        }

        final InstanceResource resource;

        final Lock lock = this.lockManager.getLock(id);
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new ManageException(e.getMessage(), e);
        }

        try {

            final Element el = this.cache.get(id);
            if (el == null) {
                resource = this.newInstance(id);
                resource.load(id); // throws DoesNotExistException if not in db

                final Calendar currTime = Calendar.getInstance();
                final Calendar termTime = resource.getTerminationTime();
                if (termTime != null && termTime.before(currTime)) {
                    boolean destroyed = this.destroy(id);
                    if (destroyed) {
                      throw new DoesNotExistException(Lager.id(id) + " expired");
                    }
                }

            } else {
                resource = (InstanceResource) el.getObjectValue();
            }

        } catch (DoesNotExistException e) {
            this.cache.remove(id);
            throw e;
        } catch (CreationException e) {
            throw new ManageException(e.getMessage(), e); // ...
        } finally {
            lock.unlock();
        }

        return resource;
    }

    public InstanceResource find(int id)
            throws ManageException, DoesNotExistException {

        return this.find(this.convertID(id));
    }

    // TODO: make this termination process less expensive and memory consuming
    // TODO: in particular, push this responsibility to Scheduler, right now
    //       this is just quickly mimicking the old, inefficient setup from GT.
    public Sweepable[] currentSweeps() {
        final int[] keys;
        try {
            keys = this.persistence.findActiveWorkspacesIDs();
        } catch (WorkspaceDatabaseException e) {
            logger.fatal(e.getMessage(), e);
            return null; // *** EARLY RETURN ***
        }

        if (keys == null || keys.length == 0) {
            return null; // *** EARLY RETURN ***
        }

        final Sweepable[] sweeps = new Sweepable[keys.length];
        for (int i = 0; i < keys.length; i++) {
            final int key = keys[i];
            try {
                sweeps[i] = this.find(key);
            } catch (ManageException e) {
                final String err = Lager.id(keys[i]) + " " + e.getMessage();
                if (logger.isDebugEnabled()) {
                    logger.error(err, e);
                } else {
                    logger.error(err);
                }
            } catch (DoesNotExistException e) {
                logger.debug(Lager.id(keys[i]) + " " + e.getMessage());
            }
        }

        return sweeps;
    }

    // -------------------------------------------------------------------------
    // CLEANUP
    // -------------------------------------------------------------------------

    public void cleanup(String id)
            throws ManageException, DoesNotExistException {

        if (id == null) {
            throw new IllegalArgumentException("id may not be null");
        }

        this._cleanup(this.convertID(id));
        this.cache.remove(id);
    }

    public void _cleanup(int id)
            throws ManageException, DoesNotExistException {
        final Lock destroy_lock = this.lockManager.getLock("destroy_" + id);
        final Lock lock = this.lockManager.getLock(id);
        try {
            destroy_lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new ManageException(e.getMessage(), e);
        }

        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            destroy_lock.unlock();
            throw new ManageException(e.getMessage(), e);
        }

        try {
            final InstanceResource resource = this.find(id);
            this.scheduler.cleanup(id);
            resource.cleanup();
        } finally {
            lock.unlock();
            destroy_lock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // DESTROY
    // -------------------------------------------------------------------------

    public boolean destroy(int id)
            throws ManageException, DoesNotExistException {

        return this.destroy(this.convertID(id));
    }

    /**
     * @param id key
     * @throws ManageException error
     * @throws DoesNotExistException already gone
     */
    public boolean destroy(String id)

            throws ManageException, DoesNotExistException {

        boolean destroyed;

        if (id == null) {
            throw new IllegalArgumentException("id may not be null");
        }

        final Lock destroy_lock = this.lockManager.getLock("destroy_" + id);
        final Lock lock = this.lockManager.getLock(id);
        try {
            destroy_lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new ManageException(e.getMessage(), e);
        }

        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            destroy_lock.unlock();
            throw new ManageException(e.getMessage(), e);
        }

        try {
            final InstanceResource resource = this.find(id);
            destroyed = resource.remove();
            if (destroyed) {
                this.cache.remove(id);
            }

        } finally {
            lock.unlock();
            destroy_lock.unlock();
        }

        return destroyed;
    }

    public String destroyMultiple(int[] workspaces, String sourceStr) {
        return this.destroyMultiple(workspaces, sourceStr, false);
    }

    public String destroyMultiple(int[] workspaces, String sourceStr, boolean block) {

        final FutureTask[] tasks = new FutureTask[workspaces.length];
        for (int i = 0; i < workspaces.length; i++) {
            tasks[i] = new FutureTask(
                            new DestroyFutureTask(workspaces[i], this, block));
        }

        for (int i = 0; i < tasks.length; i++) {
            this.executor.submit(tasks[i]);
        }

        final StringBuilder buf = new StringBuilder(tasks.length * 256);

        // Log any unexpected errors.  Wait twenty seconds (normal destroy time
        // should be a matter of seconds even if there is high congestion).
        // todo: make timeout configurable
        for (int i = 0; i < tasks.length; i++) {
            try {
                final String msg = (String) tasks[i].get(20L, TimeUnit.SECONDS);
                if (msg != null) {
                    buf.append(msg);
                }
            } catch (Exception e) {
                final String msg = "Error removing workspace #" +
                        workspaces[i] + " " + sourceStr + ": " + e.getMessage();
                logger.error(msg);
                buf.append(msg);
                if (lager.traceLog) {
                    logger.error("[TRACE] " + msg, e);
                }
            }
        }

        final String ret = buf.toString();
        if (ret.trim().length() == 0) {
            return null;
        } else {
            return ret;
        }
    }


    // -------------------------------------------------------------------------
    // LIFECYCLE
    // -------------------------------------------------------------------------

    public void shutdownImmediately() {
        if (this.scheduledExecutor != null) {
            this.scheduledExecutor.shutdownNow();
        }
        if (this.executor != null) {
            this.executor.shutdownNow();
        }
        if (this.cache != null) {
            this.cache.removeAll();
        }
    }

    /**
     * @see org.nimbustools.api.services.rm.Manager#recover_initialize
     * @throws Exception problem
     */
    public void recover_initialize() throws Exception {

        if (!this.initialized) {
            throw new Exception("loading problem, not initialized yet");
        }

        if (this.scheduledExecutor != null) {
            // todo
            // should have a latch mechanism for handling many different layers
            // initializing this service instance; would need to coordinate
            // somehow.  currently we know that only one layer is interested
            // in registering listeners which is the problem...
            logger.debug("already recovered (multiple actors above)");
            return; // *** EARLY RETURN ***
        }

        this.recover_find_active_workspaces();

        // todo: options etc. by wrapping as IoC bean
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(2);
        this.scheduledExecutor.setRemoveOnCancelPolicy(true);
        this.scheduledExecutor.setMaximumPoolSize(5);

        // Pass in the general executor, not the scheduled one.  Sweeper uses that to
        // execute any tasks it creates.
        final ResourceSweeper sweeper =
                new ResourceSweeper(this.executor, this, this.lager);

        // Can't use FutureTask because of the way scheduleWithFixedDelay
        // will wrap the object.  Results in just one call instead of
        // repeating (thread state gets put into "RAN" in the inner
        // callable.  Instead, made ResourceSweeper implement
        // Runnable interface and so now is native parameter to
        // scheduleWithFixedDelay method instead of wrapped in FutureTask.
        //NOPE: final FutureTask task = new FutureTask(action);

        logger.debug("Launching sweeper with " +
                                this.sweeperDelay + "ms delay");
        this.scheduledExecutor.scheduleWithFixedDelay(sweeper,
                                                      this.sweeperDelay,
                                                      this.sweeperDelay,
                                                      TimeUnit.MILLISECONDS);
    }


    /*
     * Some may need to be expired after a container crash/restart.
     */
    private void recover_find_active_workspaces() throws ManageException {

        if (lager.traceLog) {
            logger.trace("find_active_workspaces()");
        }

        final int[] keys;
        try {
            keys = this.persistence.findActiveWorkspacesIDs();
        } catch (WorkspaceDatabaseException e) {
            throw new ManageException(e.getMessage(), e);
        }

        if (keys == null || keys.length == 0) {
            final String msg = "No workspaces were persisted when the" +
                        " container last shut down";
            if (this.lager.eventLog) {
                logger.info(msg);
            } else if (logger.isDebugEnabled()) {
                logger.debug(msg);
            }
            this.scheduler.recover(0);
            return;
        }

        // TODO: add bit to set notification pending counter

        int numRecovered = 0;
        for (int i = 0; i < keys.length; i++) {
            if (lager.traceLog) {
                logger.trace("found #" + keys[i] + " in DB");
            }

            // To better support 1000s of resources, we should create a custom
            // query for ones that need to be terminated

            try {
                this.find(keys[i]);
                if (lager.eventLog) {
                    logger.info(Lager.ev(keys[i]) + "recovered");
                }
                numRecovered += 1;
            } catch (DoesNotExistException e) {
                // if the workspace is terminated during the load process,
                // that is OK; but, it will return NoSuchResourceException
                if (lager.eventLog) {
                    logger.info(Lager.ev(keys[i]) + "resource terminated " +
                            "during recovery");
                }
            } catch (ManageException e) {
                throw e;
            }
        }

        // The ones terminated during the load process will generate a
        // notification to the scheduler before this is sent (when state
        // is set to destroying, while the destroy process does block in
        // StateTransition, the notification to scheduler is sent before
        // that happens).
        this.scheduler.recover(numRecovered);
    }


    // -------------------------------------------------------------------------
    // EXTRAS
    // -------------------------------------------------------------------------

    public ExecutorService getSharedExecutor() {
        return this.executor;
    }

    public boolean isActiveWorkspaceID(int id) throws ManageException {
        try {
            return this.persistence.isActiveWorkspaceID(id);
        } catch (WorkspaceDatabaseException e) {
            throw new ManageException(e.getMessage(), e);
        }
    }

    public boolean isActiveWorkspaceID(String id) throws ManageException {
        try {
            return this.persistence.isActiveWorkspaceID(this.convertID(id));
        } catch (WorkspaceDatabaseException e) {
            throw new ManageException(e.getMessage(), e);
        }
    }

    public InstanceResource[] findByCaller(String callerID)

            throws ManageException {

        // not an efficient way to do this, waiting on some object model
        // revisions to do anything about it
        final int[] ids = this.findIDsByCaller(callerID);

        if (ids == null || ids.length == 0) {
            return EMPTY_RESOURCE_ARRAY;
        }

        final List resourceList = new ArrayList(ids.length);

        // Not worried about races w/ destruction here.

        for (int i = 0; i < ids.length; i++) {
            try {
                resourceList.add(this.find(ids[i]));
            } catch (DoesNotExistException e) {
                if (lager.traceLog) {
                    logger.trace("findByCaller could not retrieve " +
                            Lager.id(ids[i]));
                }
            }
        }

        if (resourceList.isEmpty()) {
            return EMPTY_RESOURCE_ARRAY;
        } else {
            return (InstanceResource[]) resourceList.toArray(
                            new InstanceResource[resourceList.size()]);
        }
    }

    public InstanceResource[] findByIP(String ip) throws ManageException {

        // Not an efficient way to do this, waiting on some object model
        // revisions to do anything about it.  Also, it is highly likely
        // that all VMs are in instance cache at this point.

        final int[] keys;
        try {
            keys = this.persistence.findActiveWorkspacesIDs();
        } catch (WorkspaceDatabaseException e) {
            throw new ManageException(e.getMessage(), e);
        }

        if (keys == null || keys.length == 0) {
            return EMPTY_RESOURCE_ARRAY;
        }

        final List resourceList = new ArrayList(keys.length);

        // Not worried about races w/ destruction here.

        for (int key : keys) {
            try {
                InstanceResource resource = this.find(key);
                NIC[] nics = this.dataConvert.getNICs(resource.getVM());
                for (NIC nic : nics) {
                    if (nic.getIpAddress().equals(ip)) {
                        resourceList.add(resource);
                        break;
                    }
                }
            } catch (DoesNotExistException e) {
                if (lager.traceLog) {
                    logger.trace("findByIP could not retrieve " +
                            Lager.id(key));
                }
            } catch (CannotTranslateException e) {
                logger.warn(e.getMessage());
            }
        }

        final InstanceResource[] ret;

        if (resourceList.isEmpty()) {
            ret = EMPTY_RESOURCE_ARRAY;
        } else {
            ret = (InstanceResource[]) resourceList.toArray(
                            new InstanceResource[resourceList.size()]);
        }

        if (lager.traceLog) {
            logger.trace("findByIP found " + ret.length);
        }

        return ret;
    }

    public synchronized InstanceResource[] findAll()

            throws ManageException {

        // not an efficient way to do this, waiting on some object model
        // revisions to do anything about it
        final int[] keys;
        try {
            keys = this.persistence.findActiveWorkspacesIDs();
        } catch (WorkspaceDatabaseException e) {
            throw new ManageException(e.getMessage(), e);
        }

        if (keys == null || keys.length == 0) {
            return EMPTY_RESOURCE_ARRAY;
        }

        final List resourceList = new ArrayList(keys.length);

        // Not worried about races w/ destruction here.

        for (int i = 0; i < keys.length; i++) {
            try {
                resourceList.add(this.find(keys[i]));
            } catch (DoesNotExistException e) {
                if (lager.traceLog) {
                    logger.trace("findGlobalAll could not retrieve " +
                            Lager.id(keys[i]));
                }
            }
        }

        final InstanceResource[] ret;

        if (resourceList.isEmpty()) {
            ret = EMPTY_RESOURCE_ARRAY;
        } else {
            ret = (InstanceResource[]) resourceList.toArray(
                            new InstanceResource[resourceList.size()]);
        }

        if (lager.traceLog) {
            logger.trace("findAll found " + ret.length);
        }

        return ret;
    }

    /**
     * @param callerID may not be null
     * @return IDs, never null, may be length zero
     * @throws ManageException problem
     */
    public int[] findIDsByCaller(String callerID)

            throws ManageException {

        try {
            return this.persistence.findVMsByOwner(callerID);
        } catch (WorkspaceDatabaseException e) {
            throw new ManageException(e.getMessage(), e);
        }
    }

    public int countIDsByCaller(String callerID)

            throws ManageException {

        return this.findIDsByCaller(callerID).length;
    }


    // -------------------------------------------------------------------------
    // OTHER
    // -------------------------------------------------------------------------

    public String getVMMReport() {
        return this.scheduler.getVMMReport();
    }

    public String[] getResourcePools() {
        try {
            return this.persistence.getResourcePools();
        }
        catch(WorkspaceDatabaseException e) {
            // Error logged down the call stack
            return new String[0];
        }
    }
}
