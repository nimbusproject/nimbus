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

import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.AllocateAddressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.AllocateAddressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.ReleaseAddressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.ReleaseAddressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeAddressesResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DescribeAddressesType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.AssociateAddressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.AssociateAddressType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DisassociateAddressResponseType;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2008_05_05.DisassociateAddressType;

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
}
