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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.service;

import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.AuthorizeSecurityGroupIngressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.ConfirmProductInstanceResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeKeyPairsResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.ConfirmProductInstanceType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.AllocateAddressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeSecurityGroupsType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeInstancesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.CreateKeyPairType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.ResetImageAttributeResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.TerminateInstancesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeImagesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.ModifyImageAttributeResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeAvailabilityZonesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.RegisterImageResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.CreateSecurityGroupResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeImageAttributeResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DeleteSecurityGroupType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.ModifyImageAttributeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.ResetImageAttributeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeAvailabilityZonesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.RevokeSecurityGroupIngressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.GetConsoleOutputResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.RevokeSecurityGroupIngressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.RebootInstancesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.CreateSecurityGroupType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.CreateKeyPairResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeKeyPairsType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeImageAttributeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeSecurityGroupsResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.ReservationInfoType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.RegisterImageType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeImagesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.RebootInstancesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.AuthorizeSecurityGroupIngressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DeregisterImageType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.RunInstancesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DeleteKeyPairResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeInstancesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DeleteSecurityGroupResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DeregisterImageResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.TerminateInstancesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DeleteKeyPairType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.GetConsoleOutputType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.ReleaseAddressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.AllocateAddressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.AssociateAddressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DisassociateAddressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DisassociateAddressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.AssociateAddressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeAddressesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeAddressesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.ReleaseAddressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.CreateSnapshotResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.CreateSnapshotType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeVolumesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.CreateVolumeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeVolumesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.CreateVolumeResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DeleteSnapshotResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DeleteSnapshotType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.AttachVolumeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeSnapshotsResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DescribeSnapshotsType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DeleteVolumeResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DeleteVolumeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DetachVolumeResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.DetachVolumeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.AttachVolumeResponseType;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceAll;

import java.rmi.RemoteException;

public class UnimplementedOperations implements ServiceAll {

    public static final String UNIMPLEMENTED =
            "This remote operation is not implemented yet: ";

    // -------------------------------------------------------------------------
    // RM MANAGER RELATED
    // -------------------------------------------------------------------------

    public ReservationInfoType runInstances(
                        RunInstancesType runInstancesRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "runInstances");
    }

    public TerminateInstancesResponseType terminateInstances(
                        TerminateInstancesType terminateInstancesRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "terminateInstances");
    }

    public RebootInstancesResponseType rebootInstances(
                        RebootInstancesType rebootInstancesRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "rebootInstances");
    }

    public DescribeInstancesResponseType describeInstances(
                        DescribeInstancesType describeInstancesRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeInstances");
    }


    // -------------------------------------------------------------------------
    // IMAGE RELATED
    // -------------------------------------------------------------------------

    public RegisterImageResponseType registerImage(
                        RegisterImageType registerImageRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "registerImage");
    }

    public DeregisterImageResponseType deregisterImage(
                        DeregisterImageType deregisterImageRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "deregisterImage");
    }

    public DescribeImagesResponseType describeImages(
                        DescribeImagesType describeImagesRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeImages");
    }

    public ModifyImageAttributeResponseType modifyImageAttribute(
                        ModifyImageAttributeType modifyImageAttributeRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "modifyImageAttribute");
    }

    public ResetImageAttributeResponseType resetImageAttribute(
                        ResetImageAttributeType resetImageAttributeRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "resetImageAttribute");
    }

    public DescribeImageAttributeResponseType describeImageAttribute(
                        DescribeImageAttributeType describeImageAttributeRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeImageAttribute");
    }

    public ConfirmProductInstanceResponseType confirmProductInstance(
                        ConfirmProductInstanceType confirmProductInstanceRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "confirmProductInstance");
    }


    // -------------------------------------------------------------------------
    // SECURITY RELATED
    // -------------------------------------------------------------------------

    public CreateKeyPairResponseType createKeyPair(
                        CreateKeyPairType createKeyPairRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "createKeyPair");
    }

    public DescribeKeyPairsResponseType describeKeyPairs(
                        DescribeKeyPairsType describeKeyPairsRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeKeyPairs");
    }

    public DeleteKeyPairResponseType deleteKeyPair(
                        DeleteKeyPairType deleteKeyPairRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "deleteKeyPair");
    }

    public CreateSecurityGroupResponseType createSecurityGroup(
                        CreateSecurityGroupType createSecurityGroupRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "createSecurityGroup");
    }

    public DeleteSecurityGroupResponseType deleteSecurityGroup(
                        DeleteSecurityGroupType deleteSecurityGroupRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "deleteSecurityGroup");
    }

    public DescribeSecurityGroupsResponseType describeSecurityGroups(
                        DescribeSecurityGroupsType describeSecurityGroupsRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeSecurityGroups");
    }


    // -------------------------------------------------------------------------
    // NETWORK RELATED
    // -------------------------------------------------------------------------

    public AllocateAddressResponseType allocateAddress(
                        AllocateAddressType allocateAddressRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "allocateAddress");
    }

    public ReleaseAddressResponseType releaseAddress(
                        ReleaseAddressType releaseAddressRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "releaseAddress");
    }

    public DescribeAddressesResponseType describeAddresses(
                        DescribeAddressesType describeAddressesRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeAddresses");
    }

    public AssociateAddressResponseType associateAddress(
                        AssociateAddressType associateAddressRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "associateAddress");
    }

    public DisassociateAddressResponseType disassociateAddress(
                        DisassociateAddressType disassociateAddressRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "disassociateAddress");
    }

    public AuthorizeSecurityGroupIngressResponseType authorizeSecurityGroupIngress(
                        AuthorizeSecurityGroupIngressType authorizeSecurityGroupIngressRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "authorizeSecurityGroupIngress");
    }

    public RevokeSecurityGroupIngressResponseType revokeSecurityGroupIngress(
                        RevokeSecurityGroupIngressType revokeSecurityGroupIngressRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "revokeSecurityGroupIngress");
    }


    // -------------------------------------------------------------------------
    // GENERAL QUERIES
    // -------------------------------------------------------------------------

    public GetConsoleOutputResponseType getConsoleOutput(
                        GetConsoleOutputType getConsoleOutputRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "getConsoleOutput");
    }

    public DescribeAvailabilityZonesResponseType describeAvailabilityZones(
                        DescribeAvailabilityZonesType describeAvailabilityZonesRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeAvailabilityZones");
    }


    // -------------------------------------------------------------------------
    // ELASTIC BLOCK STORE RELATED
    // -------------------------------------------------------------------------

    public CreateVolumeResponseType createVolume(
                CreateVolumeType createVolumeRequestMsgReq)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "createVolume");
    }

    public DeleteVolumeResponseType deleteVolume(
                DeleteVolumeType deleteVolumeRequestMsgReq)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "deleteVolume");
    }

    public DescribeVolumesResponseType describeVolumes(
                DescribeVolumesType describeVolumesRequestMsgReq)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeVolumes");
    }
    
    public AttachVolumeResponseType attachVolume(
                AttachVolumeType attachVolumeRequestMsgReq)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "attachVolume");
    }

    public DetachVolumeResponseType detachVolume(
                DetachVolumeType detachVolumeRequestMsgReq)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "detachVolume");
    }

    public CreateSnapshotResponseType createSnapshot(
                CreateSnapshotType createSnapshotRequestMsgReq)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "createSnapshot");
    }

    public DeleteSnapshotResponseType deleteSnapshot(
                DeleteSnapshotType deleteSnapshotRequestMsgReq)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "deleteSnapshot");
    }

    public DescribeSnapshotsResponseType describeSnapshots(
                DescribeSnapshotsType describeSnapshotsRequestMsgReq)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeSnapshots");
    }
}
