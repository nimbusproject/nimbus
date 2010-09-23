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

package org.nimbustools.api.defaults.services.rm;

import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CustomizationRequest;
import org.nimbustools.api.repr.ctx.Context;
import org.nimbustools.api.repr.vm.Kernel;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.vm.RequiredVMM;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.api.repr.vm.VMFile;
import org.nimbustools.api.repr.vm.Schedule;
import org.nimbustools.api.services.rm.BasicLegality;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.MetadataException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.SchedulingException;

/**
 * <p>May be used to perform checks for nulls, negative numbers, etc.</p>
 *
 * <p>RM implementation does not need to use this if it doesn't want to but
 * it relieves developer of tending to those often tedious "that would never
 * happen, right?" sanity checks.</p>
 *
 * @see org.nimbustools.api.defaults.services.rm.DefaultBasicLegality
 */
public class DefaultBasicLegality implements BasicLegality {

    // -------------------------------------------------------------------------
    // implements BasicLegality
    // -------------------------------------------------------------------------

    public void checkCreateRequest(CreateRequest req)

            throws CreationException,
                   MetadataException,
                   SchedulingException,
                   ResourceRequestDeniedException {

        this.checkCreateNulls(req);

        this.checkRA(req.getRequestedRA());

        this.checkNICs(req.getRequestedNics());

        this.checkFiles(req.getVMFiles());

        this.checkCustTasks(req.getCustomizationRequests());

        this.checkSchedule(req.getRequestedSchedule());
    }
    
    
    // -------------------------------------------------------------------------
    // CHECK FOR MISSING OBJECTS
    // -------------------------------------------------------------------------

    protected void checkCreateNulls(CreateRequest req)

            throws CreationException,
                   MetadataException,
                   ResourceRequestDeniedException {

        if (req == null) {
            throw new CreationException("no CreateRequest?");
        }

        final RequiredVMM reqVMM = req.getRequiredVMM();
        if (reqVMM != null && reqVMM.getType() == null) {
            throw new MetadataException(
                    "RequiredVMM encountered with missing type");
        }

        final Kernel kernel = req.getRequestedKernel();
        if (kernel != null && kernel.getKernel() == null) {
            throw new MetadataException(
                    "RequestedKernel encountered with missing location URI");
        }

        final Context context = req.getContext();
        if (context != null) {
            if (context.getBootstrapPath() == null) {
                throw new CreationException(
                    "Context encountered with missing bootstrap path");
            }
            if (context.getBootstrapText() == null) {
                throw new CreationException(
                    "Context encountered with missing bootstrap text");
            }
        }
    }

    // -------------------------------------------------------------------------
    // CHECK FILES
    // -------------------------------------------------------------------------

    protected void checkFiles(VMFile[] vmFiles) throws MetadataException {
        
        if (vmFiles == null || vmFiles.length == 0) {
            throw new MetadataException("no files in request");
        }

        for (int i = 0; i < vmFiles.length; i++) {
            if (vmFiles[i] == null) {
                throw new MetadataException("VM file request array " +
                        "may not contain nulls");
            }
        }
    }
    

    // -------------------------------------------------------------------------
    // CHECK NICS
    // -------------------------------------------------------------------------

    protected void checkNICs(NIC[] nics) throws MetadataException {

        if (nics == null) {
            return; // *** EARLY RETURN ***
        }

        for (int i = 0; i < nics.length; i++) {

            if (nics[i] == null) {
                throw new MetadataException("NIC request array " +
                    "may not contain nulls");
            }

            final String acqMethod = nics[i].getAcquisitionMethod();
            if (acqMethod == null) {
                throw new MetadataException("NIC acquisition method " +
                    "is missing");
            }

            if (!acqMethod.equals(NIC.ACQUISITION_AcceptAndConfigure)
                  && !acqMethod.equals(NIC.ACQUISITION_Advisory)
                  && !acqMethod.equals(NIC.ACQUISITION_AllocateAndConfigure)) {

                throw new MetadataException("NIC acquisition method " +
                    "is unknown");
            }
        }
    }

    // -------------------------------------------------------------------------
    // CHECK CUSTOMIZATION TASKS
    // -------------------------------------------------------------------------

    protected void checkCustTasks(CustomizationRequest[] custTasks)

            throws CreationException {

        if (custTasks == null) {
            return; // *** EARLY RETURN ***
        }

        for (int i = 0; i < custTasks.length; i++) {
            final CustomizationRequest custTask = custTasks[i];
            if (custTask == null) {
                throw new CreationException("CustomizationRequest array " +
                    "may not contain nulls");
            }
            if (custTask.getContent() == null) {
                throw new CreationException("CustomizationRequest " +
                    "encountered with no content");
            }
            if (custTask.getPathOnVM() == null) {
                throw new CreationException("CustomizationRequest " +
                    "encountered with no target path");
            }
        }
    }


    // -------------------------------------------------------------------------
    // CHECK RA
    // -------------------------------------------------------------------------

    protected void checkRA(ResourceAllocation ra)
            throws ResourceRequestDeniedException {

        if (ra == null) {
            throw new ResourceRequestDeniedException("resource allocation is missing");
        }

        if (ra.getNodeNumber() < 1) {
            throw new ResourceRequestDeniedException(
                    "node number may not be less than one");
        }
    }


    // -------------------------------------------------------------------------
    // CHECK SCHEDULE
    // -------------------------------------------------------------------------

    protected void checkSchedule(Schedule sched)
            throws SchedulingException {

        if (sched == null) {
            return; // *** EARLY RETURN ***
        }

        if (sched.getDurationSeconds() < 0) {
            throw new SchedulingException(
                    "requested duration may not be less than zero");
        }
    }

}
