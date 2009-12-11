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

import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.*;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceAll;

import java.rmi.RemoteException;

public class UnimplementedOperations implements ServiceAll {

    public static final String UNIMPLEMENTED =
            "This remote operation is not implemented yet: ";

    // -------------------------------------------------------------------------
    // RM MANAGER RELATED
    // -------------------------------------------------------------------------

    public RunInstancesResponseType runInstances(
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

    public BundleInstanceResponseType bundleInstance(
            BundleInstanceType bundleInstanceRequestMsg)
        throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "bundleInstance");
    }

    public DescribeBundleTasksResponseType describeBundleTasks(
            DescribeBundleTasksType describeBundleTasksRequestMsg)
        throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeBundleTasks");
    }

    public CancelBundleTaskResponseType cancelBundleTask(
            CancelBundleTaskType cancelBundleTaskRequestMsg)
        throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "cancelBundleTask");
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

    public GetPasswordDataResponseType getPasswordData(
            GetPasswordDataType getPasswordRequestMsg) throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "getPasswordData");
    }

    public DescribeAvailabilityZonesResponseType describeAvailabilityZones(
                        DescribeAvailabilityZonesType describeAvailabilityZonesRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeAvailabilityZones");
    }

    public DescribeRegionsResponseType describeRegions(
                        DescribeRegionsType describeRegionsRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeRegions");
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
    
    public ModifySnapshotAttributeResponseType modifySnapshotAttribute(
                ModifySnapshotAttributeType modifySnapshotAttributeRequestMsgReq)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "modifySnapshotAttribute");
    }
    
    public ResetSnapshotAttributeResponseType resetSnapshotAttribute(
                ResetSnapshotAttributeType resetSnapshotAttributeRequestMsgReq)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "resetSnapshotAttribute");
    }
    
    public DescribeSnapshotAttributeResponseType describeSnapshotAttribute(
                DescribeSnapshotAttributeType describeSnapshotAttributeRequestMsgReq)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeSnapshotAttribute");
    }

    public DescribeReservedInstancesOfferingsResponseType describeReservedInstancesOfferings(
            DescribeReservedInstancesOfferingsType describeReservedInstancesOfferingsRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeReservedInstancesOfferings");
    }

    public PurchaseReservedInstancesOfferingResponseType purchaseReservedInstancesOffering(
            PurchaseReservedInstancesOfferingType purchaseReservedInstancesOfferingRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "purchaseReservedInstancesOffering");
    }

    public DescribeReservedInstancesResponseType describeReservedInstances(
            DescribeReservedInstancesType describeReservedInstancesRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeReservedInstances");
    }

    public MonitorInstancesResponseType monitorInstances(
            MonitorInstancesType monitorInstancesRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "monitorInstances");
    }

    public MonitorInstancesResponseType unmonitorInstances(
            MonitorInstancesType unmonitorInstancesRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "unmonitorInstances");
    }

    public CreateCustomerGatewayResponseType createCustomerGateway(
            CreateCustomerGatewayType createCustomerGatewayRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "createCustomerGateway");
    }

    public DeleteCustomerGatewayResponseType deleteCustomerGateway(
            DeleteCustomerGatewayType deleteCustomerGatewayRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "deleteCustomerGateway");
    }

    public DescribeCustomerGatewaysResponseType describeCustomerGateways(
            DescribeCustomerGatewaysType describeCustomerGatewaysRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeCustomerGateways");
    }

    public CreateVpnGatewayResponseType createVpnGateway(
            CreateVpnGatewayType createVpnGatewayRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "createVpnGateway");
    }

    public DeleteVpnGatewayResponseType deleteVpnGateway(
            DeleteVpnGatewayType deleteVpnGatewayRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "deleteVpnGateway");
    }

    public DescribeVpnGatewaysResponseType describeVpnGateways(
            DescribeVpnGatewaysType describeVpnGatewaysRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeVpnGateways");
    }

    public CreateVpnConnectionResponseType createVpnConnection(
            CreateVpnConnectionType createVpnConnectionRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "createVpnConnection");
    }

    public DeleteVpnConnectionResponseType deleteVpnConnection(
            DeleteVpnConnectionType deleteVpnConnectionRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "deleteVpnConnection");
    }

    public DescribeVpnConnectionsResponseType describeVpnConnections(
            DescribeVpnConnectionsType describeVpnConnectionsRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeVpnConnections");
    }

    public AttachVpnGatewayResponseType attachVpnGateway(
            AttachVpnGatewayType attachVpnGatewayRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "attachVpnGateway");
    }

    public DetachVpnGatewayResponseType detachVpnGateway(
            DetachVpnGatewayType detachVpnGatewayRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "detachVpnGateway");
    }

    public CreateVpcResponseType createVpc(
            CreateVpcType createVpcRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "createVpc");
    }

    public DeleteVpcResponseType deleteVpc(
            DeleteVpcType deleteVpcRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "deleteVpc");
    }

    public DescribeVpcsResponseType describeVpcs(
            DescribeVpcsType describeVpcsRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeVpcs");
    }

    public CreateSubnetResponseType createSubnet(
            CreateSubnetType createSubnetRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "createSubnet");
    }

    public DeleteSubnetResponseType deleteSubnet(
            DeleteSubnetType deleteSubnetRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "deleteSubnet");
    }

    public DescribeSubnetsResponseType describeSubnets(
            DescribeSubnetsType describeSubnetsRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeSubnets");
    }

    public CreateDhcpOptionsResponseType createDhcpOptions(
            CreateDhcpOptionsType createDhcpOptionsRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "createDhcpOptions");
    }

    public DescribeDhcpOptionsResponseType describeDhcpOptions(
            DescribeDhcpOptionsType describeDhcpOptionsRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "describeDhcpOptions");
    }

    public DeleteDhcpOptionsResponseType deleteDhcpOptions(
            DeleteDhcpOptionsType deleteDhcpOptionsRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "deleteDhcpOptions");
    }

    public AssociateDhcpOptionsResponseType associateDhcpOptions(
            AssociateDhcpOptionsType associateDhcpOptionsRequestMsg)
            throws RemoteException {
        throw new RemoteException(UNIMPLEMENTED + "associateDhcpOptions");
    }

}
