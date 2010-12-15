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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05;

import org.nimbustools.messaging.gt4_0_elastic.generated.v2010_08_31.*;

import java.rmi.RemoteException;

public interface ServiceNetwork {

    // -------------------------------------------------------------------------
    // NETWORK RELATED
    // -------------------------------------------------------------------------

    public AllocateAddressResponseType allocateAddress(
                        AllocateAddressType allocateAddressRequestMsg)
            throws RemoteException;

    public ReleaseAddressResponseType releaseAddress(
                        ReleaseAddressType releaseAddressRequestMsg)
            throws RemoteException;

    public DescribeAddressesResponseType describeAddresses(
                        DescribeAddressesType describeAddressesRequestMsg)
            throws RemoteException;

    public AssociateAddressResponseType associateAddress(
                        AssociateAddressType associateAddressRequestMsg)
            throws RemoteException;

    public DisassociateAddressResponseType disassociateAddress(
                        DisassociateAddressType disassociateAddressRequestMsg)
            throws RemoteException;

    public CreateVpnGatewayResponseType createVpnGateway(
            CreateVpnGatewayType createVpnGatewayRequestMsg)
            throws RemoteException;

    public DeleteVpnGatewayResponseType deleteVpnGateway(
            DeleteVpnGatewayType deleteVpnGatewayRequestMsg)
            throws RemoteException;

    public DescribeVpnGatewaysResponseType describeVpnGateways(
            DescribeVpnGatewaysType describeVpnGatewaysRequestMsg)
            throws RemoteException;

    public CreateVpnConnectionResponseType createVpnConnection(
            CreateVpnConnectionType createVpnConnectionRequestMsg)
            throws RemoteException;

    public DeleteVpnConnectionResponseType deleteVpnConnection(
            DeleteVpnConnectionType deleteVpnConnectionRequestMsg)
            throws RemoteException;

    public DescribeVpnConnectionsResponseType describeVpnConnections(
            DescribeVpnConnectionsType describeVpnConnectionsRequestMsg)
            throws RemoteException;

    public AttachVpnGatewayResponseType attachVpnGateway(
            AttachVpnGatewayType attachVpnGatewayRequestMsg)
            throws RemoteException;

    public DetachVpnGatewayResponseType detachVpnGateway(
            DetachVpnGatewayType detachVpnGatewayRequestMsg)
            throws RemoteException;

    public CreateVpcResponseType createVpc(
            CreateVpcType createVpcRequestMsg)
            throws RemoteException;

    public DeleteVpcResponseType deleteVpc(
            DeleteVpcType deleteVpcRequestMsg)
            throws RemoteException;

    public DescribeVpcsResponseType describeVpcs(
            DescribeVpcsType describeVpcsRequestMsg)
            throws RemoteException;

    public CreateSubnetResponseType createSubnet(
            CreateSubnetType createSubnetRequestMsg)
            throws RemoteException;

    public DeleteSubnetResponseType deleteSubnet(
            DeleteSubnetType deleteSubnetRequestMsg)
            throws RemoteException;

    public DescribeSubnetsResponseType describeSubnets(
            DescribeSubnetsType describeSubnetsRequestMsg)
            throws RemoteException;

    public CreateDhcpOptionsResponseType createDhcpOptions(
            CreateDhcpOptionsType createDhcpOptionsRequestMsg)
            throws RemoteException;

    public DescribeDhcpOptionsResponseType describeDhcpOptions(
            DescribeDhcpOptionsType describeDhcpOptionsRequestMsg)
            throws RemoteException;

    public DeleteDhcpOptionsResponseType deleteDhcpOptions(
            DeleteDhcpOptionsType deleteDhcpOptionsRequestMsg)
            throws RemoteException;

    public AssociateDhcpOptionsResponseType associateDhcpOptions(
            AssociateDhcpOptionsType associateDhcpOptionsRequestMsg)
            throws RemoteException;
}
