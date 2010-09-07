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

package org.globus.workspace.service.binding.defaults;

import org.globus.workspace.service.binding.BindNetwork;
import org.globus.workspace.service.binding.GlobalPolicies;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.ExceptionDuringBackoutHandlerException;
import org.globus.workspace.persistence.DataConvert;
import org.globus.workspace.network.AssociationAdapter;
import org.globus.workspace.network.AssociationEntry;
import org.globus.workspace.xen.XenUtil;
import org.nimbustools.api.repr.vm.NIC;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.nimbustools.api.services.rm.ManageException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;

public class DefaultBindNetwork implements BindNetwork {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DefaultBindNetwork.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final AssociationAdapter networkAdapter;
    protected final GlobalPolicies globals;
    protected final DataConvert dataConvert;
    

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultBindNetwork(GlobalPolicies globalPolicies,
                              AssociationAdapter networkAdapterImpl,
                              DataConvert dataConvertImpl) {
        
        if (globalPolicies == null) {
            throw new IllegalArgumentException("globalPolicies may not be null");
        }
        this.globals = globalPolicies;

        if (networkAdapterImpl == null) {
            throw new IllegalArgumentException("networkAdapterImpl may not be null");
        }
        this.networkAdapter = networkAdapterImpl;

        if (dataConvertImpl == null) {
            throw new IllegalArgumentException("dataConvertImpl may not be null");
        }
        this.dataConvert = dataConvertImpl;
    }

    
    // -------------------------------------------------------------------------
    // implements BindNetwork
    // -------------------------------------------------------------------------

    public void consume(VirtualMachine[] vms, NIC[] nics)
            throws CreationException, ResourceRequestDeniedException {

        if (vms == null || vms.length == 0) {
            throw new IllegalArgumentException("vms may not be null or missing");
        }

        if (nics == null || nics.length == 0) {
            final String info = "networking specification is not present in " +
                    "'" + vms[0].getName() + "' request, setting default";
            logger.info(info);
            //TODO: make default networking configurable/policy driven

            for (int i = 0; i < vms.length; i++) {
                final VirtualMachine vm = vms[i];
                vm.setNetwork("NONE");
                vm.setAssociationsNeeded(null);
            }

            return; // *** EARLY RETURN ***
        }

        final ArrayList nicnames = new ArrayList(nics.length);
        for (int i = 0; i < nics.length; i++) {
            final NIC nic = nics[i];
            final String name = nic.getName();
            if (nicnames.contains(name)) {
                throw new CreationException("You can not specify multiple " +
                        "NICs with the same name");
            }
            nicnames.add(name);
        }

        final boolean staticIPAllowed = this.globals.isAllowStaticIPs();

        int bailed = -1;
        Throwable failure = null;
        for (int i = 0; i < vms.length; i++) {
            try {
                bindOne(vms[i], nics, staticIPAllowed);
            } catch (Throwable t) {
                bailed = i;
                failure = t;
                break;
            }
        }

        if (failure == null) {
            return; // *** EARLY RETURN ***
        }

        if (bailed < 1) {
            if (failure instanceof ResourceRequestDeniedException) {
                throw (ResourceRequestDeniedException) failure;
            } else if (failure instanceof CreationException) {
                throw (CreationException) failure;
            } else {
                throw new CreationException("Unknown problem while " +
                        "processing networking", failure);
            }
        }

        logger.error("Problem with network validations or reservations, " +
                     "backing out the " + bailed + " already processed.");

        for (int i = 0; i < bailed; i++) {
            try {

                this.backOutIPAllocations(vms[i]);

            } catch (Throwable t) {

                String msg = t.getMessage();
                if (msg == null) {
                    msg = t.getClass().getName();
                }

                if (logger.isDebugEnabled()) {
                    logger.error("Error with one backout: " + msg, t);
                } else {
                    logger.error("Error with one backout: " + msg);
                }

                // continue trying anyhow
            }
        }

        String clientMsg;
        if (bailed == 1) {
            clientMsg = "Problem reserving enough addresses or validating " +
                        "all NICs (did succeed for one VM)";
        } else {
            clientMsg = "Problem reserving enough addresses or validating " +
                        "all NICs (did succeed for " + bailed + " VMs)";
        }

        if (failure.getMessage() != null) {
            clientMsg = clientMsg + ".  Error encountered: " +
                        failure.getMessage();
        }

        throw new ResourceRequestDeniedException(clientMsg);

    }

    public void backOutIPAllocations(VirtualMachine vm)
            throws WorkspaceException {

        if (vm == null) {
            throw new IllegalArgumentException("vm may not be null");
        }

        final int vmid = vm.getID().intValue();

        final NIC[] nics;
        try {
            nics = this.dataConvert.getNICs(vm);
        } catch (CannotTranslateException e) {
            logger.debug(e.getMessage()); // it can not be a problem at all
            return; // *** EARLY RETURN ***
        }

        if (nics == null || nics.length == 0) {
            return; // *** EARLY RETURN ***
        }

        for (int i = 0; i < nics.length; i++) {

            final NIC nic = nics[i];
            if (nic == null) {
                logger.error("null NIC in VirtualMachine");
                continue; // *** SKIP ***
            }

            final String method = nic.getAcquisitionMethod();
            if (method == null) {
                logger.error("null acquisition method in VirtualMachine");
                continue; // *** SKIP ***
            }

            if (!NIC.ACQUISITION_AllocateAndConfigure.equals(method)) {
                continue; // *** SKIP ***
            }

            final String name = nic.getNetworkName();
            final String ip = nic.getIpAddress();
            try {
                this.networkAdapter.retireEntry(name, ip, vmid);
            } catch (ManageException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("problem retiring '" + name
                        + "'->'" + ip + "': ", e);
                } else {
                    logger.error("problem retiring '" + name
                        + "'->'" + ip + "': " + e.getMessage());
                }
            }
        }
        
    }

    public void backOutIPAllocations(VirtualMachine[] vms)
            throws WorkspaceException {

        if (vms == null || vms.length == 0) {
            logger.debug("backOutAllocations: no vms");
            return; // *** EARLY RETURN ***
        }

        for (int i = 0; i < vms.length; i++) {
            final VirtualMachine vm = vms[i];
            if (vm != null) {
                this.backOutIPAllocations(vm);
            }
        }
    }
    

    // -------------------------------------------------------------------------
    // IMPL
    // -------------------------------------------------------------------------
    
    protected void bindOne(VirtualMachine vm,
                           NIC[] nics,
                           boolean staticIPAllowed)
            throws CreationException,
                   ResourceRequestDeniedException,
                   ExceptionDuringBackoutHandlerException {

        if (vm == null) {
            throw new IllegalArgumentException("vm may not be null");
        }

        //TODO: this is workspace_control specific for now. That is bad.

        String assocs = null;

        final StringBuffer net = new StringBuffer(128);

        int bailed = -1;
        Throwable failure = null;
        
        for (int i = 0; i < nics.length; i++) {

            try {
                net.append(this.bindNIC(nics[i],
                                        vm,
                                        staticIPAllowed));

                if (i != nics.length-1) {
                    net.append(XenUtil.WC_GROUP_SEPARATOR);
                }

                if (i == 0) {
                    assocs = getAssoc(nics[i]);
                } else {
                    assocs += "," + getAssoc(nics[i]);
                }

            } catch (Throwable t) {
                bailed = i;
                failure = t;
                break;
            }
            
        }

        if (failure == null) {
            vm.setNetwork(net.toString());
            vm.setAssociationsNeeded(assocs);
            return; // *** EARLY RETURN ***
        }

        // problem:

        final Throwable toThrow;
        if (failure instanceof CreationException) {
            toThrow = failure;
        } else if (failure instanceof ResourceRequestDeniedException) {
            toThrow = failure;
        } else {
            toThrow = new CreationException("", failure);
        }

        if (bailed < 1) {

            // love it
            if (failure instanceof CreationException) {
                throw (CreationException)toThrow;
            } else if (failure instanceof ResourceRequestDeniedException) {
                throw (ResourceRequestDeniedException)toThrow;
            }

        }

        logger.error("Problem with NIC in a multi-NIC workspace, backing " +
                     "out the " + bailed + " already processed NIC(s).");

        vm.setNetwork(net.toString());
        vm.setAssociationsNeeded(assocs);
        try {
            this.backOutIPAllocations(vm);
        } catch (WorkspaceException e) {
            final ExceptionDuringBackoutHandlerException f =
                    new ExceptionDuringBackoutHandlerException();
            f.setOriginalProblem(toThrow);
            f.setBackoutProblem(e);
            throw f;
        }

        // love it
        if (failure instanceof CreationException) {
            throw (CreationException)toThrow;
        } else if (failure instanceof ResourceRequestDeniedException) {
            throw (ResourceRequestDeniedException)toThrow;
        }
    }

    private static String getAssoc(NIC nic) {
        String association = nic.getNetworkName();
        if (association == null) {
            // TODO: default should be configurable/policy driven
            association = "default";
        }
        return association;
    }

    // TODO: move to sane network representation
    private StringBuffer bindNIC(NIC nic,
                                 VirtualMachine vm,
                                 boolean staticIPAllowed)
            throws CreationException,
                   ResourceRequestDeniedException {

        
        final StringBuffer net = new StringBuffer();

        final String nicName = nic.getName();
        net.append(nicName);
        net.append(XenUtil.WC_FIELD_SEPARATOR);

        final String association = getAssoc(nic);
        net.append(association);
        net.append(XenUtil.WC_FIELD_SEPARATOR);

        final String clientProvidedMAC = nic.getMAC();
        if (clientProvidedMAC != null
                && clientProvidedMAC.trim().length() > 0) {
            throw new CreationException("no security policy in place for " +
                    "client MAC specification, this is disabled, resubmit " +
                    "without specific MAC requirement");
        }

        final String method = nic.getAcquisitionMethod();
        if (method == null) {
            throw new CreationException("no network method specification");
        }

        if (method.equals(NIC.ACQUISITION_AcceptAndConfigure)
                || method.equals(NIC.ACQUISITION_Advisory)) {

            // todo: verify IP syntax here to identify a problem earlier
            // (workspace_control will validate)
            // (e.g., xml constraints don't check for >255 )

            // TODO: move to authorization check section
            if (!staticIPAllowed) {
                throw new ResourceRequestDeniedException("request for " +
                        "non-allocate networking method is denied");
            }

            if (nic.getIpAddress() == null
                    || nic.getBroadcast() == null
                    || nic.getNetmask() == null) {
                
                final String err = "acquisition method '" + method + "' " +
                        "requires at least IP, broadcast, and netmask settings";
                throw new CreationException(err);
            }

            final String newMac = this.networkAdapter.newMAC();
            if (newMac == null) {
                net.append("ANY");
            } else {
                net.append(newMac);
            }
            net.append(XenUtil.WC_FIELD_SEPARATOR);

            //only handling bridged
            net.append("Bridged");
            net.append(XenUtil.WC_FIELD_SEPARATOR);

            net.append(method);
            net.append(XenUtil.WC_FIELD_SEPARATOR);

            // broadcast, gateway, and netmask can be null
            net.append(nic.getIpAddress()).
                append(XenUtil.WC_FIELD_SEPARATOR).
                append(nic.getGateway()).
                append(XenUtil.WC_FIELD_SEPARATOR).
                append(nic.getBroadcast()).
                append(XenUtil.WC_FIELD_SEPARATOR).
                append(nic.getNetmask()).
                append(XenUtil.WC_FIELD_SEPARATOR).
                append("null").
                append(XenUtil.WC_FIELD_SEPARATOR).
                append("null").
                append(XenUtil.WC_FIELD_SEPARATOR).
                append("null").
                append(XenUtil.WC_FIELD_SEPARATOR).
                append("null").
                append(XenUtil.WC_FIELD_SEPARATOR).
                append("null").
                append(XenUtil.WC_FIELD_SEPARATOR).
                append("null");

        } else if (method.equals(NIC.ACQUISITION_AllocateAndConfigure)) {

            //todo: once default association is configurable, there
            //  will also be an option to disallow defaults.
            // association being null means this
            //if (association == null) {
            //    throw errorBind("noAllocate");
            //}

            if (nic.getIpAddress() != null ||
                nic.getHostname()  != null ||
                nic.getBroadcast() != null ||
                nic.getGateway()   != null ||
                nic.getNetmask()   != null ||
                nic.getNetwork()   != null) {

                final String err = "no specific NIC network settings should " +
                        "be specified for acquisition method '" + method + "'";
                throw new CreationException(err);
            }

            int vmid = -1; // for logging
            if (vm != null) {
                final Integer integer = vm.getID();
                if (integer != null) {
                    vmid = integer.intValue();
                }
            }

            final Object[] entryAndDns =
                    this.networkAdapter.getNextEntry(association, vmid);

            if (entryAndDns == null || entryAndDns[0] == null) {
                // can't happen here, exception already thrown, but this is here
                // for clarity (and code analysis tools)
                final String err = "network '" + association
                        + "' is not currently available";
                logger.error(err);
                throw new ResourceRequestDeniedException(err);
            }

            final AssociationEntry entry = (AssociationEntry) entryAndDns[0];

            final String assignedMAC = entry.getMac();
            if (assignedMAC == null) {
                net.append("ANY");
            } else {
                net.append(assignedMAC);
            }
            net.append(XenUtil.WC_FIELD_SEPARATOR);

            //only handling bridged
            net.append("Bridged");
            net.append(XenUtil.WC_FIELD_SEPARATOR);

            net.append(method);
            net.append(XenUtil.WC_FIELD_SEPARATOR);

            net.append(entry.getIpAddress()).
                append(XenUtil.WC_FIELD_SEPARATOR).
                append(entry.getGateway()).
                append(XenUtil.WC_FIELD_SEPARATOR).
                append(entry.getBroadcast()).
                append(XenUtil.WC_FIELD_SEPARATOR).
                append(entry.getSubnetMask()).
                append(XenUtil.WC_FIELD_SEPARATOR).
                append(entryAndDns[1]).
                append(XenUtil.WC_FIELD_SEPARATOR).
                append(entry.getHostname()).
                append(XenUtil.WC_FIELD_SEPARATOR);

            // cert paths, old implementation
            net.append("null").
                append(XenUtil.WC_FIELD_SEPARATOR).
                append("null").
                append(XenUtil.WC_FIELD_SEPARATOR).
                append("null").
                append(XenUtil.WC_FIELD_SEPARATOR).
                append("null");

        } else {

            // todo: or just leave it up to the implementation?
            throw new CreationException("network method '" + method + 
                    "' is not supported");
        }

        return net;
    }
}
