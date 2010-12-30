/*
 * Copyright 1999-2011 University of Chicago
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

package org.globus.workspace.scheduler.backfill;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.persistence.PersistenceAdapter;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.nimbustools.api._repr._AsyncCreateRequest;
import org.nimbustools.api._repr._Caller;
import org.nimbustools.api._repr.vm._NIC;
import org.nimbustools.api._repr.vm._RequiredVMM;
import org.nimbustools.api._repr.vm._ResourceAllocation;
import org.nimbustools.api._repr.vm._Schedule;
import org.nimbustools.api._repr.vm._VMFile;
import org.nimbustools.api.repr.AsyncCreateRequest;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.RequestInfo;
import org.nimbustools.api.repr.si.SIConstants;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.VMFile;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.CoSchedulingException;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.DoesNotExistException;
import org.nimbustools.api.services.rm.ManageException;
import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.rm.MetadataException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.SchedulingException;

import java.net.URI;
import java.net.URISyntaxException;

public class Backfill {

    private static final Log logger =
            LogFactory.getLog(Backfill.class.getName());

    private final PersistenceAdapter persistenceAdapter;
    private final ReprFactory reprFactory;
    private final Lager lager;
    private Manager manager;

    private boolean backfillEnabled;
    private int maxInstances;
    private String diskImage;
    private int memoryMB;
    private int vcpus;
    private int durationSeconds;
    private String network;
    private int siteCapacity;

    public Backfill(PersistenceAdapter persistenceAdapter,
                    ReprFactory reprFactory,
                    Lager lager) {
        this.persistenceAdapter = persistenceAdapter;
        this.reprFactory = reprFactory;
        this.lager = lager;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public boolean isBackfillDisabled() {
        return !this.backfillEnabled;
    }

    public boolean isBackfillEnabled() {
        return this.backfillEnabled;
    }

    public int getMaxInstances() {
        return this.maxInstances;
    }

    public String getDiskImage() {
        return this.diskImage;
    }

    public int getMemoryMB() {
        return this.memoryMB;
    }

    public int getVcpus() {
        return this.vcpus;
    }

    public int getDurationSeconds() {
        return this.durationSeconds;
    }

    public String getNetwork() {
        return this.network;
    }

    public int getSiteCapacity() {
        return siteCapacity;
    }

    public void setBackfillEnabled(boolean backfillEnabled) {
        this.backfillEnabled = backfillEnabled;
    }

    public void setMaxInstances(int maxInstances) {
        this.maxInstances = maxInstances;
    }

    public void setDiskImage(String diskImage) {
        this.diskImage = diskImage;
    }

    public void setMemoryMB(int memoryMB) {
        this.memoryMB = memoryMB;
    }

    public void setVcpus(int vcpus) {
        this.vcpus = vcpus;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public void setSiteCapacity(int siteCapacity) {
        this.siteCapacity = siteCapacity;
    }

    /**
     * This method is called after service startup (not bean initialization).  There are
     * five situations to deal with:
     *
     * 1. Backfill was disabled and it is still disabled
     * 2. Backfill was disabled and now it is enabled
     * 3. Backfill was enabled and now it is disabled
     * 4. Backfill was enabled, is still enabled, and now there is a new configuration
     * 5. Backfill was enabled, is still enabled, and it's the same configuration
     * 6. Backfill was enabled, is still enabled, and it's the same configuration -- and the
     *    configuration is for max-instances and the site's resource pool has been changed.
    */
    public void initiateBackfill() throws Exception {

        if (this.persistenceAdapter == null || this.reprFactory == null) {
            throw new IllegalStateException("You may not invoke initiateBackfill() on a " +
                                            "backfill instance not instantiated via IoC");
        }
        if (this.manager == null) {
            throw new IllegalStateException("You may not invoke initiateBackfill() without " +
                                            "priming a manager instance");
        }

        final int currentSiteCapacity = this.pollLiveSiteCapacity();
        logger.debug("current site capacity MB: " + currentSiteCapacity);
        this.setSiteCapacity(currentSiteCapacity);

        final Backfill oldBackfill = this.persistenceAdapter.getStoredBackfill();

        if (this.isBackfillDisabled()) {
            logger.info("Backfill is disabled.");
            if (oldBackfill != null && oldBackfill.isBackfillEnabled()) {
                // 3. Backfill was enabled and now it is disabled
                logger.info("Backfill was previously enabled.");
                this.withdrawAllRequests();
            } else {
                // 1. Backfill was disabled and it is still disabled
                logger.info("Backfill was previously disabled, nothing to do");
            }
            this.saveCurrentConfiguration();
            return; // EARLY RETURN
        }
        
        logger.info("Backfill is enabled.");

        // 2. Backfill was disabled and now it is enabled
        if (oldBackfill != null && oldBackfill.isBackfillDisabled()) {
            logger.info("Backfill was previously disabled.");
            this.registerBackfillRequest();
            this.saveCurrentConfiguration();
            return; // EARLY RETURN
        }

        boolean newConfig = false;
        if (oldBackfill == null || !oldBackfill.equals(this)) {
            newConfig = true;
            // 4. Backfill was enabled, is still enabled, and now there is a new configuration
            logger.info("Backfill was previously enabled, but has a new configuration.");
        }

        // 6. Backfill was enabled, is still enabled, and it's the same configuration -- and the
        //    configuration is for max-instances and the site's resource pool has been changed.
        // If newConfig is true, no need to check because this will be recalculated anyhow.
        // What we need to catch is where the (max) config hasn't changed but the site has!
        if (!newConfig && this.getMaxInstances() == 0) {
            if (oldBackfill != null
                    && oldBackfill.getSiteCapacity() != currentSiteCapacity) {
                newConfig = true;
                logger.info("Site reports different VM capacity, going to recalculate " +
                                "backfill instance number and reconfigure.");
            }
        }

        if (newConfig) {
            this.withdrawAllRequests();
            this.registerBackfillRequest();
            this.saveCurrentConfiguration();
        } else {
            // 5. Backfill was enabled, is still enabled, and it's the same configuration
            logger.info("Backfill was previously enabled and has the same configuration, " +
                                "nothing to do.");
        }
    }

    private void withdrawAllRequests()
            throws ManageException, AuthorizationException, DoesNotExistException {

        // From http://docs.amazonwebservices.com/AWSEC2/latest/APIReference/ApiReference-soap-CancelSpotInstanceRequests.html
        // Canceling a Spot Instance request does not terminate running Spot Instances
        // associated with the request.
        // But, with backfill we want to withdraw, so the async manager will do that for these
        // requests (that are marked backfill by "isSpotRequest()" being false.

        logger.info("Withdrawing previous backfill configuration from the scheduler");
        if (this.persistenceAdapter == null
                || this.manager == null || this.reprFactory == null) {
            throw new IllegalStateException("Programmer error.");
        }
        _Caller superuser = this.reprFactory._newCaller();
        superuser.setIdentity("BACKFILL_SUPERUSER");
        superuser.setSuperUser(true);
        RequestInfo[] bfRequests = this.manager.getBackfillRequestsByCaller(superuser);

        if (bfRequests == null || bfRequests.length == 0) {
            logger.info("No previous backfill requests were registered");
            return;
        }

        final String[] ids = new String[bfRequests.length];

        for (int i = 0; i < bfRequests.length; i++) {
            final String reqID = bfRequests[i].getRequestID();
            ids[i] = reqID;
        }

        this.manager.cancelBackfillRequests(ids, superuser);
    }

    private void registerBackfillRequest()
            throws WorkspaceDatabaseException, SchedulingException,
                   ResourceRequestDeniedException, CreationException, AuthorizationException,
                   MetadataException, CoSchedulingException {

        logger.info("Registering backfill configuration with the scheduler");

        if (this.persistenceAdapter == null
                || this.manager == null || this.reprFactory == null) {
            throw new IllegalStateException("Programmer error.");
        }

        int numInstances;
        if (this.maxInstances == 0) {
            // If zero, request is for as many as possible.
            // This functionality should be consolidated: you put in a type of instance and
            // it tells you how many can fit in a given capacity.  This calculation does not
            // take into account a possible reduction because of limited network mappings.
            // But the scheduler can't launch more instances than there are networks available
            // so it will top out.
            numInstances = this.getSiteCapacity() / this.memoryMB;
            if (numInstances < 1) {
                logger.warn("Not enough total VM capacity for even one backfill node.");
                numInstances = 1;
            }
        } else {
            numInstances = this.maxInstances;
        }

        _Caller superuser = this.reprFactory._newCaller();
        superuser.setIdentity("BACKFILL(SUPERUSER)");
        superuser.setSuperUser(true);

        for (int i = 0; i < numInstances; i++) {
            AsyncCreateRequest req = this.getBackfillRequest(1, "BACKFILL-" + (i+1));
            this.manager.addBackfillRequest(req, superuser);
        }
    }

    // Allow easy IoC driven extension for customizing this better
    public AsyncCreateRequest getBackfillRequest(int numInstances, String name)
            throws WorkspaceDatabaseException {

        if (this.lager.traceLog) {
            final StringBuilder sb = new StringBuilder("Backfill request:");
            sb.append("\nNum instances: ").append(numInstances);
            sb.append("\nImage: ").append(this.getDiskImage());
            sb.append("\nNetwork: ").append(this.getNetwork());
            sb.append("\nMemory: ").append(this.getMemoryMB());
            sb.append("\nvcpus: ").append(this.getVcpus());
            logger.debug(sb.toString());
        }

        final _AsyncCreateRequest req = this.reprFactory._newBackfillRequest();
        req.setInstanceType(SIConstants.SI_TYPE_BASIC);

        req.setName(name);

        final _NIC nic = this.reprFactory._newNIC();
        nic.setNetworkName(this.getNetwork());
        nic.setAcquisitionMethod(NIC.ACQUISITION_AllocateAndConfigure);
        req.setRequestedNics(new _NIC[]{nic});


        final _ResourceAllocation ra = this.reprFactory._newResourceAllocation();
        req.setRequestedRA(ra);
        final _Schedule schedule = this.reprFactory._newSchedule();
        schedule.setDurationSeconds(this.getDurationSeconds());
        req.setRequestedSchedule(schedule);
        ra.setNodeNumber(numInstances);
        ra.setIndCpuCount(this.getVcpus());
        ra.setMemory(this.getMemoryMB());
        req.setShutdownType(CreateRequest.SHUTDOWN_TYPE_TRASH);
        req.setInitialStateRequest(CreateRequest.INITIAL_STATE_RUNNING);

        ra.setArchitecture(ResourceAllocation.ARCH_x86);
        ra.setSpotInstance(true);
        final _RequiredVMM reqVMM = this.reprFactory._newRequiredVMM();
        reqVMM.setType("Xen");
        reqVMM.setVersions(new String[]{"3"});
        req.setRequiredVMM(reqVMM);

        final _VMFile file = this.reprFactory._newVMFile();
        file.setRootFile(true);
        file.setBlankSpaceName(null);
        file.setBlankSpaceSize(-1);
        try {
            file.setURI(new URI("file://" + this.getDiskImage()));
        } catch (URISyntaxException e) {
            throw new WorkspaceDatabaseException(e.getMessage());
        }
        file.setMountAs("sda1");
        file.setDiskPerms(VMFile.DISKPERMS_ReadWrite);
        req.setVMFiles(new _VMFile[]{file});

        return req;
    }

    private int pollLiveSiteCapacity() throws WorkspaceDatabaseException {
        return this.persistenceAdapter.getTotalMaxMemory();
    }

    private void saveCurrentConfiguration() throws WorkspaceDatabaseException {
        if (this.persistenceAdapter == null) {
            throw new IllegalStateException("Programmer error.");
        }
        this.persistenceAdapter.setBackfill(this);
    }

    public void validate() throws Exception {

        if (this.persistenceAdapter == null) {
            throw new IllegalStateException("There is no persistenceAdapter configured");
        }

        if (this.maxInstances < 0) {
            throw new Exception("maxInstances may not be less than 0");
        }
        if (this.diskImage == null) {
            throw new Exception("diskImage may not be null");
        }
        if (this.memoryMB < 8) {
            throw new Exception("memoryMB must be a reasonable value. " +
                                "Have you considered at least 64 MB?");
        }
        if (this.durationSeconds < 60) {
            throw new Exception("durationSeconds must be 60 seconds or longer.");
        }
        if (this.network == null) {
            throw new Exception("network may not be null");
        }

        logger.debug("Validated backfill settings");
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Backfill backfill = (Backfill) o;

        if (backfillEnabled != backfill.backfillEnabled) return false;
        if (durationSeconds != backfill.durationSeconds) return false;
        if (maxInstances != backfill.maxInstances) return false;
        if (memoryMB != backfill.memoryMB) return false;
        if (vcpus != backfill.vcpus) return false;
        if (diskImage != null ? !diskImage.equals(backfill.diskImage) :
                backfill.diskImage != null)
            return false;
        if (network != null ? !network.equals(backfill.network) : backfill.network != null)
            return false;

        return true;
    }

    public int hashCode() {
        int result = (backfillEnabled ? 1 : 0);
        result = 31 * result + maxInstances;
        result = 31 * result + (diskImage != null ? diskImage.hashCode() : 0);
        result = 31 * result + memoryMB;
        result = 31 * result + vcpus;
        result = 31 * result + durationSeconds;
        result = 31 * result + (network != null ? network.hashCode() : 0);
        return result;
    }
}
