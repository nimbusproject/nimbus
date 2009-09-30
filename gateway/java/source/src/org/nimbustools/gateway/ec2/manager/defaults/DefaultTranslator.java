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

package org.nimbustools.gateway.ec2.manager.defaults;

import org.nimbustools.gateway.ec2.manager.Translator;
import org.nimbustools.gateway.ec2.manager.SecurityUtil;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.vm.*;
import org.nimbustools.api._repr.vm._VM;
import org.nimbustools.api._repr.vm._Schedule;
import org.nimbustools.api.defaults.repr.vm.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.xerox.amazonws.ec2.LaunchConfiguration;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.InstanceType;

import java.util.Calendar;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

public class DefaultTranslator implements Translator {

    private static final Log logger =
            LogFactory.getLog(DefaultTranslator.class.getName());

    int smallMemory;

    String publicNetworkName;
    String privateNetworkName;

    public void setSmallMemory(int smallMemory) {
        this.smallMemory = smallMemory;
    }

    public void setPublicNetworkName(String publicNetworkName) {
        this.publicNetworkName = publicNetworkName;
    }

    public void setPrivateNetworkName(String privateNetworkName) {
        this.privateNetworkName = privateNetworkName;
    }

    void validate() throws Exception {
        if (smallMemory < 1) {
            throw new Exception("Invalid: small memory must be at least 1");
        }

        if (publicNetworkName == null) {
            throw new Exception("Invalid: public network name may not be null");
        }

        if (privateNetworkName == null) {
            throw new Exception("Invalid: private network name may not be null");
        }
    }


    public LaunchConfiguration translateCreateRequest(CreateRequest request, Caller caller)
        throws CannotTranslateException {

        if (request == null) {
            throw new IllegalArgumentException("request may not be null");
        }

        final String amiName = request.getName();
        if (amiName == null || amiName.length() == 0) {
            throw new CannotTranslateException("Request is missing image name");
        }

        final ResourceAllocation ra = request.getRequestedRA();
        if (ra == null) {
            throw new CannotTranslateException("Request is missing resource allocation");
        }

        int nodeCount = ra.getNodeNumber();

        LaunchConfiguration lc =
            new LaunchConfiguration(amiName, nodeCount, nodeCount);

        if (request.getMdUserData() != null) {
            final byte[] bytes = request.getMdUserData().getBytes();
            lc.setUserData(bytes);
        }

        if (request.getSshKeyName() != null) {
            String keyName = request.getSshKeyName();
            String userHash = SecurityUtil.getCallerHash(caller);

            keyName = SecurityUtil.prefixKeyName(keyName, userHash);

            lc.setKeyName(keyName);
        }

        return lc;
    }

    public VM[] translateReservationInstances(ReservationDescription desc, Caller caller)
            throws CannotTranslateException {

        if (desc == null) {
            throw new IllegalArgumentException("desc may not be null");
        }

        if (caller == null) {
            throw new IllegalArgumentException("caller may not be null");
        }

        final List<ReservationDescription.Instance> instances =
            desc.getInstances();

        if (instances == null) {
            throw new CannotTranslateException("instances is null ??");
        }

        VM[] vms = new VM[instances.size()];

        for (int i=0; i<instances.size(); i++) {
            vms[i] = translateVM(instances.get(i), caller, i);
        }
        return vms;
    }

    public _VM translateVM(ReservationDescription.Instance instance,
                          Caller caller,
                          int launchIndex) throws CannotTranslateException {

        if (instance == null) {
            throw new IllegalArgumentException("instance may not be null");
        }

        if (caller == null) {
            throw new IllegalArgumentException("caller may not be null");
        }

        _VM vm = new DefaultVM();

        vm.setCreator(caller);
        vm.setID(instance.getInstanceId());
        vm.setLaunchIndex(launchIndex);

        if (instance.getKeyName() != null) {
            final String keyName = instance.getKeyName();
            final String userHash = SecurityUtil.getCallerHash(caller);
            if (SecurityUtil.checkKeyName(keyName, userHash)) {
                vm.setSshKeyName(SecurityUtil.trimKeyName(keyName, userHash));
            }
        }

        updateVM(vm, instance);

        return vm;
    }

    public void updateVM(_VM vm, ReservationDescription.Instance instance)
            throws CannotTranslateException {
        if (instance == null) {
            throw new IllegalArgumentException("instance may not be null");
        }
        if (vm == null) {
            throw new IllegalArgumentException("vm may not be null");
        }
        
        _Schedule sched = null;
        final Calendar launchTime = instance.getLaunchTime();
        if (launchTime != null) {
            sched = new DefaultSchedule();
            sched.setStartTime(launchTime);
        }
        vm.setSchedule(sched);

        vm.setResourceAllocation(translateInstanceType(instance.getInstanceType()));

        if (vm.getVMFiles() == null) {
            DefaultVMFile vmfile = new DefaultVMFile();
            vmfile.setRootFile(true);
            // the EC2 interfaces look at the last component of URI for the image-id
            // so we make up a fake URI (is there a real s3:// URI we could use that
            // would actually be sensible?)
            try {
                URI uri = new URI("ami://amazon/"+instance.getImageId());
                vmfile.setURI(uri);
            } catch (URISyntaxException e) {
                logger.warn("Failed to build AMI id URI (??)");
            }
            vm.setVMFiles(new VMFile[] {vmfile});
        }


        vm.setState(this.translateStateCode(instance.getStateCode()));

        vm.setNics(this.createNics(instance));
    }

    private ResourceAllocation translateInstanceType(InstanceType instanceType) {

        DefaultResourceAllocation ra = new DefaultResourceAllocation();
        ra.setNodeNumber(1); // uhhhhhhh

        // this isn't done

        int memory = smallMemory;
        ra.setMemory(memory);

        return ra;
    }

    public NIC[] createNics(ReservationDescription.Instance instance) {
        if (instance == null) {
            throw new IllegalArgumentException("instance may not be null");
        }

        DefaultNIC publicNic = new DefaultNIC();
        publicNic.setNetworkName(this.publicNetworkName);
        publicNic.setHostname(instance.getDnsName());

        DefaultNIC privateNic = new DefaultNIC();
        privateNic.setNetworkName(this.privateNetworkName);
        privateNic.setHostname(instance.getPrivateDnsName());

        return new NIC[] {publicNic, privateNic};
    }


    private static final int EC2_STATE_PENDING = 0;
    private static final int EC2_STATE_RUNNING = 16;
    private static final int EC2_STATE_SHUTTING_DOWN = 32;
    private static final int EC2_STATE_TERMINATED = 48;

    public State translateStateCode(int stateCode)
        throws CannotTranslateException {

        DefaultState state = new DefaultState();

        switch (stateCode) {
            case EC2_STATE_PENDING:
                state.setState(State.STATE_Propagated);
                break;
            case EC2_STATE_RUNNING:
                state.setState(State.STATE_Running);
                break;
            case EC2_STATE_SHUTTING_DOWN:
                state.setState(State.STATE_Cancelled);
                break;
            case EC2_STATE_TERMINATED:
                state.setState(State.STATE_Cancelled);
                break;
            default:
                throw new CannotTranslateException("Invalid EC2 state code: "+
                    stateCode);
        }
        return state;
    }

}
