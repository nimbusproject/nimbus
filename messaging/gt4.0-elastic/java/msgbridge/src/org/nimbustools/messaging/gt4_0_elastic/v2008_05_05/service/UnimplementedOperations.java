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

import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.AuthorizeSecurityGroupIngressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.ConfirmProductInstanceResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeKeyPairsResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.ConfirmProductInstanceType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.AllocateAddressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeSecurityGroupsType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeInstancesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.CreateKeyPairType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.ResetImageAttributeResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.TerminateInstancesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeImagesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.ModifyImageAttributeResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeAvailabilityZonesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.RegisterImageResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.CreateSecurityGroupResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeImageAttributeResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DeleteSecurityGroupType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.ModifyImageAttributeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.ResetImageAttributeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeAvailabilityZonesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.RevokeSecurityGroupIngressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.GetConsoleOutputResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.RevokeSecurityGroupIngressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.RebootInstancesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.CreateSecurityGroupType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.CreateKeyPairResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeKeyPairsType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeImageAttributeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeSecurityGroupsResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.ReservationInfoType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.RegisterImageType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeImagesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.RebootInstancesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.AuthorizeSecurityGroupIngressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DeregisterImageType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.RunInstancesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DeleteKeyPairResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeInstancesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DeleteSecurityGroupResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DeregisterImageResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.TerminateInstancesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DeleteKeyPairType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.GetConsoleOutputType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.ReleaseAddressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.AllocateAddressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.AssociateAddressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DisassociateAddressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DisassociateAddressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.AssociateAddressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeAddressesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeAddressesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.ReleaseAddressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.CreateSnapshotResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.CreateSnapshotType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeVolumesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.CreateVolumeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeVolumesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.CreateVolumeResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DeleteSnapshotResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DeleteSnapshotType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.AttachVolumeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeSnapshotsResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeSnapshotsType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DeleteVolumeResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DeleteVolumeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DetachVolumeResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DetachVolumeType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.AttachVolumeResponseType;
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
