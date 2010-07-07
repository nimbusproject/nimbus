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

package org.globus.workspace;

import org.globus.workspace.manager.DelegatingManager;
import org.globus.workspace.scheduler.defaults.SlotManagement;

import org.nimbustools.api._repr._Caller;
import org.nimbustools.api._repr._CreateRequest;
import org.nimbustools.api._repr.vm._NIC;
import org.nimbustools.api._repr.vm._RequiredVMM;
import org.nimbustools.api._repr.vm._ResourceAllocation;
import org.nimbustools.api._repr.vm._Schedule;
import org.nimbustools.api._repr.vm._VMFile;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CreateResult;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.VMFile;
import org.nimbustools.api.repr.ReprFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;
import java.net.URI;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;

import commonj.timers.Timer;
import commonj.timers.TimerManager;

public class Backfill {
    
    private static final Log logger =
            LogFactory.getLog(Backfill.class.getName());

    protected final ReprFactory repr;
    protected final PathConfigs pathConfigs;
    protected final TimerManager timerManager;
    protected final SlotManagement slotManager;
    protected DelegatingManager manager;

    private boolean backfillDisabled;
    private int maxInstances;
    private int curNumInstances = 0;
    private String diskImage;
    private int memoryMB;
    private int durationSeconds;
    private String terminationPolicy;
    private int retryPeriod;
    private String network;
    private File backfillReqFile;
    private Timer backfillTimer = null;

    public Backfill(ReprFactory reprFactory,
                    PathConfigs pathConfigs,
                    TimerManager timerManager,
                    SlotManagement slotManager) {

        if (reprFactory == null) {
            throw new IllegalArgumentException("reprFactory may not be null");
        }
        this.repr = reprFactory;

        if (pathConfigs == null) {
            throw new IllegalArgumentException("pathConfigs may not be null");
        }
        this.pathConfigs = pathConfigs;
        this.backfillReqFile =
                new File(this.pathConfigs.getLocalTempDirPath() +
                         "/backfill.request");

        if (timerManager == null) {
            throw new IllegalArgumentException("timerManager may not be null");
        }
        this.timerManager = timerManager;

        if (slotManager == null) {
            throw new IllegalArgumentException("slotManager may not be null");
        }
        this.slotManager = slotManager;
    }

    public boolean getBackfillDisabled() {
        return this.backfillDisabled;
    }

    public int getMaxInstances() {
        return this.maxInstances;
    }

    public int getCurNumInstances() {
        return this.curNumInstances;
    }

    public String getDiskImage() {
        return this.diskImage;
    }

    public int getMemoryMB() {
        return this.memoryMB;
    }

    public int getDurationSeconds() {
        return this.durationSeconds;
    }

    public String getTerminationPolicy() {
        return this.terminationPolicy;
    }

    public int getRetryPeriod() {
        return this.retryPeriod;
    }

    public String getNetwork() {
        return this.network;
    }

    public void setBackfillDisabled(boolean backfillDisabled) {
        this.backfillDisabled = backfillDisabled;
    }

    public void setMaxInstances(int maxInstances) {
        this.maxInstances = maxInstances;
    }

    public void addCurInstances(int num) {
        this.curNumInstances += num;
    }

    public void subCurInstances(int num) {
        this.curNumInstances -= num;
    }

    public void setDiskImage(String diskImage) {
        this.diskImage = diskImage;
    }

    public void setMemoryMB(int memoryMB) {
        this.memoryMB = memoryMB;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public void setTerminationPolicy(String terminationPolicy) {
        this.terminationPolicy = terminationPolicy;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public void setRetryPeriod(int retryPeriod) {
        this.retryPeriod = retryPeriod;
    }

    public void setDelegatingManager(DelegatingManager manager) {
        this.manager = manager;
    }

    public void cancelBackfillRequest() {
        logger.info("Cancelling backfill request");
        this.backfillTimer.cancel();
        this.backfillReqFile.delete();
    }

    /**
     * This method should be called on service startup. It is responsible for
     * starting the backfill timer (if the backfill feature is enabled).
     *
     * It also checks to see if a persistence backfill file exists, and if the
     * backfill configuration has changed. If the backfill configuration has
     * changed then it cancels the previous backfill request and submits a new
     * one.
     */
    public void initiateBackfill() throws Exception {

        if (this.backfillDisabled == false) {
            logger.debug("Backfill is enabled");

            if (this.backfillReqFile.exists()) {
                String curBackfillStr = buildCurBackfillReqStrB().toString();
                String prevBackfillStr = readPrevBackfillReqStrB().toString();
                if ((curBackfillStr.compareTo(prevBackfillStr)) == 0) {
                    logger.debug("Current and previous backfill requests " +
                                 "match");
                } else {
                    logger.debug("The current backfill request doesn't " +
                                 "match the previous backfill request");

                    this.cancelBackfillRequest();
                    this.writeCurBackfillReq();
                }
            } else {
                this.writeCurBackfillReq();
            }

            this.launchBackfillTimer();
        } else {
            logger.info("Backfill is disabled");
        }
    }

    public void launchBackfillTimer() {

        if (this.backfillDisabled == true) {
            return;
        }

        if (this.backfillTimer != null) {
            this.backfillTimer.cancel();
        }
        BackfillTimer backfillTimer;
        backfillTimer = new BackfillTimer(this);
        Date backfillStart = new Date();
        this.backfillTimer = this.timerManager.schedule(backfillTimer,
                backfillStart,
                this.retryPeriod * 1000);
    }

    // Returns true if a backfill node was successfully killed.
    // False, if it wasn't able to kill a backfill node.
    public boolean terminateBackfillNode() {

        int vmid;
        if ((this.terminationPolicy.compareTo("ANY")) == 0) {
            logger.debug("Backfill is using the ANY policy");
            vmid = this.slotManager.getBackfillVMID();
        } else {
            logger.debug("Backfill is using the MOST_RECENT policy");
            vmid = this.slotManager.getMostRecentBackfillVMID();
        }

        String vmidStr = Integer.toString(vmid);
        logger.debug("Terminating backfill node with ID: " + vmidStr);

        Caller caller = this.getBackfillCaller();
        try {
            this.manager.trash(vmidStr, 0, caller);
            this.subCurInstances(1);
            return true;
        } catch (Exception e) {
            logger.error("Problem terminating backfill node: " +
                         e.getMessage());
        }
        return false;
    }

    /**
     * This attempts to launch a new backfill node.
     * It should only be called by the backfill timer (ugh,
     * should really be changed so ONLY that can happen).
     * If backfill nodes should be launched (at some place else
     * in the code) then the backfill timer should simply be relaunched
     * (via the launchBackfillTimer method in this class)
     * as it will attempt to launch backfill nodes. The timer is what
     * respects the max backfill instances configuration value, and if
     * backfill is disabled then it won't be relaunched.
     */
    public void createBackfillNode() throws Exception {

        CreateRequest req = this.getBackfillRequest("BACKFILL_REQUEST");
        logger.debug("Backfill create request:\n" + req.toString());

        Caller caller = this.getBackfillCaller();
        logger.debug("Backfill caller:\n" + caller.toString());

        CreateResult create = this.manager.create(req, caller);
    }

    /**
     * writeCurBackfillReq, readPrevBackfillReqStrB, and
     * buildCurBackfillReqStrB are simply a hack to support persistence
     * between service restarts or crashes. Ideally, they should become
     * obsolete once the backfill feature is integrated with support for Spot
     * Instances.
     */
    private void writeCurBackfillReq() throws Exception {

        if (this.backfillReqFile.exists()) {
            logger.debug("Backfill request file already exists");
            logger.debug("Skipping write to backfill request file");
        } else {
            logger.debug("Attempting to write backfill file:\n" +
                         this.backfillReqFile.toString());
            this.backfillReqFile.createNewFile();
            FileWriter backfillFW = new FileWriter(this.backfillReqFile);
            BufferedWriter backfillBW = new BufferedWriter(backfillFW);
            backfillBW.write(this.buildCurBackfillReqStrB().toString());
            backfillBW.close();
        }
    }

    private StringBuilder readPrevBackfillReqStrB() throws Exception {

        StringBuilder prevBackfillStrB = new StringBuilder();

        try {
            logger.debug("Attempting to read backfill request file:\n" +
                         this.backfillReqFile.toString());
            FileReader backfillFR = new FileReader(this.backfillReqFile);
            BufferedReader backfillBR = new BufferedReader(backfillFR);
            String line;

            while (( line = backfillBR.readLine()) != null) {
                prevBackfillStrB.append(line);
                prevBackfillStrB.append("\n");
            }
            logger.debug("For the previous backfill request file," +
                         " we read:\n" + prevBackfillStrB.toString());
            backfillBR.close();
        } catch (FileNotFoundException e) {
            logger.debug("Can't read a file that doesn't exist:\n" +
                         this.backfillReqFile.toString());
        }

        return prevBackfillStrB;
    }

    private StringBuilder buildCurBackfillReqStrB() {

        StringBuilder curBackfillReqStrB = new StringBuilder();

        curBackfillReqStrB.append(Boolean.toString(this.backfillDisabled));
        curBackfillReqStrB.append("\n");
        curBackfillReqStrB.append(Integer.toString(this.maxInstances));
        curBackfillReqStrB.append("\n");
        curBackfillReqStrB.append(this.diskImage);
        curBackfillReqStrB.append("\n");
        curBackfillReqStrB.append(Integer.toString(this.memoryMB));
        curBackfillReqStrB.append("\n");
        curBackfillReqStrB.append(Integer.toString(this.durationSeconds));
        curBackfillReqStrB.append("\n");
        curBackfillReqStrB.append(this.network);
        curBackfillReqStrB.append("\n");
        curBackfillReqStrB.append(this.pathConfigs.getLocalTempDirPath());
        curBackfillReqStrB.append("\n");

        return curBackfillReqStrB;
    }

    private Caller getBackfillCaller() {
        final _Caller caller = this.repr._newCaller();
        caller.setIdentity("BACKFILL_CALLER");
        return caller;
    }

    private CreateRequest getBackfillRequest(String name) throws Exception {

        final _CreateRequest req = this.repr._newCreateRequest();
        req.setName(name);

        final _NIC nic = this.repr._newNIC();
        nic.setNetworkName(this.network);
        nic.setAcquisitionMethod(NIC.ACQUISITION_AllocateAndConfigure);
        req.setRequestedNics(new _NIC[]{nic});

        final _ResourceAllocation ra = this.repr._newResourceAllocation();
        req.setRequestedRA(ra);
        final _Schedule schedule = this.repr._newSchedule();
        schedule.setDurationSeconds(this.durationSeconds);
        schedule.setBackfillReq(true);
        req.setRequestedSchedule(schedule);
        ra.setNodeNumber(1);
        ra.setMemory(this.memoryMB);
        req.setShutdownType(CreateRequest.SHUTDOWN_TYPE_TRASH);
        req.setInitialStateRequest(CreateRequest.INITIAL_STATE_RUNNING);

        ra.setArchitecture(ResourceAllocation.ARCH_x86);
        final _RequiredVMM reqVMM = this.repr._newRequiredVMM();
        reqVMM.setType("Xen");
        reqVMM.setVersions(new String[]{"3"});
        req.setRequiredVMM(reqVMM);

        final _VMFile file = this.repr._newVMFile();
        file.setRootFile(true);
        file.setBlankSpaceName(null);
        file.setBlankSpaceSize(-1);
        file.setURI(new URI("file://" + this.diskImage));
        file.setMountAs("sda1");
        file.setDiskPerms(VMFile.DISKPERMS_ReadWrite);
        req.setVMFiles(new _VMFile[]{file});

        return req;
    }

    public void validate() throws Exception {

        logger.debug("validating");

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
            throw new Exception("durationSeconds must be 60 seconds " +
                                "or longer.");
        }
        if (this.retryPeriod < 1) {
            throw new Exception("retryPeriod must be 1 or greater");   
        }
        if (this.network == null) {
            throw new Exception("network may not be null");
        }

        logger.debug("validated");
    }
}
