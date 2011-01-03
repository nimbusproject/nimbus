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

package org.globus.workspace.service.binding.authorization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.accounting.AccountingReaderAdapter;
import org.globus.workspace.accounting.ElapsedAndReservedMinutes;
import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.service.binding.Authorize;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachineDeployment;
import org.globus.workspace.service.CurrentVMs;
import org.nimbustools.api.services.rm.AuthorizationException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;

import javax.security.auth.Subject;
import java.util.List;
import java.util.Arrays;

public class DefaultAuthorize implements Authorize {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------
    
    private static final Log logger =
            LogFactory.getLog(DefaultAuthorize.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final GlobalPolicies globals;
    protected final CurrentVMs currentVMs;
    protected final CreationAuthorizationCallout authzCallout;
    protected AccountingReaderAdapter accountingReader;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DefaultAuthorize(CreationAuthorizationCallout calloutImpl,
                            GlobalPolicies globalPolicies,
                            CurrentVMs currentVMs) {
        if (globalPolicies == null) {
            throw new IllegalArgumentException("globalPolicies may not be null");
        }
        this.globals = globalPolicies;
        
        if (currentVMs == null) {
            throw new IllegalArgumentException("currentVMs may not be null");
        }
        this.currentVMs = currentVMs;

        if (calloutImpl == null) {
            throw new IllegalArgumentException("calloutImpl may not be null");
        }
        this.authzCallout = calloutImpl;
    }
    
    
    // -------------------------------------------------------------------------
    // SET
    // -------------------------------------------------------------------------

    public void setAccountingReaderAdapter(AccountingReaderAdapter reader) {
        this.accountingReader = reader;
    }

    
    // -------------------------------------------------------------------------
    // AUTHORIZE
    // -------------------------------------------------------------------------

    // With authz/accounting and scheduling, currently we suffer from a
    // check-then-act problem.  Client could be sending tons of requests
    // to try for a race condition in order to bypass some of the limits.
    // This is ok for now (clients are not anonymous, low priority to
    // defend against this).
    // So future TODO: put a lock around create() on a per-ID basis

    public void authz(VirtualMachine[] bindings,
                      String callerID,
                      Subject peerSubject,
                      double chargeRatio)
            throws ResourceRequestDeniedException,
                   AuthorizationException {

        if (this.authzCallout.isEnabled()) {
            try {

                Long elapsedMins = null;
                Long reservedMins = null;

                if (this.accountingReader != null) {
                    final ElapsedAndReservedMinutes elapRes =
                            this.accountingReader.
                                 totalElapsedAndReservedMinutesTuple(callerID);

                    final long elapsed = elapRes.getElapsed();
                    final long reserved = elapRes.getReserved();

                    if (elapsed < 0 || reserved < 0) {

                        logger.error("Accounting reader returned negative " +
                                     "value? Aborting.");

                        throw new ResourceRequestDeniedException(
                                             "Request denied, internal issue");
                    }

                    elapsedMins = new Long(elapRes.getElapsed());
                    reservedMins = new Long(elapRes.getReserved());
                }

                final int numWorkspaces =
                        this.currentVMs.countIDsByCaller(callerID);

                final boolean permitted =
                        Callout.isPermitted(this.authzCallout,
                                            callerID,
                                            peerSubject,
                                            bindings,
                                            elapsedMins,
                                            reservedMins,
                                            numWorkspaces,
                                            chargeRatio);

                // if a reason to client is desired, throw
                // WorkspaceResourceRequestDeniedException in callout
                if (!permitted) {
                    throw new ResourceRequestDeniedException(
                            "Your request was denied and no reason " +
                                          "was supplied for you to see.");
                }

            } catch (ResourceRequestDeniedException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Problem in authorization callout", e);
                // do not reveal anything specific to the client
                throw new ResourceRequestDeniedException(
                                             "Request denied, internal issue");
            }
        }

        // enforce (global) factory policies
        this.authorize(bindings);
    }
    
    /**
     * Internal authorization and configuration policy enforcement.
     *
     * @param vms mutable, cannot be null
     * @throws AuthorizationException exc
     * @throws ResourceRequestDeniedException exc
     */
    public void authorize(final VirtualMachine[] vms)
            throws AuthorizationException,
                   ResourceRequestDeniedException {

        if (vms == null || vms.length == 0) {
            throw new IllegalArgumentException("no vms");
        }

        final int maxGroupSize = this.globals.getMaximumGroupSize();
        // zero means infinite
        if (maxGroupSize > 0 && vms.length > maxGroupSize) {
            throw new ResourceRequestDeniedException(
                        "request number of VMs (" + vms.length + ")" +
                                " exceeds the maximum permitted (" + maxGroupSize +
                                    " instances per group)");
        }


        final int defaultSecs = this.globals.getDefaultRunningTimeSeconds();
        final int maxSecs = this.globals.getMaximumRunningTimeSeconds();


        // for each because duration could be changed
        for (int i = 0; i < vms.length; i++) {
            authorizeDuration(vms[i], defaultSecs, maxSecs);
        }

        // we know these will all be same across VMs currently
        this.checkArchitecture(vms[0]);
        this.checkVMM(vms[0]);
    }

    protected void checkVMM(VirtualMachine vm)
            throws ResourceRequestDeniedException {

        final String vmm = this.globals.getVmm();
        if (vmm == null) {
            // if policy is null, means 'any'
            return; // *** EARLY RETURN ***
        }

        if (vm.getVmm() != null) {
            if (!vmm.equals(vm.getVmm())) {
                throw new ResourceRequestDeniedException(
                    "incorrect VMM");
            }
        }

        final String[] versions = this.globals.getVmmVersions();
        if (versions != null) {
            final String fromReq = vm.getVmmVersion();
            if (fromReq != null) {
                boolean found = false;
                for (int i = 0; i < versions.length; i++) {
                    if (versions[i].equals(fromReq)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new ResourceRequestDeniedException(
                                                    "unsupported VMM version");
                }
            }
        }

    }

    protected static void authorizeDuration(VirtualMachine vm,
                                            int defaultSecs,
                                            int maxSecs)
                               throws AuthorizationException,
                                      ResourceRequestDeniedException {

        final VirtualMachineDeployment dep = vm.getDeployment();
        if (dep == null) {
            throw new AuthorizationException(
                        "internal error, no deployment, vm #" + vm.getID());
        }

        final int requestedSecs = dep.getMinDuration();
        if (requestedSecs == VirtualMachineDeployment.NOTSET) {
            dep.setMinDuration(defaultSecs);
        } else if (requestedSecs > maxSecs) {
                // client visible message:
                throw new ResourceRequestDeniedException(
                        "request duration (" + requestedSecs + " seconds)" +
                                " exceeds the maximum allowed (" + maxSecs +
                                " seconds)");
        }
    }


    protected void checkArchitecture(VirtualMachine vm)
            throws ResourceRequestDeniedException {

        final String[] allowedCPUArchitectures = this.globals.getCpuArchitectureNames();
        if (allowedCPUArchitectures == null) {
            // if policy is null, means 'any'
            return; // *** EARLY RETURN ***
        }

        final VirtualMachineDeployment dep = vm.getDeployment();
        if (dep == null) {
            throw new ResourceRequestDeniedException(
                            "no CPU architecture description (no dep)");
        }

        final String requestedArch = dep.getCPUArchitecture();
        if (requestedArch == null) {
            throw new ResourceRequestDeniedException(
                            "no CPU architecture description (no dep.arch)");
        }

        List archList = Arrays.asList(allowedCPUArchitectures);

        if (!archList.contains(requestedArch)) {

            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (int i=0; i < allowedCPUArchitectures.length ; i++) {
                if (first) {
                    first = false;
                }
                else {
                    sb.append(", ");
                }
                sb.append(allowedCPUArchitectures[i]);
            }
            final String allArchitectures = sb.toString();

            throw new ResourceRequestDeniedException(
                    "incorrect CPU architecture, only '" + allArchitectures +
                            "' supported, you requested '" + requestedArch + "'");
        }
    }

}
